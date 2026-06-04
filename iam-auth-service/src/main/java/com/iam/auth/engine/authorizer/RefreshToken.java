package com.iam.auth.engine.authorizer;

import com.iam.auth.config.AuthProperties;
import com.iam.auth.domain.AuthApplication;
import com.iam.auth.domain.AuthRefreshToken;
import com.iam.auth.domain.AuthUser;
import com.iam.auth.dto.pojo.Client;
import com.iam.auth.dto.pojo.UserAppPermission;
import com.iam.auth.dto.pojo.UserPermissionRow;
import com.iam.auth.dto.request.TokenRequest;
import com.iam.auth.dto.response.TokenResponse;
import com.iam.auth.engine.token.IdTokenClaims;
import com.iam.auth.engine.token.TokenForUser;
import com.iam.auth.enums.Constant;
import com.iam.auth.enums.ErrorCode;
import com.iam.auth.exception.TokenException;
import com.iam.auth.repository.jpa.AuthApplicationRepository;
import com.iam.auth.repository.jpa.AuthRepository;
import com.iam.auth.repository.jpa.AuthUserRepository;
import com.iam.auth.service.JwtService;
import com.iam.auth.service.RefreshTokenService;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshToken implements Authorizer {

    private static final String CONFIDENTIAL  = "confidential";
    private static final String PUBLIC        = "public";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_LOCKED = "LOCKED";

    private final AuthRepository authRepository;
    private final AuthUserRepository authUserRepository;
    private final AuthApplicationRepository authApplicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final AuthProperties authProperties;

    @Override
    public String getMethod() {
        return Constant.GRANT_TYPE.REFRESH_TOKEN;
    }

    @Override
    public TokenResponse issuerToken(TokenRequest input) throws JOSEException {
        log.info("Refresh token ........");
        this.validateRequestParams(input);
        Client client = this.validateClient(input);
        AuthRefreshToken tokenData = this.lookupRefreshToken(input.getRefreshToken());
        this.validateTokenBinding(tokenData, client);
        List<String> validScopes = this.validateScope(input, tokenData);
        AuthUser user = this.validateUser(tokenData, client);

        List<AuthApplication> apps = authApplicationRepository.getAppByClientId(client.getId(), AuthApplication.STATUS.ACTIVE);
        if (apps.isEmpty()) {
            throw new TokenException(ErrorCode.INVALID_CLIENT, "Application not found or inactive");
        }
        AuthApplication app = apps.getFirst();
        List<UserPermissionRow> rows = authRepository.getUserPermission(user.getId(), app.getId());

        // Step 10: revoke old refresh token before issuing new tokens
        refreshTokenService.revokeRefreshToken(input.getRefreshToken());

        // Step 11: new access token
        String accessToken = this.generateAccessToken(user, client, validScopes, app, rows);

        // Step 12: new refresh token (rotation)
        String newRefreshToken = this.generateRefreshToken(
                user, client, tokenData.getAppId(), validScopes, tokenData.getUserSessionId());

        // Step 13: new id_token — no nonce at refresh (OIDC Core §12.2)
        String idToken = null;
        if (validScopes.contains("openid")) {
            idToken = this.generateIdToken(user, client, validScopes);
        }

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .idToken(idToken)
                .expiresIn(client.getAccessTokenTTL())
                .scope(String.join(" ", validScopes))
                .tokenType("Bearer")
                .build();
    }

    // ── Step 1 ────────────────────────────────────────────────────────────────

    private void validateRequestParams(TokenRequest request) {
        if (!Constant.GRANT_TYPE.REFRESH_TOKEN.equals(request.getGrantType())) {
            throw new TokenException(ErrorCode.UNSUPPORTED_GRANT_TYPE, "grant_type must be refresh_token");
        }
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new TokenException(ErrorCode.INVALID_REQUEST, "refresh_token is required");
        }
    }

    // ── Step 2 + 3 ───────────────────────────────────────────────────────────

    private Client validateClient(TokenRequest request) {
        if (request.getClientId() == null || request.getClientId().isBlank()) {
            throw new TokenException(ErrorCode.INVALID_REQUEST, "client_id is required");
        }

        Client client = authRepository.getClientByClientId(request.getClientId());
        if (client == null) {
            throw new TokenException(ErrorCode.INVALID_CLIENT, "Client not found");
        }

        if (CONFIDENTIAL.equalsIgnoreCase(client.getClientType())) {
            if (request.getClientSecret() == null || request.getClientSecret().isBlank()) {
                throw new TokenException(ErrorCode.INVALID_CLIENT, "client_secret is required for confidential clients");
            }
            if (!passwordEncoder.matches(request.getClientSecret(), client.getClientSecret())) {
                throw new TokenException(ErrorCode.INVALID_CLIENT, "Invalid client_secret");
            }
        } else if (PUBLIC.equalsIgnoreCase(client.getClientType())) {
            // Public client identifies itself via client_id only — no secret, no code_verifier at refresh step
            if (request.getClientSecret() != null && !request.getClientSecret().isBlank()) {
                throw new TokenException(ErrorCode.INVALID_REQUEST, "Public clients must not send client_secret");
            }
        } else {
            throw new TokenException(ErrorCode.INVALID_CLIENT, "Unknown client_type");
        }

        if (!client.isEnabled()) {
            throw new TokenException(ErrorCode.INVALID_CLIENT, "Client is disabled");
        }

        // Step 3: grant type check
        if (client.getGrantTypes() == null || !client.getGrantTypes().contains(Constant.GRANT_TYPE.REFRESH_TOKEN)) {
            throw new TokenException(ErrorCode.UNAUTHORIZED_CLIENT,
                    "Client is not authorized to use the refresh_token grant type");
        }

        return client;
    }

    // ── Step 4 + 6 ───────────────────────────────────────────────────────────
    // Redis TTL tự xóa key khi expired → null = không tồn tại HOẶC đã hết hạn

    private AuthRefreshToken lookupRefreshToken(String token) {
        AuthRefreshToken data = refreshTokenService.getRefreshToken(token);
        if (data == null) {
            throw new TokenException(ErrorCode.INVALID_GRANT, "Refresh token is invalid or expired");
        }
        return data;
    }

    // ── Step 5 ────────────────────────────────────────────────────────────────

    private void validateTokenBinding(AuthRefreshToken data, Client client) {
        if (!client.getClientId().equals(data.getClientId())) {
            throw new TokenException(ErrorCode.INVALID_GRANT, "Refresh token was not issued to this client");
        }
    }

    // ── Step 7 ────────────────────────────────────────────────────────────────

    private List<String> validateScope(TokenRequest request, AuthRefreshToken data) {
        if (request.getScope() == null || request.getScope().isBlank()) {
            return Arrays.asList(data.getScopes().trim().split(",")); // No new scope requested, inherit from original token
        }
        List<String> requested = Arrays.asList(request.getScope().trim().split("\\s+"));
        List<String> original = Arrays.asList(data.getScopes().trim().split(","));
        if (!original.containsAll(requested)) {
            throw new TokenException(ErrorCode.INVALID_SCOPE,
                    "Requested scope exceeds the scope granted by the resource owner");
        }
        return requested;
    }

    // ── Step 8 + 9 ───────────────────────────────────────────────────────────

    private AuthUser validateUser(AuthRefreshToken data, Client client) {
        AuthUser user = authUserRepository.findUserById(data.getUserId())
                .orElseThrow(() -> new TokenException(ErrorCode.INVALID_GRANT, "User not found"));

        if (STATUS_LOCKED.equals(user.getStatus())) {
            throw new TokenException(ErrorCode.ACCOUNT_LOCKED, "User account is locked");
        }
        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new TokenException(ErrorCode.ACCOUNT_DISABLED, "User account is inactive");
        }

        UserAppPermission permit = authRepository.getUserAppPermit(data.getUserId(), client.getId());
        if (permit == null) {
            throw new TokenException(ErrorCode.INVALID_GRANT, "User is not authorized to access this application");
        }

        return user;
    }

    // ── Step 11 ───────────────────────────────────────────────────────────────

    private String generateAccessToken(AuthUser user, Client client, List<String> scopes,
                                       AuthApplication app, List<UserPermissionRow> rows) throws JOSEException {
        String role = rows.isEmpty() ? null : rows.getFirst().getRole();
        if (role == null) {
            role = authUserRepository.findRoleCodeByUserId(user.getId()).orElse(null);
        }

        String employeeCode = authUserRepository.findEmployeeCodeByUserId(user.getId()).orElse(null);

        List<String> permissions = rows.stream()
                .filter(r -> r.getResourceCode() != null && r.getUserActions() != null)
                .flatMap(r -> {
                    String prefix = app.getServiceCode() + "/" + r.getResourceCode() + ":";
                    return r.getUserActions().stream().map(a -> prefix + a);
                })
                .distinct()
                .toList();

        long now = Instant.now().getEpochSecond();
        TokenForUser claims = TokenForUser.builder()
                .jti(UUID.randomUUID().toString())
                .iss(authProperties.getIssuer())
                .sub(String.valueOf(user.getId()))
                .aud(client.getClientId())
                .iat(now)
                .exp(now + client.getAccessTokenTTL())
                .type("Bearer")
                .username(user.getUsername())
                .employeeCode(employeeCode)
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .mobile(user.getMobile())
                .appId(app.getId())
                .serviceCode(app.getServiceCode())
                .clientId(client.getClientId())
                .scopes(scopes)
                .role(role)
                .permissions(permissions)
                .build();

        return jwtService.sign(claims);
    }

    // ── Step 12 ───────────────────────────────────────────────────────────────

    private String generateRefreshToken(AuthUser user, Client client, Long appId,
                                        List<String> scopes, String userSessionId) {
        return refreshTokenService.createRefreshToken(
                user.getId(), client.getClientId(), appId, scopes, userSessionId, client.getRefreshTokenTTL());
    }

    // ── Step 13 ───────────────────────────────────────────────────────────────

    private String generateIdToken(AuthUser user, Client client, List<String> scopes) throws JOSEException {
        long now = Instant.now().getEpochSecond();
        long ttl = client.getIdTokenTTL() != null && client.getIdTokenTTL() > 0
                ? client.getIdTokenTTL()
                : client.getAccessTokenTTL();

        IdTokenClaims claims = IdTokenClaims.builder()
                .jti(UUID.randomUUID().toString())
                .iss(authProperties.getIssuer())
                .sub(String.valueOf(user.getId()))
                .aud(client.getClientId())
                .iat(now)
                .exp(now + ttl)
                .nonce(null)
                .name(scopes.contains("profile") ? user.getDisplayName() : null)
                .preferredUsername(scopes.contains("profile") ? user.getUsername() : null)
                .email(scopes.contains("email") ? user.getEmail() : null)
                .phoneNumber(scopes.contains("phone") ? user.getMobile() : null)
                .build();

        return jwtService.sign(claims);
    }
}

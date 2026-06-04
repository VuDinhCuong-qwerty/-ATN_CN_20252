package com.iam.auth.engine.authorizer;

import com.iam.auth.config.AuthProperties;
import com.iam.auth.domain.AuthApplication;
import com.iam.auth.domain.AuthUser;
import com.iam.auth.dto.pojo.AuthCode;
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
import com.iam.auth.service.AuthorizationCodeService;
import com.iam.auth.service.JwtService;
import com.iam.auth.service.RefreshTokenService;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationCode implements Authorizer {

    private static final String CONFIDENTIAL = "confidential";
    private static final String PUBLIC = "public";
    private static final Long AUTH_CODE_TTL_SEC = 60L;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_LOCKED = "LOCKED";

    private final AuthRepository authRepository;
    private final AuthUserRepository authUserRepository;
    private final AuthApplicationRepository authApplicationRepository;
    private final AuthorizationCodeService authorizationCodeService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final AuthProperties authProperties;
    private final PasswordEncoder passwordEncoder;
    @Override
    public String getMethod() {
        return Constant.GRANT_TYPE.AUTHORIZATION_CODE;
    }

    @Override
    public TokenResponse issuerToken(TokenRequest input) throws JOSEException {
        log.info("Authorization Code .............");
        this.validateRequestParams(input);
        Client client = this.validateClient(input);
        AuthCode authCode = this.validateAuthorizationCode(input, client);
        List<String> validScopes = this.crossCheckAuthorizeVsToken(input, authCode, client);
        this.validatePKCE(input, authCode, client);
        AuthUser user = this.validateUser(authCode, client);

        List<AuthApplication> apps = authApplicationRepository.getAppByClientId(client.getId(), AuthApplication.STATUS.ACTIVE);
        if (apps.isEmpty()) {
            throw new TokenException(ErrorCode.INVALID_CLIENT, "Application not found or inactive");
        }
        AuthApplication app = apps.getFirst();
        List<UserPermissionRow> rows = authRepository.getUserPermission(user.getId(), app.getId());

        String accessToken = this.generateAccessToken(user, client, validScopes, app, rows);

        String refreshToken = null;
        if (client.getRefreshTokenTTL() != null && client.getRefreshTokenTTL() > 0
                && client.getGrantTypes().contains(Constant.GRANT_TYPE.REFRESH_TOKEN)) {
            refreshToken = this.generateRefreshToken(user, client, app.getId(), validScopes, authCode.getUserSessionId());
        }

        String idToken = null;
        if (validScopes.contains("openid")) {
            idToken = this.generateIdToken(user, client, validScopes, authCode.getNonce());
        }

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .idToken(idToken)
                .expiresIn(client.getAccessTokenTTL())
                .scope(String.join(" ", validScopes))
                .tokenType("Bearer")
                .build();
    }

    private void validateRequestParams(TokenRequest request) {
        if (request.getGrantType() == null || !Constant.GRANT_TYPE.AUTHORIZATION_CODE.equals(request.getGrantType())) {
            throw new TokenException(ErrorCode.UNSUPPORTED_GRANT_TYPE, "Unsupported grant_type");
        }

        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new TokenException(ErrorCode.INVALID_REQUEST, "code is required");
        }
    }

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
            if (request.getClientSecret() != null && !request.getClientSecret().isBlank()) {
                throw new TokenException(ErrorCode.INVALID_REQUEST,
                        "Public clients must not send client_secret");
            }
            if (request.getCodeVerifier() == null || request.getCodeVerifier().isBlank()) {
                throw new TokenException(ErrorCode.INVALID_REQUEST,
                        "code_verifier is required for public clients");
            }
        } else {
            throw new TokenException(ErrorCode.INVALID_CLIENT, "Invalid client_type");
        }

        if (!client.isEnabled()) {
            throw new TokenException(ErrorCode.INVALID_CLIENT, "Client is disabled");
        }

        if (client.getGrantTypes() == null
                || !client.getGrantTypes().contains(Constant.GRANT_TYPE.AUTHORIZATION_CODE)) {
            throw new TokenException(ErrorCode.UNAUTHORIZED_CLIENT, "Client is not authorized to use the authorization_code grant type");
        }

        return client;
    }

    private AuthCode validateAuthorizationCode(TokenRequest input, Client client) {
        AuthCode authCode = authorizationCodeService.getAuthorizationCode(input.getCode());
        if (authCode == null) {
            throw new TokenException(ErrorCode.INVALID_GRANT, "Invalid authorization code");
        }
        if (this.authorizationCodeService.isUsed(input.getCode())) {
            throw new TokenException(ErrorCode.INVALID_GRANT, "Authorization code already used");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(authCode.getIssuerAt().plusSeconds(AUTH_CODE_TTL_SEC))) {
            throw new TokenException(ErrorCode.INVALID_GRANT, "Authorization code has expired");
        }
        if (!authCode.getClientId().equals(client.getClientId())) {
            throw new TokenException(ErrorCode.INVALID_GRANT, "Authorization code was not issued to this client");
        }
        boolean marked = authorizationCodeService.markCodeUsed(input.getCode());
        if (!marked) {
            throw new TokenException(ErrorCode.INVALID_GRANT, "Authorization code already used");
        }
        return authCode;
    }

    private List<String> crossCheckAuthorizeVsToken(TokenRequest request, AuthCode authCode, Client client) {
        String savedUri = authCode.getRedirectUri();
        if (savedUri == null || savedUri.isBlank()) {
            if (request.getRedirectUri() != null && !request.getRedirectUri().isBlank()
                    && !client.getRedirectUris().contains(request.getRedirectUri())) {
                throw new TokenException(ErrorCode.INVALID_GRANT, "redirect_uri invalid");
            }

        } else {
            if (request.getRedirectUri() == null || request.getRedirectUri().isBlank()) {
                throw new TokenException(ErrorCode.INVALID_GRANT, "redirect_uri is required");
            }
            if (!savedUri.equals(request.getRedirectUri())) {
                throw new TokenException(ErrorCode.INVALID_GRANT, "redirect_uri mismatch");
            }
        }

        List<String> validScope = new ArrayList<>();
        if (request.getScope() == null || request.getScope().isBlank()) {
            validScope.addAll(authCode.getScopes());
        } else {
            List<String> requiredScoped = Arrays.asList(request.getScope().trim().split("\\s+"));
            for (String scope : requiredScoped) {
                if (!authCode.getScopes().contains(scope)) {
                    throw new TokenException(ErrorCode.INVALID_SCOPE, "invalid scope");
                }
                validScope.add(scope);
            }
        }
        return validScope;
    }

    // ─── PHASE 5 — Validate PKCE ─────────────────────────────────────────────

    private void validatePKCE(TokenRequest request, AuthCode authCode, Client client) {
        String codeChallenge = authCode.getCodeChallenge();
        if (codeChallenge != null && !codeChallenge.isBlank()) {
            if (request.getCodeVerifier() == null || request.getCodeVerifier().isBlank()) {
                throw new TokenException(ErrorCode.INVALID_GRANT, "code_verifier is required");
            }
            String codeVerifier = request.getCodeVerifier();
            if (codeVerifier.length() < 43 || codeVerifier.length() > 128) {
                throw new TokenException(ErrorCode.INVALID_GRANT, "Invalid code_verifier format");
            }
            if (!codeVerifier.matches("^[A-Za-z0-9\\-._~]+$")) {
                throw new TokenException(ErrorCode.INVALID_GRANT, "Invalid code_verifier format");
            }
            String method = authCode.getCodeChallengeMethod();
            String computed;
            if ("S256".equals(method)) {
                computed = this.computeS256(codeVerifier);
            } else if ("plain".equals(method)) {
                computed = codeVerifier;
            } else {
                throw new TokenException(ErrorCode.INVALID_REQUEST, "Unsupported code_challenge_method: " + method);
            }
            if (!computed.equals(codeChallenge)) {
                throw new TokenException(ErrorCode.INVALID_GRANT, "code_verifier does not match code_challenge");
            }
        } else if (PUBLIC.equals(client.getClientType())) {
            throw new TokenException(ErrorCode.INVALID_REQUEST, "PKCE is required for public client");
        }
    }

    private String computeS256(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new TokenException(ErrorCode.UNKNOWN, "An unexpected error occurred");
        }
    }

    // ─── PHASE 7 — Generate Token ────────────────────────────────────────────

    private String generateAccessToken(AuthUser user, Client client, List<String> scopes,
                                       AuthApplication app, List<UserPermissionRow> rows) throws JOSEException {
        String role = rows.isEmpty() ? null : rows.getFirst().getRole();
        if (role == null) {
            role = authUserRepository.findRoleCodeByUserId(user.getId()).orElse(null);
        }

        List<String> permissions = rows.stream()
                .filter(r -> r.getResourceCode() != null && r.getUserActions() != null)
                .flatMap(r -> {
                    String prefix = app.getServiceCode() + "/" + r.getResourceCode() + ":";
                    return r.getUserActions().stream().map(a -> prefix + a);
                })
                .distinct()
                .toList();

        String employeeCode = authUserRepository.findEmployeeCodeByUserId(user.getId()).orElse(null);

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

    private String generateRefreshToken(AuthUser user, Client client, Long appId,
                                        List<String> scopes, String userSessionId) {
        return refreshTokenService.createRefreshToken(
                user.getId(), client.getClientId(), appId, scopes, userSessionId, client.getRefreshTokenTTL()
        );
    }

    private String generateIdToken(AuthUser user, Client client, List<String> scopes, String nonce) throws JOSEException {
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
                .nonce(nonce)
                .name(scopes.contains("profile") ? user.getDisplayName() : null)
                .preferredUsername(scopes.contains("profile") ? user.getUsername() : null)
                .email(scopes.contains("email") ? user.getEmail() : null)
                .phoneNumber(scopes.contains("phone") ? user.getMobile() : null)
                .build();

        return jwtService.sign(claims);
    }

    // ─── PHASE 6 — Validate User ─────────────────────────────────────────────

    private AuthUser validateUser(AuthCode authCode, Client client) {
        if (authCode.getUserId() == null) {
            throw new TokenException(ErrorCode.INVALID_GRANT, "User not found");
        }
        AuthUser user = authUserRepository.findUserById(authCode.getUserId())
                .orElseThrow(() -> new TokenException(ErrorCode.INVALID_GRANT, "User not found"));

        if (STATUS_LOCKED.equals(user.getStatus())) {
            throw new TokenException(ErrorCode.ACCOUNT_LOCKED, "User account is locked");
        }
        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new TokenException(ErrorCode.ACCOUNT_DISABLED, "User account is inactive");
        }

        UserAppPermission permit = authRepository.getUserAppPermit(authCode.getUserId(), client.getId());
        if (permit == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED, "User is not authorized to access this application");
        }

        return user;
    }

}

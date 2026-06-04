package com.iam.auth.service.impl;

import com.iam.auth.config.AuthProperties;
import com.iam.auth.domain.AuthRefreshToken;
import com.iam.auth.domain.AuthSigningKey;
import com.iam.auth.domain.AuthUserSession;
import com.iam.auth.dto.pojo.Client;
import com.iam.auth.dto.request.AuthorizeRequest;
import com.iam.auth.dto.request.RevokeTokenRequest;
import com.iam.auth.dto.request.TokenIntrospectRequest;
import com.iam.auth.dto.request.TokenRequest;
import com.iam.auth.dto.response.*;
import com.iam.auth.engine.*;
import com.iam.auth.engine.authenticator.Authenticator;
import com.iam.auth.engine.authenticator.AuthenticatorRegistry;
import com.iam.auth.engine.authorizer.AuthorizerRegistry;
import com.iam.auth.enums.ErrorCode;
import com.iam.auth.exception.TokenException;
import com.iam.auth.repository.jpa.AuthRepository;
import com.iam.auth.service.*;
import com.iam.auth.utils.KeyUtility;
import com.iam.auth.utils.Utility;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.web.util.UriComponentsBuilder;

import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl extends BaseService implements OAuth2Service {

    private final AuthProperties authProperties;
    private final SigningKeyService signingKeyService;
    private final SSOService ssoService;
    private final AuthFlowCache authFlowCache;
    private final AuthSessionService authSessionService;
    private final AuthRepository authRepository;
    private final AuthenticatorRegistry authenticatorRegistry;
    private final AuthorizationCodeService authorizationCodeService;
    private final RefreshTokenService refreshTokenService;
    private final AuthorizerRegistry authorizerRegistry;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String ERROR_PAGE = "default/error/error";
    private static final String CONFIDENTIAL = "confidential";
    private static final String PUBLIC = "public";
    private static final String PREFIX_AUTH_BLACKLIST_RERESH_TOKEN = "auth:token:refresh:blacklist:";
    private static final String PREFIX_AUTH_BLACKLIST_ACCESS_TOKEN = "auth:token:access:blacklist:";

    // todo: cần cải tiến logic tăng acr_level cho ssoSession
    @Override
    public AuthorizeResponse authorize(AuthorizeRequest request, HttpServletResponse response, HttpSession session) {

        // validate client_id, redirect_uri
        Client client = this.authRepository.getClientByClientId(request.getClientId());
        AuthorizeResponse authorizeResponse = this.validateAuthorizeRequest(request, client);
        if (authorizeResponse != null)
            return authorizeResponse;
        // validate sso_session
        AuthUserSession ssoSession = this.ssoService.verifyUserSession(request.getSsoSession(), client.getId());

        if (ssoSession != null) {
            // implement logic gen code
            log.info("[{}] hợp lệ", request.getSsoSession());

            return this.genCodeAuthorization(request, client, ssoSession);
        }

        // implement logic redirect
        return this.handleErrorSSOSession(client, request, session);
    }

    private AuthorizeResponse validateAuthorizeRequest(AuthorizeRequest request, Client client) {
        if (client == null) {
            return AuthorizeResponse.builder().status(AuthorizeResponse.STATUS.BAD_REQUEST)
                    .redirectUri(ERROR_PAGE).build();
        }
        if (client.getRedirectUris() == null || client.getRedirectUris().isEmpty()) {
            return AuthorizeResponse.builder().status(AuthorizeResponse.STATUS.BAD_REQUEST)
                    .redirectUri(ERROR_PAGE).build();
        }
        if ((request.getRedirectUri() == null || request.getRedirectUri().isBlank())
                && client.getRedirectUris().size() > 1) {
            return AuthorizeResponse.builder().status(AuthorizeResponse.STATUS.BAD_REQUEST)
                    .redirectUri(ERROR_PAGE).build();
        }
        String redirectUri;
        if (request.getRedirectUri() == null || request.getRedirectUri().isBlank()) {
            redirectUri = client.getRedirectUris().getFirst();
        } else {
            if (!client.getRedirectUris().contains(request.getRedirectUri())) {
                return AuthorizeResponse.builder().status(AuthorizeResponse.STATUS.BAD_REQUEST)
                        .redirectUri(ERROR_PAGE).build();
            }
            redirectUri = request.getRedirectUri();
        }
        if (request.getState() == null || request.getState().isBlank()) {
            return AuthorizeResponse.builder().status(AuthorizeResponse.STATUS.UNAUTHENTICATED)
                    .redirectUri(redirectUri + "?error=invalid_request&error_description=Required+state")
                    .build();
        }
        if (!"code".equals(request.getResponseType())) {
            return AuthorizeResponse.builder().status(AuthorizeResponse.STATUS.UNAUTHENTICATED)
                    .redirectUri(redirectUri
                            + "?error=unsupported_response_type&error_description=response_type+must+be+code&state="
                            + request.getState())
                    .build();
        }
        if (request.getCodeChallenge() != null && !request.getCodeChallenge().isBlank()
                && !"S256".equals(request.getCodeChallengeMethod())
                && !"plain".equals(request.getCodeChallengeMethod())) {
            return AuthorizeResponse.builder().status(AuthorizeResponse.STATUS.UNAUTHENTICATED)
                    .redirectUri(redirectUri + "?error=invalid_request&error_description=unsupported+code_challenge_method&state="
                            + request.getState())
                    .build();
        }
        if (PUBLIC.equals(client.getClientType())
                && (request.getCodeChallenge() == null || request.getCodeChallenge().isBlank())) {
            return AuthorizeResponse.builder().status(AuthorizeResponse.STATUS.UNAUTHENTICATED)
                    .redirectUri(redirectUri + "?error=invalid_request&error_description=PKCE+is+required&state="
                            + request.getState())
                    .build();
        }
        List<String> allowedScopes = client.getAllowedScopes();
        List<String> validScopes = new ArrayList<>();
        if (request.getScopes() == null || request.getScopes().isEmpty()) {
            validScopes.addAll(allowedScopes);
        } else {
            for (String scope : request.getScopes()) {
                if (allowedScopes.contains(scope))
                    validScopes.add(scope);
            }
        }
        if (validScopes.isEmpty()) {
            return AuthorizeResponse.builder().status(AuthorizeResponse.STATUS.UNAUTHENTICATED)
                    .redirectUri(redirectUri
                            + "?error=invalid_scope&error_description=Requested+scope+is+invalid+or+not+allowed&state="
                            + request.getState())
                    .build();
        }
        request.setScopes(validScopes);
        return null;
    }

    private AuthorizeResponse genCodeAuthorization(AuthorizeRequest request, Client client, AuthUserSession session) {

        String code = this.authorizationCodeService.createAuthorizationCode(request, client, session);
        String redirectUri = request.getRedirectUri() +
                "?code=" + code +
                "&state=" + request.getState();
        return AuthorizeResponse.builder()
                .status(AuthorizeResponse.STATUS.OK)
                .redirectUri(redirectUri)
                .build();
    }

    private AuthorizeResponse handleErrorSSOSession(Client client, AuthorizeRequest request, HttpSession session) {
        AuthFlow flow = authFlowCache.getByAppId(client.getAppId());
        FlowNode defaultFirstNode = flow.getDefaultFirstNode();

        String sessionId = Utility.generateSessionID();
        LocalDateTime now = LocalDateTime.now();

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(authProperties.getEndpoints().getAuthorize())
                .queryParam("client_id", request.getClientId())
                .queryParam("redirect_uri", request.getRedirectUri())
                .queryParam("response_type", request.getResponseType())
                .queryParam("state", request.getState());
        if (request.getScopes() != null && !request.getScopes().isEmpty()) {
            uriBuilder.queryParam("scope", String.join(" ", request.getScopes()));
        }
        if (request.getCodeChallenge() != null) {
            uriBuilder.queryParam("code_challenge", request.getCodeChallenge());
        }
        if (request.getCodeChallengeMethod() != null) {
            uriBuilder.queryParam("code_challenge_method", request.getCodeChallengeMethod());
        }
        String orgUri = uriBuilder.build().toUriString();

        AuthSession authSession = AuthSession.builder()
                .orgRequestUri(orgUri)
                .sessionId(sessionId).clientId(client.getId())
                .flowId(flow.getFlowId())
                .arcLevel(flow.getDefaultFirstNode().getArcLevel())
                .appId(flow.getDefaultFirstNode().getAppId())
                .currentNodeId(defaultFirstNode.getExecutionId())
                .authorizeRequest(request)
                .nodeStatus(new HashMap<>())
                .preparedAt(now)
                .expiredAt(now.plusMinutes(5))
                .build();

        authSessionService.save(authSession);
        PrepareResult prepareResult = this.prepareNode(defaultFirstNode, authSession);

        Map<String, Object> uiContext = new HashMap<>();
        uiContext.put("authSessionId", sessionId);
        uiContext.put("clientId", client.getId());
        session.setAttribute("auth:ui:context", uiContext);

        String actionType = defaultFirstNode.getMethod().toLowerCase();
        log.info("SSO session invalid — initiating new auth flow: clientId={}, sessionId={}, actionType={}",
                client.getId(), sessionId, actionType);

        return AuthorizeResponse.builder()
                .status(AuthorizeResponse.STATUS.UNAUTHENTICATED)
                .redirectUri(prepareResult.getHint())
                .build();
    }

    private PrepareResult prepareNode(FlowNode node, AuthSession session) {
        Authenticator authenticator = authenticatorRegistry.get(node.getMethod());
        return authenticator.prepare(session);
    }

    @Override
    public TokenResponse issueToken(TokenRequest request) throws JOSEException {
        log.info("TokenRequest: {}", Utility.toJson(request));
        return this.authorizerRegistry.issuerToken(request);
    }

    @Override
    public TokenResponse refreshToken(String refreshToken, String clientId, String clientSecret) {
        return null;
    }

    @Override
    @Transactional
    public void revokeToken(RevokeTokenRequest request) {
        Client client = this.authRepository.getClientByClientId(request.getClientId());
        if (client == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED, "invalid_client", "Client authentication failed");
        }
        if (CONFIDENTIAL.equals(client.getClientType())) {
            if (!passwordEncoder.matches(request.getClientSecret(), client.getClientSecret())) {
                throw new TokenException(ErrorCode.UNAUTHORIZED, "invalid_client", "Client authentication failed");
            }
        }
        if ("refresh_token".equalsIgnoreCase(request.getTokenTypeHint())) {
            AuthRefreshToken refreshToken = this.refreshTokenService.getRefreshToken(request.getToken());
            if (refreshToken != null && refreshToken.getClientId().equals(client.getClientId())) {
                if (refreshToken.getExpiresAt() < System.currentTimeMillis()
                        || !"ACTIVE".equals(refreshToken.getStatus())) {
                    log.info("Refresh token already expired: {}", request.getToken());
                } else {
                    this.refreshTokenService.revokeRefreshToken(request.getToken());
                    String blacklistKey = PREFIX_AUTH_BLACKLIST_RERESH_TOKEN + refreshToken.getUserId() + ":"
                            + refreshToken.getClientId();
                    redisTemplate.opsForValue().set(blacklistKey, "1", client.getAccessTokenTTL(), TimeUnit.SECONDS);
                    log.info("Revoked refresh token: {}", request.getToken());
                }
            }
            return;
        } else if ("access_token".equalsIgnoreCase(request.getTokenTypeHint())) {
            Map<String, Object> claims = this.jwtService.verify(request.getToken());
            if (claims == null) {
                log.warn("Access token invalid.");
                return;
            }
            String clientId = (String) claims.get("aud");
            if (clientId != null && clientId.equals(client.getClientId())) {
                String blacklistKey = PREFIX_AUTH_BLACKLIST_ACCESS_TOKEN + claims.get("jti");
                long ttlSeconds = ((Number) claims.get("exp")).longValue() - (System.currentTimeMillis() / 1000);
                if (ttlSeconds > 0) {
                    redisTemplate.opsForValue().set(blacklistKey, "1", ttlSeconds, TimeUnit.SECONDS);
                    log.info("Blacklisted access token jti={} for {} seconds", claims.get("jti"), ttlSeconds);
                } else {
                    log.info("Access token already expired: jti={}", claims.get("jti"));
                }
                log.info("Revoked access token: {}", request.getToken());
            } else {
                log.warn("Access token client_id mismatch: jti={}", claims.get("jti"));
                return;
            }
        } else {
            // implement logic revoke both
            AuthRefreshToken refreshToken = this.refreshTokenService.getRefreshToken(request.getToken());
            if (refreshToken != null) {
                if (!refreshToken.getClientId().equals(client.getClientId())) {
                    log.warn("Refresh token client_id mismatch: {}", request.getToken());
                    return;
                }
                if (refreshToken.getExpiresAt() < System.currentTimeMillis()) {
                    log.info("Refresh token already expired: {}", request.getToken());
                } else {
                    this.refreshTokenService.revokeRefreshToken(request.getToken());
                    String blacklistKey = PREFIX_AUTH_BLACKLIST_RERESH_TOKEN + refreshToken.getUserId() + ":"
                            + refreshToken.getClientId();
                    redisTemplate.opsForValue().set(blacklistKey, "1", client.getAccessTokenTTL(), TimeUnit.SECONDS);
                    log.info("Revoked refresh token: {}", request.getToken());
                }

            } else {
                Map<String, Object> claims = this.jwtService.verify(request.getToken());
                if (claims != null) {
                    String clientId = (String) claims.get("aud");
                    if (clientId != null && clientId.equals(client.getClientId())) {
                        String blacklistKey = PREFIX_AUTH_BLACKLIST_ACCESS_TOKEN + claims.get("jti");
                        long ttlSeconds = ((Number) claims.get("exp")).longValue()
                                - (System.currentTimeMillis() / 1000);
                        if (ttlSeconds > 0) {
                            redisTemplate.opsForValue().set(blacklistKey, "1", ttlSeconds, TimeUnit.SECONDS);
                            log.info("Blacklisted access token jti={} for {} seconds", claims.get("jti"), ttlSeconds);
                        } else {
                            log.info("Access token already expired: jti={}", claims.get("jti"));
                        }
                        log.info("Revoked access token: {}", request.getToken());
                    } else {
                        log.warn("Access token client_id mismatch: jti={}", claims.get("jti"));
                    }
                } else {
                    log.warn("Token not found as refresh or access token.");
                }
            }
            return;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public TokenIntrospectResponse introspect(TokenIntrospectRequest request) {
        Client client = this.authRepository.getClientByClientId(request.getClientId());
        if (client == null || PUBLIC.equals(client.getClientType()) || (CONFIDENTIAL.equals(client.getClientType())
                && !passwordEncoder.matches(request.getClientSecret(), client.getClientSecret()))) {
            throw new TokenException(ErrorCode.UNAUTHORIZED, "invalid_client", "Client authentication failed");
        }
        Map<String, Object> claims = this.jwtService.verify(request.getToken());
        if (claims == null) {
            // fallback: try as opaque refresh token
            AuthRefreshToken rt = refreshTokenService.getRefreshToken(request.getToken());
            if (rt == null || !rt.getClientId().equals(client.getClientId())) {
                return TokenIntrospectResponse.builder().active(false).build();
            }
            return TokenIntrospectResponse.builder()
                    .active(true)
                    .sub(String.valueOf(rt.getUserId()))
                    .clientId(rt.getClientId())
                    .scope(rt.getScopes().replace(",", " "))
                    .exp(rt.getExpiresAt() / 1000)
                    .tokenType("refresh_token")
                    .build();
        }
        if (claims.get("jti") != null) {
            String blacklistKey = PREFIX_AUTH_BLACKLIST_ACCESS_TOKEN + claims.get("jti");
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                log.info("Access token is blacklisted: jti={}", claims.get("jti"));
                return TokenIntrospectResponse.builder().active(false).build();
            }
        }
        if (claims.get("sub") != null && claims.get("aud") != null) {
            String blacklistKey = PREFIX_AUTH_BLACKLIST_RERESH_TOKEN + claims.get("sub") + ":"
                    + claims.get("aud");
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                log.info("Refresh token blacklist hit for sub={}, aud={}", claims.get("sub"), claims.get("aud"));
                return TokenIntrospectResponse.builder().active(false).build();
            }
        }
        TokenIntrospectResponse response = new TokenIntrospectResponse();
        response.setActive(true);
        // OAuth2 Standard claims
        response.setClientId((String) claims.get("aud"));
        response.setSub((String) claims.get("sub"));
        response.setScope((String) claims.get("scope"));
        response.setExp(((Number) claims.get("exp")).longValue());
        response.setIat(((Number) claims.get("iat")).longValue());
        response.setJti((String) claims.get("jti"));
        response.setTokenType((String) claims.get("type"));

        // User info claims
        if (claims.get("username") != null) {
            response.setUsername((String) claims.get("username"));
        }
        if (claims.get("email") != null) {
            response.setEmail((String) claims.get("email"));
        }
        if (claims.get("displayName") != null) {
            response.setDisplayName((String) claims.get("displayName"));
        }
        if (claims.get("mobile") != null) {
            response.setMobile((String) claims.get("mobile"));
        }

        // Oauth context
        if (claims.get("client_id") != null) {
            response.setClientId((String) claims.get("client_id"));
        }
        if (claims.get("scopes") != null) {
            response.setScope(String.join(" ", (List<String>) claims.get("scopes")));
        }

        // Authorization claims
        if (claims.get("role") != null) {
            response.setRole((String) claims.get("role"));
        }
        if (claims.get("permissions") != null) {
            response.setPermissions((List<String>) claims.get("permissions"));
        }

        // App context claims
        if (claims.get("appId") != null) {
            response.setAppId(((Number) claims.get("appId")).longValue());
        }
        if (claims.get("serviceCode") != null) {
            response.setServiceCode((String) claims.get("serviceCode"));
        }
        return response;
    }

    @Override
    public GetUserInfo getUserInfo(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new TokenException(ErrorCode.INVALID_REQUEST, "invalid_request",
                    "Missing or invalid Authorization header");
        }
        String token = bearerToken.substring("Bearer ".length());
        if (token.isBlank()) {
            throw new TokenException(ErrorCode.INVALID_REQUEST, "invalid_request", "Missing or invalid token");
        }
        Map<String, Object> claims = this.jwtService.verify(token);
        if (claims == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED, "invalid_token", "Token is invalid or expired");
        }
        if ("SERVICE".equals(claims.get("type"))) {
            throw new TokenException(ErrorCode.UNAUTHORIZED, "invalid_token", "Service tokens cannot access UserInfo endpoint");
        }
        if (claims.get("jti") != null) {
            String blacklistKey = PREFIX_AUTH_BLACKLIST_ACCESS_TOKEN + claims.get("jti");
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                log.info("Access token is blacklisted: jti={}", claims.get("jti"));
                throw new TokenException(ErrorCode.UNAUTHORIZED, "invalid_token", "Token has been revoked");
            }
        }

        String sub = (String) claims.get("sub");
        if (sub == null || sub.isBlank()) {
            throw new TokenException(ErrorCode.UNAUTHORIZED, "invalid_request", "Missing sub in token");
        }
        Long userId;
        try {
            userId = Long.parseLong(sub);
        } catch (NumberFormatException e) {
            throw new TokenException(ErrorCode.UNAUTHORIZED, "invalid_token", "Invalid sub claim");
        }
        GetUserInfo userInfo = this.authRepository.getUserInfoByUserId(userId);
        if (userInfo == null) {
            throw new TokenException(ErrorCode.UNAUTHORIZED, "invalid_token", "User not found");
        }
        if (!GetUserInfo.STATUS.ACTIVE.equals(userInfo.getStatus())) {
            throw new TokenException(ErrorCode.UNAUTHORIZED, "invalid_token", "User is not active");
        }
        userInfo.setSub(sub);

        @SuppressWarnings("unchecked")
        List<String> permissions = claims.get("permissions") instanceof List<?> list
                ? (List<String>) list : List.of();
        userInfo.setPermissions(permissions);

        return userInfo;
    }

    @Override
    public JwksResponse getPublicJwks() {
        log.info("Building JWKS response");

        List<AuthSigningKey> keys = signingKeyService.findAllVerifiableKeys();

        if (keys == null || keys.isEmpty()) {
            log.warn("No verifiable keys found — generating initial key");
            AuthSigningKey newKey = signingKeyService.generateAndSaveKey();
            keys = List.of(newKey);
        }

        List<JwksResponse.JwkKey> jwkKeys = keys.stream()
                .map(key -> {
                    try {
                        ECPublicKey ecPublicKey = KeyUtility.parseEcPublicKey(key.getPublicKey());
                        ECPoint point = ecPublicKey.getW();
                        return JwksResponse.JwkKey.builder()
                                .kty("EC")
                                .kid(key.getKid())
                                .use("sig")
                                .alg("ES256")
                                .crv("P-256")
                                .x(KeyUtility.toBase64Url(point.getAffineX()))
                                .y(KeyUtility.toBase64Url(point.getAffineY()))
                                .build();
                    } catch (Exception e) {
                        log.error("Failed to parse public key for kid={}: {}", key.getKid(), e.getMessage());
                        return null;
                    }
                })
                .filter(jwkKey -> jwkKey != null)
                .toList();

        log.info("JWKS response built with {} key(s)", jwkKeys.size());
        return JwksResponse.builder().keys(jwkKeys).build();
    }

    @Override
    public OpenIdConfigurationResponse getOpenIdConfiguration() {
        log.info("Building OpenID Connect discovery document");

        String baseUrl = authProperties.getBaseUrl();
        AuthProperties.Endpoints ep = authProperties.getEndpoints();

        return OpenIdConfigurationResponse.builder()
                .issuer(authProperties.getIssuer())
                .authorizationEndpoint(baseUrl + ep.getAuthorize())
                .tokenEndpoint(baseUrl + ep.getToken())
                .jwksUri(baseUrl + ep.getJwks())
                .userinfoEndpoint(baseUrl + ep.getUserinfo())
                .endSessionEndpoint(baseUrl + ep.getLogout())
                .introspectionEndpoint(baseUrl + ep.getIntrospect())
                .revocationEndpoint(baseUrl + ep.getRevoke())
                .responseTypesSupported(List.of("code"))
                .grantTypesSupported(List.of("authorization_code", "refresh_token", "client_credentials"))
                .subjectTypesSupported(List.of("public"))
                .idTokenSigningAlgValuesSupported(List.of("ES256"))
                .tokenEndpointAuthMethodsSupported(List.of("client_secret_basic", "client_secret_post", "none"))
                .scopesSupported(List.of("openid", "profile", "email"))
                .claimsSupported(List.of("sub", "iss", "aud", "exp", "iat", "name", "email"))
                .codeChallengeMethodsSupported(List.of("S256", "plain"))
                .build();
    }
}

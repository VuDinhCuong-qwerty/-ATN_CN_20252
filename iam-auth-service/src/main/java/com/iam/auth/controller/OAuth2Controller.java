package com.iam.auth.controller;

import com.iam.auth.dto.request.RevokeTokenRequest;
import com.iam.auth.dto.request.TokenIntrospectRequest;
import com.iam.auth.dto.request.TokenRequest;
import com.iam.auth.dto.response.JwksResponse;
import com.iam.auth.dto.response.OpenIdConfigurationResponse;
import com.iam.auth.dto.response.TokenIntrospectResponse;
import com.iam.auth.dto.response.TokenResponse;
import com.iam.auth.enums.Constant;
import com.iam.auth.enums.ErrorCode;
import com.iam.auth.exception.TokenException;
import com.iam.auth.service.OAuth2Service;
import com.nimbusds.jose.JOSEException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ms-internal-iam/auth")
public class OAuth2Controller {

    private final OAuth2Service oAuth2Service;

    public OAuth2Controller(OAuth2Service tokenService) {
        this.oAuth2Service = tokenService;
    }

    // ─── Token ────────────────────────────────────────────────────────────────

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<TokenResponse> token(@RequestParam Map<String, String> params) throws JOSEException {
        TokenRequest tokenRequest = TokenRequest.builder()
                .grantType(params.getOrDefault("grant_type", null))
                .code(params.getOrDefault("code", null))
                .clientId(params.getOrDefault("client_id", null))
                .clientSecret(params.getOrDefault("client_secret", null))
                .redirectUri(params.getOrDefault("redirect_uri", null))
                .codeVerifier(params.getOrDefault("code_verifier", null))
                .refreshToken(params.getOrDefault("refresh_token", null))
                .scope(params.getOrDefault("scope", null))
                .clientAssertionType(params.getOrDefault("client_assertion_type", null))
                .clientAssertion(params.getOrDefault("client_assertion", null))
                .build();
        TokenResponse tokenResponse = oAuth2Service.issueToken(tokenRequest);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping(value = "/token/refresh", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<TokenResponse> refresh(
            @RequestParam("refresh_token")                         String refreshToken,
            @RequestParam("client_id")                             String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret) throws JOSEException {

        TokenRequest tokenRequest = TokenRequest.builder()
                .grantType(Constant.GRANT_TYPE.REFRESH_TOKEN)
                .refreshToken(refreshToken)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
        return ResponseEntity.ok(oAuth2Service.issueToken(tokenRequest));
    }

    @PostMapping(value = "/token/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> revoke(
            @RequestHeader(value = "Authorization", required = false) String bearerToken,
            @RequestParam(value = "token", required = true) String token,
            @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret) {

        RevokeTokenRequest revokeTokenRequest = new RevokeTokenRequest(token, tokenTypeHint, clientId, clientSecret, bearerToken);
        oAuth2Service.revokeToken(revokeTokenRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/token/introspect")
    public ResponseEntity<TokenIntrospectResponse> introspect(
            @RequestHeader(value = "Authorization", required = false) String bearerToken,
            @RequestParam("token") String token,
            @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret
    ) {
        TokenIntrospectRequest introspectRequest = new TokenIntrospectRequest(token, tokenTypeHint, clientId, clientSecret, bearerToken);
        return ResponseEntity.ok(oAuth2Service.introspect(introspectRequest));
    }

    // ─── OIDC ─────────────────────────────────────────────────────────────────

    @GetMapping("/userinfo")
    public ResponseEntity<?> userinfo(
            @RequestHeader(value = "Authorization", required = false) String bearerToken) {
        try {
            return ResponseEntity.ok(oAuth2Service.getUserInfo(bearerToken));
        } catch (TokenException e) {
            int status = e.getErrorCode() == ErrorCode.INVALID_REQUEST ? 400 : 401;
            String wwwAuth = "Bearer error=\"" + e.getError()
                    + "\", error_description=\"" + e.getErrorDesc() + "\"";
            return ResponseEntity.status(status)
                    .header("WWW-Authenticate", wwwAuth)
                    .body(Map.of("error", e.getError(), "error_description", e.getErrorDesc()));
        }
    }

    @GetMapping(value = "/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JwksResponse> jwks() {
        return ResponseEntity.ok(oAuth2Service.getPublicJwks());
    }

    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OpenIdConfigurationResponse> openidConfiguration() {
        return ResponseEntity.ok(oAuth2Service.getOpenIdConfiguration());
    }
}

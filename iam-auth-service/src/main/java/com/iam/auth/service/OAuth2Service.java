package com.iam.auth.service;

import com.iam.auth.dto.request.AuthorizeRequest;
import com.iam.auth.dto.request.RevokeTokenRequest;
import com.iam.auth.dto.request.TokenIntrospectRequest;
import com.iam.auth.dto.request.TokenRequest;
import com.iam.auth.dto.response.*;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public interface OAuth2Service {

    AuthorizeResponse authorize(AuthorizeRequest request, HttpServletResponse response, HttpSession session);

    TokenResponse issueToken(TokenRequest request) throws JOSEException;

    TokenResponse refreshToken(String refreshToken, String clientId, String clientSecret);

    void revokeToken(RevokeTokenRequest request);

    TokenIntrospectResponse introspect(TokenIntrospectRequest request);

    GetUserInfo getUserInfo(String bearerToken);

    JwksResponse getPublicJwks();

    OpenIdConfigurationResponse getOpenIdConfiguration();
}

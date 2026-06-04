package com.iam.auth.engine.authorizer;

import com.iam.auth.config.AuthProperties;
import com.iam.auth.dto.pojo.Client;
import com.iam.auth.dto.request.TokenRequest;
import com.iam.auth.dto.response.TokenResponse;
import com.iam.auth.engine.token.TokenForService;
import com.iam.auth.enums.Constant;
import com.iam.auth.enums.ErrorCode;
import com.iam.auth.exception.TokenException;
import com.iam.auth.repository.jpa.AuthRepository;
import com.iam.auth.service.JwtService;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClientCredentials implements Authorizer{

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties authProperties;
    private final String CONFIDENTIAL = "confidential";

    @Override
    public String getMethod() {
        return Constant.GRANT_TYPE.CLIENT_CREDENTIALS;
    }

    @Override
    public TokenResponse issuerToken(TokenRequest input) throws JOSEException {
        if (input.getClientId() == null || input.getClientId().isBlank()) {
            throw new TokenException(ErrorCode.INVALID_REQUEST);
        }
        Client client = this.authRepository.getClientByClientId(input.getClientId());
        if (client == null) {
            throw new TokenException(ErrorCode.INVALID_CLIENT, "Client not found");
        }
        if (!CONFIDENTIAL.equals(client.getClientType())) {
            throw new TokenException(ErrorCode.UNAUTHORIZED_CLIENT, "Only confidential clients allowed");
        }
        if (!client.isEnabled()) {
            throw new TokenException(ErrorCode.INVALID_CLIENT, "Client is disabled");
        }
        if (input.getClientSecret() == null || input.getClientSecret().isBlank()
                || !passwordEncoder.matches(input.getClientSecret(), client.getClientSecret())) {
            throw new TokenException(ErrorCode.INVALID_CLIENT, "Invalid client secret");
        }

        if (!client.getGrantTypes().contains(input.getGrantType())) {
            throw new TokenException(ErrorCode.UNAUTHORIZED_CLIENT, "Grant type not allowed");
        }
        // validate scopes
        List<String> validScopes = this.validateScopes(client, input);
        String accessToken = this.generateToken(client, validScopes);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .expiresIn(client.getAccessTokenTTL())
                .scope(String.join(" ", validScopes))
                .tokenType("Bearer")
                .build();
    }

    private List<String> validateScopes(Client client, TokenRequest input) {
        List<String> allowedScopes = client.getAllowedScopes();

        String scopeInput = input.getScope();
        if (scopeInput == null || scopeInput.isBlank()) {
            return allowedScopes;
        }

        List<String> requiredScopes = Arrays.stream(scopeInput.trim().split("\\s+"))
                .filter(s -> !s.isEmpty()).toList();

        List<String> validScopes = requiredScopes.stream()
                .filter(allowedScopes::contains).toList();

        if (validScopes.isEmpty()) {
            throw new TokenException(ErrorCode.INVALID_SCOPE);
        }
        return validScopes;
    }

    private String generateToken(Client client, List<String> scopes) throws JOSEException {
        long now = Instant.now().getEpochSecond();
        TokenForService claims = TokenForService.builder()
                .jti(UUID.randomUUID().toString())
                .iss(authProperties.getIssuer())
                .sub(client.getClientId())
                .iat(now)
                .exp(now + client.getAccessTokenTTL())
                .type("SERVICE")
                .clientId(client.getClientId())
                .clientName(client.getName())
                .scopes(scopes)
                .build();

        return jwtService.sign(claims);
    }
}

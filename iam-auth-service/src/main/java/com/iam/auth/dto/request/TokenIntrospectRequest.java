package com.iam.auth.dto.request;

import com.iam.auth.enums.ErrorCode;
import com.iam.auth.exception.TokenException;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TokenIntrospectRequest {
    private String token;
    private String tokenTypeHint;
    private String clientId;
    private String clientSecret;

    public TokenIntrospectRequest(String token, String tokenTypeHint, String clientId, String clientSecret, String authorizationHeader) {
        this.token = token;
        this.tokenTypeHint = tokenTypeHint;

        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            this.extractClientCredentialsFromAuthorizationHeader(authorizationHeader);
        } else {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }
    }

    private void extractClientCredentialsFromAuthorizationHeader(String authorizationHeader) {
        try {
            String base64Credentials = authorizationHeader.substring("Basic ".length());
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            String[] parts = credentials.split(":", 2);
            if (parts.length != 2) {
                throw new TokenException(ErrorCode.VALIDATION_FAILED, "invalid_request", "Invalid Authorization header format");
            }
            String decodedClientId = parts[0];
            String clientSecret = parts[1];
            if (clientId != null && !clientId.equals(decodedClientId)) {
                throw new TokenException(ErrorCode.VALIDATION_FAILED, "invalid_request", "Client ID in request does not match Authorization header");
            }
            this.clientId = decodedClientId;
            this.clientSecret = clientSecret;
        } catch (IllegalArgumentException e) {
            throw new TokenException(ErrorCode.VALIDATION_FAILED, "invalid_request", "Failed to decode Authorization header");
        }
    }
    
}

package com.iam.auth.dto.request;

import com.iam.auth.enums.ErrorCode;
import com.iam.auth.exception.TokenException;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RevokeTokenRequest {
    private String token;
    private String tokenTypeHint;
    private String clientId;
    private String clientSecret;

    public RevokeTokenRequest(String token, String tokenTypeHint, String clientId, String clientSecret, String authorizationHeader) {
        this.token = token;
        this.tokenTypeHint = tokenTypeHint;
        // Extract clientId and clientSecret from bearerToken if needed
        if (authorizationHeader != null && authorizationHeader.startsWith("Basic ")) {
            extractClientCredentialsFromAuthorizationHeader(authorizationHeader);
        } else {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }

    }
    private void extractClientCredentialsFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
            throw new TokenException(ErrorCode.VALIDATION_FAILED, "Missing or invalid Authorization header");
        }
        try {
            String base64Credentials = authorizationHeader.substring("Basic ".length());
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            String[] parts = credentials.split(":", 2);
            if (parts.length != 2) {
                throw new TokenException(ErrorCode.VALIDATION_FAILED, "Invalid Authorization header format");
            }
            this.clientId = parts[0];
            this.clientSecret = parts[1];
        } catch (IllegalArgumentException e) {
            throw new TokenException(ErrorCode.VALIDATION_FAILED, "Failed to decode Authorization header");
        }
    }
}

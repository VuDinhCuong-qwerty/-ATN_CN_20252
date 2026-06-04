package com.iam.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwksResponse {

    @JsonProperty("keys")
    private List<JwkKey> keys;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JwkKey {

        @JsonProperty("kty")
        private String kty;   // "EC"

        @JsonProperty("kid")
        private String kid;   // Key ID from AuthSigningKey

        @JsonProperty("use")
        private String use;   // "sig"

        @JsonProperty("alg")
        private String alg;   // "ES256"

        @JsonProperty("crv")
        private String crv;   // "P-256"

        @JsonProperty("x")
        private String x;     // Base64URL-encoded X coordinate

        @JsonProperty("y")
        private String y;     // Base64URL-encoded Y coordinate
    }
}

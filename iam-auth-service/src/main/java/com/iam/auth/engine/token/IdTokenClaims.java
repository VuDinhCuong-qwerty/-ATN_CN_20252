package com.iam.auth.engine.token;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdTokenClaims {

    // REQUIRED — JWT standard claims
    private String jti;
    private String iss;
    private String sub;             // userId
    private String aud;             // clientId
    private Long iat;
    private Long exp;

    // REQUIRED when client sent nonce in /authorize — replay protection
    private String nonce;

    // scope: profile
    private String name;

    @JsonProperty("preferred_username")
    private String preferredUsername;

    // scope: email
    private String email;

    // scope: phone
    @JsonProperty("phone_number")
    private String phoneNumber;
}

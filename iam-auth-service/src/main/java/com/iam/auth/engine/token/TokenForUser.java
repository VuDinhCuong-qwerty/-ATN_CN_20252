package com.iam.auth.engine.token;

import lombok.*;

import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenForUser {

    // JWT standard claims
    private String jti;
    private String iss;
    private String sub;            // userId
    private String aud;            // clientId
    private Long iat;
    private Long exp;
    private String type;

    // User profile
    private String username;
    private String employeeCode;
    private String email;
    private String displayName;
    private String mobile;

    // App context
    private Long appId;
    private String serviceCode;

    // OAuth context
    private String clientId;
    private List<String> scopes;

    // Authorization
    private String role;
    private List<String> permissions;  // format: "serviceCode/resourceCode:action"
}

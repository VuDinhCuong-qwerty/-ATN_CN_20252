package com.iam.auth.engine.token;

import lombok.*;

import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenForService {
    private String jti;
    private String iss;
    private String sub;
    private Long iat;
    private Long exp;
    private String type;
    private String clientId;
    private String clientName;
    private List<String> scopes;
}
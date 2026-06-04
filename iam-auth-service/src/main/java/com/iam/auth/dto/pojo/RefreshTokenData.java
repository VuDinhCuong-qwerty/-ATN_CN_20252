package com.iam.auth.dto.pojo;

import lombok.*;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenData {
    private Long userId;
    private String clientId;
    private Long appId;
    private List<String> scopes;
    private String userSessionId;
}

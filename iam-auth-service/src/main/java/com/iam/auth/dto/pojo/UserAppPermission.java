package com.iam.auth.dto.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAppPermission {
    private Long userId;
    private String username;
    private Long applicationId;
    private String applicationName;
    private String applicationType;
    private Long clientId;
    private Boolean clientStatus;
    private String role;
}

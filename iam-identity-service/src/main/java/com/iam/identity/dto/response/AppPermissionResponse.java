package com.iam.identity.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppPermissionResponse {
    private Long permissionId;
    private Long appId;
    private String appName;
    private String serviceCode;
    private String appType;
    private String status;
    private String grantSource;
    private LocalDateTime grantedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime inactiveFromDate;
    private LocalDateTime inactiveToDate;
}

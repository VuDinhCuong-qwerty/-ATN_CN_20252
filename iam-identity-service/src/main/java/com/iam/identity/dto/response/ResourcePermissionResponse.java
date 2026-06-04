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
public class ResourcePermissionResponse {
    private Long id;
    private Long resourceId;
    private String resourceCode;
    private String resourceName;
    private String resourceType;
    private String action;
    private Long appId;
    private String appName;
    private String status;
    private LocalDateTime grantedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime inactiveFromDate;
    private LocalDateTime inactiveToDate;
}

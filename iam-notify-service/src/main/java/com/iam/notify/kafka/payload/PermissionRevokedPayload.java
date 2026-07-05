package com.iam.notify.kafka.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// Matches AppPermissionRevokePayload from iam-identity-service
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionRevokedPayload {
    private Long userId;
    private String employeeCode;         // người bị thu hồi quyền
    private List<Long> revokedAppIds;
    private String appName;              // để consumer không cần lookup thêm
    private List<Long> revokedResourceIds;
    private String revokedBy;
    private LocalDateTime revokedAt;
}

package com.iam.identity.kafka.event.payload;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppPermissionRevokePayload {
    private Long userId;
    private String employeeCode;
    private List<Long> revokedAppIds;
    private String appName; // để consumer không cần lookup thêm
    private List<Long> revokedResourceIds ; // danh sách resource bị cascade revoke
    private String revokedBy; // null until JWT enabled
    private LocalDateTime revokedAt;

}

package com.iam.auth.kafka.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppPermissionRevokePayload {
    private Long userId;
    private String employeeCode;
    private List<Long> revokedAppIds;
    private List<Long> revokedResourceIds;
    private String revokedBy;
    // revokedAt intentionally omitted — unused and avoids cross-service date format issues
}

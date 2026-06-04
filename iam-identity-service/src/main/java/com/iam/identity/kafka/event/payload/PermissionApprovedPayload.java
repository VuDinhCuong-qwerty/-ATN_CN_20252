package com.iam.identity.kafka.event.payload;

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
public class PermissionApprovedPayload {
    private Long requestId;
    private String requestedBy;   // employeeCode của requester
    private String reviewedBy;    // employeeCode của CAB
    private String grantedTo;     // employeeCode của người thực sự được cấp quyền (requestFor hoặc requestedBy)
    private LocalDateTime approvedAt;
}

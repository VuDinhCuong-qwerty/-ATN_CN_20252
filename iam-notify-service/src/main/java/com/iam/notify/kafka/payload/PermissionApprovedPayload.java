package com.iam.notify.kafka.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Matches PermissionApprovedPayload from iam-identity-service
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionApprovedPayload {
    private Long requestId;
    private String requestedBy;  // employeeCode của requester
    private String reviewedBy;   // employeeCode của CAB
    private String grantedTo;    // employeeCode của người được cấp quyền
    private LocalDateTime approvedAt;

    // Không có trong Kafka payload — được set bởi consumer sau khi đọc event.getEventType()
    private String action; // "APPROVED" | "REJECTED"
}

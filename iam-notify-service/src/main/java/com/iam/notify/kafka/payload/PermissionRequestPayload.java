package com.iam.notify.kafka.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Matches PermissionRequestCreatedPayload from iam-identity-service
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionRequestPayload {
    private Long requestId;
    private String requesterCode;
    private String reviewerCode;
    private String granteeCode;
    private String reason;
    private LocalDateTime requestedAt;
}

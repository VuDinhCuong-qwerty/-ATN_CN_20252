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
public class PermissionRequestCreatedPayload {
    private Long requestId;
    private String requesterCode;
    private String reviewerCode;
    private String granteeCode;
    private String reason;
    private LocalDateTime requestedAt;
}

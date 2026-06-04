package com.iam.identity.dto.pojo;

import java.time.LocalDateTime;

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
public class PermissionRequest {
    private Long requestId;
    private String status;
    private String reason;
    private String note;
    private String reviewerCode;
    private String reviewerUsername;
    private String reviewerFullName;
    private String requesterCode;
    private String requesterUsername;
    private String requesterFullName;
    private String granteeCode;
    private String granteeUsername;
    private String granteeFullName;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
}

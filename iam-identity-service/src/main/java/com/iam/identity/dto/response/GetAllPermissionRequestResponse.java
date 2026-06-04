package com.iam.identity.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iam.identity.dto.pojo.PermissionRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetAllPermissionRequestResponse {
    private Long requestId;
    private String status;
    private String reason;
    private String note;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedAt;
    private Person reviewer;
    private Person requester;
    private Person grantee;

    public GetAllPermissionRequestResponse(PermissionRequest permissionRequest) {
        this.requestId = permissionRequest.getRequestId();
        this.status = permissionRequest.getStatus();
        this.reason = permissionRequest.getReason();
        this.note = permissionRequest.getNote();
        this.reviewer = new Person(permissionRequest.getReviewerCode(), permissionRequest.getReviewerUsername(),
                permissionRequest.getReviewerFullName());
        this.requester = new Person(permissionRequest.getRequesterCode(), permissionRequest.getRequesterUsername(),
                permissionRequest.getRequesterFullName());
        this.grantee = new Person(permissionRequest.getGranteeCode(), permissionRequest.getGranteeUsername(),
                permissionRequest.getGranteeFullName());
        this.requestedAt = permissionRequest.getRequestedAt();
        this.reviewedAt = permissionRequest.getReviewedAt();
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String employeeCode;
        private String username;
        private String fullName;
    }
}

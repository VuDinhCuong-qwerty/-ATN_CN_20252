package com.demo.change.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChangeDetailResponse {

    private Long id;
    private String changeId;
    private String changeName;
    private String content;
    private String gitLink;
    private String status;
    private LocalDateTime goliveAt;
    private String createdBy;
    private String createdByCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<JobDetail> jobs;
    private List<ChecklistDetail> checklistItems;
    private List<TeamMemberDetail> teamMembers;
    private List<ApproverDetail> approvers;

    @Data
    @Builder
    public static class JobDetail {
        private Long id;
        private String name;
        private String link;
        private String jobType;
        private Integer orderNum;
        private String createdBy;
        private String createdByCode;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class ChecklistDetail {
        private Long id;
        private String phase;
        private String stepText;
        private Integer orderNum;
        private String assignedTo;
        private String assignedToCode;
        private String taskStatus;
        private String createdBy;
        private String createdByCode;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class TeamMemberDetail {
        private Long id;
        private String userId;
        private String username;
        private String fullName;
        private String employeeCode;
        private String memberRole;
        private Integer isLead;
        private String createdBy;
        private String createdByCode;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class ApproverDetail {
        private Long id;
        private String userId;
        private String username;
        private String fullName;
        private String employeeCode;
        private String approveStatus;
        private String note;
        private LocalDateTime decidedAt;
        private String createdBy;
        private String createdByCode;
        private LocalDateTime createdAt;
    }
}

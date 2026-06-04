package com.demo.change.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateChangeRequest {

    @NotBlank(message = "Tên change không được để trống")
    private String changeName;

    private String content;

    private String gitLink;

    @NotNull(message = "Thời gian golive không được để trống")
    private LocalDateTime goliveAt;

    @Valid
    private List<JobInput> jobs;

    @Valid
    private List<ChecklistInput> checklistItems;

    @Valid
    private List<TeamMemberInput> teamMembers;

    @Valid
    private List<ApproverInput> approvers;

    @Data
    public static class JobInput {
        private Long id;        // null → insert mới, non-null → update

        @NotBlank(message = "Tên job không được để trống")
        private String name;

        private String link;

        @NotBlank(message = "Loại job không được để trống")
        private String jobType;

        private Integer orderNum;
        private Integer status; // 0 → soft-delete, 1 → active (chỉ dùng khi id != null)
    }

    @Data
    public static class ChecklistInput {
        private Long id;

        @NotBlank(message = "Phase không được để trống")
        private String phase;

        @NotBlank(message = "Nội dung bước không được để trống")
        private String stepText;

        private Integer orderNum;
        private String  assignedTo;
        private String  assignedToCode;
        private Integer status;
    }

    @Data
    public static class TeamMemberInput {
        private Long id;

        @NotBlank(message = "Username thành viên không được để trống")
        private String username;

        private String  userId;
        private String  fullName;
        private String  employeeCode;

        @NotBlank(message = "Vai trò thành viên không được để trống")
        private String  memberRole;

        private Boolean isLead;
        private Integer status;
    }

    @Data
    public static class ApproverInput {
        private Long id;

        @NotBlank(message = "Username CAB không được để trống")
        private String username;

        private String  userId;
        private String  fullName;
        private String  employeeCode;
        private Integer status;
    }
}

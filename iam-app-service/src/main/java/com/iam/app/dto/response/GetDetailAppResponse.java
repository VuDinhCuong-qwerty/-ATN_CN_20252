package com.iam.app.dto.response;

import java.time.LocalDateTime;

import com.iam.app.dto.pojo.AppInfor;
import com.iam.app.dto.pojo.Department;

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
public class GetDetailAppResponse {

    private String name;
    private String description;
    private String appType; // INTERNAL | THIRD_PARTY_LDAP
    private String logoUri;
    private String defaultUrl;
    private String status; // ACTIVE | INACTIVE
    private Long departmentId;
    private DepartmentResponse department;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String serviceCode;
    private Long groupId;
    private String groupName;
    private Long acrLevel;

    public GetDetailAppResponse(AppInfor app, Department department) {
        if (app != null) {
            this.name = app.getName();
            this.description = app.getDescription();
            this.appType = app.getAppType();
            this.logoUri = app.getLogoUri();
            this.defaultUrl = app.getDefaultUrl();
            this.status = app.getStatus();
            this.departmentId = app.getDepartmentId();
            this.createdAt = app.getCreatedAt();
            this.updatedAt = app.getUpdatedAt();
            this.serviceCode = app.getServiceCode();
            this.groupId = app.getGroupId();
            this.groupName = app.getGroupName();
            this.acrLevel = app.getAcrLevel();
        }

        if (department != null) {
            this.department = new DepartmentResponse(department);
        }
    }

    // ===== INNER CLASS =====
    @Setter
    @Getter
    @NoArgsConstructor
    public static class DepartmentResponse {
        private Long id;
        private String code;
        private String name;
        private Long parentId;
        private String detail;

        public DepartmentResponse(Department department) {
            this.id = department.getId();
            this.code = department.getCode();
            this.name = department.getName();
            this.parentId = department.getParentId();
            this.detail = department.getDetail();
        }
    }
}

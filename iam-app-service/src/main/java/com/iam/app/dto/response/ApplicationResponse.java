package com.iam.app.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.iam.app.domain.AuthApplication;

import lombok.Getter;
import lombok.Setter;

@Getter
public class ApplicationResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final String appType;
    private final String logoUri;
    private final String defaultUrl;
    private final String status;
    private final Long departmentId;
    private final String serviceCode;
    private final Long groupId;
    private final Long acrLevel;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @Setter
    private List<AppWarning> warnings;

    public ApplicationResponse(AuthApplication entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.description = entity.getDescription();
        this.appType = entity.getAppType();
        this.logoUri = entity.getLogoUri();
        this.defaultUrl = entity.getDefaultUrl();
        this.status = entity.getStatus();
        this.departmentId = entity.getDepartmentId();
        this.serviceCode = entity.getServiceCode();
        this.groupId = entity.getGroupId();
        this.acrLevel = entity.getAcrLevel();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }
}

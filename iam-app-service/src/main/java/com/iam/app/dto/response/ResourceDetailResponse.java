package com.iam.app.dto.response;

import com.iam.app.domain.AuthApplication;
import com.iam.app.domain.AuthResource;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
public class ResourceDetailResponse {

    private final Long id;
    private final String resourceCode;
    private final String resourceName;
    private final String resourceType;
    private final List<String> actions;
    private final String ldapGroupName;
    private final String description;
    private final String status;
    private final LocalDateTime createdAt;
    private final AppInfo app;

    @Setter
    private List<AppWarning> warnings;

    public ResourceDetailResponse(AuthResource r, AuthApplication application) {
        this.id = r.getId();
        this.resourceCode = r.getResourceCode();
        this.resourceName = r.getResourceName();
        this.resourceType = r.getResourceType();
        this.actions = r.getActions() != null
                ? Arrays.asList(r.getActions().split(","))
                : Collections.emptyList();
        this.ldapGroupName = r.getLdapGroupName();
        this.description = r.getDescription();
        this.status = r.getStatus();
        this.createdAt = r.getCreatedAt();
        this.app = new AppInfo(application);
    }

    @Getter
    public static class AppInfo {
        private final Long id;
        private final String name;
        private final String serviceCode;
        private final String appType;
        private final String status;

        public AppInfo(AuthApplication app) {
            this.id = app.getId();
            this.name = app.getName();
            this.serviceCode = app.getServiceCode();
            this.appType = app.getAppType();
            this.status = app.getStatus();
        }
    }
}

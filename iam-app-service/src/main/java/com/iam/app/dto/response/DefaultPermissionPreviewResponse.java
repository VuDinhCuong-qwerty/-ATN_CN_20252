package com.iam.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DefaultPermissionPreviewResponse {

    private final String roleCode;
    private final String positionCode;
    private final List<ApplicationItem> applications;
    private final List<ResourceItem> resources;

    @Getter
    @AllArgsConstructor
    public static class ApplicationItem {
        private final Long applicationId;
        private final String name;
    }

    @Getter
    @AllArgsConstructor
    public static class ResourceItem {
        private final Long resourceId;
        private final String resourceCode;
        private final String appName;
        private final List<String> actions;
    }
}

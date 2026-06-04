package com.iam.identity.dto.pojo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResourceRow {
    private Long   resourceId;
    private Long   appId;
    private String resourceCode;
    private String resourceName;
    private String resourceType;
    private String actions;
    private String ldapGroupName;
    private String description;
    private String resourceStatus;
    private String appName;
    private String appType;
    private String serviceCode;
    private String appStatus;
}

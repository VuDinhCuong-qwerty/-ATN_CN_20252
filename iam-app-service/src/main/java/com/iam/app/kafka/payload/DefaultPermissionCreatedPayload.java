package com.iam.app.kafka.payload;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DefaultPermissionCreatedPayload {

    /** APP | RESOURCE */
    private String permissionType;

    private Long applicationId;   // chỉ có khi permissionType=APP
    private Long resourceId;      // chỉ có khi permissionType=RESOURCE

    private Long roleId;          // canonical, FK AUTH_ROLE.ID — luôn resolve trước khi publish
    private String roleCode;      // chỉ để log/debug

    private String positionCode;

    private List<String> actions; // chỉ có khi permissionType=RESOURCE
}

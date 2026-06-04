package com.iam.app.dto.response;

import com.iam.app.domain.AuthDefaultResource;
import lombok.Getter;

import java.time.Instant;

@Getter
public class DefaultResourcePermissionResponse {

    private final Long id;
    private final Long roleId;
    private final String roleName;
    private final String positionCode;
    private final String positionName;
    private final Long resourceId;
    private final String resourceCode;
    private final String resourceName;
    private final String actions;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public DefaultResourcePermissionResponse(AuthDefaultResource entity,
                                             String roleName,
                                             String positionName,
                                             String resourceCode,
                                             String resourceName) {
        this.id = entity.getId();
        this.roleId = entity.getRoleId();
        this.roleName = roleName;
        this.positionCode = entity.getPositionCode();
        this.positionName = positionName;
        this.resourceId = entity.getResourceId();
        this.resourceCode = resourceCode;
        this.resourceName = resourceName;
        this.actions = entity.getActions();
        this.status = entity.getStatus();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }
}

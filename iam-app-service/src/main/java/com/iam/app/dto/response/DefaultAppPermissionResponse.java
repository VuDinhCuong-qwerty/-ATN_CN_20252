package com.iam.app.dto.response;

import com.iam.app.domain.AuthDefaultAppPermission;
import lombok.Getter;

import java.time.Instant;

@Getter
public class DefaultAppPermissionResponse {

    private final Long id;
    private final String roleId;
    private final String roleName;
    private final String positionCode;
    private final String positionName;
    private final Long applicationId;
    private final String applicationName;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;
    /** Số user ACTIVE hiện có khớp role+position — chỉ hiển thị thông tin, null nếu không tính (vd response của updateStatuses) */
    private final Long affectedUserCount;

    public DefaultAppPermissionResponse(AuthDefaultAppPermission entity,
                                        String roleName,
                                        String positionName,
                                        String applicationName) {
        this(entity, roleName, positionName, applicationName, null);
    }

    public DefaultAppPermissionResponse(AuthDefaultAppPermission entity,
                                        String roleName,
                                        String positionName,
                                        String applicationName,
                                        Long affectedUserCount) {
        this.id = entity.getId();
        this.roleId = entity.getRoleId();
        this.roleName = roleName;
        this.positionCode = entity.getPositionCode();
        this.positionName = positionName;
        this.applicationId = entity.getApplicationId();
        this.applicationName = applicationName;
        this.status = entity.getStatus();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
        this.affectedUserCount = affectedUserCount;
    }
}

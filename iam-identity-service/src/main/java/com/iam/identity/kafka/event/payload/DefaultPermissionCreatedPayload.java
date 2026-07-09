package com.iam.identity.kafka.event.payload;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DefaultPermissionCreatedPayload {

    /** APP | RESOURCE */
    private String permissionType;

    private Long applicationId;   // chỉ có khi permissionType=APP
    private Long resourceId;      // chỉ có khi permissionType=RESOURCE

    private Long roleId;          // canonical, FK AUTH_ROLE.ID
    private String roleCode;      // chỉ để log/debug

    private String positionCode;

    private List<String> actions; // chỉ có khi permissionType=RESOURCE
}

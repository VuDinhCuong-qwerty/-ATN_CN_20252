package com.iam.identity.dto.pojo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserRoleRow {
    private String roleCode;
    private String roleName;
}

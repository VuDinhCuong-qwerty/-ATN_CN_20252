package com.iam.identity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleResponse {
    private Long userId;
    private String username;
    private Long userRoleId;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private LocalDateTime grantedAt;
    private LocalDateTime expiredAt;
    private String status;
}

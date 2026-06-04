package com.iam.auth.dto.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionRow {
    private Long userId;
    private String username;
    private String displayName;
    private String mobile;
    private List<String> userActions;      // split từ user_action "read,write" → ["read","write"]
    private String resourceCode;
    private List<String> resourceActions;  // split từ resource_actions "read,write,delete" → [...]
    private Long appId;
    private String role;
}

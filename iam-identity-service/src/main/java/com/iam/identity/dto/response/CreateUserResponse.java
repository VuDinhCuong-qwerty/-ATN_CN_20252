package com.iam.identity.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String status;
    private String createdAt;
    private List<String> roles;
}

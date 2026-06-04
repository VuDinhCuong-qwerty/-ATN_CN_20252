package com.iam.app.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iam.app.domain.AuthRole;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetRolesResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public GetRolesResponse(AuthRole role) {
        this.id = role.getId();
        this.code = role.getCode();
        this.name = role.getName();
        this.description = role.getDescription();
        this.createdAt = role.getCreatedAt();
    }
}

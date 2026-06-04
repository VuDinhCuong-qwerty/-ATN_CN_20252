package com.iam.app.dto.response;

import com.iam.app.domain.AuthClientGroup;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AppGroupResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final Integer status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public AppGroupResponse(AuthClientGroup entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.description = entity.getDescription();
        this.status = entity.getStatus();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }
}

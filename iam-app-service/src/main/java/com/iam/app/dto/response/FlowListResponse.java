package com.iam.app.dto.response;

import com.iam.app.domain.AuthFlow;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FlowListResponse {

    private final Long id;
    private final String alias;
    private final String description;
    private final Integer isBuiltIn;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public FlowListResponse(AuthFlow flow) {
        this.id = flow.getId();
        this.alias = flow.getAlias();
        this.description = flow.getDescription();
        this.isBuiltIn = flow.getIsBuiltIn();
        this.status = flow.getStatus();
        this.createdAt = flow.getCreatedAt();
        this.updatedAt = flow.getUpdatedAt();
    }
}

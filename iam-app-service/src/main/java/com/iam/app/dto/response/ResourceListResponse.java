package com.iam.app.dto.response;

import com.iam.app.domain.AuthResource;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
public class ResourceListResponse {

    private final Long id;
    private final String resourceCode;
    private final String resourceName;
    private final String resourceType;
    private final List<String> actions;
    private final String status;

    public ResourceListResponse(AuthResource r) {
        this.id = r.getId();
        this.resourceCode = r.getResourceCode();
        this.resourceName = r.getResourceName();
        this.resourceType = r.getResourceType();
        this.actions = r.getActions() != null
                ? Arrays.asList(r.getActions().split(","))
                : Collections.emptyList();
        this.status = r.getStatus();
    }
}

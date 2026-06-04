package com.iam.app.dto.response;

import com.iam.app.dto.projection.FlowExecutionProjection;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class FlowExecutionNode {

    private final Long id;
    private final Long clientMethodId;
    private final String clientMethodName;
    private final String requirement;
    private final Integer isDefault;
    private final String status;
    @Setter
    private List<FlowExecutionNode> children = new ArrayList<>();

    public FlowExecutionNode(FlowExecutionProjection p) {
        this.id = p.getId();
        this.clientMethodId = p.getClientMethodId();
        this.clientMethodName = p.getMethodName();
        this.requirement = p.getRequirement();
        this.isDefault = p.getIsDefault();
        this.status = p.getStatus();
    }
}

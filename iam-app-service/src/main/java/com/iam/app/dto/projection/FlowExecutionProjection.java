package com.iam.app.dto.projection;

public interface FlowExecutionProjection {
    Long getId();
    Long getParentNodeId();
    Long getClientMethodId();
    String getMethodName();
    String getRequirement();
    Integer getIsDefault();
    String getStatus();
}

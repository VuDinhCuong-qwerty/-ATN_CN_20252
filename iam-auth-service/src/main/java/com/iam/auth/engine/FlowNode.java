package com.iam.auth.engine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FlowNode {
    private Long executionId;
    private Long appId;
    private Long arcLevel;
    private Long parentNodeId;
    private Long clientMethodId;
    private String method;
    private String requirement;
    private boolean isDefault;
    private String theme;
    private List<FlowNode> children;

    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    @JsonIgnore
    public FlowNode getDefaultChild() {
        return children.stream()
                .filter(item -> item.isDefault)
                .findFirst().orElse(children.getFirst());
    }

    public interface REQUIREMENT {
        String REQUIRED = "REQUIRED";
        String OPTIONAL = "OPTIONAL";
    }
}

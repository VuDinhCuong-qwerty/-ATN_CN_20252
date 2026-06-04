package com.iam.auth.engine;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class AuthFlow {

    private Long flowId;
    private FlowNode entryPoint;
    private Map<Long, FlowNode> executionMap;
    private String theme;

    public FlowNode getDefaultFirstNode() {
        return entryPoint.getChildren().stream()
                .filter(FlowNode::isDefault)
                .findFirst()
                .orElse(entryPoint.getChildren().getFirst());
    }

    public FlowNode getNode(Long nodeId) {
        return executionMap.get(nodeId);
    }

    public FlowNode getDefaultChild(Long nodeId) {
        FlowNode parentNode = this.executionMap.get(nodeId);
        if (parentNode == null || parentNode.isLeaf()) return null;
        return parentNode.getChildren().stream()
                .filter(FlowNode::isDefault)
                .findFirst()
                .orElse(parentNode.getChildren().getFirst());
    }
}

package com.iam.auth.engine;


import com.iam.auth.repository.jpa.AuthRepository;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AuthFlowLoader {

    private final AuthRepository authRepository;

    public AuthFlowLoader(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public AuthFlow load(Long flowId) {
        // load các exe từ db
        List<FlowNode> nodeList = this.authRepository.getNodeByFlowId(flowId);

        if (nodeList == null || nodeList.isEmpty()) {
            throw new RuntimeException("Flow not found for id: " + flowId);
        }

        Map<Long, FlowNode> nodeMap = new HashMap<>();
        for (FlowNode node: nodeList) {
            nodeMap.put(node.getExecutionId(), node);
        }

        List<FlowNode> roots = new ArrayList<>();
        for (FlowNode node: nodeList) {
            if (node.getParentNodeId() == null) {
                roots.add(node);
            } else {
                FlowNode parent = nodeMap.get(node.getParentNodeId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }

        nodeMap.values().forEach(node ->
                node.getChildren().sort(Comparator.comparing(FlowNode::isDefault).reversed())
        );

        FlowNode entryPoint = FlowNode.builder()
                .executionId(null).clientMethodId(null)
                .requirement("REQUIRED").isDefault(false)
                .children(roots).build();

        return AuthFlow.builder()
                .flowId(flowId).entryPoint(entryPoint).theme(nodeList.getFirst().getTheme())
                .executionMap(nodeMap).build();
    }

}

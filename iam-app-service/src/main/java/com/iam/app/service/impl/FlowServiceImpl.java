package com.iam.app.service.impl;

import com.iam.app.config.KafkaConfig;
import com.iam.app.domain.AuthApplication;
import com.iam.app.domain.AuthClientMethod;
import com.iam.app.domain.AuthFlow;
import com.iam.app.domain.AuthFlowExecution;
import com.iam.app.domain.AuthMethod;
import com.iam.app.dto.projection.FlowExecutionProjection;
import com.iam.app.dto.request.CreateFlowRequest;
import com.iam.app.dto.request.UpdateFlowRequest;
import com.iam.app.dto.response.FlowDetailResponse;
import com.iam.app.dto.response.FlowExecutionNode;
import com.iam.app.dto.response.FlowListResponse;
import com.iam.app.enums.ErrorCode;
import com.iam.app.exception.BusinessException;
import com.iam.app.kafka.payload.FlowExecutionUpdatedPayload;
import com.iam.app.kafka.producer.AppEventProducer;
import com.iam.app.repository.jpa.AuthApplicationRepository;
import com.iam.app.repository.jpa.AuthClientMethodRepository;
import com.iam.app.repository.jpa.AuthFlowExecutionRepository;
import com.iam.app.repository.jpa.AuthFlowRepository;
import com.iam.app.repository.jpa.AuthMethodRepository;
import com.iam.app.service.FlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowServiceImpl implements FlowService {

    private final AuthApplicationRepository appRepository;
    private final AuthFlowRepository flowRepository;
    private final AuthFlowExecutionRepository executionRepository;
    private final AuthClientMethodRepository clientMethodRepository;
    private final AuthMethodRepository methodRepository;
    private final AppEventProducer eventProducer;

    /** Internal normalized representation — maps from both Create/Update request items. */
    private record ExecItem(Integer nodeId, Integer parentNodeId, Long methodId,
                            String requirement, Integer isDefault) {

        static ExecItem from(CreateFlowRequest.ExecutionItem e) {
            return new ExecItem(e.getNodeId(), e.getParentNodeId(), e.getMethodId(),
                    e.getRequirement(), e.getIsDefault());
        }

        static ExecItem from(UpdateFlowRequest.ExecutionItem e) {
            return new ExecItem(e.getNodeId(), e.getParentNodeId(), e.getMethodId(),
                    e.getRequirement(), e.getIsDefault());
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    @Override
    public List<FlowListResponse> getFlows(Long appId, String status) {
        appRepository.findById(appId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Ứng dụng không tồn tại"));

        return flowRepository.findByAppId(appId, status).stream()
                .map(FlowListResponse::new)
                .toList();
    }

    @Override
    public FlowDetailResponse getFlowDetail(Long appId, Long flowId) {
        appRepository.findById(appId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Ứng dụng không tồn tại"));

        AuthFlow flow = flowRepository.findByIdAndAppId(flowId, appId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Flow không tồn tại"));

        List<FlowExecutionProjection> projections = executionRepository.findActiveByFlowId(flowId);
        return new FlowDetailResponse(flow, buildTree(projections));
    }

    @Override
    @Transactional
    public FlowDetailResponse createFlow(Long appId, CreateFlowRequest request) {
        appRepository.findById(appId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Ứng dụng không tồn tại"));

        if (flowRepository.countActiveByAppId(appId) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "App đã có flow đang ACTIVE. Vui lòng tắt trước khi tạo mới.");
        }

        if (flowRepository.countByAppIdAndAlias(appId, request.getAlias()) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Alias '" + request.getAlias() + "' đã tồn tại trong ứng dụng này");
        }

        List<CreateFlowRequest.ExecutionItem> rawItems = request.getExecutions();
        boolean hasExecutions = rawItems != null && !rawItems.isEmpty();

        if (!hasExecutions) {
            AuthFlow flow = AuthFlow.builder()
                    .appId(appId)
                    .alias(request.getAlias())
                    .description(request.getDescription())
                    .isBuiltIn(0)
                    .status(AuthFlow.STATUS.INACTIVE)
                    .build();
            flow = flowRepository.save(flow);
            return new FlowDetailResponse(flow, Collections.emptyList());
        }

        List<ExecItem> items = rawItems.stream().map(ExecItem::from).toList();
        validateExecutionItems(items);
        Map<Long, Long> methodIdToClientMethodId = validateAndResolveMethodMap(appId, items);

        AuthFlow flow = AuthFlow.builder()
                .appId(appId)
                .alias(request.getAlias())
                .description(request.getDescription())
                .isBuiltIn(0)
                .status(AuthFlow.STATUS.ACTIVE)
                .build();
        flow = flowRepository.save(flow);

        insertExecutionTree(flow.getId(), items, methodIdToClientMethodId);

        return getFlowDetail(appId, flow.getId());
    }

    @Override
    @Transactional
    public FlowDetailResponse updateFlow(Long appId, Long flowId, UpdateFlowRequest request) {
        AuthApplication app = appRepository.findById(appId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Ứng dụng không tồn tại"));

        AuthFlow flow = flowRepository.findByIdAndAppId(flowId, appId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Flow không tồn tại"));

        List<UpdateFlowRequest.ExecutionItem> rawItems = request.getExecutions();
        boolean hasExecutions = rawItems != null && !rawItems.isEmpty();
        String newStatus = hasExecutions ? AuthFlow.STATUS.ACTIVE : AuthFlow.STATUS.INACTIVE;

        if (Integer.valueOf(1).equals(flow.getIsBuiltIn()) && AuthFlow.STATUS.INACTIVE.equals(newStatus)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Không thể tắt flow hệ thống (IS_BUILT_IN=1). Vui lòng cung cấp ít nhất một execution.");
        }

        flow.setDescription(request.getDescription());
        flow.setStatus(newStatus);
        flowRepository.save(flow);

        executionRepository.deactivateAllByFlowId(flowId);

        if (hasExecutions) {
            List<ExecItem> items = rawItems.stream().map(ExecItem::from).toList();
            validateExecutionItems(items);
            Map<Long, Long> methodIdToClientMethodId = validateAndResolveMethodMap(appId, items);
            insertExecutionTree(flowId, items, methodIdToClientMethodId);
        }

        eventProducer.publish(
                KafkaConfig.TOPIC_FLOW_EXECUTION_UPDATED,
                "FLOW_EXECUTION_UPDATED",
                FlowExecutionUpdatedPayload.builder()
                        .appId(appId)
                        .flowId(flowId)
                        .appServiceCode(app.getServiceCode())
                        .build()
        );

        return getFlowDetail(appId, flowId);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private List<FlowExecutionNode> buildTree(List<FlowExecutionProjection> projections) {
        Map<Long, FlowExecutionNode> nodeMap = new LinkedHashMap<>();
        for (FlowExecutionProjection p : projections) {
            nodeMap.put(p.getId(), new FlowExecutionNode(p));
        }

        List<FlowExecutionNode> roots = new ArrayList<>();
        for (FlowExecutionProjection p : projections) {
            FlowExecutionNode node = nodeMap.get(p.getId());
            if (p.getParentNodeId() == null || p.getParentNodeId() == 0) {
                roots.add(node);
            } else {
                FlowExecutionNode parent = nodeMap.get(p.getParentNodeId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        return roots;
    }

    /**
     * Validates methodIds against AUTH_METHOD (system support) and AUTH_CLIENT_METHOD (app config).
     * Returns map: methodId → clientMethodId (AUTH_CLIENT_METHOD.ID) for DB insert.
     */
    private Map<Long, Long> validateAndResolveMethodMap(Long appId, List<ExecItem> items) {
        List<Long> methodIds = items.stream().map(ExecItem::methodId).toList();

        Map<Long, AuthMethod> authMethodMap = methodRepository.findAllByIdIn(methodIds)
                .stream().collect(Collectors.toMap(AuthMethod::getId, m -> m));
        for (Long id : methodIds) {
            if (!authMethodMap.containsKey(id) || !Integer.valueOf(1).equals(authMethodMap.get(id).getStatus())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Method " + id + " không được hệ thống hỗ trợ");
            }
        }

        Map<Long, AuthClientMethod> clientMethodMap = clientMethodRepository
                .findAllByAppIdAndMethodIdIn(appId, methodIds)
                .stream().collect(Collectors.toMap(AuthClientMethod::getMethodId, m -> m));
        for (Long id : methodIds) {
            if (!clientMethodMap.containsKey(id)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Method " + id + " chưa được cấu hình cho ứng dụng này");
            }
            if (!"ACTIVE".equals(clientMethodMap.get(id).getStatus())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Method " + id + " đã bị tắt trên ứng dụng này");
            }
        }

        return clientMethodMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getId()));
    }

    /** BFS-inserts the execution tree. nodeId/parentNodeId are client-side temp IDs — NOT stored in DB. */
    private void insertExecutionTree(Long flowId, List<ExecItem> items,
                                     Map<Long, Long> methodIdToClientMethodId) {
        Map<Integer, Integer> resolvedIsDefault = resolveIsDefault(items);
        Map<Integer, Long> nodeIdToRealId = new HashMap<>();

        Queue<ExecItem> queue = new LinkedList<>(
                items.stream().filter(e -> e.parentNodeId() == null).toList()
        );

        while (!queue.isEmpty()) {
            ExecItem item = queue.poll();
            Long parentRealId = item.parentNodeId() == null
                    ? null
                    : nodeIdToRealId.get(item.parentNodeId());

            AuthFlowExecution exec = AuthFlowExecution.builder()
                    .flowId(flowId)
                    .parentNodeId(parentRealId)
                    .clientMethodId(methodIdToClientMethodId.get(item.methodId()))
                    .requirement(item.requirement())
                    .isDefault(resolvedIsDefault.getOrDefault(item.nodeId(), 0))
                    .status(AuthFlowExecution.STATUS.ACTIVE)
                    .build();

            AuthFlowExecution saved = executionRepository.save(exec);
            nodeIdToRealId.put(item.nodeId(), saved.getId());

            items.stream()
                    .filter(e -> item.nodeId().equals(e.parentNodeId()))
                    .forEach(queue::add);
        }
    }

    private void validateExecutionItems(List<ExecItem> items) {
        Set<Integer> nodeIds = new HashSet<>();
        for (ExecItem item : items) {
            if (!nodeIds.add(item.nodeId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "nodeId " + item.nodeId() + " bị trùng trong danh sách");
            }
        }

        for (ExecItem item : items) {
            if (item.parentNodeId() != null && !nodeIds.contains(item.parentNodeId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "parentNodeId " + item.parentNodeId() + " không tồn tại trong danh sách");
            }
        }

        Map<Integer, Integer> parentMap = items.stream()
                .collect(Collectors.toMap(
                        ExecItem::nodeId,
                        e -> e.parentNodeId() != null ? e.parentNodeId() : -1
                ));

        for (ExecItem item : items) {
            int depth = 1;
            Set<Integer> visited = new HashSet<>();
            visited.add(item.nodeId());
            Integer cur = item.parentNodeId();
            while (cur != null) {
                if (!visited.add(cur)) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "Phát hiện vòng lặp trong cây execution");
                }
                depth++;
                if (depth > 3) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "Cây execution không được vượt quá 3 cấp");
                }
                Integer parent = parentMap.get(cur);
                cur = (parent != null && parent != -1) ? parent : null;
            }
        }

        Set<Long> methodIds = new HashSet<>();
        for (ExecItem item : items) {
            if (!methodIds.add(item.methodId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "methodId " + item.methodId() + " xuất hiện nhiều hơn 1 lần");
            }
        }

        Map<Integer, Long> defaultCountPerGroup = new HashMap<>();
        for (ExecItem item : items) {
            if (Integer.valueOf(1).equals(item.isDefault())) {
                defaultCountPerGroup.merge(item.parentNodeId(), 1L, Long::sum);
            }
        }
        for (var entry : defaultCountPerGroup.entrySet()) {
            if (entry.getValue() > 1) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Mỗi nhóm node chỉ được có 1 node mặc định (isDefault=1)");
            }
        }
    }

    private Map<Integer, Integer> resolveIsDefault(List<ExecItem> items) {
        Map<Integer, Integer> result = new HashMap<>();

        Map<Integer, List<ExecItem>> siblingGroups = new HashMap<>();
        for (ExecItem item : items) {
            siblingGroups.computeIfAbsent(item.parentNodeId(), k -> new ArrayList<>()).add(item);
        }

        for (List<ExecItem> group : siblingGroups.values()) {
            boolean hasDefault = group.stream().anyMatch(e -> Integer.valueOf(1).equals(e.isDefault()));
            if (hasDefault) {
                group.forEach(e -> result.put(e.nodeId(), Integer.valueOf(1).equals(e.isDefault()) ? 1 : 0));
            } else {
                Integer minNodeId = group.stream()
                        .map(ExecItem::nodeId)
                        .min(Integer::compare)
                        .orElseThrow();
                group.forEach(e -> result.put(e.nodeId(), minNodeId.equals(e.nodeId()) ? 1 : 0));
            }
        }

        return result;
    }
}

package com.iam.app.service.impl;

import com.iam.app.config.context.RequestContext;
import com.iam.app.domain.AuthDefaultResource;
import com.iam.app.domain.AuthPosition;
import com.iam.app.domain.AuthResource;
import com.iam.app.domain.AuthRole;
import com.iam.app.dto.request.BatchUpdateDefaultResourcePermissionRequest;
import com.iam.app.dto.request.CreateDefaultResourcePermissionRequest;
import com.iam.app.dto.response.DefaultResourcePermissionResponse;
import com.iam.app.enums.ErrorCode;
import com.iam.app.exception.BusinessException;
import com.iam.app.repository.jpa.AuthDefaultResourceRepository;
import com.iam.app.repository.jpa.AuthPositionRepository;
import com.iam.app.repository.jpa.AuthResourceRepository;
import com.iam.app.repository.jpa.AuthRoleRepository;
import com.iam.app.service.DefaultResourcePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultResourcePermissionServiceImpl implements DefaultResourcePermissionService {

    private final AuthDefaultResourceRepository resourcePermissionRepository;
    private final AuthRoleRepository roleRepository;
    private final AuthPositionRepository positionRepository;
    private final AuthResourceRepository resourceRepository;

    @Override
    public Page<DefaultResourcePermissionResponse> getPermissions(Long roleId, String positionCode,
                                                                  Long resourceId, String status,
                                                                  Pageable pageable) {
        Page<AuthDefaultResource> page = resourcePermissionRepository.findByFilters(
                roleId, positionCode, resourceId, status, pageable);

        List<AuthDefaultResource> content = page.getContent();
        if (content.isEmpty()) {
            return page.map(entity -> buildResponse(entity, Map.of(), Map.of(), Map.of()));
        }

        List<Long> roleIds = content.stream().map(AuthDefaultResource::getRoleId).distinct().toList();
        List<String> positionCodes = content.stream().map(AuthDefaultResource::getPositionCode).distinct().toList();
        List<Long> resourceIds = content.stream().map(AuthDefaultResource::getResourceId).distinct().toList();

        Map<Long, String> roleNameMap = fetchRoleNameMap(roleIds);
        Map<String, String> positionNameMap = fetchPositionNameMap(positionCodes);
        Map<Long, AuthResource> resourceMap = fetchResourceMap(resourceIds);

        return page.map(entity -> buildResponse(entity, roleNameMap, positionNameMap, resourceMap));
    }

    @Override
    @Transactional
    public List<DefaultResourcePermissionResponse> createPermissions(CreateDefaultResourcePermissionRequest request) {
        List<CreateDefaultResourcePermissionRequest.Item> items = request.getItems();

        // 1. Intra-batch duplicate check
        Set<String> seen = new HashSet<>();
        for (CreateDefaultResourcePermissionRequest.Item item : items) {
            String key = item.getRoleId() + "|" + item.getPositionCode() + "|" + item.getResourceId();
            if (!seen.add(key)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Danh sách có bộ (roleId, positionCode, resourceId) trùng nhau: "
                                + item.getRoleId() + " / " + item.getPositionCode() + " / " + item.getResourceId());
            }
        }

        // 2. Validate reference data (batch)
        List<Long> roleIds = items.stream().map(CreateDefaultResourcePermissionRequest.Item::getRoleId).distinct().toList();
        List<String> positionCodes = items.stream().map(CreateDefaultResourcePermissionRequest.Item::getPositionCode).distinct().toList();
        List<Long> resourceIds = items.stream().map(CreateDefaultResourcePermissionRequest.Item::getResourceId).distinct().toList();

        Map<Long, AuthRole> roleMap = roleRepository.findAllById(roleIds)
                .stream().collect(Collectors.toMap(AuthRole::getId, r -> r));
        for (Long rid : roleIds) {
            if (!roleMap.containsKey(rid)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Không tìm thấy role id=" + rid);
            }
        }

        Map<String, AuthPosition> positionMap = positionRepository.findAllById(positionCodes)
                .stream().collect(Collectors.toMap(AuthPosition::getCode, p -> p));
        for (String code : positionCodes) {
            if (!positionMap.containsKey(code)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Không tìm thấy chức danh: " + code);
            }
        }

        Map<Long, AuthResource> resourceMap = resourceRepository.findAllById(resourceIds)
                .stream().collect(Collectors.toMap(AuthResource::getId, r -> r));
        for (Long resId : resourceIds) {
            if (!resourceMap.containsKey(resId)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Không tìm thấy resource id=" + resId);
            }
        }

        // 3. Validate actions per item
        for (CreateDefaultResourcePermissionRequest.Item item : items) {
            validateActions(item.getActions(), resourceMap.get(item.getResourceId()));
        }

        // 4. DB duplicate check
        Set<String> existingKeys = resourcePermissionRepository.findByRoleIdIn(roleIds).stream()
                .map(p -> p.getRoleId() + "|" + p.getPositionCode() + "|" + p.getResourceId())
                .collect(Collectors.toSet());

        List<String> duplicates = new ArrayList<>();
        for (CreateDefaultResourcePermissionRequest.Item item : items) {
            String key = item.getRoleId() + "|" + item.getPositionCode() + "|" + item.getResourceId();
            if (existingKeys.contains(key)) {
                duplicates.add(item.getRoleId() + " / " + item.getPositionCode() + " / " + item.getResourceId());
            }
        }
        if (!duplicates.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Các bộ quyền đã tồn tại trong hệ thống: " + String.join("; ", duplicates));
        }

        // 5. Save all
        String createdBy = RequestContext.getEmployeeCode();
        List<AuthDefaultResource> toSave = items.stream()
                .map(item -> AuthDefaultResource.builder()
                        .roleId(item.getRoleId())
                        .positionCode(item.getPositionCode())
                        .resourceId(item.getResourceId())
                        .actions(String.join(",", item.getActions()))
                        .status("ACTIVE")
                        .createdBy(createdBy)
                        .build())
                .toList();

        List<AuthDefaultResource> saved = resourcePermissionRepository.saveAll(toSave);

        Map<Long, String> roleNameMap = roleMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));
        Map<String, String> positionNameMap = positionMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));

        return saved.stream()
                .map(entity -> buildResponse(entity,
                        roleNameMap.get(entity.getRoleId()),
                        positionNameMap.get(entity.getPositionCode()),
                        resourceMap.get(entity.getResourceId())))
                .toList();
    }

    @Override
    @Transactional
    public List<DefaultResourcePermissionResponse> updatePermissions(BatchUpdateDefaultResourcePermissionRequest request) {
        List<BatchUpdateDefaultResourcePermissionRequest.Item> items = request.getItems();
        List<Long> ids = items.stream().map(BatchUpdateDefaultResourcePermissionRequest.Item::getId).toList();

        Map<Long, AuthDefaultResource> entityMap = resourcePermissionRepository.findAllById(ids)
                .stream().collect(Collectors.toMap(AuthDefaultResource::getId, e -> e));

        List<Long> notFound = ids.stream().filter(id -> !entityMap.containsKey(id)).toList();
        if (!notFound.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "Không tìm thấy các bản ghi id: " + notFound);
        }

        // Batch load resources for actions validation
        List<Long> resourceIds = entityMap.values().stream()
                .map(AuthDefaultResource::getResourceId).distinct().toList();
        Map<Long, AuthResource> resourceMap = resourceRepository.findAllById(resourceIds)
                .stream().collect(Collectors.toMap(AuthResource::getId, r -> r));

        // Validate and apply updates
        for (BatchUpdateDefaultResourcePermissionRequest.Item item : items) {
            AuthDefaultResource entity = entityMap.get(item.getId());
            AuthResource resource = resourceMap.get(entity.getResourceId());
            validateActions(item.getActions(), resource);
            entity.setStatus(item.getStatus());
            entity.setActions(String.join(",", item.getActions()));
        }

        List<AuthDefaultResource> updated = resourcePermissionRepository.saveAll(entityMap.values());

        List<Long> roleIds = updated.stream().map(AuthDefaultResource::getRoleId).distinct().toList();
        List<String> positionCodes = updated.stream().map(AuthDefaultResource::getPositionCode).distinct().toList();
        List<Long> updatedResourceIds = updated.stream().map(AuthDefaultResource::getResourceId).distinct().toList();

        Map<Long, String> roleNameMap = fetchRoleNameMap(roleIds);
        Map<String, String> positionNameMap = fetchPositionNameMap(positionCodes);
        Map<Long, AuthResource> updatedResourceMap = fetchResourceMap(updatedResourceIds);

        return updated.stream()
                .map(entity -> buildResponse(entity, roleNameMap, positionNameMap, updatedResourceMap))
                .toList();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void validateActions(List<String> requestedActions, AuthResource resource) {
        if (resource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Resource không tồn tại trong hệ thống");
        }

        Set<String> seen = new HashSet<>();
        for (String action : requestedActions) {
            if (!seen.add(action)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Danh sách actions có giá trị trùng nhau: " + action);
            }
        }

        String resourceActions = resource.getActions();
        if (resourceActions == null || resourceActions.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Resource id=" + resource.getId() + " chưa định nghĩa actions");
        }

        Set<String> allowedActions = Arrays.stream(resourceActions.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        Set<String> invalid = requestedActions.stream()
                .filter(a -> !allowedActions.contains(a))
                .collect(Collectors.toSet());
        if (!invalid.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Resource id=" + resource.getId() + " không hỗ trợ action: " + invalid);
        }
    }

    private DefaultResourcePermissionResponse buildResponse(AuthDefaultResource entity,
                                                            Map<Long, String> roleNameMap,
                                                            Map<String, String> positionNameMap,
                                                            Map<Long, AuthResource> resourceMap) {
        AuthResource resource = resourceMap != null ? resourceMap.get(entity.getResourceId()) : null;
        return new DefaultResourcePermissionResponse(
                entity,
                roleNameMap != null ? roleNameMap.get(entity.getRoleId()) : null,
                positionNameMap != null ? positionNameMap.get(entity.getPositionCode()) : null,
                resource != null ? resource.getResourceCode() : null,
                resource != null ? resource.getResourceName() : null);
    }

    private DefaultResourcePermissionResponse buildResponse(AuthDefaultResource entity,
                                                            String roleName,
                                                            String positionName,
                                                            AuthResource resource) {
        return new DefaultResourcePermissionResponse(
                entity,
                roleName,
                positionName,
                resource != null ? resource.getResourceCode() : null,
                resource != null ? resource.getResourceName() : null);
    }

    private Map<Long, String> fetchRoleNameMap(List<Long> roleIds) {
        return roleRepository.findAllById(roleIds).stream()
                .collect(Collectors.toMap(AuthRole::getId, AuthRole::getName));
    }

    private Map<String, String> fetchPositionNameMap(List<String> positionCodes) {
        return positionRepository.findAllById(positionCodes).stream()
                .collect(Collectors.toMap(AuthPosition::getCode, AuthPosition::getName));
    }

    private Map<Long, AuthResource> fetchResourceMap(List<Long> resourceIds) {
        return resourceRepository.findAllById(resourceIds).stream()
                .collect(Collectors.toMap(AuthResource::getId, r -> r));
    }
}

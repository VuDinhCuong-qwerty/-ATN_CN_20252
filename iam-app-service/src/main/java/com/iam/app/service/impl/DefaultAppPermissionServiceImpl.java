package com.iam.app.service.impl;

import com.iam.app.config.context.RequestContext;
import com.iam.app.domain.AuthApplication;
import com.iam.app.domain.AuthDefaultAppPermission;
import com.iam.app.domain.AuthPosition;
import com.iam.app.domain.AuthRole;
import com.iam.app.dto.request.BatchUpdateDefaultAppPermissionStatusRequest;
import com.iam.app.dto.request.CreateDefaultAppPermissionRequest;
import com.iam.app.dto.response.DefaultAppPermissionResponse;
import com.iam.app.enums.ErrorCode;
import com.iam.app.exception.BusinessException;
import com.iam.app.repository.jpa.AuthApplicationRepository;
import com.iam.app.repository.jpa.AuthDefaultAppPermissionRepository;
import com.iam.app.repository.jpa.AuthPositionRepository;
import com.iam.app.repository.jpa.AuthRoleRepository;
import com.iam.app.service.DefaultAppPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAppPermissionServiceImpl implements DefaultAppPermissionService {

    private final AuthDefaultAppPermissionRepository permissionRepository;
    private final AuthRoleRepository roleRepository;
    private final AuthPositionRepository positionRepository;
    private final AuthApplicationRepository applicationRepository;

    @Override
    public Page<DefaultAppPermissionResponse> getPermissions(String roleId, String positionCode,
                                                             Long applicationId, String status,
                                                             Pageable pageable) {
        Page<AuthDefaultAppPermission> page = permissionRepository.findByFilters(
                roleId, positionCode, applicationId, status, pageable);

        List<AuthDefaultAppPermission> content = page.getContent();
        if (content.isEmpty()) {
            return page.map(entity -> buildResponse(entity, Map.of(), Map.of(), Map.of()));
        }

        List<String> roleIds = content.stream().map(AuthDefaultAppPermission::getRoleId).distinct().toList();
        List<String> positionCodes = content.stream().map(AuthDefaultAppPermission::getPositionCode).distinct().toList();
        List<Long> appIds = content.stream().map(AuthDefaultAppPermission::getApplicationId).distinct().toList();

        Map<String, String> roleNameMap = fetchRoleNameMap(roleIds);
        Map<String, String> positionNameMap = fetchPositionNameMap(positionCodes);
        Map<Long, String> appNameMap = fetchAppNameMap(appIds);

        return page.map(entity -> buildResponse(entity, roleNameMap, positionNameMap, appNameMap));
    }

    @Override
    @Transactional
    public List<DefaultAppPermissionResponse> createPermissions(CreateDefaultAppPermissionRequest request) {
        List<CreateDefaultAppPermissionRequest.Item> items = request.getItems();

        // 1. Intra-batch duplicate check
        Set<String> seen = new HashSet<>();
        for (CreateDefaultAppPermissionRequest.Item item : items) {
            String key = item.getRoleId() + "|" + item.getPositionCode() + "|" + item.getApplicationId();
            if (!seen.add(key)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Danh sách có bộ (roleId, positionCode, applicationId) trùng nhau: "
                                + item.getRoleId() + " / " + item.getPositionCode() + " / " + item.getApplicationId());
            }
        }

        // 2. Validate reference data (batch)
        List<String> roleIds = items.stream().map(CreateDefaultAppPermissionRequest.Item::getRoleId).distinct().toList();
        List<String> positionCodes = items.stream().map(CreateDefaultAppPermissionRequest.Item::getPositionCode).distinct().toList();
        List<Long> appIds = items.stream().map(CreateDefaultAppPermissionRequest.Item::getApplicationId).distinct().toList();

        Map<String, AuthRole> roleMap = roleRepository.findByCodes(roleIds)
                .stream().collect(Collectors.toMap(AuthRole::getCode, r -> r));
        for (String rid : roleIds) {
            if (!roleMap.containsKey(rid)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Không tìm thấy role: " + rid);
            }
        }

        Map<String, AuthPosition> positionMap = positionRepository.findAllById(positionCodes)
                .stream().collect(Collectors.toMap(AuthPosition::getCode, p -> p));
        for (String code : positionCodes) {
            if (!positionMap.containsKey(code)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Không tìm thấy chức danh: " + code);
            }
        }

        Map<Long, AuthApplication> appMap = applicationRepository.findAllById(appIds)
                .stream().collect(Collectors.toMap(AuthApplication::getId, a -> a));
        for (Long appId : appIds) {
            if (!appMap.containsKey(appId)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Không tìm thấy ứng dụng id=" + appId);
            }
        }

        // 3. DB duplicate check — load all existing records for these roleIds, match combo in memory
        Set<String> existingKeys = permissionRepository.findByRoleIdIn(roleIds).stream()
                .map(p -> p.getRoleId() + "|" + p.getPositionCode() + "|" + p.getApplicationId())
                .collect(Collectors.toSet());

        List<String> duplicates = new ArrayList<>();
        for (CreateDefaultAppPermissionRequest.Item item : items) {
            String key = item.getRoleId() + "|" + item.getPositionCode() + "|" + item.getApplicationId();
            if (existingKeys.contains(key)) {
                duplicates.add(key.replace("|", " / "));
            }
        }
        if (!duplicates.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Các bộ quyền đã tồn tại trong hệ thống: " + String.join("; ", duplicates));
        }

        // 4. Save all
        String createdBy = RequestContext.getEmployeeCode();
        List<AuthDefaultAppPermission> toSave = items.stream()
                .map(item -> AuthDefaultAppPermission.builder()
                        .roleId(item.getRoleId())
                        .positionCode(item.getPositionCode())
                        .applicationId(item.getApplicationId())
                        .status("ACTIVE")
                        .createdBy(createdBy)
                        .build())
                .toList();

        List<AuthDefaultAppPermission> saved = permissionRepository.saveAll(toSave);

        Map<String, String> roleNameMap = roleMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));
        Map<String, String> positionNameMap = positionMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));
        Map<Long, String> appNameMap = appMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));

        return saved.stream()
                .map(entity -> new DefaultAppPermissionResponse(
                        entity,
                        roleNameMap.get(entity.getRoleId()),
                        positionNameMap.get(entity.getPositionCode()),
                        appNameMap.get(entity.getApplicationId())))
                .toList();
    }

    @Override
    @Transactional
    public List<DefaultAppPermissionResponse> updateStatuses(BatchUpdateDefaultAppPermissionStatusRequest request) {
        List<BatchUpdateDefaultAppPermissionStatusRequest.Item> items = request.getItems();
        List<Long> ids = items.stream().map(BatchUpdateDefaultAppPermissionStatusRequest.Item::getId).toList();

        Map<Long, AuthDefaultAppPermission> entityMap = permissionRepository.findAllById(ids)
                .stream().collect(Collectors.toMap(AuthDefaultAppPermission::getId, e -> e));

        List<Long> notFound = ids.stream().filter(id -> !entityMap.containsKey(id)).toList();
        if (!notFound.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "Không tìm thấy các bản ghi id: " + notFound);
        }

        for (BatchUpdateDefaultAppPermissionStatusRequest.Item item : items) {
            entityMap.get(item.getId()).setStatus(item.getStatus());
        }

        List<AuthDefaultAppPermission> updated = permissionRepository.saveAll(entityMap.values());

        List<String> roleIds = updated.stream().map(AuthDefaultAppPermission::getRoleId).distinct().toList();
        List<String> positionCodes = updated.stream().map(AuthDefaultAppPermission::getPositionCode).distinct().toList();
        List<Long> appIds = updated.stream().map(AuthDefaultAppPermission::getApplicationId).distinct().toList();

        Map<String, String> roleNameMap = fetchRoleNameMap(roleIds);
        Map<String, String> positionNameMap = fetchPositionNameMap(positionCodes);
        Map<Long, String> appNameMap = fetchAppNameMap(appIds);

        return updated.stream()
                .map(entity -> new DefaultAppPermissionResponse(
                        entity,
                        roleNameMap.get(entity.getRoleId()),
                        positionNameMap.get(entity.getPositionCode()),
                        appNameMap.get(entity.getApplicationId())))
                .toList();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private DefaultAppPermissionResponse buildResponse(AuthDefaultAppPermission entity,
                                                       Map<String, String> roleNameMap,
                                                       Map<String, String> positionNameMap,
                                                       Map<Long, String> appNameMap) {
        return new DefaultAppPermissionResponse(
                entity,
                roleNameMap.get(entity.getRoleId()),
                positionNameMap.get(entity.getPositionCode()),
                appNameMap.get(entity.getApplicationId()));
    }

    private Map<String, String> fetchRoleNameMap(List<String> roleIds) {
        return roleRepository.findByCodes(roleIds).stream()
                .collect(Collectors.toMap(AuthRole::getCode, AuthRole::getName));
    }

    private Map<String, String> fetchPositionNameMap(List<String> positionCodes) {
        return positionRepository.findAllById(positionCodes).stream()
                .collect(Collectors.toMap(AuthPosition::getCode, AuthPosition::getName));
    }

    private Map<Long, String> fetchAppNameMap(List<Long> appIds) {
        return applicationRepository.findAllById(appIds).stream()
                .collect(Collectors.toMap(AuthApplication::getId, AuthApplication::getName));
    }
}

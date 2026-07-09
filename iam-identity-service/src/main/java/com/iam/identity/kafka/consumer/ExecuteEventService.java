package com.iam.identity.kafka.consumer;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.iam.identity.domain.AuthAppPermission;
import com.iam.identity.domain.AuthDefaultAppPermission;
import com.iam.identity.domain.AuthDefaultResource;
import com.iam.identity.domain.AuthUserResource;
import com.iam.identity.kafka.event.payload.DefaultPermissionCreatedPayload;
import com.iam.identity.kafka.event.payload.UserCreatedPermissionPayload;
import com.iam.identity.repository.jpa.AuthAppPermissionRepository;
import com.iam.identity.repository.jpa.AuthDefaultAppPermissionRepository;
import com.iam.identity.repository.jpa.AuthDefaultResourceRepository;
import com.iam.identity.repository.jpa.AuthUserResourceRepository;
import com.iam.identity.repository.jpa.AuthUserRoleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecuteEventService {

    private final AuthDefaultAppPermissionRepository defaultAppPermissionRepository;
    private final AuthDefaultResourceRepository defaultResourceRepository;
    private final AuthAppPermissionRepository appPermissionRepository;
    private final AuthUserResourceRepository userResourceRepository;
    private final AuthUserRoleRepository userRoleRepository;

    @SuppressWarnings("null")
    @Transactional
    public void executeGrantDefaultEvent(UserCreatedPermissionPayload payload) {
        List<String> roles = payload.getRoles();
        String position = payload.getPositionCode();
        Long userId = payload.getUserId();

        List<AuthDefaultAppPermission> defaultApps = defaultAppPermissionRepository
                .getDefaultAppByRoleAndPosition(roles, position);
        if (defaultApps != null) {
            for (AuthDefaultAppPermission defaultApp : defaultApps) {
                grantAppPermission(userId, defaultApp.getApplicationId());
            }
        }
        // log gán app thành công

        List<AuthDefaultResource> defaultResources = defaultResourceRepository
                .getDefaultResourceByRoleAndPosition(roles, position);
        if (defaultResources == null || defaultResources.isEmpty()) {
            // xử lý xong
            return;
        }

        // Bước 1: gom tất cả actions theo resourceId, merge union nếu nhiều role cùng cấp 1 resource
        Map<Long, Set<String>> resourceActionMap = new LinkedHashMap<>();
        for (AuthDefaultResource item : defaultResources) {
            Long resourceId = item.getResourceId();
            Set<String> actions = resourceActionMap.computeIfAbsent(resourceId, k -> new LinkedHashSet<>());
            if (item.getActions() != null) {
                for (String action : item.getActions().split(",")) {
                    String trimmed = action.trim();
                    if (!trimmed.isEmpty()) actions.add(trimmed);
                }
            }
        }

        // Bước 2: apply từng resourceId vào DB
        for (Map.Entry<Long, Set<String>> entry : resourceActionMap.entrySet()) {
            grantResourceActions(userId, entry.getKey(), entry.getValue());
        }
        // log thành công
    }

    /**
     * Backfill quyền cho user hiện có khi có 1 default-permission MỚI được tạo (không phải khi có user event).
     * Chiều ngược lại với {@link #executeGrantDefaultEvent}: từ 1 (role, position) tìm N user thay vì từ 1
     * user tìm N default-permission. Dùng chung logic insert-or-reactivate với luồng cũ qua
     * {@link #grantAppPermission} / {@link #grantResourceActions}.
     */
    @SuppressWarnings("null")
    @Transactional
    public void executeBackfillEvent(DefaultPermissionCreatedPayload payload) {
        List<Long> userIds = userRoleRepository.findActiveUserIdsByRoleIdAndPosition(
                payload.getRoleId(), payload.getPositionCode());
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        if ("APP".equals(payload.getPermissionType())) {
            for (Long userId : userIds) {
                grantAppPermission(userId, payload.getApplicationId());
            }
        } else if ("RESOURCE".equals(payload.getPermissionType())) {
            Set<String> actions = new LinkedHashSet<>(payload.getActions());
            for (Long userId : userIds) {
                grantResourceActions(userId, payload.getResourceId(), actions);
            }
        } else {
            log.warn("Unknown permissionType in DefaultPermissionCreatedPayload: {}", payload.getPermissionType());
        }
    }

    /**
     * Insert-or-reactivate 1 app permission cho 1 user. Tách ra từ {@link #executeGrantDefaultEvent}
     * để dùng chung với {@code DefaultPermissionBackfillConsumer} (backfill ngược — từ 1
     * default-permission mới tìm N user, thay vì từ 1 user tìm N default-permission).
     * Idempotent: gọi lại nhiều lần với cùng (userId, appId) không tạo trùng dòng.
     */
    @Transactional
    public void grantAppPermission(Long userId, Long appId) {
        List<AuthAppPermission> existing = appPermissionRepository.findByUserIdAndAppId(userId, appId);
        if (existing.isEmpty()) {
            appPermissionRepository.save(AuthAppPermission.builder()
                    .userId(userId).appId(appId)
                    .status("ACTIVE")
                    .grantedBy("system").grantedAt(LocalDateTime.now())
                    .revokedBy(null).revokedAt(null)
                    .grantSource("SYSTEM").requestId(null)
                    .inactiveFromDate(null).inactiveToDate(null)
                    .build());
        } else {
            AuthAppPermission record = existing.get(0);
            if ("REVOKED".equals(record.getStatus())) {
                record.setStatus("ACTIVE");
                record.setGrantedBy("system");
                record.setGrantedAt(LocalDateTime.now());
                record.setRevokedBy(null);
                record.setRevokedAt(null);
                record.setGrantSource("SYSTEM");
                record.setInactiveFromDate(null);
                record.setInactiveToDate(null);
                appPermissionRepository.save(record);
            }
            // ACTIVE hoặc SUSPENDED: đã có quyền → bỏ qua
        }
    }

    /**
     * Insert-or-reactivate 1 resource permission (union actions) cho 1 user. Tách ra từ
     * {@link #executeGrantDefaultEvent} — xem ghi chú ở {@link #grantAppPermission}.
     */
    @Transactional
    public void grantResourceActions(Long userId, Long resourceId, Set<String> actions) {
        String mergedActions = String.join(",", actions);

        List<AuthUserResource> existing = userResourceRepository.findByUserIdAndResourceId(userId, resourceId);
        if (existing.isEmpty()) {
            AuthUserResource newResource = AuthUserResource.builder()
                    .userId(userId).resourceId(resourceId)
                    .action(mergedActions).status("ACTIVE")
                    .grantedBy("system").grantedAt(LocalDateTime.now())
                    .grantSource("SYSTEM")
                    .expiredAt(null).revokedBy(null).revokedAt(null)
                    .requestId(null).inactiveFromDate(null).inactiveToDate(null)
                    .build();
            userResourceRepository.save(newResource);
        } else {
            AuthUserResource record = existing.get(0);
            if ("REVOKED".equals(record.getStatus())) {
                record.setStatus("ACTIVE");
                record.setAction(mergedActions);
                record.setGrantedBy("system");
                record.setGrantedAt(LocalDateTime.now());
                record.setRevokedBy(null);
                record.setRevokedAt(null);
                record.setGrantSource("SYSTEM");
                record.setInactiveFromDate(null);
                record.setInactiveToDate(null);
                userResourceRepository.save(record);
            } else {
                // ACTIVE: bổ sung action còn thiếu (không overwrite action đã có)
                Set<String> existingActions = new LinkedHashSet<>();
                if (record.getAction() != null) {
                    for (String a : record.getAction().split(",")) {
                        String trimmed = a.trim();
                        if (!trimmed.isEmpty()) existingActions.add(trimmed);
                    }
                }
                boolean changed = existingActions.addAll(actions);
                if (changed) {
                    record.setAction(String.join(",", existingActions));
                    userResourceRepository.save(record);
                }
            }
        }
    }

}

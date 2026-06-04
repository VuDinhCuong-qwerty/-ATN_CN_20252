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
import com.iam.identity.kafka.event.payload.UserCreatedPermissionPayload;
import com.iam.identity.repository.jpa.AuthAppPermissionRepository;
import com.iam.identity.repository.jpa.AuthDefaultAppPermissionRepository;
import com.iam.identity.repository.jpa.AuthDefaultResourceRepository;
import com.iam.identity.repository.jpa.AuthUserResourceRepository;

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

    @SuppressWarnings("null")
    @Transactional
    public void executeGrantDefaultEvent(UserCreatedPermissionPayload payload) {
        List<String> roles = payload.getRoles();
        String position = payload.getPositionCode();
        Long userId = payload.getUserId();

        List<AuthDefaultAppPermission> defaultApps = defaultAppPermissionRepository
                .getDefaultAppByRoleAndPosition(roles, position);
        if (defaultApps == null || defaultApps.isEmpty()) {
            // xử lý xong và return
            return;
        }

        for (AuthDefaultAppPermission defaultApp : defaultApps) {
            Long appId = defaultApp.getApplicationId();
            List<AuthAppPermission> existing = appPermissionRepository.findByUserIdAndAppId(userId, appId);
            if (existing.isEmpty()) {
                appPermissionRepository.save(AuthAppPermission.builder()
                        .userId(userId).appId(appId)
                        .status("ACTIVE")
                        .grantedBy("system").grantedAt(LocalDateTime.now())
                        .revokedBy(null).revokedAt(null)
                        .grantSource("AUTO").requestId(null)
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
                    record.setGrantSource("AUTO");
                    record.setInactiveFromDate(null);
                    record.setInactiveToDate(null);
                    appPermissionRepository.save(record);
                }
                // ACTIVE hoặc SUSPENDED: đã có quyền → bỏ qua
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
            Long resourceId = entry.getKey();
            String mergedActions = String.join(",", entry.getValue());

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
                    boolean changed = existingActions.addAll(entry.getValue());
                    if (changed) {
                        record.setAction(String.join(",", existingActions));
                        userResourceRepository.save(record);
                    }
                }
                
            }
        }
        // log thành công
    }

}

package com.iam.identity.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import com.iam.identity.config.KafkaConfig;
import com.iam.identity.config.context.RequestContext;
import com.iam.identity.domain.AuthAppPermission;
import com.iam.identity.domain.AuthApplication;
import com.iam.identity.domain.AuthDefaultAppPermission;
import com.iam.identity.domain.AuthDefaultResource;
import com.iam.identity.domain.AuthRequestDetail;
import com.iam.identity.domain.AuthRequestHeader;
import com.iam.identity.domain.AuthResource;
import com.iam.identity.domain.AuthRole;
import com.iam.identity.domain.AuthUser;
import com.iam.identity.domain.AuthUserProfile;
import com.iam.identity.domain.AuthUserResource;
import com.iam.identity.domain.AuthUserRole;
import com.iam.identity.dto.pojo.PermissionRequest;
import com.iam.identity.dto.pojo.ResourceRow;
import com.iam.identity.dto.pojo.UserInfoRow;
import com.iam.identity.dto.request.ApprovePermissionRequest;
import com.iam.identity.dto.request.AssignRoleRequest;
import com.iam.identity.dto.request.CreatePermissionRequestRequest;
import com.iam.identity.dto.request.RejectPermissionRequest;
import com.iam.identity.dto.request.RevokeAppPermissionRequest;
import com.iam.identity.dto.request.RevokeResourcePermissionRequest;
import com.iam.identity.dto.request.SubmitRequest;
import com.iam.identity.dto.request.UpdatePermissionRequestRequest;
import com.iam.identity.dto.response.AppPermissionResponse;
import com.iam.identity.dto.response.GetAllPermissionRequestResponse;
import com.iam.identity.dto.response.GetDetailPermissionRequest;
import com.iam.identity.dto.response.PermissionRequestResponse;
import com.iam.identity.dto.response.ResourcePermissionResponse;
import com.iam.identity.dto.response.RevokeAppPermissionResponse;
import com.iam.identity.dto.response.RevokeResourcePermissionResponse;
import com.iam.identity.dto.response.UpdatePermisssionRequestResponse;
import com.iam.identity.dto.response.UserRoleResponse;
import com.iam.identity.enums.ErrorCode;
import com.iam.identity.exception.BusinessException;
import com.iam.identity.kafka.event.payload.AppPermissionRevokePayload;
import com.iam.identity.kafka.event.payload.PermissionApprovedPayload;
import com.iam.identity.kafka.event.payload.PermissionRequestCreatedPayload;
import com.iam.identity.kafka.event.payload.UserCreatedPermissionPayload;
import com.iam.identity.kafka.producer.IdentityEventProducer;
import com.iam.identity.repository.cache.DataCached;
import com.iam.identity.repository.jpa.AuthAppPermissionRepository;
import com.iam.identity.repository.jpa.AuthApplicationRepository;
import com.iam.identity.repository.jpa.AuthRepository;
import com.iam.identity.repository.jpa.AuthRequestDetailRepository;
import com.iam.identity.repository.jpa.AuthRequestHeaderRepository;
import com.iam.identity.repository.jpa.AuthResourceRepository;
import com.iam.identity.repository.jpa.AuthDefaultAppPermissionRepository;
import com.iam.identity.repository.jpa.AuthDefaultResourceRepository;
import com.iam.identity.repository.jpa.AuthRoleRepository;
import com.iam.identity.repository.jpa.AuthUserProfileRepository;
import com.iam.identity.repository.jpa.AuthUserRepository;
import com.iam.identity.repository.jpa.AuthUserResourceRepository;
import com.iam.identity.repository.jpa.AuthUserRoleRepository;
import com.iam.identity.service.UserPermissionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserPermissionServiceImpl implements UserPermissionService {

    private final AuthUserRepository userRepository;
    private final AuthAppPermissionRepository appPermissionRepository;
    private final AuthUserResourceRepository userResourceRepository;
    private final AuthApplicationRepository applicationRepository;
    private final AuthUserRoleRepository userRoleRepository;
    private final AuthResourceRepository resourceRepository;
    private final AuthRequestHeaderRepository requestHeaderRepository;
    private final AuthRequestDetailRepository requestDetailRepository;
    private final AuthRepository authRepository;
    private final IdentityEventProducer identityEventProducer;
    private final DataCached dataCached;
    private final AuthRoleRepository authRoleRepository;
    private final AuthDefaultAppPermissionRepository defaultAppPermissionRepository;
    private final AuthDefaultResourceRepository defaultResourceRepository;
    private final AuthUserProfileRepository authUserProfileRepository;

    // ── Phase 4.1: Role ───────────────────────────────────────────────────────

    @Override
    public Page<UserRoleResponse> getUserRoles(String employeeCode, Pageable pageable) {
        List<AuthUser> users = userRepository.findUserByEmployeeCode(employeeCode);
        if (ObjectUtils.isEmpty(users))
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "EmployeeCode is invalid");
        AuthUser user = users.get(0);
        Long userId = user.getId();

        Page<AuthUserRole> rolePage = userRoleRepository.findPagedByUserIdAndStatus(userId, "ACTIVE", pageable);

        Set<Long> roleIds = rolePage.getContent().stream()
                .map(AuthUserRole::getRoleId).collect(Collectors.toSet());
        Map<Long, AuthRole> roleMap = roleIds.isEmpty() ? Collections.emptyMap()
                : authRoleRepository.findAllById(roleIds).stream()
                        .collect(Collectors.toMap(AuthRole::getId, r -> r));

        return rolePage.map(ur -> {
            AuthRole role = roleMap.get(ur.getRoleId());
            return UserRoleResponse.builder()
                    .userId(userId)
                    .username(user.getUsername())
                    .userRoleId(ur.getId())
                    .roleId(ur.getRoleId())
                    .roleCode(role != null ? role.getCode() : null)
                    .roleName(role != null ? role.getName() : null)
                    .grantedAt(ur.getGrantedAt())
                    .expiredAt(ur.getExpiredAt())
                    .status(ur.getStatus())
                    .build();
        });
    }

    @Override
    @Transactional
    public UserRoleResponse assignRole(String employeeCode, AssignRoleRequest request) {
        // 1. Load user — check tồn tại + ACTIVE
        List<AuthUser> users = userRepository.findUserByEmployeeCode(employeeCode);
        if (ObjectUtils.isEmpty(users)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "EmployeeCode is invalid");
        }
        AuthUser user = users.get(0);
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "User is not active");
        }
        Long userId = user.getId();

        // 2. Validate roleCode tồn tại
        List<AuthRole> matchedRoles = authRoleRepository.getRolesByCode(List.of(request.getRoleCode()));
        if (ObjectUtils.isEmpty(matchedRoles)) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND.getCode(), ErrorCode.ROLE_NOT_FOUND.getDesc());
        }
        AuthRole role = matchedRoles.get(0);

        // 3. Check chưa được gán role này
        if (userRoleRepository.existsByUserIdAndRoleIdAndStatus(userId, role.getId(), "ACTIVE")) {
            throw new BusinessException(ErrorCode.ROLE_ALREADY_ASSIGNED.getCode(),
                    ErrorCode.ROLE_ALREADY_ASSIGNED.getDesc());
        }

        // 4. Reactivate REVOKED record nếu tồn tại, tránh vi phạm UNIQUE(USER_ID, ROLE_ID)
        AuthUserRole saved;
        java.util.Optional<AuthUserRole> existingRole = userRoleRepository.findByUserIdAndRoleId(userId, role.getId());
        if (existingRole.isPresent()) {
            AuthUserRole userRole = existingRole.get();
            userRole.setStatus("ACTIVE");
            userRole.setGrantedBy(RequestContext.getEmployeeCode());
            userRole.setExpiredAt(null);
            saved = userRoleRepository.save(userRole);
        } else {
            saved = userRoleRepository.save(AuthUserRole.builder()
                    .userId(userId)
                    .roleId(role.getId())
                    .grantedBy(RequestContext.getEmployeeCode())
                    .build());
        }

        // 5. Load profile để lấy positionCode + departmentId cho event
        List<AuthUserProfile> profiles = authUserProfileRepository.findByEmployeeCode(employeeCode);
        String positionCode = ObjectUtils.isEmpty(profiles) ? null : profiles.get(0).getPosition();
        Long departmentId = ObjectUtils.isEmpty(profiles) ? null : profiles.get(0).getDepartmentId();

        // 6. Publish DEFAULT-GRANT-PERMISSION-USER — consumer gán quyền mặc định theo
        // (role, position)
        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_DEFAULT_GRANT_PERMISSION_USER,
                    "ROLE_ASSIGNED",
                    UserCreatedPermissionPayload.builder()
                            .userId(userId)
                            .roles(List.of(role.getCode()))
                            .positionCode(positionCode)
                            .departmentId(departmentId != null ? String.valueOf(departmentId) : null)
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish default grant event for assignRole userId={}: {}", userId, e.getMessage());
        }

        return UserRoleResponse.builder()
                .userId(userId)
                .username(user.getUsername())
                .userRoleId(saved.getId())
                .roleId(role.getId())
                .roleCode(role.getCode())
                .roleName(role.getName())
                .grantedAt(saved.getGrantedAt())
                .expiredAt(saved.getExpiredAt())
                .status(saved.getStatus())
                .build();
    }

    @Override
    @Transactional
    public void revokeRole(String employeeCode, String roleCode) {
        // 1. Load user — check tồn tại + ACTIVE
        List<AuthUser> users = userRepository.findUserByEmployeeCode(employeeCode);
        if (ObjectUtils.isEmpty(users)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "EmployeeCode is invalid");
        }
        AuthUser user = users.get(0);
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "User is not active");
        }
        Long userId = user.getId();

        // 2. Validate role code tồn tại
        List<AuthRole> matchedRoles = authRoleRepository.getRolesByCode(List.of(roleCode));
        if (ObjectUtils.isEmpty(matchedRoles)) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND.getCode(), ErrorCode.ROLE_NOT_FOUND.getDesc());
        }
        AuthRole role = matchedRoles.get(0);

        // 3. Check role đã được gán cho user và đang ACTIVE
        if (!userRoleRepository.existsByUserIdAndRoleIdAndStatus(userId, role.getId(), "ACTIVE")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                    "Role is not assigned to user or already revoked");
        }

        LocalDateTime now = LocalDateTime.now();

        // 4. Load positionCode từ profile
        List<AuthUserProfile> profiles = authUserProfileRepository.findByEmployeeCode(employeeCode);
        String positionCode = ObjectUtils.isEmpty(profiles) ? null : profiles.get(0).getPosition();

        // 5. Load toàn bộ ACTIVE roles hiện tại (dùng cho cả tính diff và revoke)
        List<AuthUserRole> allActiveUserRoles = userRoleRepository.findByUserIdAndStatus(userId, "ACTIVE");

        List<Long> revokedAppIdsForKafka = new ArrayList<>();

        if (positionCode == null) {
            log.warn("revokeRole: positionCode is null for user {}, skipping permission cascade", employeeCode);
        } else {
            // 6. Tính remaining role codes (loại trừ role đang revoke)
            List<Long> remainingRoleIds = allActiveUserRoles.stream()
                    .map(AuthUserRole::getRoleId)
                    .filter(id -> !id.equals(role.getId()))
                    .collect(Collectors.toList());
            List<String> remainingRoleCodes = remainingRoleIds.isEmpty() ? Collections.emptyList()
                    : authRoleRepository.findAllById(remainingRoleIds).stream()
                            .map(AuthRole::getCode).collect(Collectors.toList());

            // 7. Cascade APP: diff defaults → revoke apps không còn được cover + cascade
            // resources
            Set<Long> revokedAppDefaults = defaultAppPermissionRepository
                    .getDefaultAppByRoleAndPosition(List.of(roleCode), positionCode).stream()
                    .map(AuthDefaultAppPermission::getApplicationId).collect(Collectors.toSet());

            Set<Long> remainingAppDefaults = remainingRoleCodes.isEmpty() ? Collections.emptySet()
                    : defaultAppPermissionRepository.getDefaultAppByRoleAndPosition(remainingRoleCodes, positionCode)
                            .stream()
                            .map(AuthDefaultAppPermission::getApplicationId).collect(Collectors.toSet());

            Set<Long> appIdsToRevoke = revokedAppDefaults.stream()
                    .filter(id -> !remainingAppDefaults.contains(id)).collect(Collectors.toSet());

            if (!appIdsToRevoke.isEmpty()) {
                List<AuthAppPermission> appsToRevoke = appPermissionRepository
                        .findAllActiveByUserIdAndAppIdIn(userId, appIdsToRevoke).stream()
                        .filter(p -> !"REQUEST".equals(p.getGrantSource()))
                        .collect(Collectors.toList());

                if (!appsToRevoke.isEmpty()) {
                    appsToRevoke.forEach(p -> {
                        p.setStatus("REVOKED");
                        p.setRevokedAt(now);
                    });
                    appPermissionRepository.saveAll(appsToRevoke);

                    // Cascade: revoke resources thuộc các app vừa revoke
                    Set<Long> actualRevokedAppIds = appsToRevoke.stream()
                            .map(AuthAppPermission::getAppId).collect(Collectors.toSet());
                    revokedAppIdsForKafka.addAll(actualRevokedAppIds);
                    List<AuthUserResource> cascadeToRevoke = userResourceRepository
                            .findActiveByUserIdAndAppIdIn(userId, actualRevokedAppIds).stream()
                            .filter(r -> !"REQUEST".equals(r.getGrantSource()))
                            .collect(Collectors.toList());
                    if (!cascadeToRevoke.isEmpty()) {
                        cascadeToRevoke.forEach(r -> {
                            r.setStatus("REVOKED");
                            r.setRevokedAt(now);
                        });
                        userResourceRepository.saveAll(cascadeToRevoke);
                    }
                }
            }

            // 8. Cascade RESOURCE: diff defaults theo từng action
            List<AuthDefaultResource> revokedResDefs = defaultResourceRepository
                    .getDefaultResourceByRoleAndPosition(List.of(roleCode), positionCode);

            if (!revokedResDefs.isEmpty()) {
                // Map resourceId → Set<action> của role bị revoke
                Map<Long, Set<String>> revokedResActions = new HashMap<>();
                for (AuthDefaultResource dr : revokedResDefs) {
                    revokedResActions.put(dr.getResourceId(),
                            Arrays.stream(dr.getActions().split(","))
                                    .map(String::trim).filter(s -> !s.isEmpty())
                                    .collect(Collectors.toSet()));
                }

                // Map resourceId → union Set<action> của các roles còn lại
                Map<Long, Set<String>> remainingResActions = new HashMap<>();
                if (!remainingRoleCodes.isEmpty()) {
                    for (AuthDefaultResource dr : defaultResourceRepository
                            .getDefaultResourceByRoleAndPosition(remainingRoleCodes, positionCode)) {
                        Set<String> actions = Arrays.stream(dr.getActions().split(","))
                                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
                        remainingResActions.merge(dr.getResourceId(), actions, (a, b) -> {
                            Set<String> merged = new HashSet<>(a);
                            merged.addAll(b);
                            return merged;
                        });
                    }
                }

                // Tính diff per resource per action
                Map<Long, Set<String>> resourceActionsToRevoke = new HashMap<>();
                for (Map.Entry<Long, Set<String>> entry : revokedResActions.entrySet()) {
                    Set<String> diff = new HashSet<>(entry.getValue());
                    diff.removeAll(remainingResActions.getOrDefault(entry.getKey(), Collections.emptySet()));
                    if (!diff.isEmpty())
                        resourceActionsToRevoke.put(entry.getKey(), diff);
                }

                if (!resourceActionsToRevoke.isEmpty()) {
                    List<AuthUserResource> toRevokeResources = userResourceRepository
                            .findActiveByUserIdAndResourceIds(userId, resourceActionsToRevoke.keySet()).stream()
                            .filter(r -> !"REQUEST".equals(r.getGrantSource()))
                            .filter(r -> {
                                Set<String> act = resourceActionsToRevoke.get(r.getResourceId());
                                return act != null && act.contains(r.getAction());
                            })
                            .collect(Collectors.toList());
                    if (!toRevokeResources.isEmpty()) {
                        toRevokeResources.forEach(r -> {
                            r.setStatus("REVOKED");
                            r.setRevokedAt(now);
                        });
                        userResourceRepository.saveAll(toRevokeResources);
                    }
                }
            }
        }

        // 9. Revoke the role record
        allActiveUserRoles.stream()
                .filter(ur -> ur.getRoleId().equals(role.getId()))
                .findFirst()
                .ifPresent(ur -> {
                    ur.setStatus("REVOKED");
                    userRoleRepository.save(ur);
                });

        // 10. Notify auth-service revoke session/token cho các app thực sự bị thu hồi
        if (!revokedAppIdsForKafka.isEmpty()) {
            try {
                identityEventProducer.publish(
                        KafkaConfig.TOPIC_REVOKED_PERMISSION_NOTIFY,
                        "REVOKED_APP_PERMISSION",
                        AppPermissionRevokePayload.builder()
                                .userId(userId)
                                .employeeCode(employeeCode)
                                .revokedAppIds(revokedAppIdsForKafka)
                                .revokedAt(now)
                                .build());
            } catch (Exception e) {
                log.error("Failed to publish role revoke notify for userId={}: {}", userId, e.getMessage());
            }
        }
    }

    // ── Phase 4.2: App Permission ─────────────────────────────────────────────

    @Override
    public Page<AppPermissionResponse> getAppPermissions(String employeeCode, Pageable pageable) {
        List<AuthUser> users = userRepository.findUserByEmployeeCode(employeeCode);
        if (ObjectUtils.isEmpty(users)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "EmployeeCode is invalid");
        }
        Long userId = users.get(0).getId();

        Page<AuthAppPermission> permPage = appPermissionRepository.findByUserIdAndStatus(userId, "ACTIVE", pageable);

        Set<Long> appIds = permPage.getContent().stream()
                .map(AuthAppPermission::getAppId)
                .collect(Collectors.toSet());

        Map<Long, AuthApplication> appMap = appIds.isEmpty() ? Collections.emptyMap()
                : applicationRepository.findAppsById(appIds).stream()
                        .collect(Collectors.toMap(AuthApplication::getId, a -> a));

        return permPage.map(perm -> {
            AuthApplication app = appMap.get(perm.getAppId());
            return AppPermissionResponse.builder()
                    .permissionId(perm.getId())
                    .appId(perm.getAppId())
                    .appName(app != null ? app.getName() : null)
                    .serviceCode(app != null ? app.getServiceCode() : null)
                    .appType(app != null ? app.getAppType() : null)
                    .status(perm.getStatus())
                    .grantSource(perm.getGrantSource())
                    .grantedAt(perm.getGrantedAt())
                    .expiredAt(perm.getExpiredAt())
                    .inactiveFromDate(perm.getInactiveFromDate())
                    .inactiveToDate(perm.getInactiveToDate())
                    .build();
        });
    }

    @Override
    @Transactional
    public RevokeAppPermissionResponse revokeAppPermission(String employeeCode, RevokeAppPermissionRequest request) {

        LocalDateTime now = LocalDateTime.now();
        List<AuthUser> authUsers = userRepository.findUserByEmployeeCode(employeeCode);
        if (ObjectUtils.isEmpty(authUsers)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "EmployeeCode is invalid");
        }
        Long userId = authUsers.get(0).getId();
        if (ObjectUtils.isEmpty(request.getApps())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "App list must not blank");
        }
        Set<Long> appIds = new HashSet<>(request.getApps());
        List<AuthAppPermission> appPermissions = appPermissionRepository.findAllActiveByUserIdAndAppIdIn(userId,
                appIds);
        if (ObjectUtils.isEmpty(appPermissions)) {
            return RevokeAppPermissionResponse.builder()
                    .revokedAppIds(List.of()).revokedResourceIds(List.of())
                    .revokedResourceCount(0).revokedAt(now)
                    .build();
        }
        appPermissions.forEach(item -> {
            item.setStatus("REVOKED");
            item.setRevokedBy(RequestContext.getEmployeeCode()); // sau này trích từ header
            item.setRevokedAt(now);
            item.setReasonRevoked(request.getReason());
        });

        appPermissionRepository.saveAll(appPermissions);
        List<AuthUserResource> userResources = userResourceRepository.findActiveByUserIdAndAppIdIn(userId, appIds);
        List<Long> revokedAppId = appPermissions.stream().map(item -> item.getAppId()).collect(Collectors.toList());
        if (!ObjectUtils.isEmpty(userResources)) {
            userResources.forEach(item -> {
                item.setStatus("REVOKED");
                item.setRevokedBy(RequestContext.getEmployeeCode());
                item.setRevokedAt(now);
            });
            userResourceRepository.saveAll(userResources);
        }
        List<Long> revokedResorceIds = userResources.stream().map(item -> item.getResourceId())
                .collect(Collectors.toList());
        // thực hiện tạo payload và publish lên kafka
        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_REVOKED_PERMISSION_NOTIFY,
                    "REVOKED_APP_PERMISSION",
                    AppPermissionRevokePayload.builder()
                            .userId(userId).employeeCode(employeeCode)
                            .revokedAppIds(revokedAppId)
                            .revokedResourceIds(revokedResorceIds)
                            .revokedBy(RequestContext.getEmployeeCode()).revokedAt(now)
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish permission request event for : {}", e.getMessage());
        }
        return RevokeAppPermissionResponse.builder()
                .revokedAppIds(revokedAppId)
                .revokedResourceIds(revokedResorceIds)
                .revokedResourceCount(revokedResorceIds.size())
                .revokedAppCount(revokedAppId.size())
                .revokedAt(now)
                .build();
    }

    // ── Phase 4.3: Resource Permission ───────────────────────────────────────

    @Override
    public Page<ResourcePermissionResponse> getResourcePermissions(String employeeCode, Pageable pageable) {
        List<AuthUser> users = userRepository.findUserByEmployeeCode(employeeCode);
        if (ObjectUtils.isEmpty(users)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "EmployeeCode is invalid");
        }
        Long userId = users.get(0).getId();

        Page<AuthUserResource> resourcePage = userResourceRepository.findByUserIdAndStatus(userId, "ACTIVE", pageable);

        Set<Long> resourceIds = resourcePage.getContent().stream()
                .map(AuthUserResource::getResourceId)
                .collect(Collectors.toSet());

        if (resourceIds.isEmpty()) {
            return resourcePage.map(r -> new ResourcePermissionResponse());
        }

        Map<Long, AuthResource> resourceMap = resourceRepository.findAllById(resourceIds).stream()
                .collect(Collectors.toMap(AuthResource::getId, r -> r));

        Set<Long> appIds = resourceMap.values().stream()
                .map(AuthResource::getAppId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, AuthApplication> appMap = appIds.isEmpty() ? Collections.emptyMap()
                : applicationRepository.findAppsById(appIds).stream()
                        .collect(Collectors.toMap(AuthApplication::getId, a -> a));

        return resourcePage.map(ur -> {
            AuthResource resource = resourceMap.get(ur.getResourceId());
            AuthApplication app = resource != null ? appMap.get(resource.getAppId()) : null;
            return ResourcePermissionResponse.builder()
                    .id(ur.getId())
                    .resourceId(ur.getResourceId())
                    .resourceCode(resource != null ? resource.getResourceCode() : null)
                    .resourceName(resource != null ? resource.getResourceName() : null)
                    .resourceType(resource != null ? resource.getResourceType() : null)
                    .action(ur.getAction())
                    .appId(resource != null ? resource.getAppId() : null)
                    .appName(app != null ? app.getName() : null)
                    .status(ur.getStatus())
                    .grantedAt(ur.getGrantedAt())
                    .expiredAt(ur.getExpiredAt())
                    .inactiveFromDate(ur.getInactiveFromDate())
                    .inactiveToDate(ur.getInactiveToDate())
                    .build();
        });
    }

    @Override
    @Transactional
    public RevokeResourcePermissionResponse revokeResourcePermission(String employeeCode,
            RevokeResourcePermissionRequest request) {
        LocalDateTime now = LocalDateTime.now();
        List<AuthUser> authUsers = userRepository.findUserByEmployeeCode(employeeCode);
        if (ObjectUtils.isEmpty(authUsers)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "EmployeeCode is invalid");
        }
        Long userId = authUsers.get(0).getId();
        if (ObjectUtils.isEmpty(request.getResourceIds())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Resource list must not blank");
        }
        Set<Long> resourceIds = new HashSet<>(request.getResourceIds());
        List<AuthUserResource> userResources = userResourceRepository.findActiveByUserIdAndResourceIds(userId,
                resourceIds);
        if (ObjectUtils.isEmpty(userResources)) {
            return RevokeResourcePermissionResponse.builder()
                    .resourceIds(List.of()).revokedCount(0)
                    .revokedAt(now).build();
        }
        userResources.forEach(item -> {
            item.setStatus("REVOKED");
            item.setRevokedBy(RequestContext.getEmployeeCode());
            item.setRevokedAt(now);
        });
        userResourceRepository.saveAll(userResources);
        List<Long> revokedResorceIds = userResources.stream()
                .map(item -> item.getResourceId()).collect(Collectors.toList());
        // thực hiện tạo payload và publish lên kafka
        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_REVOKED_PERMISSION_NOTIFY,
                    "REVOKED_RESOURCE_PERMISSION",
                    AppPermissionRevokePayload.builder()
                            .userId(userId).employeeCode(employeeCode)
                            .revokedAppIds(List.of())
                            .revokedResourceIds(revokedResorceIds)
                            .revokedBy(RequestContext.getEmployeeCode()).revokedAt(now)
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish permission request event for : {}", e.getMessage());
        }

        return RevokeResourcePermissionResponse.builder()
                .resourceIds(revokedResorceIds)
                .revokedCount(revokedResorceIds.size())
                .revokedAt(now).build();
    }

    // ── Phase 4.4: Luồng xin quyền ───────────────────────────────────────────

    @Override
    @Transactional
    public PermissionRequestResponse createPermissionRequest(CreatePermissionRequestRequest request) {

        // 1. Validate reviewer: tồn tại, không DELETED, có role CAB
        List<AuthUser> reviewers = userRepository.findUserByEmployeeCode(request.getReviewerCode());
        if (reviewers == null || reviewers.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Reviewer not found");
        }
        AuthUser reviewer = reviewers.get(0);
        if (!"ACTIVE".equals(reviewer.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Reviewer is not valid");
        }
        if (userRoleRepository.countActiveByUserIdAndRoleCode(reviewer.getId(), "CAB") == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Reviewer does not have CAB role");
        }

        // 1b. Resolve requestFor — fallback về requesterCode nếu null/blank
        String requestFor = (request.getRequestForCode() == null || request.getRequestForCode().isBlank())
                ? request.getRequesterCode()
                : request.getRequestForCode();
        List<AuthUser> grantees = userRepository.findUserByEmployeeCode(requestFor);
        if (grantees == null || grantees.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "Grantee not found: " + requestFor);
        }
        if (!"ACTIVE".equals(grantees.get(0).getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Grantee is not active: " + requestFor);
        }
        if (requestFor.equalsIgnoreCase(request.getReviewerCode())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Người duyệt không thể là người thụ hưởng quyền");
        }

        // 2. Build details
        List<AuthRequestDetail> details = new ArrayList<>();

        if (request.getApps() != null && !request.getApps().isEmpty()) {
            Set<Long> appIds = request.getApps().stream()
                    .map(CreatePermissionRequestRequest.App::getAppId)
                    .collect(Collectors.toSet());

            List<AuthApplication> applications = applicationRepository.findAppsById(appIds);
            if (applications == null || applications.size() != appIds.size()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                        "One or more apps not found or inactive");
            }

            for (Long appId : appIds) {
                details.add(AuthRequestDetail.builder().appId(appId).status("ACTIVE").build());
            }
        }

        if (request.getResources() != null && !request.getResources().isEmpty()) {
            Set<Long> resourceIds = request.getResources().stream()
                    .map(CreatePermissionRequestRequest.Resource::getResourceId)
                    .collect(Collectors.toSet());
            Map<Long, AuthResource> resourceMap = resourceRepository.findAllById(resourceIds).stream()
                    .collect(Collectors.toMap(AuthResource::getId, r -> r));

            for (CreatePermissionRequestRequest.Resource item : request.getResources()) {
                AuthResource resource = resourceMap.get(item.getResourceId());
                if (resource == null) {
                    throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND.getCode(),
                            "Resource not found: " + item.getResourceId());
                }
                if (!"ACTIVE".equals(resource.getStatus())) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                            "Resource is not active: " + item.getResourceId());
                }
                Set<String> allowedActions = Arrays.stream(resource.getActions().split(","))
                        .map(String::trim).collect(Collectors.toSet());
                Set<String> requestedActions = Arrays.stream(item.getActions().split(","))
                        .map(String::trim).collect(Collectors.toSet());
                if (!allowedActions.containsAll(requestedActions)) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                            "Invalid actions for resource " + item.getResourceId());
                }
                details.add(AuthRequestDetail.builder()
                        .resourceId(item.getResourceId())
                        .actions(item.getActions())
                        .status("ACTIVE")
                        .build());
            }
        }

        if (details.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                    "Request must include at least one app or resource");
        }

        // 3. Lưu header, lấy ID, lưu details
        AuthRequestHeader header = AuthRequestHeader.builder()
                .requestedBy(request.getRequesterCode())
                .reviewedBy(request.getReviewerCode())
                .requestFor(requestFor)
                .reason(request.getReason())
                .status(request.getType())
                .build();
        header = requestHeaderRepository.saveAndFlush(header);

        final Long headerId = header.getId();
        details.forEach(d -> d.setRequestId(headerId));
        requestDetailRepository.saveAll(details);

        // 4. Notify reviewer nếu gửi chính thức
        if (AuthRequestHeader.STATUS.OFFICIAL.equals(request.getType())) {
            try {
                identityEventProducer.publish(
                        KafkaConfig.TOPIC_REQUEST_PERMISSION_NOTIFY,
                        "PERMISSION_REQUEST_CREATED",
                        PermissionRequestCreatedPayload.builder()
                                .requestId(headerId)
                                .requesterCode(request.getRequesterCode())
                                .reviewerCode(request.getReviewerCode())
                                .granteeCode(requestFor)
                                .reason(request.getReason())
                                .requestedAt(header.getRequestedAt())
                                .build());
            } catch (Exception e) {
                log.error("Failed to publish permission request event for requestId={}: {}", headerId, e.getMessage());
            }
        }

        return PermissionRequestResponse.builder()
                .requestHeaderId(String.valueOf(headerId))
                .status(request.getType()).createAt(header.getCreatedAt())
                .build();
    }

    @Override
    public PermissionRequestResponse submitRequest(SubmitRequest request) {
        Long requestId = Long.parseLong(request.getRequestId());
        AuthRequestHeader header = requestHeaderRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                        "Permission request not found: " + requestId));
        if (!AuthRequestHeader.STATUS.DRAFT.equals(header.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                    "Request must be OFFICIAL to approve. Current: " + header.getStatus());
        }

        // validate người submit có phải là người tạo requets không?
        List<AuthUser> requesters = userRepository.findUserByEmployeeCode(header.getRequestedBy());
        if (requesters == null || requesters.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(),
                    "Requester not found: " + header.getRequestedBy());
        }
        AuthUser requester = requesters.get(0);
        if (requester.getId() != Long.valueOf(RequestContext.getUserId())
                || !header.getRequestedBy().equals(RequestContext.getEmployeeCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(),
                    "User does not have permission for this action.");
        }

        LocalDateTime now = LocalDateTime.now();
        header.setStatus(AuthRequestHeader.STATUS.OFFICIAL);
        header.setRequestedAt(now);
        requestHeaderRepository.save(header);

        // gửi thông báo
        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_REQUEST_PERMISSION_NOTIFY,
                    "PERMISSION_REQUEST_CREATED",
                    PermissionRequestCreatedPayload.builder()
                            .requestId(requestId)
                            .requesterCode(request.getSubmitCode())
                            .reviewerCode(header.getReviewedBy())
                            .reason(header.getReason())
                            .requestedAt(header.getRequestedAt())
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish permission request event for requestId={}: {}", requestId, e.getMessage());
        }

        return PermissionRequestResponse.builder()
                .requestHeaderId(String.valueOf(requestId))
                .status(AuthRequestHeader.STATUS.OFFICIAL).createAt(header.getCreatedAt())
                .build();

    }

    @Override
    public Page<GetAllPermissionRequestResponse> getPermissionRequests(String status, String requester,
            String reviewer, LocalDate from, LocalDate to, Pageable pageable) {
        String sortDir = "";
        Sort.Order order = pageable.getSort().getOrderFor("requestedAt");
        if (order != null && order.getDirection() == Sort.Direction.ASC) {
            sortDir = "ASC";
        } else {
            sortDir = "DESC";
        }
        String currentUserCode = RequestContext.getEmployeeCode();
        long totalElement = authRepository.countPermissionRequests(status, requester, reviewer, from, to, currentUserCode);
        if (totalElement == 0) {
            return new PageImpl<>(Collections.emptyList(), pageable, totalElement);
        }
        List<PermissionRequest> permissionRequests = authRepository.getPermissionRequests(status, requester, reviewer,
                from, to, pageable.getOffset(), pageable.getPageSize(), sortDir, currentUserCode);
        if (permissionRequests == null || permissionRequests.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, totalElement);
        }
        List<GetAllPermissionRequestResponse> contents = permissionRequests.stream()
                .map(item -> new GetAllPermissionRequestResponse(item)).collect(Collectors.toList());
        return new PageImpl<>(contents, pageable, totalElement);
    }

    @Override
    public GetDetailPermissionRequest getPermissionRequestById(String request) {

        Long requestId = Long.parseLong(request);
        AuthRequestHeader header = requestHeaderRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                        "Permission request not found: " + requestId));

        String currentUserCode = RequestContext.getEmployeeCode();
        if (!header.getRequestedBy().equals(currentUserCode) && !header.getReviewedBy().equals(currentUserCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(),
                    "User does not have permission to view this request");
        }

        String requesterCode = header.getRequestedBy();
        String reviewerCode = header.getReviewedBy();

        UserInfoRow requesterProfile = authRepository.getUserInfo(null, requesterCode);
        UserInfoRow reviewerProfile = authRepository.getUserInfo(null, reviewerCode);
        if (requesterProfile == null || reviewerProfile == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Requester or Reviewer is invalid");
        }

        List<AuthRequestDetail> requestDetails = requestDetailRepository.findActiveByRequestId(requestId);
        if (requestDetails == null || requestDetails.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Request details is empty");
        }

        GetDetailPermissionRequest output = new GetDetailPermissionRequest();
        output.setRequestId(request);
        output.setStatus(header.getStatus());
        output.setReason(header.getReason());
        output.setNote(header.getNote());

        GetDetailPermissionRequest.User requester = new GetDetailPermissionRequest.User();
        requester.setEmployeeCode(requesterCode);
        requester.setUsername(requesterProfile.getUsername());
        requester.setFullName(requesterProfile.getFullName());
        requester.setDepartment(dataCached.getDetailDepartment(requesterProfile.getDepartmentId()));
        output.setRequester(requester);

        GetDetailPermissionRequest.User reviewer = new GetDetailPermissionRequest.User();
        reviewer.setEmployeeCode(reviewerCode);
        reviewer.setUsername(reviewerProfile.getUsername());
        reviewer.setFullName(reviewerProfile.getFullName());
        reviewer.setDepartment(dataCached.getDetailDepartment(reviewerProfile.getDepartmentId()));
        output.setReviewer(reviewer); // fix: was output.setRequester(requester) — reviewer bị mất

        // requestFor luôn được set tại bước create/update — dùng trực tiếp từ DB
        String granteeCode = header.getRequestFor();
        if (granteeCode != null && !granteeCode.isBlank()) {
            UserInfoRow granteeProfile = granteeCode.equals(requesterCode)
                    ? requesterProfile : authRepository.getUserInfo(null, granteeCode);
            if (granteeProfile != null) {
                GetDetailPermissionRequest.User grantee = new GetDetailPermissionRequest.User();
                grantee.setEmployeeCode(granteeCode);
                grantee.setUsername(granteeProfile.getUsername());
                grantee.setFullName(granteeProfile.getFullName());
                grantee.setDepartment(dataCached.getDetailDepartment(granteeProfile.getDepartmentId()));
                output.setGrantee(grantee);
            }
        }

        List<GetDetailPermissionRequest.App> apps = new ArrayList<>();
        List<GetDetailPermissionRequest.Resource> resources = new ArrayList<>();

        Set<Long> appIds = requestDetails.stream()
                .filter(item -> item.getAppId() != null && item.getAppId() != 0)
                .map(AuthRequestDetail::getAppId)
                .collect(Collectors.toSet());
        if (!appIds.isEmpty()) {
            List<AuthApplication> applications = applicationRepository.findAppsById(appIds);
            for (AuthApplication application : applications) {
                GetDetailPermissionRequest.App app = new GetDetailPermissionRequest.App();
                app.setId(application.getId());
                app.setCode(application.getServiceCode());
                app.setName(application.getName());
                app.setStatus(application.getStatus());
                apps.add(app);
            }
        }

        for (AuthRequestDetail detail : requestDetails) {
            if (detail.getResourceId() == null || detail.getResourceId() == 0)
                continue;
            // fix: query by resourceId (param 2), không phải appId (param 1)
            List<ResourceRow> rows = authRepository.getResource(null, detail.getResourceId(), null, null);
            if (rows == null || rows.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                        "Requested resource is invalid: " + detail.getResourceId());
            }
            ResourceRow row = rows.get(0);
            GetDetailPermissionRequest.Resource resource = new GetDetailPermissionRequest.Resource();
            resource.setId(row.getResourceId());
            resource.setCode(row.getResourceCode());
            resource.setName(row.getResourceName());
            resource.setAppId(row.getAppId());
            resource.setAppCode(row.getServiceCode());
            resource.setActions(detail.getActions());
            resource.setStatus(detail.getStatus());
            resources.add(resource);
        }

        output.setDetails(new GetDetailPermissionRequest.Detail(apps, resources));
        return output;
    }

    // ── Phase 4.5: CAB duyệt / từ chối / thu hồi ─────────────────────────────

    /**
     * noRollbackFor = BusinessException.class: cho phép save DRAFT trước khi throw
     * mà không bị rollback, để requester biết cần điều chỉnh request.
     */
    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public PermissionRequestResponse approvePermissionRequest(ApprovePermissionRequest request) {

        Long requestId = Long.parseLong(request.getRequestId());
        AuthRequestHeader header = requestHeaderRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                        "Permission request not found: " + requestId));
        if (!AuthRequestHeader.STATUS.OFFICIAL.equals(header.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                    "Request must be OFFICIAL to approve. Current: " + header.getStatus());
        }

        if (RequestContext.getEmployeeCode() == null
                || !RequestContext.getEmployeeCode().equals(header.getReviewedBy())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(),
                    "User does not have permission for this action.");
        }
        // requestFor luôn được set tại bước create/update — dùng trực tiếp, không fallback
        String grantToCode = header.getRequestFor();
        List<AuthUser> grantees = userRepository.findUserByEmployeeCode(grantToCode);
        if (grantees == null || grantees.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(),
                    "Grantee not found: " + grantToCode);
        }
        Long userId = grantees.get(0).getId();

        List<AuthRequestDetail> details = requestDetailRepository.findActiveByRequestId(requestId);
        if (details == null || details.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                    "No active details found for request: " + requestId);
        }

        Set<Long> requestedAppIdSet = details.stream()
                .filter(d -> d.getAppId() != null)
                .map(AuthRequestDetail::getAppId)
                .collect(Collectors.toSet());
        List<AuthRequestDetail> resourceDetails = details.stream()
                .filter(d -> d.getResourceId() != null && d.getActions() != null)
                .collect(Collectors.toList());
        Set<Long> resourceIdSet = resourceDetails.stream()
                .map(AuthRequestDetail::getResourceId)
                .collect(Collectors.toSet());

        List<AuthAppPermission> allUserApps = appPermissionRepository.findAllByUserId(userId);
        Map<Long, AuthAppPermission> existingAppMap = allUserApps.stream()
                .filter(a -> requestedAppIdSet.contains(a.getAppId()))
                .collect(Collectors.toMap(AuthAppPermission::getAppId, a -> a));
        Set<Long> userAppIds = allUserApps.stream()
                .filter(a -> "ACTIVE".equals(a.getStatus()) || "SUSPENDED".equals(a.getStatus()))
                .map(AuthAppPermission::getAppId)
                .collect(Collectors.toSet());

        Map<Long, AuthResource> resourceMap = resourceIdSet.isEmpty() ? Collections.emptyMap()
                : resourceRepository.findAllById(resourceIdSet).stream()
                        .collect(Collectors.toMap(AuthResource::getId, r -> r));

        for (AuthRequestDetail resDetail : resourceDetails) {
            AuthResource resource = resourceMap.get(resDetail.getResourceId());
            if (resource == null) {
                header.setStatus(AuthRequestHeader.STATUS.DRAFT);
                requestHeaderRepository.save(header);
                throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND.getCode(),
                        "Resource not found: " + resDetail.getResourceId() + ". Request reverted to DRAFT.");
            }
            Long appIdOfResource = resource.getAppId();
            if (!userAppIds.contains(appIdOfResource) && !requestedAppIdSet.contains(appIdOfResource)) {
                header.setStatus(AuthRequestHeader.STATUS.DRAFT);
                requestHeaderRepository.save(header);
                throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                        "Resource " + resDetail.getResourceId() + " belongs to app " + appIdOfResource
                                + " which requester has no access to and is not in this request."
                                + " Request reverted to DRAFT.");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<AuthAppPermission> appPermsToSave = new ArrayList<>();
        for (Long appId : requestedAppIdSet) {
            AuthAppPermission existing = existingAppMap.get(appId);
            if (existing == null) {
                appPermsToSave.add(
                        AuthAppPermission.builder()
                                .userId(userId).appId(appId).status("ACTIVE")
                                .grantedBy(header.getReviewedBy()).grantedAt(now)
                                .grantSource("REQUEST").requestId(requestId)
                                .build());
            } else if ("REVOKED".equals(existing.getStatus())) {
                existing.setStatus("ACTIVE");
                existing.setGrantedBy(header.getReviewedBy());
                existing.setGrantedAt(now);
                existing.setRevokedBy(null);
                existing.setRevokedAt(null);
                existing.setGrantSource("REQUEST");
                existing.setRequestId(requestId);
                existing.setInactiveFromDate(null);
                existing.setInactiveToDate(null);
                appPermsToSave.add(existing);
            }
            // ACTIVE hoặc SUSPENDED: đã có quyền → bỏ qua
        }
        if (!appPermsToSave.isEmpty()) {
            appPermissionRepository.saveAll(appPermsToSave);
        }

        // Lấy tất cả records (kể cả REVOKED) để tránh vi phạm UNIQUE(USER_ID, RESOURCE_ID)
        Map<Long, AuthUserResource> existingResMap = resourceIdSet.isEmpty() ? Collections.emptyMap()
                : userResourceRepository.findAnyByUserIdAndResourceIdIn(userId, resourceIdSet).stream()
                        .collect(Collectors.toMap(AuthUserResource::getResourceId, r -> r));
        List<AuthUserResource> resourcesToSave = new ArrayList<>();
        for (AuthRequestDetail resDetail : resourceDetails) {
            Set<String> requestedActions = Arrays.stream(resDetail.getActions().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            Set<String> allowedActions = Arrays
                    .stream(resourceMap.get(resDetail.getResourceId()).getActions().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            if (!allowedActions.containsAll(requestedActions)) {
                header.setStatus(AuthRequestHeader.STATUS.DRAFT);
                requestHeaderRepository.save(header);
                throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Action is invalid");
            }
            AuthUserResource existing = existingResMap.get(resDetail.getResourceId());
            if (existing != null && "ACTIVE".equals(existing.getStatus())) {
                // ACTIVE: merge thêm actions còn thiếu
                Set<String> currentActions = Arrays.stream(existing.getAction().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
                Set<String> missing = requestedActions.stream()
                        .filter(a -> !currentActions.contains(a)).collect(Collectors.toSet());
                if (!missing.isEmpty()) {
                    currentActions.addAll(missing);
                    existing.setAction(String.join(",", currentActions));
                    resourcesToSave.add(existing);
                }
            } else if (existing != null) {
                // REVOKED: reactivate thay vì insert mới
                existing.setStatus("ACTIVE");
                existing.setAction(String.join(",", requestedActions));
                existing.setGrantedBy(header.getReviewedBy());
                existing.setGrantedAt(now);
                existing.setRevokedBy(null);
                existing.setRevokedAt(null);
                existing.setGrantSource("REQUEST");
                existing.setRequestId(requestId);
                resourcesToSave.add(existing);
            } else {
                resourcesToSave.add(AuthUserResource.builder()
                        .userId(userId).resourceId(resDetail.getResourceId())
                        .action(String.join(",", requestedActions))
                        .grantedBy(header.getReviewedBy()).requestId(requestId)
                        .grantSource("REQUEST")
                        .build());
            }
        }
        if (!resourcesToSave.isEmpty()) {
            userResourceRepository.saveAll(resourcesToSave);
        }

        header.setStatus(AuthRequestHeader.STATUS.APPROVED);
        header.setReviewedAt(now);
        header.setNote(request.getNote());
        requestHeaderRepository.save(header);

        // Publish Kafka notify (ngoài transaction logic, chỉ log nếu fail)
        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_APPROVE_PERMISSION_NOTIFY,
                    "PERMISSION_REQUEST_APPROVED",
                    PermissionApprovedPayload.builder()
                            .requestId(requestId)
                            .requestedBy(header.getRequestedBy())
                            .reviewedBy(header.getReviewedBy())
                            .grantedTo(grantToCode)
                            .approvedAt(now)
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish approve event for requestId={}: {}", requestId, e.getMessage());
        }

        return PermissionRequestResponse.builder()
                .requestHeaderId(String.valueOf(requestId))
                .status(AuthRequestHeader.STATUS.APPROVED).createAt(header.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public PermissionRequestResponse rejectPermissionRequest(RejectPermissionRequest request) {
        // validate request
        Long requestId = Long.parseLong(request.getRequestId());
        AuthRequestHeader header = requestHeaderRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                        "Permission request not found: " + requestId));
        if (!AuthRequestHeader.STATUS.OFFICIAL.equals(header.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                    "Request must be OFFICIAL to approve. Current: " + header.getStatus());
        }

        if (RequestContext.getEmployeeCode() == null
                || !RequestContext.getEmployeeCode().equals(header.getReviewedBy())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(),
                    "User does not have permission for this action.");
        }

        // thực hiện validate người thực hiện thông qua header/jwt -> todo
        LocalDateTime now = LocalDateTime.now();
        header.setNote(request.getNote());
        header.setReviewedAt(now);
        header.setStatus(AuthRequestHeader.STATUS.REJECTED);
        requestHeaderRepository.save(header);

        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_APPROVE_PERMISSION_NOTIFY,
                    "REJECT_PERMISSION",
                    PermissionApprovedPayload.builder()
                            .requestId(requestId)
                            .requestedBy(header.getRequestedBy())
                            .reviewedBy(header.getReviewedBy())
                            .approvedAt(now)
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish approve event for requestId={}: {}", requestId, e.getMessage());
        }

        return PermissionRequestResponse.builder()
                .requestHeaderId(String.valueOf(requestId))
                .status(AuthRequestHeader.STATUS.REJECTED).createAt(header.getCreatedAt())
                .build();
    }

    // ── Phase 4.6: Người dùng huỷ request ──────────────────────oke ──────────────

    @Override
    @Transactional
    public PermissionRequestResponse cancelPermissionRequest(String requestId) {
        Long id = Long.parseLong(requestId);
        AuthRequestHeader header = requestHeaderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                        "Permission request not found: " + requestId));

        if (!AuthRequestHeader.STATUS.DRAFT.equals(header.getStatus())
                && !AuthRequestHeader.STATUS.OFFICIAL.equals(header.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                    "Only DRAFT or OFFICIAL requests can be cancelled. Current: " + header.getStatus());
        }

        if (!RequestContext.getEmployeeCode().equals(header.getRequestedBy())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(),
                    "User does not have permission for this action.");
        }
        header.setStatus(AuthRequestHeader.STATUS.CANCELLED);
        requestHeaderRepository.save(header);

        return PermissionRequestResponse.builder()
                .requestHeaderId(String.valueOf(id))
                .status(AuthRequestHeader.STATUS.CANCELLED)
                .createAt(header.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public UpdatePermisssionRequestResponse updatePermissionRequest(UpdatePermissionRequestRequest request) {

        // 1. Load header — không tìm thấy → VALIDATION_FAILED
        Long requestId = request.getRequestId();
        AuthRequestHeader header = requestHeaderRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                        "Permission request not found: " + requestId));

        // 2. Chỉ cập nhật khi trạng thái là DRAFT
        if (!AuthRequestHeader.STATUS.DRAFT.equals(header.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                    "Only DRAFT requests can be updated. Current status: " + header.getStatus());
        }

        // 3. Validate reviewer: tồn tại, ACTIVE, có role CAB
        List<AuthUser> reviewers = userRepository.findUserByEmployeeCode(request.getReviewerCode());
        if (reviewers == null || reviewers.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Reviewer not found");
        }
        AuthUser reviewer = reviewers.get(0);
        if (!"ACTIVE".equals(reviewer.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Reviewer is not valid");
        }
        if (userRoleRepository.countActiveByUserIdAndRoleCode(reviewer.getId(), "CAB") == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Reviewer does not have CAB role");
        }

        // 3b. Resolve requestFor — fallback về requesterCode nếu null/blank
        String requestFor = (request.getRequestForCode() == null || request.getRequestForCode().isBlank())
                ? request.getRequesterCode()
                : request.getRequestForCode();
        List<AuthUser> grantees = userRepository.findUserByEmployeeCode(requestFor);
        if (grantees == null || grantees.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "Grantee not found: " + requestFor);
        }
        if (!"ACTIVE".equals(grantees.get(0).getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Grantee is not active: " + requestFor);
        }

        // 4. Build danh sách details mới (validation giống createPermissionRequest)
        List<AuthRequestDetail> newDetails = new ArrayList<>();

        if (request.getApps() != null && !request.getApps().isEmpty()) {
            Set<Long> appIds = request.getApps().stream()
                    .map(UpdatePermissionRequestRequest.App::getAppId)
                    .collect(Collectors.toSet());
            List<AuthApplication> applications = applicationRepository.findAppsById(appIds);
            if (applications == null || applications.size() != appIds.size()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                        "One or more apps not found or inactive");
            }
            for (Long appId : appIds) {
                newDetails.add(AuthRequestDetail.builder().appId(appId).status("ACTIVE").build());
            }
        }

        if (request.getResources() != null && !request.getResources().isEmpty()) {
            Set<Long> resourceIds = request.getResources().stream()
                    .map(UpdatePermissionRequestRequest.Resource::getResourceId)
                    .collect(Collectors.toSet());
            Map<Long, AuthResource> resourceMap = resourceRepository.findAllById(resourceIds).stream()
                    .collect(Collectors.toMap(AuthResource::getId, r -> r));

            for (UpdatePermissionRequestRequest.Resource item : request.getResources()) {
                AuthResource resource = resourceMap.get(item.getResourceId());
                if (resource == null) {
                    throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND.getCode(),
                            "Resource not found: " + item.getResourceId());
                }
                if (!"ACTIVE".equals(resource.getStatus())) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                            "Resource is not active: " + item.getResourceId());
                }
                Set<String> allowedActions = Arrays.stream(resource.getActions().split(","))
                        .map(String::trim).collect(Collectors.toSet());
                Set<String> requestedActions = Arrays.stream(item.getActions().split(","))
                        .map(String::trim).collect(Collectors.toSet());
                if (!allowedActions.containsAll(requestedActions)) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                            "Invalid actions for resource " + item.getResourceId());
                }
                newDetails.add(AuthRequestDetail.builder()
                        .resourceId(item.getResourceId())
                        .actions(item.getActions())
                        .status("ACTIVE")
                        .build());
            }
        }

        if (newDetails.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                    "Request must include at least one app or resource");
        }

        // 5. Cập nhật header
        header.setRequestedBy(request.getRequesterCode());
        header.setReviewedBy(request.getReviewerCode());
        header.setRequestFor(requestFor);
        header.setReason(request.getReason());
        header.setStatus(request.getType());
        header = requestHeaderRepository.save(header);

        // 6. Xóa details cũ, lưu details mới
        requestDetailRepository.deleteAllByRequestId(requestId);
        newDetails.forEach(d -> d.setRequestId(requestId));
        requestDetailRepository.saveAll(newDetails);

        return UpdatePermisssionRequestResponse.builder()
                .requestHeaderId(String.valueOf(requestId))
                .status(header.getStatus())
                .createAt(header.getCreatedAt())
                .updatedAt(header.getUpdatedAt())
                .build();
    }

}

package com.iam.app.service.impl;

import com.iam.app.domain.AuthApplication;
import com.iam.app.domain.AuthDefaultAppPermission;
import com.iam.app.domain.AuthDefaultResource;
import com.iam.app.domain.AuthResource;
import com.iam.app.domain.AuthRole;
import com.iam.app.dto.response.DefaultPermissionPreviewResponse;
import com.iam.app.enums.ErrorCode;
import com.iam.app.exception.BusinessException;
import com.iam.app.repository.jpa.AuthApplicationRepository;
import com.iam.app.repository.jpa.AuthDefaultAppPermissionRepository;
import com.iam.app.repository.jpa.AuthDefaultResourceRepository;
import com.iam.app.repository.jpa.AuthResourceRepository;
import com.iam.app.repository.jpa.AuthRoleRepository;
import com.iam.app.service.DefaultPermissionPreviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultPermissionPreviewServiceImpl implements DefaultPermissionPreviewService {

    private final AuthDefaultAppPermissionRepository appPermissionRepository;
    private final AuthDefaultResourceRepository resourcePermissionRepository;
    private final AuthRoleRepository roleRepository;
    private final AuthApplicationRepository applicationRepository;
    private final AuthResourceRepository resourceRepository;

    @Override
    public DefaultPermissionPreviewResponse preview(String roleCode, String positionCode) {
        boolean noRoleCode = roleCode == null || roleCode.isBlank();
        boolean noPositionCode = positionCode == null || positionCode.isBlank();

        if (noRoleCode && noPositionCode) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Phải cung cấp ít nhất roleCode hoặc positionCode");
        }

        String normalizedRoleCode = noRoleCode ? null : roleCode;
        String normalizedPositionCode = noPositionCode ? null : positionCode;

        // Resolve numeric roleId for resource permission query
        Long roleId = null;
        if (normalizedRoleCode != null) {
            roleId = roleRepository.findByCodes(List.of(normalizedRoleCode)).stream()
                    .findFirst()
                    .map(AuthRole::getId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                            "Không tìm thấy role: " + normalizedRoleCode));
        }

        List<DefaultPermissionPreviewResponse.ApplicationItem> applications =
                buildApplicationItems(normalizedRoleCode, normalizedPositionCode);

        List<DefaultPermissionPreviewResponse.ResourceItem> resources =
                buildResourceItems(roleId, normalizedPositionCode);

        return new DefaultPermissionPreviewResponse(normalizedRoleCode, normalizedPositionCode, applications, resources);
    }

    private List<DefaultPermissionPreviewResponse.ApplicationItem> buildApplicationItems(
            String roleCode, String positionCode) {

        List<AuthDefaultAppPermission> appPerms = appPermissionRepository
                .findActiveByRoleAndPosition(roleCode, positionCode);

        if (appPerms.isEmpty()) {
            return List.of();
        }

        List<Long> appIds = appPerms.stream()
                .map(AuthDefaultAppPermission::getApplicationId).distinct().toList();
        Map<Long, String> appNameMap = applicationRepository.findAllById(appIds).stream()
                .collect(Collectors.toMap(AuthApplication::getId, AuthApplication::getName));

        return appPerms.stream()
                .map(p -> new DefaultPermissionPreviewResponse.ApplicationItem(
                        p.getApplicationId(),
                        appNameMap.get(p.getApplicationId())))
                .toList();
    }

    private List<DefaultPermissionPreviewResponse.ResourceItem> buildResourceItems(
            Long roleId, String positionCode) {

        List<AuthDefaultResource> resourcePerms = resourcePermissionRepository
                .findActiveByRoleAndPosition(roleId, positionCode);

        if (resourcePerms.isEmpty()) {
            return List.of();
        }

        List<Long> resourceIds = resourcePerms.stream()
                .map(AuthDefaultResource::getResourceId).distinct().toList();
        Map<Long, AuthResource> resourceMap = resourceRepository.findAllById(resourceIds).stream()
                .collect(Collectors.toMap(AuthResource::getId, r -> r));

        List<Long> appIds = resourceMap.values().stream()
                .map(AuthResource::getAppId).distinct().toList();
        Map<Long, String> appNameMap = applicationRepository.findAllById(appIds).stream()
                .collect(Collectors.toMap(AuthApplication::getId, AuthApplication::getName));

        return resourcePerms.stream()
                .map(p -> {
                    AuthResource res = resourceMap.get(p.getResourceId());
                    String appName = res != null ? appNameMap.get(res.getAppId()) : null;
                    List<String> actions = p.getActions() != null && !p.getActions().isBlank()
                            ? Arrays.asList(p.getActions().split(","))
                            : List.of();
                    return new DefaultPermissionPreviewResponse.ResourceItem(
                            p.getResourceId(),
                            res != null ? res.getResourceCode() : null,
                            appName,
                            actions);
                })
                .toList();
    }
}

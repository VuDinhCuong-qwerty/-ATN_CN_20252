package com.iam.app.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iam.app.domain.AuthApplication;
import com.iam.app.dto.pojo.AppInfor;
import com.iam.app.dto.pojo.Department;
import com.iam.app.dto.request.CreateApplicationRequest;
import com.iam.app.dto.request.UpdateApplicationRequest;
import com.iam.app.dto.response.AppWarning;
import com.iam.app.dto.response.ApplicationResponse;
import com.iam.app.dto.response.GetApplicationsResponse;
import com.iam.app.dto.response.GetDetailAppResponse;
import com.iam.app.dto.response.SetupStatusResponse;
import com.iam.app.enums.ErrorCode;
import com.iam.app.exception.BusinessException;
import com.iam.app.repository.jpa.ApplicationSpec;
import com.iam.app.repository.jpa.AuthApplicationRepository;
import com.iam.app.repository.jpa.AuthClientGroupRepository;
import com.iam.app.repository.jpa.AuthClientMethodRepository;
import com.iam.app.repository.jpa.AuthClientRepository;
import com.iam.app.repository.jpa.AuthDefaultAppPermissionRepository;
import com.iam.app.repository.jpa.AuthDefaultResourceRepository;
import com.iam.app.repository.jpa.AuthDepartmentRepository;
import com.iam.app.repository.jpa.AuthFlowRepository;
import com.iam.app.repository.jpa.AuthRepository;
import com.iam.app.repository.jpa.AuthResourceRepository;
import com.iam.app.service.ApplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final AuthApplicationRepository appRepository;
    private final AuthRepository authRepository;
    private final AuthDepartmentRepository departmentRepository;
    private final AuthClientGroupRepository groupRepository;
    private final AuthClientRepository clientRepository;
    private final AuthResourceRepository resourceRepository;
    private final AuthClientMethodRepository clientMethodRepository;
    private final AuthFlowRepository flowRepository;
    private final AuthDefaultAppPermissionRepository defaultAppPermissionRepository;
    private final AuthDefaultResourceRepository defaultResourceRepository;

    @Override
    public Page<GetApplicationsResponse> getApps(
            String type, String status, String serviceCode, String name, String groupName, Pageable pageable) {
        return appRepository
                .findAll(ApplicationSpec.withFilters(type, status, serviceCode, name, groupName), pageable)
                .map(GetApplicationsResponse::new);
    }

    @Override
    public GetDetailAppResponse getDetailApp(Long id) {
        AppInfor app = this.authRepository.getAppInforById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Application not found");
        }
        Department department = this.authRepository.getDepartmentById(app.getDepartmentId());

        return new GetDetailAppResponse(app, department);
    }

    @Override
    @Transactional
    public ApplicationResponse createApp(CreateApplicationRequest request) {
        if (appRepository.countByServiceCode(request.getServiceCode()) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Service code đã tồn tại");
        }
        if (departmentRepository.countById(request.getDepartmentId()) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Phòng ban không tồn tại");
        }
        if (request.getGroupId() != null && groupRepository.countById(request.getGroupId()) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Nhóm ứng dụng không tồn tại");
        }

        AuthApplication entity = AuthApplication.builder()
                .name(request.getName())
                .description(request.getDescription())
                .appType(request.getAppType())
                .logoUri(request.getLogoUri())
                .defaultUrl(request.getDefaultUrl())
                .departmentId(request.getDepartmentId())
                .serviceCode(request.getServiceCode())
                .groupId(request.getGroupId())
                .acrLevel(request.getAcrLevel())
                .build();

        AuthApplication saved = appRepository.save(entity);
        return new ApplicationResponse(saved);
    }

    @Override
    @Transactional
    public ApplicationResponse updateApp(Long id, UpdateApplicationRequest request) {
        log.info("updateApp called: id={}, serviceCode={}", id, request.getServiceCode());

        AuthApplication entity = appRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Ứng dụng không tồn tại"));
        log.info("Found app: id={}, name={}, serviceCode={}", entity.getId(), entity.getName(), entity.getServiceCode());

        String newServiceCode = request.getServiceCode().trim();
        if (!newServiceCode.equals(entity.getServiceCode())) {
            log.info("serviceCode changed: {} -> {}", entity.getServiceCode(), newServiceCode);
            if (appRepository.countByServiceCodeAndIdNot(newServiceCode, id) > 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Service code đã tồn tại");
            }
        }

        Long newDepartmentId = request.getDepartmentId();
        if (!newDepartmentId.equals(entity.getDepartmentId())) {
            log.info("departmentId changed: {} -> {}", entity.getDepartmentId(), newDepartmentId);
            if (departmentRepository.countById(newDepartmentId) == 0) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Phòng ban không tồn tại");
            }
        }

        Long newGroupId = request.getGroupId();
        if (!Objects.equals(newGroupId, entity.getGroupId())) {
            log.info("groupId changed: {} -> {}", entity.getGroupId(), newGroupId);
            if (newGroupId != null && groupRepository.countById(newGroupId) == 0) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Nhóm ứng dụng không tồn tại");
            }
        }

        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        entity.setAppType(request.getAppType().trim());
        entity.setLogoUri(request.getLogoUri().trim());
        entity.setDefaultUrl(request.getDefaultUrl().trim());
        entity.setServiceCode(newServiceCode);
        entity.setDepartmentId(newDepartmentId);
        entity.setGroupId(newGroupId);
        entity.setAcrLevel(request.getAcrLevel());

        log.info("Saving updated app: id={}", entity.getId());
        AuthApplication saved = appRepository.save(entity);
        log.info("App updated successfully: id={}", saved.getId());
        return new ApplicationResponse(saved);
    }

    @Override
    @Transactional
    public ApplicationResponse toggleAppStatus(Long id, String status) {
        log.info("toggleAppStatus called: id={}, newStatus={}", id, status);

        AuthApplication entity = appRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Ứng dụng không tồn tại"));
        log.info("Found app: id={}, currentStatus={}", entity.getId(), entity.getStatus());

        log.info("Toggling status: {} -> {}", entity.getStatus(), status);
        entity.setStatus(status);

        AuthApplication saved = appRepository.save(entity);
        log.info("Status updated successfully: id={}, status={}", saved.getId(), saved.getStatus());

        ApplicationResponse response = new ApplicationResponse(saved);

        if (AuthApplication.STATUS.INACTIVE.equals(status)) {
            List<AppWarning> warnings = new ArrayList<>();
            clientRepository.findEnabledByAppId(id)
                    .forEach(c -> warnings.add(new AppWarning("CLIENT", c.getId(), c.getName())));
            resourceRepository.findActiveByAppId(id)
                    .forEach(r -> warnings.add(new AppWarning("RESOURCE", r.getId(), r.getResourceName())));
            if (!warnings.isEmpty()) {
                log.info("Found {} active client(s)/resource(s) affected by INACTIVE: appId={}", warnings.size(), id);
                response.setWarnings(warnings);
            }
        }

        return response;
    }

    @Override
    public SetupStatusResponse getSetupStatus(Long id) {
        AuthApplication app = appRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Ứng dụng không tồn tại"));

        boolean isInternal = "INTERNAL".equals(app.getAppType());

        boolean hasResource = !resourceRepository.findActiveByAppId(id).isEmpty();
        boolean hasDefaultPermission = defaultAppPermissionRepository.countActiveByApplicationId(id) > 0
                || defaultResourceRepository.countActiveByApplicationId(id) > 0;

        // App THIRD_PARTY_LDAP (vd: GitLab) thường tự quản lý tài nguyên/quyền riêng —
        // Resource + Default Permission chỉ bắt buộc với app INTERNAL.
        List<SetupStatusResponse.Step> steps = new ArrayList<>();
        steps.add(buildStep("APPLICATION", "Tạo ứng dụng", true, true));
        steps.add(buildStep("RESOURCE", "Tài nguyên", hasResource, isInternal));
        steps.add(buildStep("DEFAULT_PERMISSION", "Quyền mặc định", hasDefaultPermission, isInternal));

        if (isInternal) {
            boolean hasClient = !clientRepository.findEnabledByAppId(id).isEmpty();
            boolean hasClientMethod = clientMethodRepository.findByAppId(id).stream()
                    .anyMatch(m -> "ACTIVE".equals(m.getStatus()));
            boolean hasFlow = flowRepository.countActiveByAppId(id) > 0;

            steps.add(buildStep("CLIENT", "OAuth2 Client", hasClient, true));
            steps.add(buildStep("CLIENT_METHOD", "Phương thức xác thực", hasClientMethod, true));
            steps.add(buildStep("AUTH_FLOW", "Luồng MFA", hasFlow, true));
        }

        String nextStep = steps.stream()
                .filter(s -> s.isRequired() && !s.isDone())
                .map(SetupStatusResponse.Step::getKey)
                .findFirst()
                .orElse(null);
        boolean overallComplete = nextStep == null;

        return SetupStatusResponse.builder()
                .appId(id)
                .appType(app.getAppType())
                .steps(steps)
                .nextStep(nextStep)
                .overallComplete(overallComplete)
                .build();
    }

    private SetupStatusResponse.Step buildStep(String key, String label, boolean done, boolean required) {
        return SetupStatusResponse.Step.builder().key(key).label(label).done(done).required(required).build();
    }
}

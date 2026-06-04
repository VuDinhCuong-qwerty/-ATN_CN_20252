package com.iam.identity.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iam.identity.config.KafkaConfig;
import com.iam.identity.config.context.RequestContext;
import com.iam.identity.domain.AuthRole;
import com.iam.identity.domain.AuthUser;
import com.iam.identity.domain.AuthUserProfile;
import com.iam.identity.domain.AuthUserRole;
import com.iam.identity.dto.request.LeaveRequest;
import com.iam.identity.dto.request.OnboardRequest;
import com.iam.identity.dto.request.TransferRequest;
import com.iam.identity.dto.response.UserStatusResponse;
import com.iam.identity.enums.ErrorCode;
import com.iam.identity.exception.BusinessException;
import com.iam.identity.kafka.event.payload.AppPermissionRevokePayload;
import com.iam.identity.kafka.event.payload.UserCreatedNotificationPayload;
import com.iam.identity.kafka.event.payload.UserCreatedPermissionPayload;
import com.iam.identity.kafka.producer.IdentityEventProducer;
import com.iam.identity.repository.jpa.AuthAppPermissionRepository;
import com.iam.identity.repository.jpa.AuthUserRepository;
import com.iam.identity.repository.jpa.AuthUserResourceRepository;
import com.iam.identity.repository.jpa.AuthUserProfileRepository;
import com.iam.identity.repository.jpa.AuthUserRoleRepository;
import com.iam.identity.service.UserLifecycleService;
import com.iam.identity.service.common.CheckInfor;
import com.iam.identity.service.common.GenDataService;
import com.iam.identity.service.common.ValidateInput;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserLifecycleServiceImpl implements UserLifecycleService {

    private final CheckInfor checkInfor;
    private final ValidateInput validateInput;
    private final GenDataService genDataService;
    private final PasswordEncoder passwordEncoder;
    private final IdentityEventProducer identityEventProducer;
    private final AuthUserRepository authUserRepository;
    private final AuthUserProfileRepository authUserProfileRepository;
    private final AuthUserRoleRepository authUserRoleRepository;
    private final AuthAppPermissionRepository appPermissionRepository;
    private final AuthUserResourceRepository userResourceRepository;

    @Override
    @Transactional
    public UserStatusResponse leave(Long userId, String employeeCode, LeaveRequest request) {
        if (checkInfor.countValidUser(userId, employeeCode) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), ErrorCode.USER_NOT_FOUND.getDesc());
        }

        List<AuthUser> users = authUserRepository.findByUserId(userId);
        if (users == null || users.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "User not exist");
        }

        AuthUser user = users.get(0);
        if ("DELETED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DELETED.getCode(), ErrorCode.USER_DELETED.getDesc());
        }

        if ("INACTIVE".equals(user.getStatus())) {
            // Gia hạn: chỉ cần toDate
            if (request.getToDate() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(), "Ngày kết thúc không được để trống");
            }
            appPermissionRepository.extendInactiveToDate(userId, request.getToDate());
            userResourceRepository.extendInactiveToDate(userId, request.getToDate());

            return UserStatusResponse.builder()
                    .employeeCode(employeeCode)
                    .status("INACTIVE")
                    .inactiveFromDate(null)
                    .inactiveToDate(request.getToDate())
                    .build();
        }

        // ACTIVE → INACTIVE: validate cả hai ngày
        validateInput.validateLeaveRequest(request);
        user.setStatus("INACTIVE");
        authUserRepository.save(user);
        appPermissionRepository.suspendWithDates(userId, request.getFromDate(), request.getToDate());
        userResourceRepository.suspendWithDates(userId, request.getFromDate(), request.getToDate());

        // Notify auth-service để revoke session/token (findAppIdByUserId bao gồm SUSPENDED)
        List<Long> leaveAppIds = appPermissionRepository.findAppIdByUserId(userId);
        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_REVOKED_PERMISSION_NOTIFY,
                    "REVOKED_APP_PERMISSION",
                    AppPermissionRevokePayload.builder()
                            .userId(userId)
                            .employeeCode(employeeCode)
                            .revokedAppIds(leaveAppIds)
                            .revokedAt(LocalDateTime.now())
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish leave event for userId={}: {}", userId, e.getMessage());
        }

        return UserStatusResponse.builder()
                .employeeCode(employeeCode)
                .status("INACTIVE")
                .inactiveFromDate(request.getFromDate())
                .inactiveToDate(request.getToDate())
                .build();
    }

    @Override
    @Transactional
    public UserStatusResponse returnFromLeave(Long userId, String employeeCode) {
        if (checkInfor.countValidUser(userId, employeeCode) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), ErrorCode.USER_NOT_FOUND.getDesc());
        }

        List<AuthUser> users = authUserRepository.findByUserId(userId);
        if (users == null || users.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "User not exist");
        }

        AuthUser user = users.get(0);
        if ("DELETED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DELETED.getCode(), ErrorCode.USER_DELETED.getDesc());
        }

        if ("LOCKED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                    "Tài khoản đang bị khóa, không thể khôi phục qua luồng này");
        }

        if ("ACTIVE".equals(user.getStatus())) {
            return UserStatusResponse.builder()
                    .employeeCode(employeeCode)
                    .status("ACTIVE")
                    .build();
        }

        // INACTIVE → ACTIVE
        user.setStatus("ACTIVE");
        authUserRepository.save(user);
        appPermissionRepository.restoreAndClearDates(userId);
        userResourceRepository.restoreAndClearDates(userId);

        return UserStatusResponse.builder()
                .employeeCode(employeeCode)
                .status("ACTIVE")
                .build();
    }

    @Override
    @Transactional
    public UserStatusResponse offboard(Long userId, String employeeCode) {
        if (checkInfor.countValidUser(userId, employeeCode) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), ErrorCode.USER_NOT_FOUND.getDesc());
        }

        List<AuthUser> users = authUserRepository.findByUserId(userId);
        if (users == null || users.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "User not exist");
        }

        AuthUser user = users.get(0);
        if ("DELETED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DELETED.getCode(), ErrorCode.USER_DELETED.getDesc());
        }

        // Query TRƯỚC khi revoke — sau khi revoke status=REVOKED sẽ không còn trong query
        List<Long> offboardAppIds = appPermissionRepository.findAppIdByUserId(userId);

        if ("INACTIVE".equals(user.getStatus())) {
            // Phân biệt "đang tạm hoãn công tác" (có SUSPENDED) với "đã offboard" (empty)
            if (offboardAppIds.isEmpty()) {
                throw new BusinessException(ErrorCode.USER_ALREADY_OFFBOARDED.getCode(),
                        ErrorCode.USER_ALREADY_OFFBOARDED.getDesc());
            }
            // Có SUSPENDED → user đang tạm hoãn, cho phép offboard luôn
        }

        // ACTIVE hoặc INACTIVE(tạm hoãn) → offboard
        LocalDateTime now = LocalDateTime.now();
        user.setStatus("INACTIVE");
        authUserRepository.save(user);

        authUserRoleRepository.revokeAllActiveByUserId(userId);
        appPermissionRepository.revokeAllByUserId(userId, now);
        userResourceRepository.revokeAllByUserId(userId, now);

        // Notify auth-service để revoke toàn bộ session/token
        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_REVOKED_PERMISSION_NOTIFY,
                    "REVOKED_APP_PERMISSION",
                    AppPermissionRevokePayload.builder()
                            .userId(userId)
                            .employeeCode(employeeCode)
                            .revokedAppIds(offboardAppIds)
                            .revokedAt(now)
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish offboard event for userId={}: {}", userId, e.getMessage());
        }

        return UserStatusResponse.builder()
                .employeeCode(employeeCode)
                .status("INACTIVE")
                .offboardedAt(now)
                .build();
    }

    @Override
    @Transactional
    public UserStatusResponse onboard(Long userId, String employeeCode, OnboardRequest request) {
        validateInput.validateOnboardRequest(request);

        if (checkInfor.countValidUser(userId, employeeCode) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), ErrorCode.USER_NOT_FOUND.getDesc());
        }

        List<AuthUser> users = authUserRepository.findByUserId(userId);
        if (users == null || users.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "User not exist");
        }

        AuthUser user = users.get(0);
        if ("DELETED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DELETED.getCode(), ErrorCode.USER_DELETED.getDesc());
        }
        if ("ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_ACTIVE.getCode(),
                    ErrorCode.USER_ALREADY_ACTIVE.getDesc());
        }
        
        // Validate all roles and collect them
        List<AuthRole> roles = new ArrayList<>();
        for (Long roleId : request.getRoleIds()) {
            roles.add(checkInfor.checkRoleById(roleId));
        }

        checkInfor.checkPosition(request.getPositionCode());

        checkInfor.checkDepartment(request.getDepartmentId());

        // Update AUTH_USER
        String newPassword = genDataService.genPassword(12);
        user.setStatus("ACTIVE");
        user.setForceChangePassword(1);
        user.setPassword(passwordEncoder.encode(newPassword));
        authUserRepository.save(user);

        // Update AUTH_USER_PROFILE
        AuthUserProfile profile = authUserProfileRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(),
                        ErrorCode.USER_NOT_FOUND.getDesc()));
        profile.setPosition(request.getPositionCode());
        profile.setDepartmentId(request.getDepartmentId());
        profile.setJoinDate(request.getJoinDate());
        authUserProfileRepository.save(profile);

        // Reactivate existing REVOKED roles if present, otherwise insert new
        String grantedBy = RequestContext.getEmployeeCode();
        for (AuthRole role : roles) {
            Optional<AuthUserRole> existingRole = authUserRoleRepository.findByUserIdAndRoleId(userId, role.getId());
            if (existingRole.isPresent()) {
                AuthUserRole userRole = existingRole.get();
                userRole.setStatus("ACTIVE");
                userRole.setGrantedBy(grantedBy);
                userRole.setExpiredAt(null);
                authUserRoleRepository.save(userRole);
            } else {
                authUserRoleRepository.save(
                        AuthUserRole.builder()
                                .userId(userId)
                                .roleId(role.getId())
                                .grantedBy(grantedBy)
                                .expiredAt(null)
                                .build());
            }
        }

        List<String> roleCodes = roles.stream().map(AuthRole::getCode).toList();
        LocalDateTime now = LocalDateTime.now();

        // Publish Kafka events (fire-and-forget, errors only logged)
        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_CREATE_SUCCESS_USER_NOTIFY,
                    "USER_ONBOARDED",
                    UserCreatedNotificationPayload.builder()
                            .userId(userId)
                            .fullName(profile.getFullName())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .tempPassword(newPassword)
                            .changePasswordLink(null)
                            .joinDate(request.getJoinDate().toString())
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish notification event for onboard userId={}: {}", userId, e.getMessage());
        }

        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_DEFAULT_GRANT_PERMISSION_USER,
                    "USER_ONBOARDED",
                    UserCreatedPermissionPayload.builder()
                            .userId(userId)
                            .roles(roleCodes)
                            .positionCode(request.getPositionCode())
                            .departmentId(String.valueOf(request.getDepartmentId()))
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish permission event for onboard userId={}: {}", userId, e.getMessage());
        }

        return UserStatusResponse.builder()
                .employeeCode(employeeCode)
                .status("ACTIVE")
                .position(request.getPositionCode())
                .departmentId(request.getDepartmentId())
                .joinDate(request.getJoinDate())
                .onboardedAt(now)
                .build();
    }

    @Override
    @Transactional
    public UserStatusResponse transfer(Long userId, String employeeCode, TransferRequest request) {
        validateInput.validateTransferRequest(request);

        if (checkInfor.countValidUser(userId, employeeCode) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), ErrorCode.USER_NOT_FOUND.getDesc());
        }

        List<AuthUser> users = authUserRepository.findByUserId(userId);
        if (users == null || users.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "User not exist");
        }

        AuthUser user = users.get(0);
        if ("DELETED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DELETED.getCode(), ErrorCode.USER_DELETED.getDesc());
        }
        if ("INACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED.getCode(),
                    "Không thể chuyển công tác khi người dùng đang không hoạt động");
        }

        // Validate all roles and collect them
        List<AuthRole> transferRoles = new ArrayList<>();
        for (Long roleId : request.getRoleIds()) {
            transferRoles.add(checkInfor.checkRoleById(roleId));
        }
        checkInfor.checkPosition(request.getPositionCode());
        checkInfor.checkDepartment(request.getDepartmentId());

        LocalDateTime now = LocalDateTime.now();

        // Query TRƯỚC khi revoke — sau khi revoke STATUS=REVOKED sẽ không còn trong query
        List<Long> oldAppIds = appPermissionRepository.findAppIdByUserId(userId);

        // Update AUTH_USER_PROFILE
        AuthUserProfile profile = authUserProfileRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(),
                        ErrorCode.USER_NOT_FOUND.getDesc()));
        profile.setPosition(request.getPositionCode());
        profile.setDepartmentId(request.getDepartmentId());
        profile.setJoinDate(request.getTransferDate());
        authUserProfileRepository.save(profile);

        // Revoke old roles and grant new ones
        authUserRoleRepository.revokeAllActiveByUserId(userId);
        String grantedBy = RequestContext.getEmployeeCode();
        for (AuthRole role : transferRoles) {
            Optional<AuthUserRole> existingRole = authUserRoleRepository.findByUserIdAndRoleId(userId, role.getId());
            if (existingRole.isPresent()) {
                AuthUserRole userRole = existingRole.get();
                userRole.setStatus("ACTIVE");
                userRole.setGrantedBy(grantedBy);
                userRole.setExpiredAt(null);
                authUserRoleRepository.save(userRole);
            } else {
                authUserRoleRepository.save(
                        AuthUserRole.builder()
                                .userId(userId)
                                .roleId(role.getId())
                                .grantedBy(grantedBy)
                                .expiredAt(null)
                                .build());
            }
        }

        // Revoke all app permissions and resources
        appPermissionRepository.revokeAllByUserId(userId, now);
        userResourceRepository.revokeAllByUserId(userId, now);

        List<String> transferRoleCodes = transferRoles.stream().map(AuthRole::getCode).toList();

        // Publish permission grant event (fire-and-forget)
        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_DEFAULT_GRANT_PERMISSION_USER,
                    "USER_TRANSFERRED",
                    UserCreatedPermissionPayload.builder()
                            .userId(userId)
                            .roles(transferRoleCodes)
                            .positionCode(request.getPositionCode())
                            .departmentId(String.valueOf(request.getDepartmentId()))
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish permission event for transfer userId={}: {}", userId, e.getMessage());
        }

        // Notify auth-service revoke session/token cũ
        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_REVOKED_PERMISSION_NOTIFY,
                    "REVOKED_APP_PERMISSION",
                    AppPermissionRevokePayload.builder()
                            .userId(userId)
                            .employeeCode(employeeCode)
                            .revokedAppIds(oldAppIds)
                            .revokedAt(now)
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish transfer revoke event for userId={}: {}", userId, e.getMessage());
        }

        return UserStatusResponse.builder()
                .employeeCode(employeeCode)
                .status("ACTIVE")
                .position(request.getPositionCode())
                .departmentId(request.getDepartmentId())
                .transferDate(request.getTransferDate())
                .transferredAt(now)
                .build();
    }
}

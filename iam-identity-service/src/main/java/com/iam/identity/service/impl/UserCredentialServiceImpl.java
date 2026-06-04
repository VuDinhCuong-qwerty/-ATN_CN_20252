package com.iam.identity.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iam.identity.config.KafkaConfig;
import com.iam.identity.domain.AuthUser;
import com.iam.identity.dto.request.ChangePasswordRequest;
import com.iam.identity.dto.request.ResetPasswordRequest;
import com.iam.identity.dto.response.ChangePasswordResponse;
import com.iam.identity.dto.response.ResetPasswordResponse;
import com.iam.identity.enums.ErrorCode;
import com.iam.identity.exception.BusinessException;
import com.iam.identity.kafka.event.payload.UserPasswordChangedPayload;
import com.iam.identity.kafka.producer.IdentityEventProducer;
import com.iam.identity.repository.jpa.AuthUserRepository;
import com.iam.identity.service.UserCredentialService;
import com.iam.identity.service.common.CheckInfor;
import com.iam.identity.service.common.ValidateInput;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCredentialServiceImpl implements UserCredentialService {

    private final CheckInfor checkInfor;
    private final ValidateInput validateInput;
    private final PasswordEncoder passwordEncoder;
    private final IdentityEventProducer identityEventProducer;
    private final AuthUserRepository authUserRepository;

    @Override
    public ChangePasswordResponse changePassword(Long userId, String employeeCode, ChangePasswordRequest request) {
        validateInput.validateChangePasswordRequest(request);

        if (checkInfor.countValidUser(userId, employeeCode) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "EmployeeCode invalid");
        }

        List<AuthUser> users = authUserRepository.findByUserId(userId);
        if (users == null || users.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "User not exist");
        }
        AuthUser user = users.get(0);
        if ("DELETED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DELETED.getCode(), ErrorCode.USER_DELETED.getDesc());
        }
        if (!passwordEncoder.matches(request.getOldpass(), user.getPassword())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "Password invalid");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setPassword(passwordEncoder.encode(request.getNewPass()));
        user.setForceChangePassword(0);
        authUserRepository.save(user);

        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_USER_CHANGED_PASSWORD,
                    "PASSWORD_CHANGED",
                    UserPasswordChangedPayload.builder()
                            .userId(userId)
                            .employeeCode(employeeCode)
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .eventType("CHANGE")
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish password reset event for userId={}: {}", userId, e.getMessage());
        }

        return ChangePasswordResponse.builder()
                .employeeCode(employeeCode)
                .changedAt(now)
                .build();
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(Long userId, String employeeCode, ResetPasswordRequest request) {
        validateInput.validateResetPasswordRequest(request);

        if (checkInfor.countValidUser(userId, employeeCode) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "EmployeeCode invalid");
        }

        List<AuthUser> users = authUserRepository.findByUserId(userId);
        if (users == null || users.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND.getCode(), "User not exist");
        }
        AuthUser user = users.get(0);
        if ("DELETED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DELETED.getCode(), ErrorCode.USER_DELETED.getDesc());
        }

        LocalDateTime now = LocalDateTime.now();
        user.setPassword(passwordEncoder.encode(request.getNewPass()));
        user.setForceChangePassword(1);
        authUserRepository.save(user);

        try {
            identityEventProducer.publish(
                    KafkaConfig.TOPIC_USER_CHANGED_PASSWORD,
                    "PASSWORD_RESET",
                    UserPasswordChangedPayload.builder()
                            .userId(userId)
                            .employeeCode(employeeCode)
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .eventType("RESET")
                            .password(request.getNewPass())
                            .build());
        } catch (Exception e) {
            log.error("Failed to publish password reset event for userId={}: {}", userId, e.getMessage());
        }

        return ResetPasswordResponse.builder()
                .employeeCode(employeeCode)
                .resetAt(now)
                .build();
    }
}

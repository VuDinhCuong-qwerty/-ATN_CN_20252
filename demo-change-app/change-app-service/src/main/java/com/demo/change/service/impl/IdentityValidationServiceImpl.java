package com.demo.change.service.impl;

import org.springframework.stereotype.Service;

import com.demo.change.constant.ErrorCode;
import com.demo.change.exception.BusinessException;
import com.demo.change.feign.fallback.IdentityFallback;
import com.demo.change.feign.output.UserDetailResponse;
import com.demo.change.feign.output.UserSummaryResponse;
import com.demo.change.feign.output.UserSummaryResponse.Content;
import com.demo.change.service.IdentityValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityValidationServiceImpl implements IdentityValidationService {

    private final IdentityFallback identityFallback;

    @Override
    public void validateUserActive(String username) {
        Content user = findActiveUser(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "Người dùng không tồn tại hoặc không active: " + username);
        }
    }

    @Override
    public void validateUserIsCab(String username) {
        Content user = findUser(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "Người dùng không tồn tại: " + username);
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Người dùng không active: " + username);
        }

        UserDetailResponse detail = identityFallback.getUserDetail(user.getUserId(), user.getEmployeeCode());
        if (detail == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "Không lấy được thông tin chi tiết user: " + username);
        }
        boolean isCab = detail.getRoles() != null && detail.getRoles().stream()
                .anyMatch(r -> "CAB".equalsIgnoreCase(r.getRoleCode()));
        if (!isCab) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Người dùng không có role CAB: " + username);
        }
        log.info("[IdentityValidation] {} is valid CAB approver", username);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Content findUser(String username) {
        UserSummaryResponse resp = identityFallback.getUsers(username, null, null, null, null, null, 0, 1);
        if (resp == null || resp.getContent() == null || resp.getContent().isEmpty()) {
            return null;
        }
        return resp.getContent().stream()
                .filter(c -> username.equalsIgnoreCase(c.getUsername()))
                .findFirst()
                .orElse(null);
    }

    private Content findActiveUser(String username) {
        UserSummaryResponse resp = identityFallback.getUsers(username, null, null, "ACTIVE", null, null, 0, 1);
        if (resp == null || resp.getContent() == null || resp.getContent().isEmpty()) {
            return null;
        }
        return resp.getContent().stream()
                .filter(c -> username.equalsIgnoreCase(c.getUsername()))
                .findFirst()
                .orElse(null);
    }
}

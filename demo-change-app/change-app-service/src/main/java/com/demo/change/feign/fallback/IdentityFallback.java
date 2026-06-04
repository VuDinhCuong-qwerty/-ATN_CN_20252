package com.demo.change.feign.fallback;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.demo.change.constant.ApiResponse;
import com.demo.change.constant.ErrorCode;
import com.demo.change.exception.BusinessException;
import com.demo.change.feign.IdentityClient;
import com.demo.change.feign.output.UserDetailResponse;
import com.demo.change.feign.output.UserSummaryResponse;
import com.demo.change.util.Utility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdentityFallback {

    private final IdentityClient identityClient;

    public UserSummaryResponse getUsers(
            String username, String employeeCode, Long departmentId, String status,
            Boolean onLeave, Boolean offboarded, Integer page, Integer size) {
        try {
            log.info("[Identity] Request GET /users username={} employeeCode={} page={} size={}",
                    username, employeeCode, page, size);
            ApiResponse<UserSummaryResponse> response = identityClient.getUsers(
                    username, employeeCode, departmentId, status, onLeave, offboarded, page, size);
            log.info("[Identity] Response GET /users → {}", Utility.toJson(response));
            return response.getData();
        } catch (WebClientResponseException e) {
            log.warn("[Identity] Response GET /users error {}: {}", e.getStatusCode(), e.getMessage());
            throw new BusinessException(ErrorCode.UNKNOWN, "Identity service lỗi: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("[Identity] Response GET /users unavailable: {}", e.getMessage());
            throw new BusinessException(ErrorCode.UNKNOWN, "Identity service không khả dụng");
        }
    }

    public UserDetailResponse getUserDetail(Long userId, String employeeCode) {
        try {
            log.info("[Identity] GET /users/detail userId={} employeeCode={}", userId, employeeCode);
            ApiResponse<UserDetailResponse> response = identityClient.getUserDetail(userId, employeeCode);
            log.info("[Identity] GET /users/detail → userId={}", userId);
            return response.getData();
        } catch (WebClientResponseException e) {
            log.warn("[Identity] GET /users/detail error {}: {}", e.getStatusCode(), e.getMessage());
            throw new BusinessException(ErrorCode.UNKNOWN, "Identity service lỗi: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("[Identity] GET /users/detail unavailable: {}", e.getMessage());
            throw new BusinessException(ErrorCode.UNKNOWN, "Identity service không khả dụng");
        }
    }

    public UserSummaryResponse searchUsers(String username) {
        try {
            log.info("[Identity] GET /users?username={}&size=10 (search)", username);
            ApiResponse<UserSummaryResponse> response = identityClient.getUsers(
                    username, null, null, null, null, null, null, 10);
            log.info("[Identity] searchUsers '{}' → {} results", username,
                    response.getData() != null ? response.getData().getTotalElement() : 0);
            return response.getData();
        } catch (WebClientResponseException e) {
            log.warn("[Identity] searchUsers error {}: {}", e.getStatusCode(), e.getMessage());
            throw new BusinessException(ErrorCode.UNKNOWN, "Identity service lỗi: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("[Identity] searchUsers unavailable: {}", e.getMessage());
            throw new BusinessException(ErrorCode.UNKNOWN, "Identity service không khả dụng");
        }
    }
}

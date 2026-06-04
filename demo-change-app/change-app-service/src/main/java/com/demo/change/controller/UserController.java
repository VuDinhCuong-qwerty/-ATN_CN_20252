package com.demo.change.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.demo.change.constant.ApiResponse;
import com.demo.change.feign.output.UserDetailResponse;
import com.demo.change.feign.output.UserSummaryResponse;
import com.demo.change.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('change-mgmt/change-request:view')")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String employeeCode,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean onLeave,
            @RequestParam(required = false) Boolean offboarded,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        log.info("[UserController] GET /api/users username={} employeeCode={} status={}", username, employeeCode, status);
        UserSummaryResponse data = userService.getUsers(username, employeeCode, departmentId,
                status, onLeave, offboarded, page, size);
        return ResponseEntity.ok(ApiResponse.ok(data, request.getRequestURI()));
    }

    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('change-mgmt/change-request:view')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserDetail(
            @RequestParam Long userId,
            @RequestParam String employeeCode,
            HttpServletRequest request) {
        log.info("[UserController] GET /api/users/detail userId={} employeeCode={}", userId, employeeCode);
        UserDetailResponse data = userService.getUserDetail(userId, employeeCode);
        return ResponseEntity.ok(ApiResponse.ok(data, request.getRequestURI()));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('change-mgmt/change-request:view')")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> searchUsers(
            @RequestParam String username,
            HttpServletRequest request) {
        log.info("[UserController] GET /api/users/search username={}", username);
        UserSummaryResponse data = userService.searchUsers(username);
        return ResponseEntity.ok(ApiResponse.ok(data, request.getRequestURI()));
    }
}

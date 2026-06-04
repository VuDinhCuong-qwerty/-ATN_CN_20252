package com.iam.identity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iam.identity.dto.request.LeaveRequest;
import com.iam.identity.dto.request.OnboardRequest;
import com.iam.identity.dto.request.TransferRequest;
import com.iam.identity.dto.response.ApiResponse;
import com.iam.identity.dto.response.UserStatusResponse;
import com.iam.identity.service.UserLifecycleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/users")
@Slf4j
@RequiredArgsConstructor
public class UserLifecycleController {

    private static final String BASE = "/iam-identity-service/users";

    private final UserLifecycleService userLifecycleService;

    // ── POST /users/leave?userId=&employeeCode= ───────────────────────────────

    @PostMapping("/leave")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<UserStatusResponse>> leave(
            @RequestParam Long userId,
            @RequestParam String employeeCode,
            @RequestBody LeaveRequest request) {
        UserStatusResponse response = userLifecycleService.leave(userId, employeeCode, request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/leave"));
    }

    // ── POST /users/return?userId=&employeeCode= ──────────────────────────────

    @PostMapping("/return")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<UserStatusResponse>> returnFromLeave(
            @RequestParam Long userId,
            @RequestParam String employeeCode) {
        UserStatusResponse response = userLifecycleService.returnFromLeave(userId, employeeCode);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/return"));
    }

    // ── POST /users/offboard?userId=&employeeCode= ────────────────────────────

    @PostMapping("/offboard")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<UserStatusResponse>> offboard(
            @RequestParam Long userId,
            @RequestParam String employeeCode) {
        UserStatusResponse response = userLifecycleService.offboard(userId, employeeCode);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/offboard"));
    }

    // ── POST /users/onboard?userId=&employeeCode= ─────────────────────────────

    @PostMapping("/onboard")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<UserStatusResponse>> onboard(
            @RequestParam Long userId,
            @RequestParam String employeeCode,
            @RequestBody OnboardRequest request) {
        UserStatusResponse response = userLifecycleService.onboard(userId, employeeCode, request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/onboard"));
    }

    // ── POST /users/transfer?userId=&employeeCode= ────────────────────────────

    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<UserStatusResponse>> transfer(
            @RequestParam Long userId,
            @RequestParam String employeeCode,
            @RequestBody TransferRequest request) {
        UserStatusResponse response = userLifecycleService.transfer(userId, employeeCode, request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/transfer"));
    }
}

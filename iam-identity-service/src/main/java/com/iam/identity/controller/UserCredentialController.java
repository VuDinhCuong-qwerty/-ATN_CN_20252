package com.iam.identity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iam.identity.dto.request.ChangePasswordRequest;
import com.iam.identity.dto.request.ResetPasswordRequest;
import com.iam.identity.dto.response.ApiResponse;
import com.iam.identity.dto.response.ChangePasswordResponse;
import com.iam.identity.dto.response.ResetPasswordResponse;
import com.iam.identity.service.UserCredentialService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/users")
@Slf4j
@RequiredArgsConstructor
public class UserCredentialController {

    private static final String BASE = "/iam-identity-service/users";

    private final UserCredentialService userCredentialService;

    // ── POST /users/change-password ──────────────────────────────────────────

    @PostMapping("/change-password")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<ChangePasswordResponse>> changePassword(
            @RequestBody ChangePasswordRequest request) {
        ChangePasswordResponse response = userCredentialService.changePassword(
                request.getUserId(), request.getEmployeeCode(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/change-password"));
    }

    // ── POST /users/reset-password ───────────────────────────────────────────

    @PostMapping("/reset-password")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(
            @RequestBody ResetPasswordRequest request) {
        ResetPasswordResponse response = userCredentialService.resetPassword(
                request.getUserId(), request.getEmployeeCode(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/reset-password"));
    }
}

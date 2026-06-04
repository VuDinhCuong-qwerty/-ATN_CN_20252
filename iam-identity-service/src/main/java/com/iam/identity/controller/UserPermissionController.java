package com.iam.identity.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iam.identity.dto.request.ApprovePermissionRequest;
import com.iam.identity.dto.request.AssignRoleRequest;
import com.iam.identity.dto.request.CreatePermissionRequestRequest;
import com.iam.identity.dto.request.RejectPermissionRequest;
import com.iam.identity.dto.request.RevokeAppPermissionRequest;
import com.iam.identity.dto.request.RevokeResourcePermissionRequest;
import com.iam.identity.dto.request.SubmitRequest;
import com.iam.identity.dto.request.UpdatePermissionRequestRequest;
import com.iam.identity.dto.response.ApiResponse;
import com.iam.identity.dto.response.AppPermissionResponse;
import com.iam.identity.dto.response.GetAllPermissionRequestResponse;
import com.iam.identity.dto.response.GetDetailPermissionRequest;
import com.iam.identity.dto.response.PermissionRequestResponse;
import com.iam.identity.dto.response.ResourcePermissionResponse;
import com.iam.identity.dto.response.RevokeResourcePermissionResponse;
import com.iam.identity.dto.response.UpdatePermisssionRequestResponse;
import com.iam.identity.dto.response.UserRoleResponse;
import com.iam.identity.service.UserPermissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
public class UserPermissionController {

    private static final String USERS_BASE = "/iam-identity-service/users";
    private static final String PR_BASE = "/iam-identity-service/permission-requests";

    private final UserPermissionService userPermissionService;

    // ── Phase 4.1: Role ───────────────────────────────────────────────────────

    @GetMapping("/users/roles")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<Page<UserRoleResponse>>> getUserRoles(
            @RequestParam String employeeCode,
            Pageable pageable) {
        Page<UserRoleResponse> response = userPermissionService.getUserRoles(employeeCode, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, USERS_BASE + "/roles"));
    }

    @PostMapping("/users/roles")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<UserRoleResponse>> assignRole(
            @RequestParam String employeeCode,
            @RequestBody AssignRoleRequest request) {
        UserRoleResponse response = userPermissionService.assignRole(employeeCode, request);
        return ResponseEntity.ok(ApiResponse.ok(response, USERS_BASE + "/roles"));
    }

    @PostMapping("/users/roles/revoke")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<Void>> revokeRole(
            @RequestParam String employeeCode,
            @RequestParam String roleCode) {
        userPermissionService.revokeRole(employeeCode, roleCode);
        return ResponseEntity.ok(ApiResponse.ok(null, USERS_BASE + "/roles/revoke"));
    }

    // ── Phase 4.2: App Permission ─────────────────────────────────────────────

    @GetMapping("/users/{employeeCode}/app-permissions")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<Page<AppPermissionResponse>>> getAppPermissions(
            @PathVariable String employeeCode,
            Pageable pageable) {
        Page<AppPermissionResponse> response = userPermissionService.getAppPermissions(employeeCode, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, USERS_BASE + "/app-permissions"));
    }

    @PostMapping("/users/{employeeCode}/app-permissions/revoke")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<Void>> revokeAppPermission(
            @PathVariable String employeeCode,
            @RequestBody @Valid RevokeAppPermissionRequest request) {
        userPermissionService.revokeAppPermission(employeeCode, request);
        return ResponseEntity.ok(ApiResponse.ok(null, USERS_BASE + "/app-permissions/revoke"));
    }

    // ── Phase 4.3: Resource Permission ───────────────────────────────────────

    @GetMapping("/users/{employeeCode}/resource-permissions")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<Page<ResourcePermissionResponse>>> getResourcePermissions(
            @PathVariable String employeeCode,
            Pageable pageable) {
        Page<ResourcePermissionResponse> response = userPermissionService.getResourcePermissions(employeeCode,
                pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, USERS_BASE + "/resource-permissions"));
    }

    @PostMapping("/users/resource-permissions/revoke")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<RevokeResourcePermissionResponse>> revokeResourcePermission(
            @RequestParam String employeeCode,
            @RequestBody RevokeResourcePermissionRequest request) {
        RevokeResourcePermissionResponse response = userPermissionService.revokeResourcePermission(employeeCode,
                request);
        return ResponseEntity.ok(ApiResponse.ok(response, USERS_BASE + "/resource-permissions/revoke"));
    }

    // ── Phase 4.4: Luồng xin quyền ───────────────────────────────────────────

    @PostMapping("/permission-requests")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<PermissionRequestResponse>> createPermissionRequest(
            @RequestBody CreatePermissionRequestRequest request) {
        PermissionRequestResponse response = userPermissionService.createPermissionRequest(request);
        return ResponseEntity.ok(ApiResponse.ok(response, PR_BASE));
    }

    @PostMapping("/permission-requests/submit")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<PermissionRequestResponse>> postMethodName(
            @RequestBody @Valid SubmitRequest request) {
        PermissionRequestResponse response = userPermissionService.submitRequest(request);
        return ResponseEntity.ok(ApiResponse.ok(response, PR_BASE + "/submit"));
    }

    @GetMapping("/permission-requests")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<Page<GetAllPermissionRequestResponse>>> getPermissionRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String requester,
            @RequestParam(required = false) String reviewer,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        Page<GetAllPermissionRequestResponse> response = userPermissionService.getPermissionRequests(status, requester,
                reviewer, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, PR_BASE));
    }

    @GetMapping("/permission-requests/detail")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<GetDetailPermissionRequest>> getPermissionRequestById(
            @RequestParam String requestId) {
        GetDetailPermissionRequest response = userPermissionService.getPermissionRequestById(requestId);
        return ResponseEntity.ok(ApiResponse.ok(response, PR_BASE + "/detail"));
    }

    @PostMapping("/permission-requests/update")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<UpdatePermisssionRequestResponse>> updatePermissionRequest(
        @RequestBody @Valid UpdatePermissionRequestRequest request
    ) {
       UpdatePermisssionRequestResponse response =  userPermissionService.updatePermissionRequest(request);
       return ResponseEntity.ok(ApiResponse.ok(response, PR_BASE + "/update"));
    }

    // ── Phase 4.5: CAB duyệt / từ chối / thu hồi ─────────────────────────────

    @PostMapping("/permission-requests/approve")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<PermissionRequestResponse>> approvePermissionRequest(
            @RequestBody ApprovePermissionRequest request) {
        PermissionRequestResponse response = userPermissionService.approvePermissionRequest(request);
        return ResponseEntity.ok(ApiResponse.ok(response, PR_BASE + "/approve"));
    }

    @PostMapping("/permission-requests/reject")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<PermissionRequestResponse>> rejectPermissionRequest(
            @RequestBody RejectPermissionRequest request) {
        PermissionRequestResponse response = userPermissionService.rejectPermissionRequest(request);
        return ResponseEntity.ok(ApiResponse.ok(response, PR_BASE + "/reject"));
    }

    @PostMapping("/permission-requests/cancel")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<PermissionRequestResponse>> cancelPermissionRequest(
            @RequestParam String requestId) {
        PermissionRequestResponse response = userPermissionService.cancelPermissionRequest(requestId);
        return ResponseEntity.ok(ApiResponse.ok(response, PR_BASE + "/cancel"));
    }
}

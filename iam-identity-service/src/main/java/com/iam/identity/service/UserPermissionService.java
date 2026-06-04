package com.iam.identity.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.iam.identity.dto.request.ApprovePermissionRequest;
import com.iam.identity.dto.request.AssignRoleRequest;
import com.iam.identity.dto.request.CreatePermissionRequestRequest;
import com.iam.identity.dto.request.RejectPermissionRequest;
import com.iam.identity.dto.request.RevokeAppPermissionRequest;
import com.iam.identity.dto.request.RevokeResourcePermissionRequest;
import com.iam.identity.dto.request.SubmitRequest;
import com.iam.identity.dto.request.UpdatePermissionRequestRequest;
import com.iam.identity.dto.response.AppPermissionResponse;
import com.iam.identity.dto.response.GetAllPermissionRequestResponse;
import com.iam.identity.dto.response.GetDetailPermissionRequest;
import com.iam.identity.dto.response.PermissionRequestResponse;
import com.iam.identity.dto.response.ResourcePermissionResponse;
import com.iam.identity.dto.response.RevokeAppPermissionResponse;
import com.iam.identity.dto.response.RevokeResourcePermissionResponse;
import com.iam.identity.dto.response.UpdatePermisssionRequestResponse;
import com.iam.identity.dto.response.UserRoleResponse;

public interface UserPermissionService {

    // ── Phase 4.1: Role ───────────────────────────────────────────────────────

    Page<UserRoleResponse> getUserRoles(String employeeCode, Pageable pageable);

    UserRoleResponse assignRole(String employeeCode, AssignRoleRequest request);

    void revokeRole(String employeeCode, String roleCode);

    // ── Phase 4.2: App Permission ─────────────────────────────────────────────

    Page<AppPermissionResponse> getAppPermissions(String employeeCode, Pageable pageable);

    RevokeAppPermissionResponse revokeAppPermission(String employeeCode, RevokeAppPermissionRequest request);

    // ── Phase 4.3: Resource Permission ───────────────────────────────────────

    Page<ResourcePermissionResponse> getResourcePermissions(String employeeCode, Pageable pageable);

    RevokeResourcePermissionResponse revokeResourcePermission(String employeeCode, RevokeResourcePermissionRequest request);

    // ── Phase 4.4: Luồng xin quyền ───────────────────────────────────────────

    PermissionRequestResponse createPermissionRequest(CreatePermissionRequestRequest request);

    PermissionRequestResponse submitRequest(SubmitRequest request);

    GetDetailPermissionRequest getPermissionRequestById(String requestId);

    Page<GetAllPermissionRequestResponse> getPermissionRequests(String status, String requester, String reviewer,
            LocalDate from, LocalDate to, Pageable pageable);

    // ── Phase 4.5: CAB duyệt / từ chối / thu hồi ─────────────────────────────

    PermissionRequestResponse approvePermissionRequest(ApprovePermissionRequest request);

    PermissionRequestResponse rejectPermissionRequest(RejectPermissionRequest request);

    // ── Phase 4.6: Người dùng huỷ request ────────────────────────────────────

    PermissionRequestResponse cancelPermissionRequest(String requestId);

    UpdatePermisssionRequestResponse updatePermissionRequest(UpdatePermissionRequestRequest request);
}

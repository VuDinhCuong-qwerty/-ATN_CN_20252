package com.iam.app.controller;

import com.iam.app.dto.request.BatchUpdateDefaultAppPermissionStatusRequest;
import com.iam.app.dto.request.BatchUpdateDefaultResourcePermissionRequest;
import com.iam.app.dto.request.CreateDefaultAppPermissionRequest;
import com.iam.app.dto.request.CreateDefaultResourcePermissionRequest;
import com.iam.app.dto.response.ApiResponse;
import com.iam.app.dto.response.DefaultAppPermissionResponse;
import com.iam.app.dto.response.DefaultPermissionPreviewResponse;
import com.iam.app.dto.response.DefaultResourcePermissionResponse;
import com.iam.app.service.DefaultAppPermissionService;
import com.iam.app.service.DefaultPermissionPreviewService;
import com.iam.app.service.DefaultResourcePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/default-permissions")
public class DefaultPermissionController {

    private static final String BASE_URL = "/iam-app-service/default-permissions";
    private final DefaultAppPermissionService defaultAppPermissionService;
    private final DefaultResourcePermissionService defaultResourcePermissionService;
    private final DefaultPermissionPreviewService defaultPermissionPreviewService;

    // ── Application permissions ────────────────────────────────────────────────

    @GetMapping("/applications")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<Page<DefaultAppPermissionResponse>>> getAppPermissions(
            @RequestParam(required = false) String roleId,
            @RequestParam(required = false) String positionCode,
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<DefaultAppPermissionResponse> result = defaultAppPermissionService
                .getPermissions(roleId, positionCode, applicationId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result, BASE_URL + "/applications"));
    }

    @PostMapping("/applications/batch")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<List<DefaultAppPermissionResponse>>> createAppPermissions(
            @Valid @RequestBody CreateDefaultAppPermissionRequest request) {

        List<DefaultAppPermissionResponse> result = defaultAppPermissionService.createPermissions(request);
        return ResponseEntity.ok(ApiResponse.ok(result, BASE_URL + "/applications/batch"));
    }

    @PostMapping("/applications/batch/status")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<List<DefaultAppPermissionResponse>>> updateAppPermissionStatuses(
            @Valid @RequestBody BatchUpdateDefaultAppPermissionStatusRequest request) {

        List<DefaultAppPermissionResponse> result = defaultAppPermissionService.updateStatuses(request);
        return ResponseEntity.ok(ApiResponse.ok(result, BASE_URL + "/applications/batch/status"));
    }

    // ── Resource permissions ───────────────────────────────────────────────────

    @GetMapping("/resources")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<Page<DefaultResourcePermissionResponse>>> getResourcePermissions(
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String positionCode,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<DefaultResourcePermissionResponse> result = defaultResourcePermissionService
                .getPermissions(roleId, positionCode, resourceId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result, BASE_URL + "/resources"));
    }

    @PostMapping("/resources/batch")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<List<DefaultResourcePermissionResponse>>> createResourcePermissions(
            @Valid @RequestBody CreateDefaultResourcePermissionRequest request) {

        List<DefaultResourcePermissionResponse> result = defaultResourcePermissionService.createPermissions(request);
        return ResponseEntity.ok(ApiResponse.ok(result, BASE_URL + "/resources/batch"));
    }

    @PostMapping("/resources/batch/update")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<List<DefaultResourcePermissionResponse>>> updateResourcePermissions(
            @Valid @RequestBody BatchUpdateDefaultResourcePermissionRequest request) {

        List<DefaultResourcePermissionResponse> result = defaultResourcePermissionService.updatePermissions(request);
        return ResponseEntity.ok(ApiResponse.ok(result, BASE_URL + "/resources/batch/update"));
    }

    // ── Preview ────────────────────────────────────────────────────────────────

    @GetMapping("/preview")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<DefaultPermissionPreviewResponse>> previewPermissions(
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String positionCode) {

        DefaultPermissionPreviewResponse result = defaultPermissionPreviewService.preview(roleCode, positionCode);
        return ResponseEntity.ok(ApiResponse.ok(result, BASE_URL + "/preview"));
    }
}

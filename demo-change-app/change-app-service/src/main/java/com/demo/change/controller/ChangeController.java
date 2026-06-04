package com.demo.change.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.demo.change.constant.ApiResponse;
import com.demo.change.dto.request.ApproveActionRequest;
import com.demo.change.dto.request.CreateChangeRequest;
import com.demo.change.dto.request.UpdateChangeRequest;
import com.demo.change.dto.request.UpdateChecklistStatusRequest;
import com.demo.change.dto.response.ChangeDetailResponse;
import com.demo.change.dto.response.ChangeListItemResponse;
import com.demo.change.dto.response.PageResponse;
import com.demo.change.service.ChangeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/changes")
@RequiredArgsConstructor
@Slf4j
public class ChangeController {

    private final ChangeService changeService;

    @GetMapping
    @PreAuthorize("hasAuthority('change-mgmt/change-request:view')")
    public ResponseEntity<ApiResponse<PageResponse<ChangeListItemResponse>>> getChanges(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdByCode,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        log.info("[ChangeController] GET /api/changes status={} createdByCode={} page={}", status, createdByCode, page);
        PageResponse<ChangeListItemResponse> data = changeService.getChanges(status, createdByCode, fromDate, toDate, page, size);
        return ResponseEntity.ok(ApiResponse.ok(data, request.getRequestURI()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('change-mgmt/change-request:create')")
    public ResponseEntity<ApiResponse<ChangeListItemResponse>> createChange(
            @Valid @RequestBody CreateChangeRequest body,
            Authentication authentication,
            HttpServletRequest request) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        String createdBy = jwt.getToken().getClaimAsString("username");
        String createdByCode = jwt.getToken().getClaimAsString("employeeCode");
        log.info("[ChangeController] POST /api/changes by={}/{}", createdBy, createdByCode);
        ChangeListItemResponse data = changeService.createChange(body, createdBy, createdByCode);
        return ResponseEntity.ok(ApiResponse.ok(data, request.getRequestURI()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('change-mgmt/change-request:view')")
    public ResponseEntity<ApiResponse<ChangeDetailResponse>> getChangeDetail(
            @PathVariable Long id,
            HttpServletRequest request) {
        log.info("[ChangeController] GET /api/changes/{}", id);
        ChangeDetailResponse data = changeService.getChangeDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(data, request.getRequestURI()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('change-mgmt/change-request:update')")
    public ResponseEntity<ApiResponse<Void>> updateChange(
            @PathVariable Long id,
            @Valid @RequestBody UpdateChangeRequest body,
            Authentication authentication,
            HttpServletRequest request) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        String updatedBy = jwt.getToken().getClaimAsString("username");
        String updatedByCode = jwt.getToken().getClaimAsString("employeeCode");
        log.info("[ChangeController] PUT /api/changes/{} by={}/{}", id, updatedBy, updatedByCode);
        changeService.updateChange(id, body, updatedBy, updatedByCode);
        return ResponseEntity.ok(ApiResponse.ok(null, request.getRequestURI()));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('change-mgmt/change-request:update')")
    public ResponseEntity<ApiResponse<Void>> submitChange(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        String submittedBy = jwt.getToken().getClaimAsString("username");
        String submittedByCode = jwt.getToken().getClaimAsString("employeeCode");
        log.info("[ChangeController] POST /api/changes/{}/submit by={}/{}", id, submittedBy, submittedByCode);
        changeService.submitChange(id, submittedBy, submittedByCode);
        return ResponseEntity.ok(ApiResponse.ok(null, request.getRequestURI()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('change-mgmt/change-request:approve')")
    public ResponseEntity<ApiResponse<Void>> approveChange(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveActionRequest body,
            Authentication authentication,
            HttpServletRequest request) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        String approverUsername = jwt.getToken().getClaimAsString("username");
        String approverCode = jwt.getToken().getClaimAsString("employeeCode");
        String note = (body != null) ? body.getNote() : null;
        log.info("[ChangeController] POST /api/changes/{}/approve by={}/{}", id, approverUsername, approverCode);
        changeService.approveChange(id, approverUsername, approverCode, note);
        return ResponseEntity.ok(ApiResponse.ok(null, request.getRequestURI()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('change-mgmt/change-request:approve')")
    public ResponseEntity<ApiResponse<Void>> rejectChange(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveActionRequest body,
            Authentication authentication,
            HttpServletRequest request) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        String approverUsername = jwt.getToken().getClaimAsString("username");
        String approverCode = jwt.getToken().getClaimAsString("employeeCode");
        String note = (body != null) ? body.getNote() : null;
        log.info("[ChangeController] POST /api/changes/{}/reject by={}/{}", id, approverUsername, approverCode);
        changeService.rejectChange(id, approverUsername, approverCode, note);
        return ResponseEntity.ok(ApiResponse.ok(null, request.getRequestURI()));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('change-mgmt/change-request:execute')")
    public ResponseEntity<ApiResponse<Void>> executeChange(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        String executedBy = jwt.getToken().getClaimAsString("username");
        String executedByCode = jwt.getToken().getClaimAsString("employeeCode");
        log.info("[ChangeController] POST /api/changes/{}/execute by={}/{}", id, executedBy, executedByCode);
        changeService.executeChange(id, executedBy, executedByCode);
        return ResponseEntity.ok(ApiResponse.ok(null, request.getRequestURI()));
    }

    @PostMapping("/{changeId}/checklist/{itemId}/status")
    @PreAuthorize("hasAuthority('change-mgmt/change-request:update')")
    public ResponseEntity<ApiResponse<Void>> updateChecklistItemStatus(
            @PathVariable Long changeId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateChecklistStatusRequest body,
            Authentication authentication,
            HttpServletRequest request) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        String username = jwt.getToken().getClaimAsString("username");
        log.info("[ChangeController] POST /api/changes/{}/checklist/{}/status taskStatus={} by={}", changeId, itemId, body.getTaskStatus(), username);
        changeService.updateChecklistItemStatus(changeId, itemId, body.getTaskStatus(), username);
        return ResponseEntity.ok(ApiResponse.ok(null, request.getRequestURI()));
    }

    @PostMapping("/{id}/result")
    @PreAuthorize("hasAuthority('change-mgmt/change-request:execute')")
    public ResponseEntity<ApiResponse<Void>> finalizeResult(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        String username = jwt.getToken().getClaimAsString("username");
        String userCode = jwt.getToken().getClaimAsString("employeeCode");
        log.info("[ChangeController] POST /api/changes/{}/result by={}/{}", id, username, userCode);
        changeService.finalizeResult(id, username, userCode);
        return ResponseEntity.ok(ApiResponse.ok(null, request.getRequestURI()));
    }
}

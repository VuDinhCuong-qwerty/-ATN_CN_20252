package com.demo.change.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.demo.change.constant.ApiResponse;
import com.demo.change.dto.response.AuditLogResponse;
import com.demo.change.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/changes/{changeId}/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('change-mgmt/change-request:view')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogs(
            @PathVariable Long changeId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(auditLogService.getAuditLogs(changeId), request.getRequestURI()));
    }
}

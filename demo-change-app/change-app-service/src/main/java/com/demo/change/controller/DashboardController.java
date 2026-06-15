package com.demo.change.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.change.constant.ApiResponse;
import com.demo.change.dto.response.DashboardStatsResponse;
import com.demo.change.service.DashboardService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('change-mgmt/change-request:view')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getStats(), request.getRequestURI()));
    }
}

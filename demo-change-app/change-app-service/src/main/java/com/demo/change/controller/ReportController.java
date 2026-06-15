package com.demo.change.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.change.constant.ApiResponse;
import com.demo.change.dto.response.ReportSummaryResponse;
import com.demo.change.service.ReportService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('change-mgmt/change-report:view')")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getSummary(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getSummary(), request.getRequestURI()));
    }
}

package com.iam.app.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iam.app.dto.request.CreateApplicationRequest;
import com.iam.app.dto.request.ToggleStatusRequest;
import com.iam.app.dto.request.UpdateApplicationRequest;
import com.iam.app.dto.response.ApiResponse;
import com.iam.app.dto.response.ApplicationResponse;
import com.iam.app.dto.response.GetApplicationsResponse;
import com.iam.app.dto.response.GetDetailAppResponse;
import com.iam.app.service.ApplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/applications")
public class ApplicationController {

    private final String BASE_URL = "/iam-app-service/applications";
    private final ApplicationService appService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<Page<GetApplicationsResponse>>> getApps(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String group,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<GetApplicationsResponse> response = appService.getApps(type, status, serviceCode, name, group, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<GetDetailAppResponse>> getDetailApp(
        @PathVariable("id") Long id) {
            GetDetailAppResponse response = this.appService.getDetailApp(id);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + String.valueOf(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> createApp(
            @RequestBody @Valid CreateApplicationRequest body) {
        ApplicationResponse response = appService.createApp(body);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL));
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApp(
            @PathVariable Long id,
            @RequestBody @Valid UpdateApplicationRequest body) {
        ApplicationResponse response = appService.updateApp(id, body);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + id + "/update"));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> toggleAppStatus(
            @PathVariable Long id,
            @RequestBody @Valid ToggleStatusRequest body) {
        ApplicationResponse response = appService.toggleAppStatus(id, body.getStatus());
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + id + "/status"));
    }
}
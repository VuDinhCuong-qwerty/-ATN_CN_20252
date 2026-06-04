package com.iam.app.controller;

import com.iam.app.dto.request.CreateAppGroupRequest;
import com.iam.app.dto.request.UpdateAppGroupRequest;
import com.iam.app.dto.response.ApiResponse;
import com.iam.app.dto.response.AppGroupResponse;
import com.iam.app.service.AppGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-groups")
public class AppGroupController {

    private static final String BASE_URL = "/iam-app-service/app-groups";

    private final AppGroupService appGroupService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<List<AppGroupResponse>>> getAppGroups(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Integer status) {

        List<AppGroupResponse> data = appGroupService.getAppGroups(id, status);
        return ResponseEntity.ok(ApiResponse.ok(data, BASE_URL));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<AppGroupResponse>> createAppGroup(
            @RequestBody @Valid CreateAppGroupRequest body) {

        AppGroupResponse data = appGroupService.createAppGroup(body);
        return ResponseEntity.ok(ApiResponse.ok(data, BASE_URL));
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<AppGroupResponse>> updateAppGroup(
            @PathVariable Long id,
            @RequestBody @Valid UpdateAppGroupRequest body) {

        AppGroupResponse data = appGroupService.updateAppGroup(id, body);
        return ResponseEntity.ok(ApiResponse.ok(data, BASE_URL + "/" + id + "/update"));
    }
}

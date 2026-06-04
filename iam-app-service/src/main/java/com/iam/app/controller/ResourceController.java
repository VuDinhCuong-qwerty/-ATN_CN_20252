package com.iam.app.controller;

import com.iam.app.dto.request.CreateResourceRequest;
import com.iam.app.dto.request.UpdateResourceRequest;
import com.iam.app.dto.response.ApiResponse;
import com.iam.app.dto.response.BatchCreateResourceResponse;
import com.iam.app.dto.response.ResourceDetailResponse;
import com.iam.app.dto.response.ResourceListResponse;
import com.iam.app.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/applications/{appId}/resources")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<Page<ResourceListResponse>>> getResources(
            @PathVariable Long appId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String resourceCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ResourceListResponse> data = resourceService.getResources(appId, status, type, name, resourceCode, pageable);
        String path = "/iam-app-service/applications/" + appId + "/resources";
        return ResponseEntity.ok(ApiResponse.ok(data, path));
    }

    @GetMapping("/{resourceId}")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<ResourceDetailResponse>> getResourceDetail(
            @PathVariable Long appId,
            @PathVariable Long resourceId) {

        ResourceDetailResponse data = resourceService.getResourceDetail(appId, resourceId);
        String path = "/iam-app-service/applications/" + appId + "/resources/" + resourceId;
        return ResponseEntity.ok(ApiResponse.ok(data, path));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<BatchCreateResourceResponse>> createResources(
            @PathVariable Long appId,
            @RequestBody @Valid CreateResourceRequest body) {

        BatchCreateResourceResponse data = resourceService.createResources(appId, body);
        String path = "/iam-app-service/applications/" + appId + "/resources/batch";
        return ResponseEntity.ok(ApiResponse.ok(data, path));
    }

    @PostMapping("/{resourceId}/update")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<ResourceDetailResponse>> updateResource(
            @PathVariable Long appId,
            @PathVariable Long resourceId,
            @RequestBody @Valid UpdateResourceRequest body) {

        ResourceDetailResponse data = resourceService.updateResource(appId, resourceId, body);
        String path = "/iam-app-service/applications/" + appId + "/resources/" + resourceId + "/update";
        return ResponseEntity.ok(ApiResponse.ok(data, path));
    }

}

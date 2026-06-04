package com.iam.app.controller;

import com.iam.app.dto.request.CreateFlowRequest;
import com.iam.app.dto.request.UpdateFlowRequest;
import com.iam.app.dto.response.ApiResponse;
import com.iam.app.dto.response.FlowDetailResponse;
import com.iam.app.dto.response.FlowListResponse;
import com.iam.app.service.FlowService;
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
@RequestMapping("/applications")
public class FlowController {

    private final String BASE_URL = "/iam-app-service/applications";
    private final FlowService flowService;

    @GetMapping("/{appId}/flows")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<List<FlowListResponse>>> getFlows(
            @PathVariable Long appId,
            @RequestParam(required = false) String status) {

        List<FlowListResponse> response = flowService.getFlows(appId, status);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + appId + "/flows"));
    }

    @GetMapping("/{appId}/flows/{flowId}")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<FlowDetailResponse>> getFlowDetail(
            @PathVariable Long appId,
            @PathVariable Long flowId) {

        FlowDetailResponse response = flowService.getFlowDetail(appId, flowId);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + appId + "/flows/" + flowId));
    }

    @PostMapping("/{appId}/flows")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<FlowDetailResponse>> createFlow(
            @PathVariable Long appId,
            @Valid @RequestBody CreateFlowRequest request) {

        FlowDetailResponse response = flowService.createFlow(appId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + appId + "/flows"));
    }

    @PostMapping("/{appId}/flows/{flowId}/update")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<FlowDetailResponse>> updateFlow(
            @PathVariable Long appId,
            @PathVariable Long flowId,
            @Valid @RequestBody UpdateFlowRequest request) {

        FlowDetailResponse response = flowService.updateFlow(appId, flowId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + appId + "/flows/" + flowId));
    }
}

package com.iam.app.controller;

import com.iam.app.dto.request.CreateClientMethodRequest;
import com.iam.app.dto.request.UpdateClientMethodRequest;
import com.iam.app.dto.response.ApiResponse;
import com.iam.app.dto.response.ClientMethodResponse;
import com.iam.app.service.ClientMethodService;
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
@RequestMapping("/applications/{appId}/methods")
public class ClientMethodController {

    private static final String BASE_URL = "/iam-app-service/applications";
    private final ClientMethodService clientMethodService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<List<ClientMethodResponse>>> getMethods(
            @PathVariable Long appId) {
        List<ClientMethodResponse> response = clientMethodService.getMethods(appId);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + appId + "/methods"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<List<ClientMethodResponse>>> createMethods(
            @PathVariable Long appId,
            @RequestBody @Valid CreateClientMethodRequest body) {
        List<ClientMethodResponse> response = clientMethodService.createMethods(appId, body);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + appId + "/methods"));
    }

    @PostMapping("/{methodId}/update")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<ClientMethodResponse>> updateMethod(
            @PathVariable Long appId,
            @PathVariable Long methodId,
            @RequestBody UpdateClientMethodRequest body) {
        ClientMethodResponse response = clientMethodService.updateMethod(appId, methodId, body);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + appId + "/methods/" + methodId + "/update"));
    }
}

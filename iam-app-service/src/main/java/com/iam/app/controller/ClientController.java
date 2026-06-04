package com.iam.app.controller;

import com.iam.app.dto.request.CreateClientRequest;
import com.iam.app.dto.request.ScopeRequest;
import com.iam.app.dto.request.UpdateClientRequest;
import com.iam.app.dto.response.ApiResponse;
import com.iam.app.dto.response.ClientDetailResponse;
import com.iam.app.dto.response.ClientListResponse;
import com.iam.app.dto.response.CreateClientResponse;
import com.iam.app.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/clients")
public class ClientController {

    private static final String BASE_URL = "/iam-app-service/clients";

    private final ClientService clientService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<Page<ClientListResponse>>> getClients(
            @RequestParam(required = false) String grantType,
            @RequestParam(required = false) Long appId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ClientListResponse> response = clientService.getClients(grantType, appId, status, clientId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL));
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<ClientDetailResponse>> getClientDetail(
            @PathVariable String clientId) {
        ClientDetailResponse response = clientService.getClientDetail(clientId);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + clientId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<CreateClientResponse>> createClient(
            @Valid @RequestBody CreateClientRequest request) {
        CreateClientResponse response = clientService.createClient(request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL));
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<ClientDetailResponse>> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClientRequest request) {
        ClientDetailResponse response = clientService.updateClient(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + id));
    }

    @PostMapping("/{id}/secret/reset")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<CreateClientResponse>> resetSecret(
            @PathVariable Long id) {
        CreateClientResponse response = clientService.resetSecret(id);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + id + "/secret/reset"));
    }

    @PostMapping("/{id}/scopes")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<ClientDetailResponse>> updateScopes(
            @PathVariable Long id,
            @Valid @RequestBody ScopeRequest request) {
        ClientDetailResponse response = clientService.updateScopes(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/" + id + "/scopes"));
    }
}

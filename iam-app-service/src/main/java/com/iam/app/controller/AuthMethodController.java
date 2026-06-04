package com.iam.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iam.app.dto.response.ApiResponse;
import com.iam.app.dto.response.AuthMethodResponse;
import com.iam.app.service.AuthMethodService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth-methods")
public class AuthMethodController {

    private final String BASE_URL = "/iam-app-service/auth-methods";
    private final AuthMethodService authMethodService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<List<AuthMethodResponse>>> getAuthMethods(
            @RequestParam(required = false) Integer status) {
        List<AuthMethodResponse> response = authMethodService.getAuthMethods(status);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL));
    }
}

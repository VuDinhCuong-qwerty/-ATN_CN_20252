package com.demo.change.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import com.demo.change.constant.ApiResponse;
import com.demo.change.dto.request.CreateDocumentRequest;
import com.demo.change.dto.response.DocumentResponse;
import com.demo.change.service.DocumentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/changes/{changeId}/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    @PreAuthorize("hasAuthority('change-mgmt/change-artifact:view')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(
            @PathVariable Long changeId, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getDocuments(changeId), request.getRequestURI()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('change-mgmt/change-artifact:manage')")
    public ResponseEntity<ApiResponse<DocumentResponse>> addDocument(
            @PathVariable Long changeId,
            @Valid @RequestBody CreateDocumentRequest body,
            Authentication authentication, HttpServletRequest request) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        String by = jwt.getToken().getClaimAsString("username");
        String code = jwt.getToken().getClaimAsString("employeeCode");
        return ResponseEntity.ok(ApiResponse.ok(documentService.addDocument(changeId, body, by, code), request.getRequestURI()));
    }

    @PostMapping("/{documentId}/delete")
    @PreAuthorize("hasAuthority('change-mgmt/change-artifact:manage')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long changeId, @PathVariable Long documentId,
            Authentication authentication, HttpServletRequest request) {
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        String by = jwt.getToken().getClaimAsString("username");
        documentService.deleteDocument(changeId, documentId, by);
        return ResponseEntity.ok(ApiResponse.ok(null, request.getRequestURI()));
    }
}

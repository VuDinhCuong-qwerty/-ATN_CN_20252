package com.iam.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iam.app.dto.response.ApiResponse;
import com.iam.app.dto.response.GetDepartmentResponse;
import com.iam.app.dto.response.GetPositionsResponse;
import com.iam.app.dto.response.GetRolesResponse;
import com.iam.app.service.ReferenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/reference")
public class ReferenceController {

    private final String BASE_URL = "/iam-app-service/reference";
    private final ReferenceService refService;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<List<GetRolesResponse>>> getRoles() {
        List<GetRolesResponse> response = this.refService.getRoles();
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/roles"));
    }

    @GetMapping("/positions")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<List<GetPositionsResponse>>> getPositions(
        @RequestParam("status") String status) {
            List<GetPositionsResponse> response = this.refService.getPositions(status);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/positions"));
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<List<GetDepartmentResponse>>> getDepartments(
        @RequestParam(value = "status", defaultValue = "1") Long status) {
        List<GetDepartmentResponse> response = this.refService.getDepartments(status);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE_URL + "/departments"));
    }
    

}

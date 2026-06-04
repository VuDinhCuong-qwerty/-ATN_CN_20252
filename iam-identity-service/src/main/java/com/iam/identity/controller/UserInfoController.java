package com.iam.identity.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iam.identity.dto.request.CreateUserRequest;
import com.iam.identity.dto.request.UpdatePersonalInfoRequest;
import com.iam.identity.dto.request.UpdateUserProfileRequest;
import com.iam.identity.dto.request.UpsertAddressRequest;
import com.iam.identity.dto.response.AddressResponse;
import com.iam.identity.dto.response.ApiResponse;
import com.iam.identity.dto.response.CreateUserResponse;
import com.iam.identity.dto.response.ProvinceResponse;
import com.iam.identity.dto.response.UpdateUserResponse;
import com.iam.identity.dto.response.UserDetailResponse;
import com.iam.identity.dto.response.UserSummaryResponse;
import com.iam.identity.dto.response.WardResponse;
import com.iam.identity.service.UserInfoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/users")
@Slf4j
@RequiredArgsConstructor
public class UserInfoController {

    private static final String BASE = "/iam-identity-service/users";

    private final UserInfoService userInfoService;

    // ── POST /users ───────────────────────────────────────────────────────────

    @PostMapping
    // @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUser(
            @RequestBody @Valid CreateUserRequest request) {
        CreateUserResponse response = userInfoService.createUser(request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE));
    }

    // ── GET /users ────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String employeeCode,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean onLeave,
            @RequestParam(required = false) Boolean offboarded,
            Pageable pageable) {
        UserSummaryResponse response = userInfoService.getUsers(username, employeeCode, departmentId, status, onLeave, offboarded, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE));
    }

    // ── GET /users/detail?userId=&employeeCode= ───────────────────────────────

    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserDetail(
            @RequestParam Long userId,
            @RequestParam String employeeCode) {
        UserDetailResponse response = userInfoService.getUserDetail(userId, employeeCode);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/detail"));
    }

    // ── POST /users/profile?employeeCode= ────────────────────────────────────

    @PostMapping("/profile")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<UpdateUserResponse>> updateUserProfile(
            @RequestParam String employeeCode,
            @RequestBody UpdateUserProfileRequest request) {
        UpdateUserResponse response = userInfoService.updateUserProfile(employeeCode, request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/profile"));
    }

    // ── POST /users/personal?employeeCode= ───────────────────────────────────

    @PostMapping("/personal")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<UpdateUserResponse>> updatePersonalInfo(
            @RequestParam String employeeCode,
            @RequestBody UpdatePersonalInfoRequest request) {
        UpdateUserResponse response = userInfoService.updatePersonalInfo(employeeCode, request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/personal"));
    }

    // ── GET /users/addresses?employeeCode= ───────────────────────────────────

    @GetMapping("/addresses")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @RequestParam String employeeCode) {
        List<AddressResponse> response = userInfoService.getAddresses(employeeCode);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/addresses"));
    }

    // ── POST /users/addresses?employeeCode=&type= ─────────────────────────────

    @PostMapping("/addresses")
    @PreAuthorize("hasAuthority('SCOPE_iam-write')")
    public ResponseEntity<ApiResponse<AddressResponse>> upsertAddress(
            @RequestParam String employeeCode,
            @RequestParam String type,
            @RequestBody UpsertAddressRequest request) {
        AddressResponse response = userInfoService.upsertAddress(employeeCode, type, request);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/addresses"));
    }

    // ── GET /users/provinces?name= ────────────────────────────────────────────

    @GetMapping("/provinces")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getProvinces(
            @RequestParam(required = false) String name) {
        List<ProvinceResponse> response = userInfoService.getProvinces(name);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/provinces"));
    }

    // ── GET /users/wards?provinceCode=&name= ─────────────────────────────────

    @GetMapping("/wards")
    @PreAuthorize("hasAuthority('SCOPE_iam-read')")
    public ResponseEntity<ApiResponse<List<WardResponse>>> getWards(
            @RequestParam Long provinceCode,
            @RequestParam(required = false) String name) {
        List<WardResponse> response = userInfoService.getWards(provinceCode, name);
        return ResponseEntity.ok(ApiResponse.ok(response, BASE + "/wards"));
    }

}

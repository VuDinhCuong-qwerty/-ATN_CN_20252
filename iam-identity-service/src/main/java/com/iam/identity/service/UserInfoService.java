package com.iam.identity.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.iam.identity.dto.request.CreateUserRequest;
import com.iam.identity.dto.request.UpdatePersonalInfoRequest;
import com.iam.identity.dto.request.UpdateUserProfileRequest;
import com.iam.identity.dto.request.UpsertAddressRequest;
import com.iam.identity.dto.response.AddressResponse;
import com.iam.identity.dto.response.CreateUserResponse;
import com.iam.identity.dto.response.ProvinceResponse;
import com.iam.identity.dto.response.UpdateUserResponse;
import com.iam.identity.dto.response.UserDetailResponse;
import com.iam.identity.dto.response.UserSummaryResponse;
import com.iam.identity.dto.response.WardResponse;

public interface UserInfoService {

    CreateUserResponse createUser(CreateUserRequest request);

    UserSummaryResponse getUsers(String username, String employeeCode,
            Long departmentId, String status, Boolean onLeave, Boolean offboarded, Pageable pageable);

    UserDetailResponse getUserDetail(Long userId, String employeeCode);

    UpdateUserResponse updateUserProfile(String employeeCode, UpdateUserProfileRequest request);

    UpdateUserResponse updatePersonalInfo(String employeeCode, UpdatePersonalInfoRequest request);

    List<AddressResponse> getAddresses(String employeeCode);

    AddressResponse upsertAddress(String employeeCode, String type, UpsertAddressRequest request);

    List<ProvinceResponse> getProvinces(String name);

    List<WardResponse> getWards(Long provinceCode, String name);
}

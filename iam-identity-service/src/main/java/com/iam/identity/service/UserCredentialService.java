package com.iam.identity.service;

import com.iam.identity.dto.request.ChangePasswordRequest;
import com.iam.identity.dto.request.ResetPasswordRequest;
import com.iam.identity.dto.response.ChangePasswordResponse;
import com.iam.identity.dto.response.ResetPasswordResponse;

public interface UserCredentialService {

    ChangePasswordResponse changePassword(Long userId, String employeeCode, ChangePasswordRequest request);

    ResetPasswordResponse resetPassword(Long userId, String employeeCode, ResetPasswordRequest request);
}

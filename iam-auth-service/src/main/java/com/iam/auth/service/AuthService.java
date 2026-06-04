package com.iam.auth.service;

import com.iam.auth.dto.request.LoginRequest;
import com.iam.auth.dto.response.ApiResponse;
import com.iam.auth.dto.response.LoginResponse;
import com.iam.auth.dto.response.LogoutResponse;
import com.iam.auth.dto.response.SelectMethodResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    ApiResponse<LoginResponse> login(HttpServletRequest request, HttpServletResponse httpServletResponse, LoginRequest input);

    SelectMethodResponse getSelectableMethods(String authSessionId);

    LoginResponse doSwitchMethod(String authSessionId, Long nodeId);

    ApiResponse<LogoutResponse> logout(String ssoSession, String bearerToken);
}

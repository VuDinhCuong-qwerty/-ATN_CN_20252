package com.iam.app.service;

import java.util.List;

import com.iam.app.dto.response.AuthMethodResponse;

public interface AuthMethodService {

    List<AuthMethodResponse> getAuthMethods(Integer status);
}

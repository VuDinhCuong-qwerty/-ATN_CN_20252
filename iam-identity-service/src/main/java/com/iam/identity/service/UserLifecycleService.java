package com.iam.identity.service;

import com.iam.identity.dto.request.LeaveRequest;
import com.iam.identity.dto.request.OnboardRequest;
import com.iam.identity.dto.request.TransferRequest;
import com.iam.identity.dto.response.UserStatusResponse;

public interface UserLifecycleService {

    UserStatusResponse leave(Long userId, String employeeCode, LeaveRequest request);

    UserStatusResponse returnFromLeave(Long userId, String employeeCode);

    UserStatusResponse offboard(Long userId, String employeeCode);

    UserStatusResponse onboard(Long userId, String employeeCode, OnboardRequest request);

    UserStatusResponse transfer(Long userId, String employeeCode, TransferRequest request);
}

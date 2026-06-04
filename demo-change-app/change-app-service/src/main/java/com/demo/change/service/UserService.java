package com.demo.change.service;

import com.demo.change.feign.output.UserDetailResponse;
import com.demo.change.feign.output.UserSummaryResponse;

public interface UserService {

    UserSummaryResponse getUsers(String username, String employeeCode, Long departmentId,
            String status, Boolean onLeave, Boolean offboarded, Integer page, Integer size);

    UserDetailResponse getUserDetail(Long userId, String employeeCode);

    UserSummaryResponse searchUsers(String username);
}

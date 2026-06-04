package com.demo.change.service.impl;

import org.springframework.stereotype.Service;

import com.demo.change.feign.fallback.IdentityFallback;
import com.demo.change.feign.output.UserDetailResponse;
import com.demo.change.feign.output.UserSummaryResponse;
import com.demo.change.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final IdentityFallback identityFallback;

    @Override
    public UserSummaryResponse getUsers(String username, String employeeCode, Long departmentId,
            String status, Boolean onLeave, Boolean offboarded, Integer page, Integer size) {
        return identityFallback.getUsers(username, employeeCode, departmentId,
                status, onLeave, offboarded, page, size);
    }

    @Override
    public UserDetailResponse getUserDetail(Long userId, String employeeCode) {
        return identityFallback.getUserDetail(userId, employeeCode);
    }

    @Override
    public UserSummaryResponse searchUsers(String username) {
        return identityFallback.searchUsers(username);
    }
}

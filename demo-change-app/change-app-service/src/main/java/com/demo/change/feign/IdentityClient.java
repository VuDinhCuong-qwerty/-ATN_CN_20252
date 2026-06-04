package com.demo.change.feign;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import com.demo.change.constant.ApiResponse;
import com.demo.change.feign.output.UserDetailResponse;
import com.demo.change.feign.output.UserSummaryResponse;

public interface IdentityClient {

    @GetExchange("/users")
    ApiResponse<UserSummaryResponse> getUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String employeeCode,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean onLeave,
            @RequestParam(required = false) Boolean offboarded,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size);

    @GetExchange("/users/detail")
    ApiResponse<UserDetailResponse> getUserDetail(
            @RequestParam Long userId,
            @RequestParam String employeeCode);
}

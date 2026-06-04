package com.iam.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutResponse {
    private String status;

    public interface STATUS {
        String SUCCESS = "SUCCESS";
        String FAIL = "FAIL";
    }
}

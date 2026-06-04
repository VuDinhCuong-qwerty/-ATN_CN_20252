package com.iam.auth.dto.response;


import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String status;
    private String sessionId;
    private Long clientId;
    private String redirectUri;
    private String method;
    private String theme;
    private ChallengeInfo challengeInfo;
    private List<MethodInfo> availableMethods;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChallengeInfo {
        private String type;
        private String hint;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MethodInfo {
        private String type;
        private String hint;
        private Long nodeId;
        private boolean isDefault;
        private String label;
    }

    public interface STATUS {
        String SUCCESS = "SUCCESS";
        String FAIL = "FAIL";
        String WAITING = "WAITING";
        String TERMINAL_FAIL = "TERMINAL_FAIL";
        String TERMINAL_SUCCESS = "TERMINAL_SUCCESS";
    }
}

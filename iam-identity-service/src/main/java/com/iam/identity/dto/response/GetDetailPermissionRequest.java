package com.iam.identity.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetDetailPermissionRequest {
    private String requestId;
    private String status;
    private String reason;
    private String note;
    private User requester;
    private User reviewer;
    private User grantee;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedAt;

    private Detail details;

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        private String username;
        private String employeeCode;
        private String fullName;
        private String department;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private List<App> apps;
        private List<Resource> resources;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class App {
        private Long id;
        private String code;
        private String name;
        private String status;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resource {
        private Long id;
        private String code;
        private String name;
        private Long appId;
        private String appCode;
        private String actions;
        private String status;
    }
}

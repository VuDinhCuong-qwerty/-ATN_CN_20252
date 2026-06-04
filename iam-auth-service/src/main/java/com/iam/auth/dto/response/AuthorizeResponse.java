package com.iam.auth.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizeResponse {
    private String status;
    private String redirectUri;

    public interface STATUS {
        String OK = "OK";
        String BAD_REQUEST = "BAD_REQUEST";
        String UNAUTHENTICATED = "UNAUTHENTICATED";
        String NOT_FOUND = "NOT_FOUND";
        String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    }
}

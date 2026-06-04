package com.iam.auth.dto.request;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LoginRequest {

    private String sessionId;
    private String type;
    private Long clientId;
    private String ssoSession;
    private Map<String, String> payload = new HashMap<>();

    public interface PayloadHeader {
        String USERNAME = "username";
        String PASSWORD = "password";
        String OTP = "otp";
        String TOTP = "totp";
    }

    public String getPayloadValue(String key) {
        return this.payload.getOrDefault(key, null);
    }
}

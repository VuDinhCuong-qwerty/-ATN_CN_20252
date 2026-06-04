package com.iam.auth.engine;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class PrepareResult {

    private String methodType;   // "OTP_EMAIL" | "TOTP"
    private String hint;         // "em***@bank.com" | null
    private Map<String, Object> extra; // data thêm nếu cần
}

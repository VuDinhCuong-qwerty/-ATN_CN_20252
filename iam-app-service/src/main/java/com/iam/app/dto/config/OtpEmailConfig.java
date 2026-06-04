package com.iam.app.dto.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OtpEmailConfig extends MethodConfig {

    @Min(value = 4, message = "Độ dài OTP phải >= 4")
    private Integer otpLength;

    @Min(value = 30, message = "Thời gian hết hạn OTP phải >= 30 giây")
    private Integer otpExpirySeconds;

    @Min(value = 1, message = "Số lần thử lại phải >= 1")
    private Integer maxRetry;

    @Override
    public void applyDefaults() {
        if (otpLength == null) otpLength = 6;
        if (otpExpirySeconds == null) otpExpirySeconds = 300;
        if (maxRetry == null) maxRetry = 3;
    }
}

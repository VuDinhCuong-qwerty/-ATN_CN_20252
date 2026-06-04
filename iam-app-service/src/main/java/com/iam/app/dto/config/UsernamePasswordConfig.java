package com.iam.app.dto.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UsernamePasswordConfig extends MethodConfig {

    @Min(value = 1, message = "Số lần đăng nhập tối đa phải >= 1")
    private Integer maxLoginAttempts;

    @Min(value = 1, message = "Thời gian khoá tài khoản phải >= 1 phút")
    private Integer lockDurationMinutes;

    @Override
    public void applyDefaults() {
        if (maxLoginAttempts == null) maxLoginAttempts = 5;
        if (lockDurationMinutes == null) lockDurationMinutes = 30;
    }
}

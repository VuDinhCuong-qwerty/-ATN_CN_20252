package com.iam.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class ToggleStatusRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "INACTIVE|ACTIVE")
    private String status;
}

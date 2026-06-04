package com.iam.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ResetPasswordRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String employeeCode;

    @NotBlank
    private String newPass;

    private String confirmNewPass;
}

package com.iam.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitRequest {
    @NotBlank
    private String requestId;

    @NotBlank
    private String submiter;

    @NotBlank
    private String submitCode;
}

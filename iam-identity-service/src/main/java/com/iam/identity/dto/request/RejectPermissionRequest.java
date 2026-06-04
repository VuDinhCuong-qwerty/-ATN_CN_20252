package com.iam.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RejectPermissionRequest {
    @NotBlank
    private String requestId;

    @NotBlank
    @Size(max = 500)
    private String note;

    @NotBlank
    private String reviewerCode;

    @NotBlank
    private String reviewer;
}

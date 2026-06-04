package com.iam.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
public class ScopeRequest {

    @NotBlank(message = "action không được để trống")
    private String action;

    private List<String> scopes;
}

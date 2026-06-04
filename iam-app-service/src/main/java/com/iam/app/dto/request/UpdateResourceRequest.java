package com.iam.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.util.List;

@Getter
public class UpdateResourceRequest {

    @NotBlank(message = "resourceCode không được để trống")
    private String resourceCode;

    @NotBlank(message = "resourceName không được để trống")
    private String resourceName;

    @NotBlank(message = "resourceType không được để trống")
    private String resourceType;

    @NotEmpty(message = "actions không được để trống")
    private List<@NotBlank(message = "action không được để trống")
            @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "action chỉ được chứa chữ cái, số, gạch dưới")
            String> actions;

    @NotBlank(message = "status không được để trống")
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "status phải là ACTIVE hoặc INACTIVE")
    private String status;

    private String ldapGroupName;

    private String description;
}

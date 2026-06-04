package com.iam.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateApplicationRequest {

    @NotBlank(message = "Tên ứng dụng không được để trống")
    private String name;

    @Size(max = 500)
    private String description;

    @NotBlank(message = "Loại ứng dụng không được để trống")
    @Pattern(regexp = "INTERNAL|THIRD_PARTY_LDAP")
    private String appType;

    @NotBlank
    @Size(max = 256)
    private String logoUri;

    @NotBlank
    @Size(max = 256)
    private String defaultUrl;

    @NotNull
    private Long departmentId;

    @NotBlank(message = "Service code không được để trống")
    private String serviceCode;

    private Long groupId;
    private Long acrLevel;
}

package com.iam.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UpdateAppGroupRequest {

    @NotBlank(message = "Tên nhóm ứng dụng không được để trống")
    private String name;

    private String description;

    private Integer status;
}

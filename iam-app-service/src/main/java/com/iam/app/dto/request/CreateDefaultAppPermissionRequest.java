package com.iam.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class CreateDefaultAppPermissionRequest {

    @NotEmpty(message = "Danh sách không được để trống")
    @Valid
    private List<Item> items;

    @Getter
    public static class Item {

        @NotBlank(message = "roleId không được để trống")
        private String roleId;

        @NotBlank(message = "positionCode không được để trống")
        private String positionCode;

        @NotNull(message = "applicationId không được để trống")
        private Long applicationId;
    }
}

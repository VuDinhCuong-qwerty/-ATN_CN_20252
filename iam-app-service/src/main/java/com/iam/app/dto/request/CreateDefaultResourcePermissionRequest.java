package com.iam.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.util.List;

@Getter
public class CreateDefaultResourcePermissionRequest {

    @NotEmpty(message = "Danh sách không được để trống")
    @Valid
    private List<Item> items;

    @Getter
    public static class Item {

        @NotNull(message = "roleId không được để trống")
        private Long roleId;

        @NotBlank(message = "positionCode không được để trống")
        private String positionCode;

        @NotNull(message = "resourceId không được để trống")
        private Long resourceId;

        @NotEmpty(message = "actions không được để trống")
        private List<@NotBlank @Pattern(regexp = "[A-Za-z0-9_]+", message = "action chỉ được chứa chữ cái, số và dấu gạch dưới") String> actions;
    }
}

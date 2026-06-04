package com.iam.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.util.List;

@Getter
public class BatchUpdateDefaultAppPermissionStatusRequest {

    @NotEmpty(message = "Danh sách không được để trống")
    @Valid
    private List<Item> items;

    @Getter
    public static class Item {

        @NotNull(message = "id không được để trống")
        private Long id;

        @NotNull(message = "status không được để trống")
        @Pattern(regexp = "ACTIVE|INACTIVE", message = "status phải là ACTIVE hoặc INACTIVE")
        private String status;
    }
}

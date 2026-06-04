package com.iam.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.util.List;

@Getter
public class CreateResourceRequest {

    @NotEmpty(message = "Danh sách tài nguyên không được rỗng")
    @Valid
    private List<Item> items;

    @Getter
    public static class Item {

        @NotBlank(message = "resourceCode không được để trống")
        private String resourceCode;

        @NotBlank(message = "resourceName không được để trống")
        private String resourceName;

        @NotBlank(message = "resourceType không được để trống")
        private String resourceType;

        @NotBlank(message = "actions không được để trống")
        @Pattern(regexp = "^[A-Za-z0-9_]+(,[A-Za-z0-9_]+)*$", message = "actions chỉ được chứa chữ cái, số, gạch dưới, ngăn cách bằng dấu phẩy")
        private String actions;

        private String ldapGroupName;

        private String description;
    }
}

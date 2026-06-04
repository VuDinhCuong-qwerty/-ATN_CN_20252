package com.iam.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class CreateClientMethodRequest {

    @NotEmpty(message = "Danh sách method không được để trống")
    @Valid
    private List<Item> items;

    @Getter
    public static class Item {

        @NotNull(message = "methodId không được để trống")
        private Long methodId;

        @NotBlank(message = "methodName không được để trống")
        private String methodName;

        private Map<String, Object> config;
    }
}

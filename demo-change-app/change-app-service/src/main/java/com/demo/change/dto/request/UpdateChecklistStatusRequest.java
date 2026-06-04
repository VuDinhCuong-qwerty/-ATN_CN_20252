package com.demo.change.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateChecklistStatusRequest {

    @NotBlank(message = "taskStatus không được trống")
    private String taskStatus; // SUCCESS hoặc FAIL
}

package com.iam.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.util.List;

@Getter
public class CreateFlowRequest {

    @NotBlank(message = "Alias không được để trống")
    private String alias;

    private String description;

    @Valid
    private List<ExecutionItem> executions;

    @Getter
    public static class ExecutionItem {

        @NotNull(message = "nodeId không được để trống")
        private Integer nodeId;

        private Integer parentNodeId;

        @NotNull(message = "methodId không được để trống")
        private Long methodId;

        @NotBlank(message = "requirement không được để trống")
        @Pattern(regexp = "REQUIRED|OPTIONAL|ALTERNATIVE|DISABLED", message = "requirement phải là REQUIRED, OPTIONAL, ALTERNATIVE hoặc DISABLED")
        private String requirement;

        private Integer isDefault;
    }
}

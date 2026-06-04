package com.iam.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectMethodResponse {
    private Long clientId;
    private String sessionToken;
    private String currentMethodType;
    private List<MethodOption> methods;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MethodOption {
        private Long nodeId;
        private String type;
        private String label;
    }
}

package com.iam.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwitchMethodConfirmRequest {
    private Long nodeId;
    private String sessionToken;
    private Long clientId;
}

package com.iam.auth.kafka.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowExecutionUpdatedPayload {
    private Long appId;
    private Long flowId;
    private String appServiceCode;
}

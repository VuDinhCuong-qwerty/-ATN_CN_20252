package com.iam.app.kafka.payload;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FlowExecutionUpdatedPayload {

    private Long appId;
    private Long flowId;
    private String appServiceCode;
}

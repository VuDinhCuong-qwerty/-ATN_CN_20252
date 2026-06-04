package com.iam.app.dto.request;

import lombok.Getter;

import java.util.Map;

@Getter
public class UpdateClientMethodRequest {

    private Map<String, Object> config;

    private String status;
}

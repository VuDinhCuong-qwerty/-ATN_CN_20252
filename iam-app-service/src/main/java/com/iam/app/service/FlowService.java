package com.iam.app.service;

import com.iam.app.dto.request.CreateFlowRequest;
import com.iam.app.dto.request.UpdateFlowRequest;
import com.iam.app.dto.response.FlowDetailResponse;
import com.iam.app.dto.response.FlowListResponse;

import java.util.List;

public interface FlowService {

    List<FlowListResponse> getFlows(Long appId, String status);

    FlowDetailResponse getFlowDetail(Long appId, Long flowId);

    FlowDetailResponse createFlow(Long appId, CreateFlowRequest request);

    FlowDetailResponse updateFlow(Long appId, Long flowId, UpdateFlowRequest request);
}

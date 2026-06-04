package com.iam.app.service;

import com.iam.app.dto.request.CreateClientMethodRequest;
import com.iam.app.dto.request.UpdateClientMethodRequest;
import com.iam.app.dto.response.ClientMethodResponse;

import java.util.List;

public interface ClientMethodService {

    List<ClientMethodResponse> getMethods(Long appId);

    List<ClientMethodResponse> createMethods(Long appId, CreateClientMethodRequest request);

    ClientMethodResponse updateMethod(Long appId, Long methodId, UpdateClientMethodRequest request);
}

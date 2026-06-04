package com.iam.app.service;

import com.iam.app.dto.request.CreateClientRequest;
import com.iam.app.dto.request.ScopeRequest;
import com.iam.app.dto.request.UpdateClientRequest;
import com.iam.app.dto.response.ClientDetailResponse;
import com.iam.app.dto.response.ClientListResponse;
import com.iam.app.dto.response.CreateClientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService {

    Page<ClientListResponse> getClients(String grantType, Long appId, String status, String clientId, Pageable pageable);

    ClientDetailResponse getClientDetail(String clientId);

    CreateClientResponse createClient(CreateClientRequest request);

    ClientDetailResponse updateClient(Long id, UpdateClientRequest request);

    CreateClientResponse resetSecret(Long id);

    ClientDetailResponse updateScopes(Long id, ScopeRequest request);
}

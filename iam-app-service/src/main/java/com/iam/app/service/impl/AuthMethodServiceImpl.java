package com.iam.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.iam.app.dto.response.AuthMethodResponse;
import com.iam.app.repository.jpa.AuthMethodRepository;
import com.iam.app.service.AuthMethodService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthMethodServiceImpl implements AuthMethodService {

    private final AuthMethodRepository authMethodRepository;

    @Override
    public List<AuthMethodResponse> getAuthMethods(Integer status) {
        log.info("getAuthMethods called: status={}", status);
        return authMethodRepository.findWithFilters(status)
                .stream()
                .map(AuthMethodResponse::new)
                .collect(Collectors.toList());
    }
}

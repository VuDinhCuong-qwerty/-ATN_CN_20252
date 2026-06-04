package com.iam.auth.engine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.iam.auth.domain.AuthApplication;
import com.iam.auth.enums.ErrorCode;
import com.iam.auth.exception.BusinessException;
import com.iam.auth.repository.jpa.AuthApplicationRepository;
import com.iam.auth.repository.jpa.AuthFlowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class AuthFlowCache {

    private final AuthFlowLoader loader;
    private final AuthFlowRepository authFlowRepository;
    private final AuthApplicationRepository applicationRepository;
    private final Cache<Long, AuthFlow> cache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(500)
            .recordStats()
            .build();

    public AuthFlow getByAppId(Long appId) {
        // 1. Tìm flow active của application
        List<AuthApplication> authApplications = this.applicationRepository.getAppByIdAndStatus(appId, AuthApplication.STATUS.ACTIVE);
        if (authApplications == null || authApplications.size() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getDesc());
        }
        List<com.iam.auth.domain.AuthFlow> flows = authFlowRepository
                .findByAppIdAndStatus(appId, com.iam.auth.domain.AuthFlow.STATUS.ACTIVE);

        if (flows == null || flows.size() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getDesc());
        }

        com.iam.auth.domain.AuthFlow flow = flows.getFirst();
        return cache.get(flow.getId(), loader::load);
    }

    public void invalidate(Long flowId) {
        cache.invalidate(flowId);
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

}

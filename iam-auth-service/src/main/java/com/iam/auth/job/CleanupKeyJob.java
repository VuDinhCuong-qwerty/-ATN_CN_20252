package com.iam.auth.job;

import com.iam.auth.domain.AuthSigningKey;
import com.iam.auth.service.SigningKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupKeyJob {

    private final SigningKeyService signingKeyService;

    @Scheduled(cron = "${cron.job.key.cleanup}")
    public void execute() {
        log.info("CleanupKeyJob started");

        List<AuthSigningKey> verifiableKeysBefore = signingKeyService.findAllVerifiableKeys();
        int countBefore = verifiableKeysBefore.size();

        signingKeyService.disableExpiredKeys();

        List<AuthSigningKey> verifiableKeysAfter = signingKeyService.findAllVerifiableKeys();
        int disabled = countBefore - verifiableKeysAfter.size();

        log.info("CleanupKeyJob finished — {} key(s) disabled", disabled);
    }
}

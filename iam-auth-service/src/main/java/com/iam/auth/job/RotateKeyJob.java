package com.iam.auth.job;

import com.iam.auth.domain.AuthSigningKey;
import com.iam.auth.service.SigningKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RotateKeyJob {

    private final SigningKeyService signingKeyService;

    @Scheduled(cron = "${cron.job.key.rotate}")
    public void execute() {
        log.info("RotateKeyJob started");

        Optional<AuthSigningKey> activeKeyOpt = signingKeyService.findActiveKey();

        if (activeKeyOpt.isEmpty()) {
            log.warn("No ACTIVE key found — generating initial key immediately");
            AuthSigningKey newKey = signingKeyService.generateAndSaveKey();
            log.info("RotateKeyJob finished — initial key generated with kid={}", newKey.getKid());
            return;
        }

        AuthSigningKey activeKey = activeKeyOpt.get();
        LocalDateTime rotationThreshold = LocalDateTime.now().plusDays(1)
                .withHour(12).withMinute(0).withSecond(0).withNano(0);

        if (activeKey.getValidUntil() != null && !activeKey.getValidUntil().isAfter(rotationThreshold)) {
            log.info("ACTIVE key kid={} expires at {} — within rotation window, rotating",
                    activeKey.getKid(), activeKey.getValidUntil());
            signingKeyService.rotateKey();
            log.info("RotateKeyJob finished — rotation complete");
        } else {
            log.info("RotateKeyJob finished — ACTIVE key kid={} is not due for rotation (validUntil={})",
                    activeKey.getKid(), activeKey.getValidUntil());
        }
    }
}

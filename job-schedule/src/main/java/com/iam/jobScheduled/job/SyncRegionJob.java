package com.iam.jobScheduled.job;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.iam.jobScheduled.connect.fallback.RegionClientFallback;
import com.iam.jobScheduled.connect.output.Province;
import com.iam.jobScheduled.connect.output.Ward;
import com.iam.jobScheduled.model.AuthProvince;
import com.iam.jobScheduled.model.AuthWard;
import com.iam.jobScheduled.repository.AuthProvinceRepository;
import com.iam.jobScheduled.repository.AuthWardReposiroty;
import com.iam.jobScheduled.util.Utility;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncRegionJob {

    private final RegionClientFallback regionClient;
    private final AuthProvinceRepository authProvinceRepository;
    private final AuthWardReposiroty authWardReposiroty;
    private final Executor executor;
    private final int threadCount = 5;

    @Scheduled(cron = "${job.region-sync.cron}", zone = "Asia/Ho_Chi_Minh") // Chạy vào lúc 00:00 hàng ngày
    public void syncProvince() {
        log.info("Starting syncProvince job");
        List<Province> provinces = regionClient.getAllProvinces(1);
        if (provinces == null || provinces.isEmpty()) {
            log.warn("No provinces found to sync");
            return;
        }
        log.info("Fetched {} provinces from region service", provinces.size());
        Queue<Province> provinceQueue = new ConcurrentLinkedQueue<>(provinces);
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    while (true) {
                        Province province = provinceQueue.poll();
                        if (province == null)
                            break;
                        try {
                            consumeProvince(province);
                        } catch (Exception e) {
                            log.error("Failed to process province {}: {}", province.getCode(), e.getMessage(), e);
                        }
                    }
                } finally {
                    log.info("Thread {} finished processing provinces", Thread.currentThread().getName());
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("SyncProvince job interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    private void consumeProvince(Province province) {
        log.info("Processing province {}", Utility.toJson(province));
        AuthProvince authProvince = new AuthProvince(province);
        authProvinceRepository.save(authProvince);
        log.info("Saved province {} to database", province.getCode());
        List<Ward> wards = regionClient.getWardsByProvinceCode(province.getCode());
        if (wards.isEmpty()) {
            log.warn("No wards found for province {}", province.getCode());
            return;
        }
        log.info("Fetched {} wards for province {}", wards.size(), province.getCode());
        List<AuthWard> authWards = wards.stream()
                .map(ward -> new AuthWard(ward))
                .toList();
        authWardReposiroty.saveAll(authWards);
        log.info("Saved {} wards for province {} to database", authWards.size(), province.getCode());
    }

}

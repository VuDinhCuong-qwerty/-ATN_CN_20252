package com.iam.jobScheduled.connect.fallback;

import java.util.List;

import org.springframework.stereotype.Component;

import com.iam.jobScheduled.connect.RegionClient;
import com.iam.jobScheduled.connect.output.Province;
import com.iam.jobScheduled.connect.output.Ward;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegionClientFallback {

    private final RegionClient regionClient;

    public List<Province> getAllProvinces(int depth) {
        try {
            return this.regionClient.getAllProvinces(depth);
        } catch (Exception e) {
            log.error("Failed to call getAllPrvinces: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Province> getPrvinces(int depth, Long code) {
        try {
            return this.regionClient.getPrvinces(depth, code);
        } catch (Exception e) {
            log.error("Failed to call getPrvinces: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<Ward> getWardsByProvinceCode(Long provinceCode) {
        try {
            return this.regionClient.getWardsByProvinceCode(provinceCode);
        } catch (Exception e) {
            log.error("Failed to call getWardsByProvinceCode: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
}

package com.demo.change.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.demo.change.dto.response.ReportSummaryResponse;
import com.demo.change.dto.response.ReportSummaryResponse.CreatorStat;
import com.demo.change.entity.ChangeRequest;
import com.demo.change.repository.ChangeRequestRepository;
import com.demo.change.service.ReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ChangeRequestRepository changeRequestRepository;

    @Override
    public ReportSummaryResponse getSummary() {
        List<Object[]> statusRows = changeRequestRepository.countByStatus();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        long totalAll = 0;
        long success  = 0;
        long fail     = 0;

        for (Object[] row : statusRows) {
            String status = String.valueOf(row[0]);
            long count    = ((Number) row[1]).longValue();
            byStatus.put(status, count);
            totalAll += count;
            if (ChangeRequest.STATUS.SUCCESS.equals(status)) success = count;
            if (ChangeRequest.STATUS.FAIL.equals(status))    fail    = count;
        }

        double successRate = (success + fail) == 0 ? 0.0 : (success * 100.0) / (success + fail);

        List<Object[]> creatorRows = changeRequestRepository.topCreators();
        List<CreatorStat> topCreators = new ArrayList<>();
        int limit = Math.min(creatorRows.size(), 5);
        for (int i = 0; i < limit; i++) {
            Object[] row = creatorRows.get(i);
            CreatorStat cs = new CreatorStat();
            cs.setCreatedBy(String.valueOf(row[0]));
            cs.setCreatedByCode(row[1] != null ? String.valueOf(row[1]) : "");
            cs.setCount(((Number) row[2]).longValue());
            topCreators.add(cs);
        }

        ReportSummaryResponse resp = new ReportSummaryResponse();
        resp.setByStatus(byStatus);
        resp.setSuccessRate(Math.round(successRate * 10.0) / 10.0);
        resp.setTopCreators(topCreators);
        resp.setTotalAll(totalAll);

        log.info("[ReportService] summary: totalAll={} successRate={}%", totalAll, successRate);
        return resp;
    }
}

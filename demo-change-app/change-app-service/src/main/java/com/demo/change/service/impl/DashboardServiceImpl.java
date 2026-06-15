package com.demo.change.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.demo.change.dto.response.ChangeListItemResponse;
import com.demo.change.dto.response.DashboardStatsResponse;
import com.demo.change.entity.ChangeRequest;
import com.demo.change.repository.ChangeRequestRepository;
import com.demo.change.service.DashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final ChangeRequestRepository changeRequestRepository;

    @Override
    public DashboardStatsResponse getStats() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth   = LocalDateTime.now();

        long totalThisMonth = changeRequestRepository
                .findWithFilters(null, null, startOfMonth, endOfMonth, PageRequest.of(0, 1))
                .getTotalElements();

        long pendingApproval = changeRequestRepository
                .findWithFilters(ChangeRequest.STATUS.PENDING, null, null, null, PageRequest.of(0, 1))
                .getTotalElements();

        long executing = changeRequestRepository
                .findWithFilters(ChangeRequest.STATUS.EXECUTING, null, null, null, PageRequest.of(0, 1))
                .getTotalElements();

        long success = changeRequestRepository
                .findWithFilters(ChangeRequest.STATUS.SUCCESS, null, null, null, PageRequest.of(0, 1))
                .getTotalElements();

        long fail = changeRequestRepository
                .findWithFilters(ChangeRequest.STATUS.FAIL, null, null, null, PageRequest.of(0, 1))
                .getTotalElements();

        double successRate = (success + fail) == 0 ? 0.0 : (success * 100.0) / (success + fail);

        List<ChangeRequest> rawRecent = changeRequestRepository
                .findWithFilters(null, null, null, null, PageRequest.of(0, 5))
                .getContent();

        List<ChangeListItemResponse> recentChanges = new ArrayList<>();
        for (ChangeRequest c : rawRecent) {
            recentChanges.add(ChangeListItemResponse.builder()
                    .id(c.getId())
                    .changeId(c.getChangeId())
                    .changeName(c.getChangeName())
                    .status(c.getStatus())
                    .goliveAt(c.getGoliveAt())
                    .createdBy(c.getCreatedBy())
                    .createdByCode(c.getCreatedByCode())
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt())
                    .build());
        }

        DashboardStatsResponse resp = new DashboardStatsResponse();
        resp.setTotalThisMonth(totalThisMonth);
        resp.setPendingApproval(pendingApproval);
        resp.setExecuting(executing);
        resp.setSuccessRate(Math.round(successRate * 10.0) / 10.0);
        resp.setRecentChanges(recentChanges);

        log.info("[DashboardService] stats: totalThisMonth={} pending={} executing={} successRate={}%",
                totalThisMonth, pendingApproval, executing, successRate);
        return resp;
    }
}

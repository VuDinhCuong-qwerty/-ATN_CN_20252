package com.demo.change.dto.response;

import java.util.Map;
import java.util.List;
import lombok.Data;

@Data
public class ReportSummaryResponse {
    private Map<String, Long> byStatus;       // DRAFT/PENDING/... → count
    private double successRate;
    private List<CreatorStat> topCreators;
    private long totalAll;

    @Data
    public static class CreatorStat {
        private String createdBy;
        private String createdByCode;
        private long count;
    }
}

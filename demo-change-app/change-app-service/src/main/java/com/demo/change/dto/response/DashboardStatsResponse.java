package com.demo.change.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class DashboardStatsResponse {
    private long totalThisMonth;
    private long pendingApproval;
    private long executing;
    private double successRate;          // 0–100
    private List<ChangeListItemResponse> recentChanges;
}

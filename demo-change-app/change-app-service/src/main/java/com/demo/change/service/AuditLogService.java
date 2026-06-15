package com.demo.change.service;

import java.util.List;

import com.demo.change.dto.response.AuditLogResponse;

public interface AuditLogService {
    List<AuditLogResponse> getAuditLogs(Long changeRequestId);
    void log(Long changeRequestId, String action, String fromStatus, String toStatus,
             String performedBy, String performedByCode, String note);
}

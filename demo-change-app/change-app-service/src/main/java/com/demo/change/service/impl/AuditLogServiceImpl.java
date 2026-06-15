package com.demo.change.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.demo.change.dto.response.AuditLogResponse;
import com.demo.change.entity.AuditLog;
import com.demo.change.repository.AuditLogRepository;
import com.demo.change.service.AuditLogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public List<AuditLogResponse> getAuditLogs(Long changeRequestId) {
        List<AuditLog> logs = auditLogRepository.findByChangeRequestIdOrderByCreatedAtDesc(changeRequestId);
        List<AuditLogResponse> result = new ArrayList<>();
        for (AuditLog e : logs) {
            result.add(toResponse(e));
        }
        return result;
    }

    @Override
    public void log(Long changeRequestId, String action, String fromStatus, String toStatus,
                    String performedBy, String performedByCode, String note) {
        AuditLog entry = AuditLog.builder()
                .changeRequestId(changeRequestId)
                .action(action)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .performedBy(performedBy)
                .performedByCode(performedByCode)
                .note(note)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(entry);
    }

    private AuditLogResponse toResponse(AuditLog e) {
        AuditLogResponse r = new AuditLogResponse();
        r.setId(e.getId());
        r.setAction(e.getAction());
        r.setFromStatus(e.getFromStatus());
        r.setToStatus(e.getToStatus());
        r.setPerformedBy(e.getPerformedBy());
        r.setPerformedByCode(e.getPerformedByCode());
        r.setNote(e.getNote());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }
}

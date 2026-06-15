package com.demo.change.dto.response;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AuditLogResponse {
    private Long id;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String performedBy;
    private String performedByCode;
    private String note;
    private LocalDateTime createdAt;
}

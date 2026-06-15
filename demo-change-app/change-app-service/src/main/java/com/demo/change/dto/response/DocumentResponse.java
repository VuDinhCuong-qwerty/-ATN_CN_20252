package com.demo.change.dto.response;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DocumentResponse {
    private Long id;
    private Long changeRequestId;
    private String docType;
    private String title;
    private String url;
    private String note;
    private String createdBy;
    private String createdByCode;
    private LocalDateTime createdAt;
}

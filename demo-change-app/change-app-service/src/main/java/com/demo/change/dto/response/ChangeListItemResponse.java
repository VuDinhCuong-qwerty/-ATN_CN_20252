package com.demo.change.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChangeListItemResponse {

    private Long id;
    private String changeId;
    private String changeName;
    private String status;
    private LocalDateTime goliveAt;
    private String createdBy;
    private String createdByCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

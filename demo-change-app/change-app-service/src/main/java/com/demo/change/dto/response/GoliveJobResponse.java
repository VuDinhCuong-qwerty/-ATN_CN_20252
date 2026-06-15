package com.demo.change.dto.response;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GoliveJobResponse {
    private Long id;
    private Long changeRequestId;
    private String name;
    private String link;
    private String jobType;
    private Integer orderNum;
    private String jobStatus;   // PENDING/RUNNING/SUCCESS/FAIL
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String resultNote;
    private Integer status;
}

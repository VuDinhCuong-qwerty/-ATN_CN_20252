package com.demo.change.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CHG_CHECKLIST_ITEM")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_chg_checklist_item")
    @SequenceGenerator(name = "seq_chg_checklist_item", sequenceName = "SEQ_CHG_CHECKLIST_ITEM", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CHANGE_REQUEST_ID")
    private Long changeRequestId;

    @Column(name = "PHASE")
    private String phase;

    @Column(name = "STEP_TEXT")
    private String stepText;

    @Column(name = "ORDER_NUM")
    private Integer orderNum;

    @Column(name = "ASSIGNED_TO")
    private String assignedTo;

    @Column(name = "ASSIGNED_TO_CODE")
    private String assignedToCode;

    @Column(name = "TASK_STATUS")
    private String taskStatus;

    @Column(name = "STATUS")
    private Integer status;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_BY_CODE")
    private String createdByCode;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    public interface PHASE {
        String PRE      = "PRE";
        String DURING   = "DURING";
        String ROLLBACK = "ROLLBACK";
    }

    public interface TASK_STATUS {
        String READY   = "READY";
        String SUCCESS = "SUCCESS";
        String FAIL    = "FAIL";
    }

    public interface STATUS {
        int ACTIVE   = 1;
        int INACTIVE = 0;
    }
}

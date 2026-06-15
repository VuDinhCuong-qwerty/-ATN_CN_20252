package com.demo.change.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CHG_AUDIT_LOG")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_chg_audit_log")
    @SequenceGenerator(name = "seq_chg_audit_log", sequenceName = "SEQ_CHG_AUDIT_LOG", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CHANGE_REQUEST_ID")
    private Long changeRequestId;

    @Column(name = "ACTION")
    private String action;   // CREATED/SUBMITTED/APPROVED/REJECTED/EXECUTED/JOB_RUN/FINALIZED

    @Column(name = "FROM_STATUS")
    private String fromStatus;

    @Column(name = "TO_STATUS")
    private String toStatus;

    @Column(name = "PERFORMED_BY")
    private String performedBy;

    @Column(name = "PERFORMED_BY_CODE")
    private String performedByCode;

    @Column(name = "NOTE")
    private String note;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    public interface ACTION {
        String CREATED   = "CREATED";
        String SUBMITTED = "SUBMITTED";
        String APPROVED  = "APPROVED";
        String REJECTED  = "REJECTED";
        String EXECUTED  = "EXECUTED";
        String JOB_RUN   = "JOB_RUN";
        String FINALIZED = "FINALIZED";
    }
}

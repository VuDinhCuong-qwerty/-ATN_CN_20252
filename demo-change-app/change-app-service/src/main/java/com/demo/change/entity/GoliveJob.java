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
@Table(name = "CHG_GOLIVE_JOB")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoliveJob {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_chg_golive_job")
    @SequenceGenerator(name = "seq_chg_golive_job", sequenceName = "SEQ_CHG_GOLIVE_JOB", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CHANGE_REQUEST_ID")
    private Long changeRequestId;

    @Column(name = "NAME")
    private String name;

    @Column(name = "LINK")
    private String link;

    @Column(name = "JOB_TYPE")
    private String jobType;

    @Column(name = "ORDER_NUM")
    private Integer orderNum;

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

    public interface JOB_TYPE {
        String MERGE  = "MERGE";
        String BUILD  = "BUILD";
        String DEPLOY = "DEPLOY";
    }

    public interface STATUS {
        int ACTIVE   = 1;
        int INACTIVE = 0;
    }
}

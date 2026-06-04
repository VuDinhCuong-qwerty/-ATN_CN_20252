package com.demo.change.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CHG_CHANGE_REQUEST")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_chg_change_request")
    @SequenceGenerator(name = "seq_chg_change_request", sequenceName = "SEQ_CHG_CHANGE_REQUEST", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CHANGE_ID")
    private String changeId;

    @Column(name = "CHANGE_NAME")
    private String changeName;

    @Lob
    @Column(name = "CONTENT")
    private String content;

    @Column(name = "GIT_LINK")
    private String gitLink;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "GOLIVE_AT")
    private LocalDateTime goliveAt;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_BY_CODE")
    private String createdByCode;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    public interface STATUS {
        String DRAFT     = "DRAFT";
        String PENDING   = "PENDING";
        String APPROVED  = "APPROVED";
        String EXECUTING = "EXECUTING";
        String SUCCESS   = "SUCCESS";
        String FAIL      = "FAIL";
    }
}

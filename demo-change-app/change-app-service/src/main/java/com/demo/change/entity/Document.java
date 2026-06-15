package com.demo.change.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CHG_DOCUMENT")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_chg_document")
    @SequenceGenerator(name = "seq_chg_document", sequenceName = "SEQ_CHG_DOCUMENT", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CHANGE_REQUEST_ID")
    private Long changeRequestId;

    @Column(name = "DOC_TYPE")
    private String docType;   // GIT / JIRA / CONFLUENCE / BUILD / OTHER

    @Column(name = "TITLE")
    private String title;

    @Column(name = "URL")
    private String url;

    @Column(name = "NOTE")
    private String note;

    @Column(name = "STATUS")
    private Integer status;   // 1=active, 0=deleted

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_BY_CODE")
    private String createdByCode;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    public interface DOC_TYPE {
        String GIT         = "GIT";
        String JIRA        = "JIRA";
        String CONFLUENCE  = "CONFLUENCE";
        String BUILD       = "BUILD";
        String OTHER       = "OTHER";
    }
}

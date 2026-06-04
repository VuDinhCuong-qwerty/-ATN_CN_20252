package com.iam.identity.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "AUTH_REQUEST_HEADER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRequestHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_request_header")
    @SequenceGenerator(name = "seq_auth_request_header", sequenceName = "SEQ_AUTH_REQUEST_HEADER", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "REQUESTED_BY")
    private String requestedBy;

    @Column(name = "REVIEWED_BY")
    private String reviewedBy;

    @Column(name = "REQUEST_FOR")
    private String requestFor;

    @Column(name = "REASON")
    private String reason;

    @Column(name = "NOTE")
    private String note;

    /** DRAFT | OFFICIAL | APPROVED | REJECTED | CANCELLED */
    @Column(name = "STATUS")
    private String status;

    @Column(name = "REQUESTED_AT")
    private LocalDateTime requestedAt;

    @Column(name = "REVIEWED_AT")
    private LocalDateTime reviewedAt;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "DRAFT";
        requestedAt = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public interface STATUS {
        String DRAFT      = "DRAFT";
        String OFFICIAL   = "OFFICIAL";
        String APPROVED   = "APPROVED";
        String REJECTED   = "REJECTED";
        String CANCELLED  = "CANCELLED";
    }
}

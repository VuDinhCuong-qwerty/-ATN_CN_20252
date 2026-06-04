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
@Table(name = "CHG_APPROVER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Approver {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_chg_approver")
    @SequenceGenerator(name = "seq_chg_approver", sequenceName = "SEQ_CHG_APPROVER", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CHANGE_REQUEST_ID")
    private Long changeRequestId;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "FULL_NAME")
    private String fullName;

    @Column(name = "EMPLOYEE_CODE")
    private String employeeCode;

    // PENDING/APPROVED/REJECTED
    @Column(name = "APPROVE_STATUS")
    private String approveStatus;

    @Column(name = "NOTE")
    private String note;

    @Column(name = "DECIDED_AT")
    private LocalDateTime decidedAt;

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

    public interface APPROVE_STATUS {
        String PENDING  = "PENDING";
        String APPROVED = "APPROVED";
        String REJECTED = "REJECTED";
    }

    public interface STATUS {
        int ACTIVE   = 1;
        int INACTIVE = 0;
    }
}

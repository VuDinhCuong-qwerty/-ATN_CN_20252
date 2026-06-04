package com.iam.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_FLOW_EXECUTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthFlowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_flow_execution")
    @SequenceGenerator(name = "seq_auth_flow_execution", sequenceName = "SEQ_AUTH_FLOW_EXECUTION", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "FLOW_ID")
    private Long flowId;

    @Column(name = "PARENT_NODE_ID")
    private Long parentNodeId;

    @Column(name = "CLIENT_METHOD_ID")
    private Long clientMethodId;

    @Column(name = "REQUIREMENT")
    private String requirement;

    @Column(name = "IS_DEFAULT")
    private Integer isDefault;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public interface STATUS {
        String ACTIVE = "ACTIVE";
        String INACTIVE = "INACTIVE";
    }

}
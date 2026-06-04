package com.iam.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUTH_CLIENT_GROUP")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthClientGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqAuthClientGroup")
    @SequenceGenerator(name = "seqAuthClientGroup", sequenceName = "SEQ_AUTH_CLIENT_GROUP", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "STATUS")
    private Integer status;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

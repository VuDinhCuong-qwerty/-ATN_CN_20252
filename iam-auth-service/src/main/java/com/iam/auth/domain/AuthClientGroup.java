package com.iam.auth.domain;

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
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "CREATED_AT")
    private LocalDateTime createAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "DESCRIPTION")
    private String description;
}

package com.iam.identity.domain;

import java.time.Instant;

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
@Table(name = "AUTH_USER_ADDRESS")
@org.hibernate.annotations.DynamicUpdate
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_auth_user_role")
    @SequenceGenerator(name = "seq_auth_user_role", sequenceName = "SEQ_AUTH_USER_ADDRESS", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "PROVINCE_CODE")
    private Long provinceCode;

    @Column(name = "WARD_CODE")
    private Long wardCode;

    @Column(name = "DETAIL")
    private String detail;

    @Column(name = "CREATED_AT")
    private Instant createdAt;

    @Column(name = "UPDATED_AT")
    private Instant updatedAt;

    public interface Type {
        String PERMANENT = "PERMANENT";
        String TEMPORARY = "TEMPORARY";
        String BIRTH = "BIRTH";
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}

/*
 * CREATE TABLE "AUTH_USER1"."AUTH_USER_ADDRESS"
 * ( "ID" NUMBER DEFAULT AUTH_USER1.SEQ_AUTH_USER_ADDRESS.NEXTVAL NOT NULL
 * ENABLE,
 * "USER_ID" NUMBER NOT NULL ENABLE,
 * "TYPE" VARCHAR2(10 BYTE) NOT NULL ENABLE,
 * "PROVINCE_CODE" NUMBER,
 * "WARD_CODE" NUMBER,
 * "DETAIL" VARCHAR2(500 BYTE),
 * "CREATED_AT" TIMESTAMP (6) DEFAULT CURRENT_TIMESTAMP NOT NULL ENABLE,
 * "UPDATED_AT" TIMESTAMP (6) DEFAULT CURRENT_TIMESTAMP NOT NULL ENABLE,
 * CONSTRAINT "CHK_AUTH_ADDRESS_TYPE" CHECK (TYPE IN ('PERMANENT', 'TEMPORARY',
 * 'BIRTH')) ENABLE,
 * CONSTRAINT "PK_AUTH_USER_ADDRESS" PRIMARY KEY ("ID")
 * USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
 * STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
 * PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
 * BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
 * TABLESPACE "USERS" ENABLE,
 * CONSTRAINT "UQ_AUTH_ADDRESS_TYPE" UNIQUE ("USER_ID", "TYPE")
 */

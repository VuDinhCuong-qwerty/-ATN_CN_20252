package com.iam.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "AUTH_WARD")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthWard {
    @Id
    @Column(name = "CODE")
    private Long code;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DIVISION_TYPE")
    private String divisionType;

    @Column(name = "CODENAME")
    private String codename;

    @Column(name = "PROVINCE_CODE")
    private Long provinceCode;

    @Column(name = "STATUS")
    private String status;

    public interface Status {
        String ACTIVE = "ACTIVE";
        String INACTIVE = "INACTIVE";
    }

}

/*
 * CREATE TABLE "AUTH_USER1"."AUTH_WARD"
 * ( "CODE" NUMBER NOT NULL ENABLE,
 * "NAME" VARCHAR2(200 BYTE) NOT NULL ENABLE,
 * "DIVISION_TYPE" VARCHAR2(50 BYTE) NOT NULL ENABLE,
 * "CODENAME" VARCHAR2(200 BYTE) NOT NULL ENABLE,
 * "PROVINCE_CODE" NUMBER NOT NULL ENABLE,
 * "STATUS" VARCHAR2(10 BYTE) DEFAULT 'ACTIVE' NOT NULL ENABLE,
 * CONSTRAINT "CHK_AUTH_WARD_STATUS" CHECK (STATUS IN ('ACTIVE', 'INACTIVE'))
 * ENABLE,
 * CONSTRAINT "PK_AUTH_WARD" PRIMARY KEY ("CODE")
 */

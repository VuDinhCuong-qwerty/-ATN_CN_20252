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
@Table(name = "AUTH_PROVINCE")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthProvince {
    @Id
    @Column(name = "CODE")
    private Long code;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DIVISION_TYPE")
    private String divisionType;

    @Column(name = "CODENAME")
    private String codename;

    @Column(name = "PHONE_CODE")
    private Long phoneCode;

    @Column(name = "STATUS")
    private String status;

    public interface Status {
        String ACTIVE = "ACTIVE";
        String INACTIVE = "INACTIVE";
    }
}

/*
 * CREATE TABLE "AUTH_USER1"."AUTH_PROVINCE"
 * ( "CODE" NUMBER NOT NULL ENABLE,
 * "NAME" VARCHAR2(200 BYTE) NOT NULL ENABLE,
 * "DIVISION_TYPE" VARCHAR2(50 BYTE) NOT NULL ENABLE,
 * "CODENAME" VARCHAR2(200 BYTE) NOT NULL ENABLE,
 * "PHONE_CODE" NUMBER,
 * "STATUS" VARCHAR2(10 BYTE) DEFAULT 'ACTIVE' NOT NULL ENABLE,
 * CONSTRAINT "CHK_AUTH_PROVINCE_STATUS" CHECK (STATUS IN ('ACTIVE',
 * 'INACTIVE')) ENABLE,
 * CONSTRAINT "PK_AUTH_PROVINCE" PRIMARY KEY ("CODE")
 * USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
 * STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
 * PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
 * BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
 * TABLESPACE "USERS" ENABLE
 * )
 */

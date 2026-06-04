package com.iam.jobScheduled.model;

import com.iam.jobScheduled.connect.output.Ward;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
CREATE TABLE AUTH_USER1.AUTH_WARD (
    CODE            NUMBER          NOT NULL,
    NAME            VARCHAR2(200)   NOT NULL,
    DIVISION_TYPE   VARCHAR2(50)    NOT NULL,
    CODENAME        VARCHAR2(200)   NOT NULL,
    PROVINCE_CODE   NUMBER          NOT NULL,
    STATUS          VARCHAR2(10)    DEFAULT 'ACTIVE' NOT NULL,

    CONSTRAINT PK_AUTH_WARD             PRIMARY KEY (CODE),
    CONSTRAINT UQ_AUTH_WARD_CNAME       UNIQUE (CODENAME),
    CONSTRAINT CHK_AUTH_WARD_STATUS     CHECK (STATUS IN ('ACTIVE', 'INACTIVE'))
    -- FK logic: PROVINCE_CODE → AUTH_PROVINCE.CODE (không tạo thực)
);

 */

@Entity
@Table(name = "AUTH_WARD")
@Getter
@Setter
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

    public boolean isActive() {
        return Status.ACTIVE.equals(this.status);
    }

    public AuthWard(Ward ward) {
        this.code = ward.getCode();
        this.name = ward.getName();
        this.divisionType = ward.getDivisionType();
        this.codename = ward.getCodename();
        this.provinceCode = ward.getProvinceCode();
        this.status = Status.ACTIVE;
    }
}

package com.iam.jobScheduled.model;

import com.iam.jobScheduled.connect.output.Province;

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
CREATE TABLE AUTH_USER1.AUTH_PROVINCE (
    CODE            NUMBER          NOT NULL,
    NAME            VARCHAR2(200)   NOT NULL,
    DIVISION_TYPE   VARCHAR2(50)    NOT NULL,
    CODENAME        VARCHAR2(200)   NOT NULL,
    PHONE_CODE      NUMBER,
    STATUS          VARCHAR2(10)    DEFAULT 'ACTIVE' NOT NULL,

    CONSTRAINT PK_AUTH_PROVINCE         PRIMARY KEY (CODE),
    CONSTRAINT UQ_AUTH_PROVINCE_CNAME   UNIQUE (CODENAME),
    CONSTRAINT CHK_AUTH_PROVINCE_STATUS CHECK (STATUS IN ('ACTIVE', 'INACTIVE'))
);
*/

@Entity 
@Table(name = "AUTH_PROVINCE")
@Getter
@Setter
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
    private Integer phoneCode;

    @Column(name = "STATUS")
    private String status;

    public interface Status {
        String ACTIVE = "ACTIVE";
        String INACTIVE = "INACTIVE";
    }

    public boolean isActive() {
        return Status.ACTIVE.equals(this.status);
    }

    public AuthProvince(Province province) {
        this.code = province.getCode();
        this.name = province.getName();
        this.divisionType = province.getDivisionType();
        this.codename = province.getCodename();
        this.phoneCode = province.getPhoneCode();
        this.status = Status.ACTIVE;
    }

}

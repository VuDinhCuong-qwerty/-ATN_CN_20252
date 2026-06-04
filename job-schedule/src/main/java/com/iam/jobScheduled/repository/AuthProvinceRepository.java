package com.iam.jobScheduled.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iam.jobScheduled.model.AuthProvince;

@Repository
public interface AuthProvinceRepository extends JpaRepository<AuthProvince, Long> {

}

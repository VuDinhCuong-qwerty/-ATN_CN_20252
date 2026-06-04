package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthProvince;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthProvinceRepository extends JpaRepository<AuthProvince, Long> {
}

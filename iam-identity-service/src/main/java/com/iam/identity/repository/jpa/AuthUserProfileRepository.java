package com.iam.identity.repository.jpa;

import java.util.List;

import com.iam.identity.domain.AuthUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserProfileRepository extends JpaRepository<AuthUserProfile, Long> {

    @Query(value = "SELECT COUNT(DISTINCT(u.EMPLOYEE_CODE)) FROM AUTH_USER_PROFILE u WHERE u.EMPLOYEE_CODE LIKE :prefix%", nativeQuery = true)
    int countEmployeeCode(String prefix);

    @Query(value = "SELECT * FROM AUTH_USER_PROFILE u WHERE u.EMPLOYEE_CODE = :employeeCode", nativeQuery = true)
    List<AuthUserProfile> findByEmployeeCode(String employeeCode);
}

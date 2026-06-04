package com.iam.identity.repository.jpa;

import java.util.List;
import com.iam.identity.domain.AuthUser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    @Query(value = "SELECT COUNT(DISTINCT u.USERNAME) FROM AUTH_USER u WHERE u.USERNAME LIKE :usernamePrefix%", nativeQuery = true)
    int countUsername(@Param("usernamePrefix") String usernamePrefix);

    @Query(value = "SELECT * FROM AUTH_USER u WHERE u.ID = :id", nativeQuery = true)
    List<AuthUser> findByUserId(@Param("id") Long id);

    @Query(value = "SELECT u.* FROM AUTH_USER u JOIN AUTH_USER_PROFILE up ON u.ID =  up.USER_ID WHERE up.EMPLOYEE_CODE = :employeeCode", nativeQuery = true)
    List<AuthUser> findUserByEmployeeCode(@Param("employeeCode")String employeeCode);
}

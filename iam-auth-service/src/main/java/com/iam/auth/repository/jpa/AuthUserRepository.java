package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    @Query(value = "SELECT * FROM AUTH_USER WHERE ID = :id", nativeQuery = true)
    Optional<AuthUser> findUserById(@Param("id") Long id);

    @Query(value = "SELECT * FROM AUTH_USER u WHERE u.USERNAME = :username", nativeQuery = true)
    Optional<AuthUser> findByUsername(@Param("username") String username);

    @Query(value = "SELECT EMPLOYEE_CODE FROM AUTH_USER_PROFILE WHERE USER_ID = :userId", nativeQuery = true)
    Optional<String> findEmployeeCodeByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT r.CODE FROM AUTH_ROLE r JOIN AUTH_USER_ROLE ur ON r.ID = ur.ROLE_ID WHERE ur.USER_ID = :userId AND ur.STATUS = 'ACTIVE' AND ROWNUM = 1", nativeQuery = true)
    Optional<String> findRoleCodeByUserId(@Param("userId") Long userId);
}

package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthRole;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRoleRepository extends JpaRepository<AuthRole, Long> {

    @Query(value = "SELECT * FROM AUTH_ROLE", nativeQuery = true)
    List<AuthRole> findAllRoles();

    @Query(value = "SELECT * FROM AUTH_ROLE WHERE CODE IN :codes", nativeQuery = true)
    List<AuthRole> findByCodes(@Param("codes") List<String> codes);
}

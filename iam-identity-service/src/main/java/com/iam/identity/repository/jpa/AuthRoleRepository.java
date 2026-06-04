package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthRole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthRoleRepository extends JpaRepository<AuthRole, Long> {

    @Query(value = "SELECT * FROM AUTH_ROLE r WHERE r.CODE IN (:roles)", nativeQuery = true)
    List<AuthRole> getRolesByCode(@Param("roles") List<String> roles);
}

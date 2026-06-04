package com.iam.identity.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iam.identity.domain.AuthDefaultResource;

@Repository
public interface AuthDefaultResourceRepository extends JpaRepository<AuthDefaultResource, Long> {

    @Query(value = "SELECT dr.* FROM AUTH_DEFAULT_RESOURCE dr INNER JOIN AUTH_ROLE r ON r.ID = dr.ROLE_ID WHERE r.CODE IN (:roles) AND dr.POSITION_CODE = :position AND dr.STATUS = 'ACTIVE'", nativeQuery = true)
    List<AuthDefaultResource> getDefaultResourceByRoleAndPosition(@Param("roles") List<String> roles, @Param("position") String position);
}

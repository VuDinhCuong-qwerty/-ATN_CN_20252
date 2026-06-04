package com.iam.identity.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iam.identity.domain.AuthDefaultAppPermission;

@Repository
public interface AuthDefaultAppPermissionRepository extends JpaRepository<AuthDefaultAppPermission, Long> {

    @Query(value = "SELECT * FROM AUTH_DEFAULT_APP_PERMISSION app WHERE app.ROLE_ID IN (:roles) AND app.POSITION_CODE = :position AND app.STATUS = 'ACTIVE'", nativeQuery = true)
    List<AuthDefaultAppPermission> getDefaultAppByRoleAndPosition(@Param("roles") List<String> roles, @Param("position") String position);
}

package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthAppPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthAppPermissionRepository extends JpaRepository<AuthAppPermission, Long> {
}

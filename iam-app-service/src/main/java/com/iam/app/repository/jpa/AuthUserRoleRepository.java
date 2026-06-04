package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserRoleRepository extends JpaRepository<AuthUserRole, Long> {
}

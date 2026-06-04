package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthUserRoleRepository extends JpaRepository<AuthUserRole, Long> {

    List<AuthUserRole> findByUserId(Long userId);

    List<AuthUserRole> findByUserIdAndStatus(Long userId, String status);
}

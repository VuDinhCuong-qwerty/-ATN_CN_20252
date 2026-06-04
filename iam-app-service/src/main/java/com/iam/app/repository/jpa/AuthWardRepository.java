package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthWard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthWardRepository extends JpaRepository<AuthWard, Long> {
}

package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthMethodRepository extends JpaRepository<AuthMethod, Long> {
}

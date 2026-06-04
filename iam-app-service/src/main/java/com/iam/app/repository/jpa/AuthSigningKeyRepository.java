package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthSigningKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthSigningKeyRepository extends JpaRepository<AuthSigningKey, Long> {
}

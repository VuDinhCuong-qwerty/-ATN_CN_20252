package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthMethodRepository extends JpaRepository<AuthMethod, Long> {

    Optional<AuthMethod> findByMethod(String method);

    boolean existsByMethod(String method);
}

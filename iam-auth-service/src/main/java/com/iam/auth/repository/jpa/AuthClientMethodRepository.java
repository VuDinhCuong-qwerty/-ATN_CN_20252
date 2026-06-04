package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthClientMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthClientMethodRepository extends JpaRepository<AuthClientMethod, Long> {
}

package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthClientOauth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthClientOauthRepository extends JpaRepository<AuthClientOauth, Long> {
}

package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserProfileRepository extends JpaRepository<AuthUserProfile, Long> {
}

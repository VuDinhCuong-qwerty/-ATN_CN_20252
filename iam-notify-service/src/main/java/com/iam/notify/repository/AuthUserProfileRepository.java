package com.iam.notify.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iam.notify.domain.AuthUserProfile;

@Repository
public interface AuthUserProfileRepository extends JpaRepository<AuthUserProfile, Long> {
}

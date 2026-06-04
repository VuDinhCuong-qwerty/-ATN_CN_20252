package com.iam.notify.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iam.notify.domain.AuthUser;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
}

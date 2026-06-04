package com.iam.identity.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iam.identity.domain.AuthRequestHeader;

@Repository
public interface AuthRequestHeaderRepository extends JpaRepository<AuthRequestHeader, Long> {
}

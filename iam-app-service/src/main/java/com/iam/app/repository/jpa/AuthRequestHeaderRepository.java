package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthRequestHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRequestHeaderRepository extends JpaRepository<AuthRequestHeader, Long> {
}

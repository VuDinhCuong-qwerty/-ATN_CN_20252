package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthUserResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserResourceRepository extends JpaRepository<AuthUserResource, Long> {
}

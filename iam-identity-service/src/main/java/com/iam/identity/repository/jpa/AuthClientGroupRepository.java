package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthClientGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthClientGroupRepository extends JpaRepository<AuthClientGroup, Long> {

    boolean existsByName(String name);
}

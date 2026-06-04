package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthResourceRepository extends JpaRepository<AuthResource, Long> {

    @Query(value = "SELECT * FROM AUTH_RESOURCE r WHERE r.ID = :id", nativeQuery = true)
    Optional<AuthResource> findById(@Param("id") Long id);
}

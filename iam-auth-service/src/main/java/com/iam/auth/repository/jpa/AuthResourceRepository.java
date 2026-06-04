package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthResourceRepository extends JpaRepository<AuthResource, Long> {

    @Query(value = "SELECT * FROM AUTH_RESOURCE WHERE ID = :id", nativeQuery = true)
    Optional<AuthResource> findResourceById(@Param("id") Long id);
}

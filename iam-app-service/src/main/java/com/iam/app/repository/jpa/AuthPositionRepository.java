package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthPosition;

import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthPositionRepository extends JpaRepository<AuthPosition, String> {

    @Query(value = "SELECT * FROM AUTH_POSITION p WHERE (:status is null OR p.STATUS = :status)", nativeQuery = true)
    List<AuthPosition> findByStatus(@Param("status") String status);
}

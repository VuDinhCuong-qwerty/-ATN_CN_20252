package com.iam.identity.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iam.identity.domain.AuthPosition;

@Repository
public interface AuthPositionRepository extends JpaRepository<AuthPosition, String> {

    @Query(value = "SELECT * FROM AUTH_POSITION p WHERE p.CODE = :code AND p.STATUS = 'ACTIVE'", nativeQuery = true)
    List<AuthPosition> getPositionBayCode(@Param("code") String code);
}

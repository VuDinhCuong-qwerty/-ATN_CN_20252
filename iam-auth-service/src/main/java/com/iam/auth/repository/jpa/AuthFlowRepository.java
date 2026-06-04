package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthFlowRepository extends JpaRepository<AuthFlow, Long> {


    @Query(value = "SELECT * FROM AUTH_FLOW e WHERE e.APP_ID = :appId AND e.STATUS = :status", nativeQuery = true)
    List<AuthFlow> findByAppIdAndStatus(@Param("appId") Long appId, @Param("status") String status);
}

package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthApplicationRepository extends JpaRepository<AuthApplication, Long> {

    @Query(value = "SELECT * FROM AUTH_APPLICATION a WHERE a.ID = :appId AND a.STATUS = :status", nativeQuery = true)
    List<AuthApplication> getAppByIdAndStatus(@Param("appId") Long appId, @Param("status") String status);

    @Query(value = "SELECT a.* FROM AUTH_APPLICATION a JOIN AUTH_CLIENT c ON c.APP_ID = a.ID WHERE c.ID = :clientId AND a.STATUS = :status", nativeQuery = true)
    List<AuthApplication> getAppByClientId(@Param("clientId") Long clientId, @Param("status") String status);
}

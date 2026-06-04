package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AuthApplicationRepository extends JpaRepository<AuthApplication, Long> {

    @Query(value = "SELECT * FROM AUTH_APPLICATION a WHERE a.ID IN (:requestApps) AND a.STATUS = 'ACTIVE'", nativeQuery = true)
    List<AuthApplication> findAppsById(@Param("requestApps") Set<Long> requestApps);
}

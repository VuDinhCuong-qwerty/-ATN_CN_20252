package com.iam.app.repository.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iam.app.domain.AuthApplication;

@Repository
public interface AuthApplicationRepository extends JpaRepository<AuthApplication, Long>,
        JpaSpecificationExecutor<AuthApplication> {

    @Query(value = "SELECT * FROM AUTH_APPLICATION a WHERE a.ID = :id", nativeQuery = true)
    Optional<AuthApplication> findById(Long id);

    @Query(value = "SELECT COUNT(1) FROM AUTH_APPLICATION WHERE GROUP_ID = :groupId", nativeQuery = true)
    int countByGroupId(@Param("groupId") Long groupId);

    @Query(value = "SELECT COUNT(1) FROM AUTH_APPLICATION WHERE SERVICE_CODE = :serviceCode", nativeQuery = true)
    int countByServiceCode(@Param("serviceCode") String serviceCode);

    @Query(value = "SELECT COUNT(1) FROM AUTH_APPLICATION WHERE SERVICE_CODE = :serviceCode AND ID <> :id", nativeQuery = true)
    int countByServiceCodeAndIdNot(@Param("serviceCode") String serviceCode, @Param("id") Long id);
}

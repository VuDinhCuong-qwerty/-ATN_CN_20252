package com.iam.identity.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iam.identity.domain.AuthRequestDetail;

@Repository
public interface AuthRequestDetailRepository extends JpaRepository<AuthRequestDetail, Long> {

    @Query(value = "SELECT * FROM AUTH_REQUEST_DETAIL WHERE REQUEST_ID = :requestId AND STATUS = 'ACTIVE'", nativeQuery = true)
    List<AuthRequestDetail> findActiveByRequestId(@Param("requestId") Long requestId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM AUTH_REQUEST_DETAIL WHERE REQUEST_ID = :requestId", nativeQuery = true)
    void deleteAllByRequestId(@Param("requestId") Long requestId);
}

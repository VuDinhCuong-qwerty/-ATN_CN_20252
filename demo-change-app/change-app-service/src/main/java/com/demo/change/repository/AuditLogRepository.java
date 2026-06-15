package com.demo.change.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.demo.change.entity.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query(value = "SELECT * FROM CHG_AUDIT_LOG WHERE CHANGE_REQUEST_ID = :changeRequestId ORDER BY CREATED_AT DESC", nativeQuery = true)
    List<AuditLog> findByChangeRequestIdOrderByCreatedAtDesc(@Param("changeRequestId") Long changeRequestId);
}

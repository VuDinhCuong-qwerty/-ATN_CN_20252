package com.demo.change.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.demo.change.entity.Approver;

@Repository
public interface ApproverRepository extends JpaRepository<Approver, Long> {

    @Query(value = "SELECT * FROM CHG_APPROVER WHERE CHANGE_REQUEST_ID = :changeRequestId AND STATUS = 1 ORDER BY CREATED_AT ASC", nativeQuery = true)
    List<Approver> findActiveByChangeRequestId(@Param("changeRequestId") Long changeRequestId);

    @Query(value = "SELECT * FROM CHG_APPROVER WHERE CHANGE_REQUEST_ID = :changeRequestId AND LOWER(USERNAME) = LOWER(:username) AND STATUS = 1 AND ROWNUM = 1", nativeQuery = true)
    Optional<Approver> findActiveByChangeRequestIdAndUsername(@Param("changeRequestId") Long changeRequestId, @Param("username") String username);

    @Query(value = "SELECT COUNT(*) FROM CHG_APPROVER WHERE CHANGE_REQUEST_ID = :changeRequestId AND STATUS = 1 AND APPROVE_STATUS = 'PENDING'", nativeQuery = true)
    int countPendingByChangeRequestId(@Param("changeRequestId") Long changeRequestId);
}

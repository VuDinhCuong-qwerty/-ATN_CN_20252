package com.demo.change.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.demo.change.entity.ChangeRequest;

@Repository
public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    @Query(value = "SELECT STATUS, COUNT(*) AS CNT FROM CHG_CHANGE_REQUEST GROUP BY STATUS", nativeQuery = true)
    List<Object[]> countByStatus();

    @Query(value = "SELECT CREATED_BY, CREATED_BY_CODE, COUNT(*) AS CNT FROM CHG_CHANGE_REQUEST GROUP BY CREATED_BY, CREATED_BY_CODE ORDER BY CNT DESC", nativeQuery = true)
    List<Object[]> topCreators();

    @Query(value = "SELECT * FROM CHG_CHANGE_REQUEST " +
                   "WHERE (:status IS NULL OR STATUS = :status) " +
                   "AND (:createdByCode IS NULL OR CREATED_BY_CODE = :createdByCode) " +
                   "AND (:fromDate IS NULL OR CREATED_AT >= :fromDate) " +
                   "AND (:toDate IS NULL OR CREATED_AT <= :toDate) " +
                   "ORDER BY CREATED_AT DESC",
           countQuery = "SELECT COUNT(*) FROM CHG_CHANGE_REQUEST " +
                        "WHERE (:status IS NULL OR STATUS = :status) " +
                        "AND (:createdByCode IS NULL OR CREATED_BY_CODE = :createdByCode) " +
                        "AND (:fromDate IS NULL OR CREATED_AT >= :fromDate) " +
                        "AND (:toDate IS NULL OR CREATED_AT <= :toDate)",
           nativeQuery = true)
    Page<ChangeRequest> findWithFilters(
            @Param("status") String status,
            @Param("createdByCode") String createdByCode,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}

package com.demo.change.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.demo.change.entity.GoliveJob;

@Repository
public interface GoliveJobRepository extends JpaRepository<GoliveJob, Long> {

    @Query(value = "SELECT * FROM CHG_GOLIVE_JOB WHERE CHANGE_REQUEST_ID = :changeRequestId AND STATUS = 1 ORDER BY ORDER_NUM ASC", nativeQuery = true)
    List<GoliveJob> findActiveByChangeRequestId(@Param("changeRequestId") Long changeRequestId);

    @Query(value = "SELECT COUNT(*) FROM CHG_GOLIVE_JOB WHERE CHANGE_REQUEST_ID = :changeRequestId AND STATUS = 1", nativeQuery = true)
    int countActiveByChangeRequestId(@Param("changeRequestId") Long changeRequestId);
}

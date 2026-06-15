package com.demo.change.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.demo.change.entity.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query(value = "SELECT * FROM CHG_DOCUMENT WHERE CHANGE_REQUEST_ID = :changeRequestId AND STATUS = :status ORDER BY CREATED_AT DESC", nativeQuery = true)
    List<Document> findByChangeRequestIdAndStatusOrderByCreatedAtDesc(
            @Param("changeRequestId") Long changeRequestId,
            @Param("status") Integer status);
}

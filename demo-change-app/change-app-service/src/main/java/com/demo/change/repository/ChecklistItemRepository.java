package com.demo.change.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.demo.change.entity.ChecklistItem;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    @Query(value = "SELECT * FROM CHG_CHECKLIST_ITEM WHERE CHANGE_REQUEST_ID = :changeRequestId AND STATUS = 1 ORDER BY PHASE ASC, ORDER_NUM ASC", nativeQuery = true)
    List<ChecklistItem> findActiveByChangeRequestId(@Param("changeRequestId") Long changeRequestId);

    @Query(value = "SELECT COUNT(*) FROM CHG_CHECKLIST_ITEM WHERE CHANGE_REQUEST_ID = :changeRequestId AND STATUS = 1", nativeQuery = true)
    int countActiveByChangeRequestId(@Param("changeRequestId") Long changeRequestId);

    @Query(value = "SELECT * FROM CHG_CHECKLIST_ITEM WHERE ID = :id AND CHANGE_REQUEST_ID = :changeRequestId AND STATUS = 1 AND ROWNUM = 1", nativeQuery = true)
    Optional<ChecklistItem> findActiveByIdAndChangeRequestId(@Param("id") Long id, @Param("changeRequestId") Long changeRequestId);

    @Query(value = "SELECT COUNT(*) FROM CHG_CHECKLIST_ITEM WHERE CHANGE_REQUEST_ID = :changeRequestId AND STATUS = 1 AND TASK_STATUS = :taskStatus", nativeQuery = true)
    int countActiveByChangeRequestIdAndTaskStatus(@Param("changeRequestId") Long changeRequestId, @Param("taskStatus") String taskStatus);
}

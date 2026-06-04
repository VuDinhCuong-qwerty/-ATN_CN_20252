package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthFlowExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthFlowExecutionRepository extends JpaRepository<AuthFlowExecution, Long> {

    @Query(value = "SELECT * FROM AUTH_FLOW_EXECUTION e WHERE e.FLOW_ID = :flowId AND e.STATUS = :status", nativeQuery = true)
    List<AuthFlowExecution> findFlowExecutionByFlowIdAndStatus(@Param("flowId") Long flowId, @Param("status") String status);
}

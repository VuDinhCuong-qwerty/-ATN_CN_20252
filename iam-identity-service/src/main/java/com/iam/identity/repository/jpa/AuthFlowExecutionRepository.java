package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthFlowExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthFlowExecutionRepository extends JpaRepository<AuthFlowExecution, Long> {

    List<AuthFlowExecution> findByFlowId(Long flowId);

    List<AuthFlowExecution> findByFlowIdAndStatus(Long flowId, String status);

    List<AuthFlowExecution> findByParentNodeId(Long parentNodeId);
}

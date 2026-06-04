package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthFlowExecution;
import com.iam.app.dto.projection.FlowExecutionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthFlowExecutionRepository extends JpaRepository<AuthFlowExecution, Long> {

    @Query(value = """
            SELECT af.ALIAS
            FROM AUTH_FLOW_EXECUTION afe
            JOIN AUTH_FLOW af ON afe.FLOW_ID = af.ID
            WHERE afe.CLIENT_METHOD_ID = :clientMethodId
              AND af.APP_ID = :appId
              AND afe.STATUS = 'ACTIVE'
            """, nativeQuery = true)
    List<String> findActiveFlowAliasesByClientMethodAndApp(
            @Param("clientMethodId") Long clientMethodId,
            @Param("appId") Long appId);

    @Query(value = """
            SELECT afe.ID               AS "id",
                   afe.PARENT_NODE_ID   AS "parentNodeId",
                   afe.CLIENT_METHOD_ID AS "clientMethodId",
                   am.METHOD            AS "methodName",
                   afe.REQUIREMENT      AS "requirement",
                   afe.IS_DEFAULT       AS "isDefault",
                   afe.STATUS           AS "status"
            FROM AUTH_FLOW_EXECUTION afe
            LEFT JOIN AUTH_CLIENT_METHOD acm ON afe.CLIENT_METHOD_ID = acm.ID
            LEFT JOIN AUTH_METHOD am ON acm.METHOD_ID = am.ID
            WHERE afe.FLOW_ID = :flowId AND afe.STATUS = 'ACTIVE'
            ORDER BY afe.ID ASC
            """, nativeQuery = true)
    List<FlowExecutionProjection> findActiveByFlowId(@Param("flowId") Long flowId);

    @Modifying
    @Query(value = "UPDATE AUTH_FLOW_EXECUTION SET STATUS = 'INACTIVE', UPDATED_AT = CURRENT_TIMESTAMP WHERE FLOW_ID = :flowId AND STATUS = 'ACTIVE'", nativeQuery = true)
    void deactivateAllByFlowId(@Param("flowId") Long flowId);
}

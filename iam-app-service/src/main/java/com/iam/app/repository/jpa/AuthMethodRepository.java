package com.iam.app.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iam.app.domain.AuthMethod;

@Repository
public interface AuthMethodRepository extends JpaRepository<AuthMethod, Long> {

    @Query(value = "SELECT * FROM AUTH_METHOD WHERE (:status IS NULL OR STATUS = :status)", nativeQuery = true)
    List<AuthMethod> findWithFilters(@Param("status") Integer status);

    @Query(value = "SELECT * FROM AUTH_METHOD WHERE ID IN (:ids)", nativeQuery = true)
    List<AuthMethod> findAllByIdIn(@Param("ids") List<Long> ids);
}

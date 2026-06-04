package com.iam.identity.repository.jpa;

import com.iam.identity.domain.AuthDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthDepartmentRepository extends JpaRepository<AuthDepartment, Long> {

    @Query(value = "SELECT * FROM AUTH_DEPARTMENT d WHERE d.STATUS = 1", nativeQuery = true)
    List<AuthDepartment> findAllActive();

    @Query(value = "SELECT * FROM AUTH_DEPARTMENT d WHERE d.ID = :id AND d.STATUS = 1", nativeQuery = true)
    List<AuthDepartment> findActiveById(@Param("id") Long id);

    @Query(value = "SELECT * FROM AUTH_DEPARTMENT d WHERE d.STATUS = 1 START WITH d.ID = :id CONNECT BY PRIOR d.PARENT_ID = d.ID AND LEVEL <= 5 ORDER BY LEVEL", nativeQuery = true)
    List<AuthDepartment> findFullTreeDepartmentById(@Param("id") Long id);
}

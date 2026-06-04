package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthDepartment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthDepartmentRepository extends JpaRepository<AuthDepartment, Long> {

    @Query(value = "SELECT * FROM AUTH_DEPARTMENT d WHERE d.STATUS = :status", nativeQuery = true)
    List<AuthDepartment> findByStatus(@Param("status") Long status);

    @Query(value = "SELECT COUNT(1) FROM AUTH_DEPARTMENT WHERE ID = :id", nativeQuery = true)
    int countById(@Param("id") Long id);
}

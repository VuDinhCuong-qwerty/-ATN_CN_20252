package com.iam.auth.repository.jpa;

import com.iam.auth.domain.AuthDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthDepartmentRepository extends JpaRepository<AuthDepartment, Long> {

    Optional<AuthDepartment> findByCode(String code);

    List<AuthDepartment> findByParentId(Long parentId);
}

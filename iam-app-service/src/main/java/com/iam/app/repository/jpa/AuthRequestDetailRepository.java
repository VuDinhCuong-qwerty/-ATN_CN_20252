package com.iam.app.repository.jpa;

import com.iam.app.domain.AuthRequestDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRequestDetailRepository extends JpaRepository<AuthRequestDetail, Long> {
}

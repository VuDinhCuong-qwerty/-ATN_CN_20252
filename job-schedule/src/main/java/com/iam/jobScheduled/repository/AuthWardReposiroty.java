package com.iam.jobScheduled.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iam.jobScheduled.model.AuthWard;

@Repository
public interface AuthWardReposiroty extends JpaRepository<AuthWard, Long> {

}

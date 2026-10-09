package com.homeauto.repository;

import com.homeauto.model.EnvironmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository extends JpaRepository<EnvironmentStatus, Long> {
}

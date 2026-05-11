package com.example.demo;

import com.example.demo.SagaLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaLogRepository extends JpaRepository<SagaLog, Long> {
}
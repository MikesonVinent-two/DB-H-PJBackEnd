package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entity.ChangeLog;
import com.example.demo.entity.StandardQuestion;

@Repository
public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long> {
    ChangeLog findByAssociatedStandardQuestion(StandardQuestion question);
} 
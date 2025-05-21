package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.StandardObjectiveAnswer;

@Repository
public interface StandardObjectiveAnswerRepository extends JpaRepository<StandardObjectiveAnswer, Long> {
    StandardObjectiveAnswer findByStandardQuestionIdAndDeletedAtIsNull(Long standardQuestionId);
} 
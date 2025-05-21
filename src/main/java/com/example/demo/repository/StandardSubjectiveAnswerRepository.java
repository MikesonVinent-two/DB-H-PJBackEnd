package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.StandardSubjectiveAnswer;

@Repository
public interface StandardSubjectiveAnswerRepository extends JpaRepository<StandardSubjectiveAnswer, Long> {
    StandardSubjectiveAnswer findByStandardQuestionIdAndDeletedAtIsNull(Long standardQuestionId);
} 
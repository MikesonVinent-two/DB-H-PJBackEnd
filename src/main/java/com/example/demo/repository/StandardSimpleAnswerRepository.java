package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.StandardSimpleAnswer;

@Repository
public interface StandardSimpleAnswerRepository extends JpaRepository<StandardSimpleAnswer, Long> {
    StandardSimpleAnswer findByStandardQuestionIdAndDeletedAtIsNull(Long standardQuestionId);
} 
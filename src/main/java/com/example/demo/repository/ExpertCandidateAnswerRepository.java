package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.ExpertCandidateAnswer;

@Repository
public interface ExpertCandidateAnswerRepository extends JpaRepository<ExpertCandidateAnswer, Long> {
    
    // 根据标准问题ID查询专家候选回答
    Page<ExpertCandidateAnswer> findByStandardQuestionId(Long standardQuestionId, Pageable pageable);
    
    // 根据用户ID查询专家候选回答
    Page<ExpertCandidateAnswer> findByUserId(Long userId, Pageable pageable);
} 
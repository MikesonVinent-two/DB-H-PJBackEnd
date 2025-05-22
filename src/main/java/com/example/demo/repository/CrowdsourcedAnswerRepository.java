package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.CrowdsourcedAnswer;

@Repository
public interface CrowdsourcedAnswerRepository extends JpaRepository<CrowdsourcedAnswer, Long> {
    
    // 根据标准问题ID查询众包回答
    Page<CrowdsourcedAnswer> findByStandardQuestionId(Long standardQuestionId, Pageable pageable);
    
    // 根据用户ID查询众包回答
    Page<CrowdsourcedAnswer> findByUserId(Long userId, Pageable pageable);
    
    // 根据审核状态查询众包回答
    Page<CrowdsourcedAnswer> findByQualityReviewStatus(
        CrowdsourcedAnswer.QualityReviewStatus status, Pageable pageable);
    
    // 根据标准问题ID和审核状态查询众包回答
    Page<CrowdsourcedAnswer> findByStandardQuestionIdAndQualityReviewStatus(
        Long standardQuestionId, CrowdsourcedAnswer.QualityReviewStatus status, Pageable pageable);
        
    // 检查用户是否已经在特定任务批次中为特定问题提交回答
    boolean existsByStandardQuestionIdAndUserIdAndTaskBatchId(
        Long standardQuestionId, Long userId, Integer taskBatchId);
} 
package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.demo.dto.CrowdsourcedAnswerDTO;

public interface CrowdsourcedAnswerService {
    // 根据标准问题ID获取众包回答
    Page<CrowdsourcedAnswerDTO> getAnswersByQuestionId(Long standardQuestionId, Pageable pageable);
    
    // 根据用户ID获取众包回答
    Page<CrowdsourcedAnswerDTO> getAnswersByUserId(Long userId, Pageable pageable);
    
    // 根据审核状态获取众包回答
    Page<CrowdsourcedAnswerDTO> getAnswersByStatus(String status, Pageable pageable);
    
    // 根据标准问题ID和审核状态获取众包回答
    Page<CrowdsourcedAnswerDTO> getAnswersByQuestionIdAndStatus(
        Long standardQuestionId, String status, Pageable pageable);
        
    // 创建众包回答
    CrowdsourcedAnswerDTO createCrowdsourcedAnswer(CrowdsourcedAnswerDTO answerDTO);
    
    // 审核众包回答
    CrowdsourcedAnswerDTO reviewAnswer(Long answerId, Long reviewerUserId, String status, String feedback);
} 
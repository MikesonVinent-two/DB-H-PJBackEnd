package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.demo.dto.ExpertCandidateAnswerDTO;

public interface ExpertCandidateAnswerService {
    // 创建专家候选回答
    ExpertCandidateAnswerDTO createExpertCandidateAnswer(ExpertCandidateAnswerDTO answerDTO);
    
    // 根据标准问题ID获取专家候选回答
    Page<ExpertCandidateAnswerDTO> getAnswersByQuestionId(Long standardQuestionId, Pageable pageable);
    
    // 根据用户ID获取专家候选回答
    Page<ExpertCandidateAnswerDTO> getAnswersByUserId(Long userId, Pageable pageable);
    
    // 更新专家候选回答的质量评分和反馈
    ExpertCandidateAnswerDTO updateQualityScoreAndFeedback(
        Long answerId, Integer qualityScore, String feedback);
} 
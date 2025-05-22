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
        
    /**
     * 修改专家候选回答内容
     * @param answerId 回答ID
     * @param userId 操作用户ID（用于权限验证）
     * @param answerText 新的回答内容
     * @return 修改后的回答
     * @throws IllegalArgumentException 如果回答不存在
     * @throws IllegalStateException 如果用户无权修改
     */
    ExpertCandidateAnswerDTO updateExpertCandidateAnswer(
        Long answerId, Long userId, String answerText);
        
    /**
     * 删除专家候选回答
     * @param answerId 回答ID
     * @param userId 操作用户ID（用于权限验证）
     * @return 是否删除成功
     * @throws IllegalArgumentException 如果回答不存在
     * @throws IllegalStateException 如果用户无权删除
     */
    boolean deleteExpertCandidateAnswer(Long answerId, Long userId);
} 
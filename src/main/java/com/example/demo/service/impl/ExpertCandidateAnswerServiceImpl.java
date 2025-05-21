package com.example.demo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.dto.ExpertCandidateAnswerDTO;
import com.example.demo.entity.ExpertCandidateAnswer;
import com.example.demo.entity.StandardQuestion;
import com.example.demo.entity.User;
import com.example.demo.repository.ExpertCandidateAnswerRepository;
import com.example.demo.repository.StandardQuestionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ExpertCandidateAnswerService;

@Service
public class ExpertCandidateAnswerServiceImpl implements ExpertCandidateAnswerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExpertCandidateAnswerServiceImpl.class);
    
    @Autowired
    private ExpertCandidateAnswerRepository expertCandidateAnswerRepository;
    
    @Autowired
    private StandardQuestionRepository standardQuestionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    @Transactional
    public ExpertCandidateAnswerDTO createExpertCandidateAnswer(ExpertCandidateAnswerDTO answerDTO) {
        logger.debug("开始创建专家候选回答 - 标准问题ID: {}, 用户ID: {}", 
            answerDTO.getStandardQuestionId(), answerDTO.getUserId());
        
        // 验证必填字段
        if (answerDTO.getStandardQuestionId() == null) {
            throw new IllegalArgumentException("标准问题ID不能为空");
        }
        if (answerDTO.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (!StringUtils.hasText(answerDTO.getCandidateAnswerText())) {
            throw new IllegalArgumentException("答案内容不能为空");
        }
        
        // 查找标准问题
        StandardQuestion standardQuestion = standardQuestionRepository
            .findById(answerDTO.getStandardQuestionId())
            .orElseThrow(() -> {
                logger.error("创建专家候选回答失败 - 标准问题不存在: {}", answerDTO.getStandardQuestionId());
                return new IllegalArgumentException("标准问题不存在");
            });
        
        // 查找用户
        User user = userRepository.findById(answerDTO.getUserId())
            .orElseThrow(() -> {
                logger.error("创建专家候选回答失败 - 用户不存在: {}", answerDTO.getUserId());
                return new IllegalArgumentException("用户不存在");
            });
        
        // 创建专家候选回答实体
        ExpertCandidateAnswer answer = new ExpertCandidateAnswer();
        answer.setStandardQuestion(standardQuestion);
        answer.setUser(user);
        answer.setCandidateAnswerText(answerDTO.getCandidateAnswerText());
        answer.setSubmissionTime(answerDTO.getSubmissionTime() != null ? 
            answerDTO.getSubmissionTime() : java.time.LocalDateTime.now());
        
        // 保存并返回
        try {
            ExpertCandidateAnswer savedAnswer = expertCandidateAnswerRepository.save(answer);
            logger.info("专家候选回答创建成功 - ID: {}, 问题ID: {}, 用户ID: {}", 
                savedAnswer.getId(), savedAnswer.getStandardQuestion().getId(), savedAnswer.getUser().getId());
            return convertToDTO(savedAnswer);
        } catch (Exception e) {
            logger.error("保存专家候选回答失败", e);
            throw new RuntimeException("保存专家候选回答失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<ExpertCandidateAnswerDTO> getAnswersByQuestionId(Long standardQuestionId, Pageable pageable) {
        return expertCandidateAnswerRepository.findByStandardQuestionId(standardQuestionId, pageable)
            .map(this::convertToDTO);
    }
    
    @Override
    public Page<ExpertCandidateAnswerDTO> getAnswersByUserId(Long userId, Pageable pageable) {
        return expertCandidateAnswerRepository.findByUserId(userId, pageable)
            .map(this::convertToDTO);
    }
    
    @Override
    @Transactional
    public ExpertCandidateAnswerDTO updateQualityScoreAndFeedback(
            Long answerId, Integer qualityScore, String feedback) {
        ExpertCandidateAnswer answer = expertCandidateAnswerRepository.findById(answerId)
            .orElseThrow(() -> new IllegalArgumentException("专家候选回答不存在"));
        
        answer.setQualityScore(qualityScore);
        answer.setFeedback(feedback);
        
        ExpertCandidateAnswer updatedAnswer = expertCandidateAnswerRepository.save(answer);
        return convertToDTO(updatedAnswer);
    }
    
    // 将实体转换为DTO
    private ExpertCandidateAnswerDTO convertToDTO(ExpertCandidateAnswer answer) {
        ExpertCandidateAnswerDTO dto = new ExpertCandidateAnswerDTO();
        dto.setId(answer.getId());
        dto.setStandardQuestionId(answer.getStandardQuestion().getId());
        dto.setUserId(answer.getUser().getId());
        dto.setCandidateAnswerText(answer.getCandidateAnswerText());
        dto.setSubmissionTime(answer.getSubmissionTime());
        dto.setQualityScore(answer.getQualityScore());
        dto.setFeedback(answer.getFeedback());
        dto.setUserUsername(answer.getUser().getUsername());
        
        return dto;
    }
} 
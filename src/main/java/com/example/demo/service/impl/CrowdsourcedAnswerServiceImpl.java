package com.example.demo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.dto.CrowdsourcedAnswerDTO;
import com.example.demo.entity.CrowdsourcedAnswer;
import com.example.demo.entity.StandardQuestion;
import com.example.demo.entity.User;
import com.example.demo.repository.CrowdsourcedAnswerRepository;
import com.example.demo.repository.StandardQuestionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CrowdsourcedAnswerService;

import java.time.LocalDateTime;

@Service
public class CrowdsourcedAnswerServiceImpl implements CrowdsourcedAnswerService {
    
    private static final Logger logger = LoggerFactory.getLogger(CrowdsourcedAnswerServiceImpl.class);
    
    @Autowired
    private CrowdsourcedAnswerRepository crowdsourcedAnswerRepository;
    
    @Autowired
    private StandardQuestionRepository standardQuestionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public Page<CrowdsourcedAnswerDTO> getAnswersByQuestionId(Long standardQuestionId, Pageable pageable) {
        return crowdsourcedAnswerRepository.findByStandardQuestionId(standardQuestionId, pageable)
            .map(this::convertToDTO);
    }
    
    @Override
    public Page<CrowdsourcedAnswerDTO> getAnswersByUserId(Long userId, Pageable pageable) {
        return crowdsourcedAnswerRepository.findByUserId(userId, pageable)
            .map(this::convertToDTO);
    }
    
    @Override
    public Page<CrowdsourcedAnswerDTO> getAnswersByStatus(String status, Pageable pageable) {
        try {
            CrowdsourcedAnswer.QualityReviewStatus reviewStatus = 
                CrowdsourcedAnswer.QualityReviewStatus.valueOf(status.toUpperCase());
            return crowdsourcedAnswerRepository.findByQualityReviewStatus(reviewStatus, pageable)
                .map(this::convertToDTO);
        } catch (IllegalArgumentException e) {
            logger.error("无效的审核状态: {}", status);
            throw new IllegalArgumentException("无效的审核状态: " + status);
        }
    }
    
    @Override
    public Page<CrowdsourcedAnswerDTO> getAnswersByQuestionIdAndStatus(
            Long standardQuestionId, String status, Pageable pageable) {
        try {
            CrowdsourcedAnswer.QualityReviewStatus reviewStatus = 
                CrowdsourcedAnswer.QualityReviewStatus.valueOf(status.toUpperCase());
            return crowdsourcedAnswerRepository
                .findByStandardQuestionIdAndQualityReviewStatus(standardQuestionId, reviewStatus, pageable)
                .map(this::convertToDTO);
        } catch (IllegalArgumentException e) {
            logger.error("无效的审核状态: {}", status);
            throw new IllegalArgumentException("无效的审核状态: " + status);
        }
    }
    
    @Override
    @Transactional
    public CrowdsourcedAnswerDTO createCrowdsourcedAnswer(CrowdsourcedAnswerDTO answerDTO) {
        logger.debug("开始创建众包回答 - 标准问题ID: {}, 用户ID: {}", 
            answerDTO.getStandardQuestionId(), answerDTO.getUserId());
        
        // 验证必填字段
        if (answerDTO.getStandardQuestionId() == null) {
            throw new IllegalArgumentException("标准问题ID不能为空");
        }
        if (answerDTO.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (!StringUtils.hasText(answerDTO.getAnswerText())) {
            throw new IllegalArgumentException("回答内容不能为空");
        }
        
        // 查找标准问题
        StandardQuestion standardQuestion = standardQuestionRepository
            .findById(answerDTO.getStandardQuestionId())
            .orElseThrow(() -> {
                logger.error("创建众包回答失败 - 标准问题不存在: {}", answerDTO.getStandardQuestionId());
                return new IllegalArgumentException("标准问题不存在");
            });
        
        // 查找用户
        User user = userRepository.findById(answerDTO.getUserId())
            .orElseThrow(() -> {
                logger.error("创建众包回答失败 - 用户不存在: {}", answerDTO.getUserId());
                return new IllegalArgumentException("用户不存在");
            });
        
        // 创建众包回答实体
        CrowdsourcedAnswer answer = new CrowdsourcedAnswer();
        answer.setStandardQuestion(standardQuestion);
        answer.setUser(user);
        answer.setAnswerText(answerDTO.getAnswerText());
        answer.setSubmissionTime(answerDTO.getSubmissionTime() != null ? 
            answerDTO.getSubmissionTime() : java.time.LocalDateTime.now());
        answer.setTaskBatchId(answerDTO.getTaskBatchId());
        answer.setQualityReviewStatus(CrowdsourcedAnswer.QualityReviewStatus.PENDING);
        
        // 保存并返回
        try {
            CrowdsourcedAnswer savedAnswer = crowdsourcedAnswerRepository.save(answer);
            logger.info("众包回答创建成功 - ID: {}, 问题ID: {}, 用户ID: {}", 
                savedAnswer.getId(), savedAnswer.getStandardQuestion().getId(), savedAnswer.getUser().getId());
            return convertToDTO(savedAnswer);
        } catch (Exception e) {
            logger.error("保存众包回答失败", e);
            throw new RuntimeException("保存众包回答失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public CrowdsourcedAnswerDTO reviewAnswer(Long answerId, Long reviewerUserId, String status, String feedback) {
        logger.debug("开始审核众包回答 - 回答ID: {}, 审核者ID: {}, 状态: {}", 
            answerId, reviewerUserId, status);
        
        // 验证必填字段
        if (answerId == null) {
            throw new IllegalArgumentException("回答ID不能为空");
        }
        if (reviewerUserId == null) {
            throw new IllegalArgumentException("审核者ID不能为空");
        }
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("审核状态不能为空");
        }
        
        // 查找众包回答
        CrowdsourcedAnswer answer = crowdsourcedAnswerRepository.findById(answerId)
            .orElseThrow(() -> {
                logger.error("审核众包回答失败 - 回答不存在: {}", answerId);
                return new IllegalArgumentException("回答不存在");
            });
        
        // 查找审核者
        User reviewer = userRepository.findById(reviewerUserId)
            .orElseThrow(() -> {
                logger.error("审核众包回答失败 - 审核者不存在: {}", reviewerUserId);
                return new IllegalArgumentException("审核者不存在");
            });
        
        try {
            // 更新审核状态
            CrowdsourcedAnswer.QualityReviewStatus reviewStatus = 
                CrowdsourcedAnswer.QualityReviewStatus.valueOf(status.toUpperCase());
            answer.setQualityReviewStatus(reviewStatus);
            answer.setReviewedByUser(reviewer);
            answer.setReviewTime(LocalDateTime.now());
            answer.setReviewFeedback(feedback);
            
            // 保存并返回
            CrowdsourcedAnswer savedAnswer = crowdsourcedAnswerRepository.save(answer);
            logger.info("众包回答审核成功 - ID: {}, 审核者: {}, 状态: {}", 
                savedAnswer.getId(), reviewer.getId(), reviewStatus);
            return convertToDTO(savedAnswer);
        } catch (IllegalArgumentException e) {
            logger.error("审核众包回答失败 - 无效的审核状态: {}", status);
            throw new IllegalArgumentException("无效的审核状态: " + status);
        } catch (Exception e) {
            logger.error("保存审核结果失败", e);
            throw new RuntimeException("保存审核结果失败: " + e.getMessage());
        }
    }
    
    // 将实体转换为DTO
    private CrowdsourcedAnswerDTO convertToDTO(CrowdsourcedAnswer answer) {
        CrowdsourcedAnswerDTO dto = new CrowdsourcedAnswerDTO();
        dto.setId(answer.getId());
        dto.setStandardQuestionId(answer.getStandardQuestion().getId());
        dto.setUserId(answer.getUser().getId());
        dto.setAnswerText(answer.getAnswerText());
        dto.setSubmissionTime(answer.getSubmissionTime());
        dto.setQualityReviewStatus(answer.getQualityReviewStatus().name());
        
        if (answer.getReviewedByUser() != null) {
            dto.setReviewedByUserId(answer.getReviewedByUser().getId());
            dto.setReviewerUsername(answer.getReviewedByUser().getUsername());
        }
        
        dto.setReviewTime(answer.getReviewTime());
        dto.setReviewFeedback(answer.getReviewFeedback());
        dto.setUserUsername(answer.getUser().getUsername());
        
        return dto;
    }
} 
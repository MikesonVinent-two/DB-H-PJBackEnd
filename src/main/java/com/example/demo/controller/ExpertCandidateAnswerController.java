package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.example.demo.dto.ExpertCandidateAnswerDTO;
import com.example.demo.service.ExpertCandidateAnswerService;

@RestController
@RequestMapping("/api/expert-candidate-answers")
@CrossOrigin(origins = "*")
public class ExpertCandidateAnswerController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExpertCandidateAnswerController.class);
    
    @Autowired
    private ExpertCandidateAnswerService expertCandidateAnswerService;
    
    // 提交专家候选回答
    @PostMapping
    public ResponseEntity<?> submitExpertCandidateAnswer(@Valid @RequestBody ExpertCandidateAnswerDTO answerDTO) {
        logger.info("接收到专家候选回答提交请求 - 问题ID: {}, 用户ID: {}", 
            answerDTO.getStandardQuestionId(), answerDTO.getUserId());
        
        try {
            ExpertCandidateAnswerDTO savedAnswer = expertCandidateAnswerService.createExpertCandidateAnswer(answerDTO);
            return ResponseEntity.ok(savedAnswer);
        } catch (IllegalArgumentException e) {
            logger.error("提交专家候选回答失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("提交专家候选回答失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
    
    // 根据问题ID获取专家候选回答
    @GetMapping("/by-question/{questionId}")
    public ResponseEntity<Page<ExpertCandidateAnswerDTO>> getAnswersByQuestionId(
            @PathVariable Long questionId,
            @PageableDefault(size = 10, sort = "submissionTime") Pageable pageable) {
        logger.info("获取问题ID为 {} 的专家候选回答", questionId);
        return ResponseEntity.ok(expertCandidateAnswerService.getAnswersByQuestionId(questionId, pageable));
    }
    
    // 根据用户ID获取专家候选回答
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Page<ExpertCandidateAnswerDTO>> getAnswersByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "submissionTime") Pageable pageable) {
        logger.info("获取用户ID为 {} 的专家候选回答", userId);
        return ResponseEntity.ok(expertCandidateAnswerService.getAnswersByUserId(userId, pageable));
    }
    
    // 更新质量评分和反馈
    @PutMapping("/{answerId}/quality")
    public ResponseEntity<?> updateQualityScoreAndFeedback(
            @PathVariable Long answerId,
            @RequestParam Integer qualityScore,
            @RequestParam(required = false) String feedback) {
        logger.info("更新专家候选回答的质量评分 - 回答ID: {}, 评分: {}", answerId, qualityScore);
        
        try {
            ExpertCandidateAnswerDTO updatedAnswer = expertCandidateAnswerService
                .updateQualityScoreAndFeedback(answerId, qualityScore, feedback);
            return ResponseEntity.ok(updatedAnswer);
        } catch (IllegalArgumentException e) {
            logger.error("更新质量评分失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("更新质量评分失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
} 
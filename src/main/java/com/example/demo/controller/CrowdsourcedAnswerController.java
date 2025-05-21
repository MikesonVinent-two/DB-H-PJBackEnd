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

import com.example.demo.dto.CrowdsourcedAnswerDTO;
import com.example.demo.service.CrowdsourcedAnswerService;

@RestController
@RequestMapping("/api/crowdsourced-answers")
@CrossOrigin(origins = "*")
public class CrowdsourcedAnswerController {
    
    private static final Logger logger = LoggerFactory.getLogger(CrowdsourcedAnswerController.class);
    
    @Autowired
    private CrowdsourcedAnswerService crowdsourcedAnswerService;
    
    // 根据问题ID获取众包回答
    @GetMapping("/by-question/{questionId}")
    public ResponseEntity<Page<CrowdsourcedAnswerDTO>> getAnswersByQuestionId(
            @PathVariable Long questionId,
            @PageableDefault(size = 10, sort = "submissionTime") Pageable pageable) {
        logger.info("获取问题ID为 {} 的众包回答", questionId);
        return ResponseEntity.ok(crowdsourcedAnswerService.getAnswersByQuestionId(questionId, pageable));
    }
    
    // 根据用户ID获取众包回答
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Page<CrowdsourcedAnswerDTO>> getAnswersByUserId(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "submissionTime") Pageable pageable) {
        logger.info("获取用户ID为 {} 的众包回答", userId);
        return ResponseEntity.ok(crowdsourcedAnswerService.getAnswersByUserId(userId, pageable));
    }
    
    // 根据审核状态获取众包回答
    @GetMapping("/by-status/{status}")
    public ResponseEntity<Page<CrowdsourcedAnswerDTO>> getAnswersByStatus(
            @PathVariable String status,
            @PageableDefault(size = 10, sort = "submissionTime") Pageable pageable) {
        logger.info("获取状态为 {} 的众包回答", status);
        return ResponseEntity.ok(crowdsourcedAnswerService.getAnswersByStatus(status, pageable));
    }
    
    // 根据问题ID和审核状态获取众包回答
    @GetMapping("/by-question/{questionId}/status/{status}")
    public ResponseEntity<Page<CrowdsourcedAnswerDTO>> getAnswersByQuestionIdAndStatus(
            @PathVariable Long questionId,
            @PathVariable String status,
            @PageableDefault(size = 10, sort = "submissionTime") Pageable pageable) {
        logger.info("获取问题ID为 {} 且状态为 {} 的众包回答", questionId, status);
        return ResponseEntity.ok(
            crowdsourcedAnswerService.getAnswersByQuestionIdAndStatus(questionId, status, pageable));
    }

    // 提交众包回答
    @PostMapping
    public ResponseEntity<?> submitCrowdsourcedAnswer(@Valid @RequestBody CrowdsourcedAnswerDTO answerDTO) {
        logger.info("接收到众包回答提交请求 - 问题ID: {}, 用户ID: {}", 
            answerDTO.getStandardQuestionId(), answerDTO.getUserId());
        
        try {
            CrowdsourcedAnswerDTO savedAnswer = crowdsourcedAnswerService.createCrowdsourcedAnswer(answerDTO);
            return ResponseEntity.ok(savedAnswer);
        } catch (IllegalArgumentException e) {
            logger.error("提交众包回答失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("提交众包回答失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }

    // 审核众包回答
    @PutMapping("/{answerId}/review")
    public ResponseEntity<?> reviewCrowdsourcedAnswer(
            @PathVariable Long answerId,
            @RequestBody ReviewRequest reviewRequest) {
        logger.info("接收到众包回答审核请求 - 回答ID: {}, 审核状态: {}", 
            answerId, reviewRequest.getStatus());
        
        try {
            CrowdsourcedAnswerDTO reviewedAnswer = crowdsourcedAnswerService.reviewAnswer(
                answerId,
                reviewRequest.getReviewerUserId(),
                reviewRequest.getStatus(),
                reviewRequest.getFeedback()
            );
            return ResponseEntity.ok(reviewedAnswer);
        } catch (IllegalArgumentException e) {
            logger.error("审核众包回答失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("审核众包回答失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
}

// 审核请求的数据传输对象
class ReviewRequest {
    private Long reviewerUserId;
    private String status;  // ACCEPTED, REJECTED, FLAGGED
    private String feedback;

    // Getters and Setters
    public Long getReviewerUserId() {
        return reviewerUserId;
    }

    public void setReviewerUserId(Long reviewerUserId) {
        this.reviewerUserId = reviewerUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
} 
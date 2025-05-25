package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

import com.example.demo.dto.CrowdsourcedAnswerDTO;
import com.example.demo.service.CrowdsourcedAnswerService;

@RestController
@RequestMapping("/crowdsourced-answers")
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
        logger.info("接收到众包回答提交请求 - 问题ID: {}, 用户ID: {}, 任务批次ID: {}", 
            answerDTO.getStandardQuestionId(), answerDTO.getUserId(), answerDTO.getTaskBatchId());
        
        try {
            CrowdsourcedAnswerDTO savedAnswer = crowdsourcedAnswerService.createCrowdsourcedAnswer(answerDTO);
            return ResponseEntity.ok(savedAnswer);
        } catch (IllegalArgumentException e) {
            logger.error("提交众包回答失败 - 参数错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "INVALID_PARAMETERS");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (IllegalStateException e) {
            // 处理已存在记录的情况
            logger.error("提交众包回答失败 - 重复提交", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "DUPLICATE_SUBMISSION");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            logger.error("提交众包回答失败 - 服务器错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "SERVER_ERROR");
            response.put("message", "服务器处理请求时发生错误");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 修改众包回答
    @PutMapping("/{answerId}")
    public ResponseEntity<?> updateCrowdsourcedAnswer(
            @PathVariable Long answerId,
            @Valid @RequestBody CrowdsourcedAnswerDTO answerDTO) {
        logger.info("接收到修改众包回答请求 - 回答ID: {}, 用户ID: {}", answerId, answerDTO.getUserId());
        
        try {
            CrowdsourcedAnswerDTO updatedAnswer = crowdsourcedAnswerService.updateCrowdsourcedAnswer(answerId, answerDTO);
            return ResponseEntity.ok(updatedAnswer);
        } catch (IllegalArgumentException e) {
            logger.error("修改众包回答失败 - 参数错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "INVALID_PARAMETERS");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (IllegalStateException e) {
            // 处理权限或状态错误
            logger.error("修改众包回答失败 - 权限或状态错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "OPERATION_NOT_ALLOWED");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            logger.error("修改众包回答失败 - 服务器错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "SERVER_ERROR");
            response.put("message", "服务器处理请求时发生错误");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "INVALID_PARAMETERS");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("审核众包回答失败 - 服务器错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "SERVER_ERROR");
            response.put("message", "服务器处理请求时发生错误");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 删除众包回答
     * @param answerId 回答ID
     * @param userId 用户ID（必须是回答的创建者）
     * @return 操作结果
     */
    @DeleteMapping("/{answerId}")
    public ResponseEntity<?> deleteCrowdsourcedAnswer(
            @PathVariable Long answerId,
            @RequestParam Long userId) {
        logger.info("接收到删除众包回答请求 - 回答ID: {}, 用户ID: {}", answerId, userId);
        
        try {
            boolean result = crowdsourcedAnswerService.deleteCrowdsourcedAnswer(answerId, userId);
            
            if (result) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "众包回答删除成功");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "删除操作未完成");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (IllegalArgumentException e) {
            // 回答不存在等参数错误
            logger.error("删除众包回答失败 - 参数错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "INVALID_PARAMETERS");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (IllegalStateException e) {
            // 权限或状态错误
            logger.error("删除众包回答失败 - 权限或状态错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "OPERATION_NOT_ALLOWED");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            // 其他服务器错误
            logger.error("删除众包回答失败 - 服务器错误", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("code", "SERVER_ERROR");
            response.put("message", "服务器处理请求时发生错误");
            response.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
package com.example.demo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.BatchTagOperationsDTO;
import com.example.demo.dto.StandardQuestionDTO;
import com.example.demo.dto.QuestionHistoryDTO;
import com.example.demo.dto.TagOperationDTO;
import com.example.demo.entity.StandardQuestion;
import com.example.demo.entity.StandardQuestionTag;
import com.example.demo.service.StandardQuestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/standard-questions")
@CrossOrigin
public class StandardQuestionController {
    
    private static final Logger logger = LoggerFactory.getLogger(StandardQuestionController.class);
    
    private final StandardQuestionService standardQuestionService;
    
    // 显式构造函数
    public StandardQuestionController(StandardQuestionService standardQuestionService) {
        this.standardQuestionService = standardQuestionService;
    }
    
    /**
     * 获取所有标准问题，支持分页
     * @param pageable 分页参数
     * @return 标准问题分页列表
     */
    @GetMapping
    public ResponseEntity<Page<StandardQuestionDTO>> getAllStandardQuestions(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        logger.info("接收到获取所有标准问题请求 - 页码: {}, 每页大小: {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<StandardQuestionDTO> questions = standardQuestionService.findAllStandardQuestions(pageable);
            logger.info("成功获取标准问题 - 总数: {}", questions.getTotalElements());
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            logger.error("获取标准问题失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<StandardQuestionDTO> createStandardQuestion(
            @RequestBody @Valid StandardQuestionDTO questionDTO) {
        StandardQuestionDTO createdQuestion = standardQuestionService.createStandardQuestion(questionDTO, questionDTO.getUserId());
        return ResponseEntity.ok(createdQuestion);
    }

    @PutMapping("/{questionId}")
    public ResponseEntity<StandardQuestionDTO> updateStandardQuestion(
            @PathVariable Long questionId,
            @RequestBody @Valid StandardQuestionDTO questionDTO) {
        StandardQuestionDTO updatedQuestion = standardQuestionService.updateStandardQuestion(questionId, questionDTO, questionDTO.getUserId());
        return ResponseEntity.ok(updatedQuestion);
    }

    /**
     * 获取标准问题的修改历史
     * 
     * @param questionId 问题ID
     * @return 问题的所有历史版本信息
     */
    @GetMapping("/{questionId}/history")
    public ResponseEntity<List<QuestionHistoryDTO>> getQuestionHistory(
            @PathVariable Long questionId) {
        logger.info("接收到获取问题修改历史请求 - 问题ID: {}", questionId);
        try {
            List<QuestionHistoryDTO> history = standardQuestionService.getQuestionHistory(questionId);
            if (history.isEmpty()) {
                logger.warn("未找到问题的修改历史 - 问题ID: {}", questionId);
                return ResponseEntity.notFound().build();
            }
            logger.info("成功获取问题修改历史 - 问题ID: {}, 版本数量: {}", questionId, history.size());
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            logger.error("获取问题修改历史失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(new ArrayList<>());
        } catch (Exception e) {
            logger.error("获取问题修改历史失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body(new ArrayList<>());
        }
    }

    /**
     * 获取问题的版本树结构
     * 
     * @param questionId 问题ID
     * @return 问题的版本树结构
     */
    @GetMapping("/{questionId}/version-tree")
    public ResponseEntity<List<QuestionHistoryDTO>> getVersionTree(
            @PathVariable Long questionId) {
        logger.info("接收到获取问题版本树请求 - 问题ID: {}", questionId);
        try {
            List<QuestionHistoryDTO> versionTree = standardQuestionService.getVersionTree(questionId);
            if (versionTree.isEmpty()) {
                logger.warn("未找到问题的版本树 - 问题ID: {}", questionId);
                return ResponseEntity.notFound().build();
            }
            logger.info("成功获取问题版本树 - 问题ID: {}", questionId);
            return ResponseEntity.ok(versionTree);
        } catch (IllegalArgumentException e) {
            logger.error("获取问题版本树失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(new ArrayList<>());
        } catch (Exception e) {
            logger.error("获取问题版本树失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body(new ArrayList<>());
        }
    }

    /**
     * 更新标准问题的标签
     * 
     * @param operationDTO 标签操作请求
     * @return 更新后的标准问题
     */
    @PostMapping("/tags")
    public ResponseEntity<?> updateQuestionTags(
            @RequestBody @Valid TagOperationDTO operationDTO) {
        logger.info("接收到更新标准问题标签请求 - 问题ID: {}, 操作类型: {}", 
            operationDTO.getQuestionId(), operationDTO.getOperationType());
        
        try {
            StandardQuestionDTO updatedQuestion = standardQuestionService.updateQuestionTags(operationDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "标签更新成功");
            response.put("question", updatedQuestion);
            
            logger.info("成功更新标准问题标签 - 问题ID: {}", operationDTO.getQuestionId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("更新标准问题标签失败 - 参数错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("更新标准问题标签失败 - 服务器错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "更新标签时发生错误");
            response.put("details", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 批量更新多个标准问题的标签
     * 
     * @param batchOperationsDTO 批量标签操作请求
     * @return 批量操作结果
     */
    @PostMapping("/batch-tags")
    public ResponseEntity<?> batchUpdateQuestionTags(
            @RequestBody @Valid BatchTagOperationsDTO batchOperationsDTO) {
        logger.info("接收到批量更新标准问题标签请求 - 操作数量: {}", 
            batchOperationsDTO.getOperations() != null ? batchOperationsDTO.getOperations().size() : 0);
        
        try {
            Map<Long, Boolean> results = standardQuestionService.batchUpdateQuestionTags(batchOperationsDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "批量标签更新完成");
            response.put("results", results);
            response.put("total", results.size());
            response.put("success", results.values().stream().filter(v -> v).count());
            response.put("failed", results.values().stream().filter(v -> !v).count());
            
            logger.info("成功批量更新标准问题标签 - 总数: {}, 成功: {}", 
                results.size(), results.values().stream().filter(v -> v).count());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("批量更新标准问题标签失败 - 参数错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("批量更新标准问题标签失败 - 服务器错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "批量更新标签时发生错误");
            response.put("details", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private Map<String, Object> convertToDTO(StandardQuestion question) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", question.getId());
        dto.put("questionText", question.getQuestionText());
        dto.put("questionType", question.getQuestionType());
        dto.put("difficulty", question.getDifficulty());
        dto.put("creationTime", question.getCreationTime());
        dto.put("createdByUserId", question.getCreatedByUser().getId());
        
        if (question.getParentStandardQuestion() != null) {
            dto.put("parentQuestionId", question.getParentStandardQuestion().getId());
        }
        
        if (question.getOriginalRawQuestion() != null) {
            dto.put("originalRawQuestionId", question.getOriginalRawQuestion().getId());
        }
        
        List<String> tags = question.getQuestionTags().stream()
            .map(tag -> tag.getTag().getTagName())
            .collect(Collectors.toList());
        dto.put("tags", tags);
        
        return dto;
    }
} 
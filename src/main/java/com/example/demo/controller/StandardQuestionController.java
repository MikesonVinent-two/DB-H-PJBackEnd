package com.example.demo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.StandardQuestionDTO;
import com.example.demo.entity.StandardQuestion;
import com.example.demo.entity.StandardQuestionTag;
import com.example.demo.service.StandardQuestionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/standard/standard-questions")
@CrossOrigin(origins = "*")
public class StandardQuestionController {
    
    private static final Logger logger = LoggerFactory.getLogger(StandardQuestionController.class);
    
    @Autowired
    private StandardQuestionService standardQuestionService;
    
    @PostMapping
    public ResponseEntity<?> createStandardQuestion(
            @Valid @RequestBody StandardQuestionDTO questionDTO) {
        logger.info("接收到创建标准问题请求：{}", questionDTO.getQuestionText());
        
        try {
            // 验证必要字段
            if (questionDTO.getUserId() == null) {
                logger.error("创建标准问题失败 - 用户ID为空");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "用户ID不能为空"));
            }
            
            if (questionDTO.getQuestionText() == null || questionDTO.getQuestionText().trim().isEmpty()) {
                logger.error("创建标准问题失败 - 问题文本为空");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "问题文本不能为空"));
            }
            
            if (questionDTO.getQuestionType() == null) {
                logger.error("创建标准问题失败 - 问题类型为空");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "问题类型不能为空"));
            }
            
            StandardQuestion savedQuestion = standardQuestionService.createStandardQuestion(
                questionDTO, questionDTO.getUserId());
            
            // 提取标签信息
            List<String> tagNames = new ArrayList<>();
            if (savedQuestion.getQuestionTags() != null && !savedQuestion.getQuestionTags().isEmpty()) {
                tagNames = savedQuestion.getQuestionTags().stream()
                    .map(tag -> tag.getTag().getTagName())
                    .collect(Collectors.toList());
            }
            
            // 构建成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedQuestion.getId());
            response.put("questionText", savedQuestion.getQuestionText());
            response.put("questionType", savedQuestion.getQuestionType());
            response.put("difficulty", savedQuestion.getDifficulty());
            response.put("createdByUserId", savedQuestion.getCreatedByUser().getId());
            response.put("tags", tagNames);
            response.put("message", "标准问题创建成功");
            
            logger.info("标准问题创建成功 - ID: {}", savedQuestion.getId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("创建标准问题失败 - 参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("创建标准问题时发生未预期的错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "处理请求时发生错误", 
                           "message", e.getMessage()));
        }
    }
} 
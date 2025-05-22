package com.example.demo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.StandardQuestionDTO;
import com.example.demo.dto.QuestionHistoryDTO;
import com.example.demo.entity.StandardQuestion;
import com.example.demo.entity.StandardQuestionTag;
import com.example.demo.service.StandardQuestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/standard-questions")
@CrossOrigin
public class StandardQuestionController {
    
    private static final Logger logger = LoggerFactory.getLogger(StandardQuestionController.class);
    
    private final StandardQuestionService standardQuestionService;
    
    // 显式构造函数
    public StandardQuestionController(StandardQuestionService standardQuestionService) {
        this.standardQuestionService = standardQuestionService;
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
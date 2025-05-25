package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.StandardAnswerDTO;
import com.example.demo.service.StandardAnswerService;

@RestController
@RequestMapping("/standard/standard-answers")
public class StandardAnswerController {
    
    private static final Logger logger = LoggerFactory.getLogger(StandardAnswerController.class);
    
    @Autowired
    private StandardAnswerService standardAnswerService;
    
    @PostMapping
    public ResponseEntity<?> createOrUpdateStandardAnswer(@RequestBody StandardAnswerDTO answerDTO) {
        logger.debug("接收到创建/更新标准答案请求 - 用户ID: {}", answerDTO.getUserId());
        try {
            Object result = standardAnswerService.createOrUpdateStandardAnswer(answerDTO, answerDTO.getUserId());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.error("创建/更新标准答案失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("创建/更新标准答案失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
    
    @GetMapping("/{standardQuestionId}")
    public ResponseEntity<?> getStandardAnswer(@PathVariable Long standardQuestionId) {
        logger.debug("接收到获取标准答案请求 - 标准问题ID: {}", standardQuestionId);
        try {
            Object result = standardAnswerService.getStandardAnswer(standardQuestionId);
            if (result == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            logger.error("获取标准答案失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("获取标准答案失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
    
    @DeleteMapping("/{standardQuestionId}")
    public ResponseEntity<?> deleteStandardAnswer(
            @PathVariable Long standardQuestionId,
            @RequestBody StandardAnswerDTO answerDTO) {
        logger.debug("接收到删除标准答案请求 - 标准问题ID: {}, 用户ID: {}", standardQuestionId, answerDTO.getUserId());
        try {
            standardAnswerService.deleteStandardAnswer(standardQuestionId, answerDTO.getUserId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.error("删除标准答案失败 - 参数错误", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("删除标准答案失败 - 服务器错误", e);
            return ResponseEntity.internalServerError().body("服务器处理请求时发生错误");
        }
    }
} 
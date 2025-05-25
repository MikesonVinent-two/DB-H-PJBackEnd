package com.example.demo.controller;

import com.example.demo.dto.AnswerPromptAssemblyConfigDTO;
import com.example.demo.dto.EvaluationPromptAssemblyConfigDTO;
import com.example.demo.service.PromptAssemblyConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/prompt-assembly")
@CrossOrigin(origins = "*")
public class PromptAssemblyConfigController {
    
    private static final Logger logger = LoggerFactory.getLogger(PromptAssemblyConfigController.class);
    
    private final PromptAssemblyConfigService configService;
    
    @Autowired
    public PromptAssemblyConfigController(PromptAssemblyConfigService configService) {
        this.configService = configService;
    }
    
    /**
     * 创建回答提示词组装配置
     */
    @PostMapping("/answer-configs")
    public ResponseEntity<?> createAnswerConfig(
            @Valid @RequestBody AnswerPromptAssemblyConfigDTO configDTO,
            @RequestParam Long userId) {
        logger.info("接收到创建回答提示词组装配置请求: {}", configDTO.getName());
        
        try {
            AnswerPromptAssemblyConfigDTO createdConfig = configService.createAnswerConfig(configDTO, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "回答提示词组装配置创建成功");
            response.put("config", createdConfig);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建回答提示词组装配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建回答提示词组装配置失败");
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 创建评测提示词组装配置
     */
    @PostMapping("/evaluation-configs")
    public ResponseEntity<?> createEvaluationConfig(
            @Valid @RequestBody EvaluationPromptAssemblyConfigDTO configDTO,
            @RequestParam Long userId) {
        logger.info("接收到创建评测提示词组装配置请求: {}", configDTO.getName());
        
        try {
            EvaluationPromptAssemblyConfigDTO createdConfig = configService.createEvaluationConfig(configDTO, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "评测提示词组装配置创建成功");
            response.put("config", createdConfig);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建评测提示词组装配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建评测提示词组装配置失败");
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取回答提示词组装配置
     */
    @GetMapping("/answer-configs/{configId}")
    public ResponseEntity<?> getAnswerConfig(@PathVariable Long configId) {
        logger.info("接收到获取回答提示词组装配置请求: ID={}", configId);
        
        try {
            AnswerPromptAssemblyConfigDTO config = configService.getAnswerConfig(configId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("config", config);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取回答提示词组装配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取回答提示词组装配置失败");
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取评测提示词组装配置
     */
    @GetMapping("/evaluation-configs/{configId}")
    public ResponseEntity<?> getEvaluationConfig(@PathVariable Long configId) {
        logger.info("接收到获取评测提示词组装配置请求: ID={}", configId);
        
        try {
            EvaluationPromptAssemblyConfigDTO config = configService.getEvaluationConfig(configId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("config", config);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取评测提示词组装配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取评测提示词组装配置失败");
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取所有活跃的回答提示词组装配置
     */
    @GetMapping("/answer-configs")
    public ResponseEntity<?> getAllActiveAnswerConfigs() {
        logger.info("接收到获取所有活跃回答提示词组装配置请求");
        
        try {
            List<AnswerPromptAssemblyConfigDTO> configs = configService.getAllActiveAnswerConfigs();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configs", configs);
            response.put("total", configs.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取所有活跃回答提示词组装配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取所有活跃回答提示词组装配置失败");
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取所有活跃的评测提示词组装配置
     */
    @GetMapping("/evaluation-configs")
    public ResponseEntity<?> getAllActiveEvaluationConfigs() {
        logger.info("接收到获取所有活跃评测提示词组装配置请求");
        
        try {
            List<EvaluationPromptAssemblyConfigDTO> configs = configService.getAllActiveEvaluationConfigs();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configs", configs);
            response.put("total", configs.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取所有活跃评测提示词组装配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取所有活跃评测提示词组装配置失败");
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户创建的回答提示词组装配置
     */
    @GetMapping("/answer-configs/user/{userId}")
    public ResponseEntity<?> getAnswerConfigsByUser(@PathVariable Long userId) {
        logger.info("接收到获取用户创建的回答提示词组装配置请求: 用户ID={}", userId);
        
        try {
            List<AnswerPromptAssemblyConfigDTO> configs = configService.getAnswerConfigsByUser(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configs", configs);
            response.put("total", configs.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取用户创建的回答提示词组装配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取用户创建的回答提示词组装配置失败");
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户创建的评测提示词组装配置
     */
    @GetMapping("/evaluation-configs/user/{userId}")
    public ResponseEntity<?> getEvaluationConfigsByUser(@PathVariable Long userId) {
        logger.info("接收到获取用户创建的评测提示词组装配置请求: 用户ID={}", userId);
        
        try {
            List<EvaluationPromptAssemblyConfigDTO> configs = configService.getEvaluationConfigsByUser(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configs", configs);
            response.put("total", configs.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取用户创建的评测提示词组装配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取用户创建的评测提示词组装配置失败");
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
} 
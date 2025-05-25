package com.example.demo.controller;

import com.example.demo.dto.LLMModelDTO;
import com.example.demo.dto.LLMModelRegistrationRequest;
import com.example.demo.dto.LLMModelRegistrationResponse;
import com.example.demo.service.LLMModelService;
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
@RequestMapping("/llm-models")
@CrossOrigin(origins = "*")
public class LLMModelController {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMModelController.class);
    
    @Autowired
    private LLMModelService llmModelService;
    
    @PostMapping("/register")
    public ResponseEntity<LLMModelRegistrationResponse> registerModels(
            @Valid @RequestBody LLMModelRegistrationRequest request) {
        LLMModelRegistrationResponse response = llmModelService.registerModels(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取所有已注册的LLM模型
     * 
     * @return 已注册的LLM模型列表
     */
    @GetMapping
    public ResponseEntity<?> getAllModels() {
        logger.info("接收到获取所有LLM模型的请求");
        
        try {
            List<LLMModelDTO> models = llmModelService.getAllModels();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("models", models);
            response.put("total", models.size());
            
            logger.info("成功获取所有LLM模型，共 {} 个", models.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取LLM模型失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取LLM模型时发生错误");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 
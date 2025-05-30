package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.jdbc.Evaluator;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.EvaluatorRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.util.ApiConstants;

/**
 * 评测者管理控制器
 */
@RestController
@RequestMapping("/evaluators")
@CrossOrigin(origins = "*")
public class EvaluatorController {
    
    private static final Logger logger = LoggerFactory.getLogger(EvaluatorController.class);
    
    private final EvaluatorRepository evaluatorRepository;
    private final UserRepository userRepository;
    
    public EvaluatorController(EvaluatorRepository evaluatorRepository, UserRepository userRepository) {
        this.evaluatorRepository = evaluatorRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * 获取所有评测者
     */
    @GetMapping
    public ResponseEntity<List<Evaluator>> getAllEvaluators() {
        logger.info("接收到获取所有评测者请求");
        List<Evaluator> evaluators = evaluatorRepository.findAll();
        return ResponseEntity.ok(evaluators);
    }
    
    /**
     * 获取所有人类评测者
     */
    @GetMapping("/human")
    public ResponseEntity<List<Evaluator>> getAllHumanEvaluators() {
        logger.info("接收到获取所有人类评测者请求");
        List<Evaluator> evaluators = evaluatorRepository.findByEvaluatorType(Evaluator.EvaluatorType.HUMAN);
        return ResponseEntity.ok(evaluators);
    }
    
    /**
     * 获取所有AI评测者
     */
    @GetMapping("/ai")
    public ResponseEntity<List<Evaluator>> getAllAiEvaluators() {
        logger.info("接收到获取所有AI评测者请求");
        List<Evaluator> evaluators = evaluatorRepository.findByEvaluatorType(Evaluator.EvaluatorType.AI_MODEL);
        return ResponseEntity.ok(evaluators);
    }
    
    /**
     * 根据ID获取评测者
     */
    @GetMapping("/{id}")
    public ResponseEntity<Evaluator> getEvaluatorById(@PathVariable Long id) {
        logger.info("接收到获取评测者请求，ID: {}", id);
        
        return evaluatorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 根据用户ID获取评测者
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Evaluator> getEvaluatorByUserId(@PathVariable Long userId) {
        logger.info("接收到根据用户ID获取评测者请求，用户ID: {}", userId);
        
        // 查找与该用户关联的评测者
        List<Evaluator> evaluators = evaluatorRepository.findAll();
        Optional<Evaluator> evaluator = evaluators.stream()
            .filter(e -> e.getUser() != null && e.getUser().getId().equals(userId))
            .findFirst();
        
        return evaluator
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 创建新评测者
     */
    @PostMapping
    public ResponseEntity<Evaluator> createEvaluator(@RequestBody Evaluator evaluator) {
        logger.info("接收到创建评测者请求，ID: {}", evaluator.getId());
        
        if (evaluator.getId() != null) {
            return ResponseEntity.badRequest().build();
        }
        
        // 如果是人类评测者，验证用户ID是否存在
        if (evaluator.getEvaluatorType() == Evaluator.EvaluatorType.HUMAN && evaluator.getUser() != null) {
            if (evaluator.getUser().getId() == null || !userRepository.existsById(evaluator.getUser().getId())) {
                return ResponseEntity.badRequest().body(null);
            }
        }
        
        Evaluator savedEvaluator = evaluatorRepository.save(evaluator);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEvaluator);
    }
    
    /**
     * 更新评测者信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<Evaluator> updateEvaluator(@PathVariable Long id, @RequestBody Evaluator evaluator) {
        logger.info("接收到更新评测者请求，ID: {}", id);
        
        if (!evaluatorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        evaluator.setId(id);
        Evaluator updatedEvaluator = evaluatorRepository.save(evaluator);
        return ResponseEntity.ok(updatedEvaluator);
    }
    
    /**
     * 删除评测者
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvaluator(@PathVariable Long id) {
        logger.info("接收到删除评测者请求，ID: {}", id);
        
        if (!evaluatorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        evaluatorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 用户注册成为评测者
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerAsEvaluator(@RequestBody Map<String, Object> request) {
        logger.info("接收到注册用户成为评测者请求");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户未登录"));
            
            if (currentUser == null) {
                response.put(ApiConstants.KEY_SUCCESS, false);
                response.put(ApiConstants.KEY_MESSAGE, "用户未登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // 检查用户是否已经是评测者
            List<Evaluator> evaluators = evaluatorRepository.findAll();
            Optional<Evaluator> existingEvaluator = evaluators.stream()
                .filter(e -> e.getUser() != null && e.getUser().getId().equals(currentUser.getId()))
                .findFirst();
            
            if (existingEvaluator.isPresent()) {
                response.put(ApiConstants.KEY_SUCCESS, false);
                response.put(ApiConstants.KEY_MESSAGE, "用户已经是评测者");
                response.put("evaluator", existingEvaluator.get());
                return ResponseEntity.ok(response);
            }
            
            // 创建新评测者
            String evaluatorName = request.get("name") != null ? 
                    request.get("name").toString() : currentUser.getName() + "的评测账号";
            
            Evaluator evaluator = new Evaluator();
            evaluator.setName(evaluatorName);
            evaluator.setEvaluatorType(Evaluator.EvaluatorType.HUMAN);
            evaluator.setUser(currentUser);
            evaluator.setCreatedByUser(currentUser);
            
            Evaluator savedEvaluator = evaluatorRepository.save(evaluator);
            
            response.put(ApiConstants.KEY_SUCCESS, true);
            response.put(ApiConstants.KEY_MESSAGE, "成功注册为评测者");
            response.put("evaluator", savedEvaluator);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            response.put(ApiConstants.KEY_SUCCESS, false);
            response.put(ApiConstants.KEY_MESSAGE, "注册评测者失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        logger.error("处理评测者请求时发生异常", e);
        
        Map<String, String> response = new HashMap<>();
        response.put("error", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
} 
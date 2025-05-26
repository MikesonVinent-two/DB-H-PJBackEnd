package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.AnswerGenerationBatchDTO;
import com.example.demo.dto.ModelAnswerRunDTO;
import com.example.demo.manager.BatchStateManager;
import com.example.demo.service.AnswerGenerationService;
import com.example.demo.service.AnswerGenerationService.AnswerGenerationBatchCreateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/answer-generation")
@CrossOrigin(origins = "*")
public class AnswerGenerationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AnswerGenerationController.class);
    
    private final AnswerGenerationService answerGenerationService;
    private final BatchStateManager batchStateManager;
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public AnswerGenerationController(AnswerGenerationService answerGenerationService, 
                                      BatchStateManager batchStateManager,
                                      JdbcTemplate jdbcTemplate) {
        this.answerGenerationService = answerGenerationService;
        this.batchStateManager = batchStateManager;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @PostMapping("/batches")
    public ResponseEntity<AnswerGenerationBatchDTO> createBatch(@Valid @RequestBody AnswerGenerationBatchCreateRequest request) {
        logger.debug("创建回答生成批次: {}", request.getName());
        AnswerGenerationBatchDTO batch = answerGenerationService.createBatch(request);
        return ResponseEntity.ok(batch);
    }
    
    @PostMapping("/batches/{batchId}/start")
    public ResponseEntity<Map<String, Object>> startBatch(@PathVariable Long batchId) {
        logger.debug("启动回答生成批次: {}", batchId);
        
        // 首先检查批次当前状态
        String currentStatus = batchStateManager.getBatchState(batchId);
        logger.info("批次{}当前Redis状态为: {}", batchId, currentStatus);
        
        // 确保Redis状态与数据库一致
        if (batchStateManager != null) {
            String dbStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM answer_generation_batches WHERE id = ?", 
                String.class, batchId);
            
            logger.info("批次{}数据库状态: {}, Redis状态: {}", batchId, dbStatus, currentStatus);
            
            // 如果状态不一致，以数据库为准更新Redis
            if (!dbStatus.equals(currentStatus)) {
                logger.warn("批次{}的Redis状态({})与数据库状态({})不一致，更新Redis状态", 
                    batchId, currentStatus, dbStatus);
                batchStateManager.setBatchState(batchId, dbStatus);
                // 如果数据库是PAUSED状态，确保中断标志一致
                if ("PAUSED".equals(dbStatus)) {
                    batchStateManager.setInterruptFlag(batchId, true);
                } else {
                    batchStateManager.setInterruptFlag(batchId, false);
                }
                currentStatus = dbStatus;
            }
        }
        
        // 创建一个后台线程启动批次，不阻塞当前请求
        new Thread(() -> {
            try {
                logger.info("启动独立线程处理批次{}", batchId);
                answerGenerationService.startBatch(batchId);
                logger.info("批次{}启动线程执行完成", batchId);
            } catch (Exception e) {
                logger.error("批次{}启动失败: {}", batchId, e.getMessage(), e);
                
                // 发送错误通知
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("batchId", batchId);
                errorData.put("error", "批次启动失败: " + e.getMessage());
                errorData.put("timestamp", System.currentTimeMillis());
                
                try {
                    answerGenerationService.sendErrorNotification(batchId, errorData);
                } catch (Exception ex) {
                    logger.error("发送错误通知失败", ex);
                }
            }
        }).start();
        
        // 立即返回响应，不等待批次启动完成
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("batchId", batchId);
        response.put("message", "批次启动请求已接收，正在后台处理");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/batches/{batchId}/pause")
    public ResponseEntity<Map<String, Object>> pauseBatch(@PathVariable Long batchId, @RequestParam String reason) {
        logger.debug("暂停回答生成批次: {}, 原因: {}", batchId, reason);
        
        // 使用BatchStateManager处理暂停
        boolean success = batchStateManager.pauseBatch(batchId, reason);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("batchId", batchId);
        response.put("status", batchStateManager.getBatchState(batchId));
        response.put("message", success ? "批次已暂停" : "暂停请求已提交，但操作可能未完成");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/batches/{batchId}/resume")
    public ResponseEntity<Map<String, Object>> resumeBatch(@PathVariable Long batchId) {
        logger.debug("恢复回答生成批次: {}", batchId);

        try {
            // 验证批次状态
            String currentStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM answer_generation_batches WHERE id = ?", 
                String.class, batchId);
                
            if (!"PAUSED".equals(currentStatus)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("batchId", batchId);
                response.put("status", currentStatus);
                response.put("message", "只能恢复PAUSED状态的批次，当前状态: " + currentStatus);
                return ResponseEntity.badRequest().body(response);
            }
            
            // 直接调用服务层方法恢复批次
            // 避免多重线程和多次调用导致的竞态条件
            logger.info("直接调用服务方法恢复批次{}", batchId);
            answerGenerationService.forceBatchResume(batchId);
            
            // 立即返回响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("batchId", batchId);
            response.put("status", "IN_PROGRESS"); // 服务层已将状态更新为IN_PROGRESS
            response.put("message", "批次恢复请求已处理");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("处理批次{}恢复请求时出错: {}", batchId, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("batchId", batchId);
            response.put("error", e.getMessage());
            response.put("message", "处理批次恢复请求时出错: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @PostMapping("/batches/{batchId}/reset-failed")
    public ResponseEntity<Map<String, Object>> resetFailedBatch(@PathVariable Long batchId) {
        logger.debug("重置失败的批次状态: {}", batchId);
        
        try {
            // 获取当前批次状态
            String currentStatus = batchStateManager.getBatchState(batchId);
            
            if (!"FAILED".equals(currentStatus)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("batchId", batchId);
                response.put("status", currentStatus);
                response.put("message", "只能重置FAILED状态的批次，当前状态: " + currentStatus);
                return ResponseEntity.badRequest().body(response);
            }
            
            // 更新数据库状态为PAUSED
            answerGenerationService.resetFailedBatch(batchId);
            
            // 同步更新Redis状态
            batchStateManager.setBatchState(batchId, "PAUSED");
            batchStateManager.setInterruptFlag(batchId, true);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("batchId", batchId);
            response.put("status", "PAUSED");
            response.put("message", "批次状态已成功从FAILED重置为PAUSED，可以重新恢复处理");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("重置批次{}状态失败", batchId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("batchId", batchId);
            response.put("error", e.getMessage());
            response.put("message", "重置批次状态失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/batches/{batchId}")
    public ResponseEntity<AnswerGenerationBatchDTO> getBatchStatus(@PathVariable Long batchId) {
        logger.debug("获取回答生成批次状态: {}", batchId);
        // 同步批次状态
        batchStateManager.syncBatchState(batchId);
        AnswerGenerationBatchDTO batch = answerGenerationService.getBatchStatus(batchId);
        return ResponseEntity.ok(batch);
    }
    
    @GetMapping("/runs/{runId}")
    public ResponseEntity<ModelAnswerRunDTO> getRunStatus(@PathVariable Long runId) {
        logger.debug("获取模型回答运行状态: {}", runId);
        ModelAnswerRunDTO run = answerGenerationService.getRunStatus(runId);
        return ResponseEntity.ok(run);
    }
    
    @GetMapping("/batches/user/{userId}")
    public ResponseEntity<List<AnswerGenerationBatchDTO>> getBatchesByUserId(@PathVariable Long userId) {
        logger.debug("获取用户创建的所有批次: {}", userId);
        List<AnswerGenerationBatchDTO> batches = answerGenerationService.getBatchesByUserId(userId);
        return ResponseEntity.ok(batches);
    }
    
    @GetMapping("/runs/batch/{batchId}")
    public ResponseEntity<List<ModelAnswerRunDTO>> getRunsByBatchId(@PathVariable Long batchId) {
        logger.debug("获取批次的所有运行: {}", batchId);
        List<ModelAnswerRunDTO> runs = answerGenerationService.getRunsByBatchId(batchId);
        return ResponseEntity.ok(runs);
    }
    
    @GetMapping("/runs/model/{modelId}")
    public ResponseEntity<List<ModelAnswerRunDTO>> getRunsByModelId(@PathVariable Long modelId) {
        logger.debug("获取特定模型的所有运行: {}", modelId);
        List<ModelAnswerRunDTO> runs = answerGenerationService.getRunsByModelId(modelId);
        return ResponseEntity.ok(runs);
    }
    
    @GetMapping("/system/test-connectivity")
    public ResponseEntity<Map<String, Object>> testSystemConnectivity() {
        logger.debug("测试系统连通性");
        
        try {
            Map<String, Object> result = answerGenerationService.testSystemConnectivity();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("系统连通性测试失败: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "系统连通性测试失败: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/models/{modelId}/test-connectivity")
    public ResponseEntity<Map<String, Object>> testModelConnectivity(@PathVariable Long modelId) {
        logger.debug("测试模型{}连通性", modelId);
        
        try {
            Map<String, Object> result = answerGenerationService.testModelConnectivity(modelId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("模型{}连通性测试失败: {}", modelId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("modelId", modelId);
            errorResponse.put("error", "模型连通性测试失败: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/batches/{batchId}/test-connectivity")
    public ResponseEntity<Map<String, Object>> testBatchModelsConnectivity(@PathVariable Long batchId) {
        logger.debug("测试批次{}关联的所有模型连通性", batchId);
        
        try {
            Map<String, Object> result = answerGenerationService.testBatchModelsConnectivity(batchId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("批次{}模型连通性测试失败: {}", batchId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("batchId", batchId);
            errorResponse.put("error", "批次模型连通性测试失败: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
} 
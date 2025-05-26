package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    
    @Autowired
    public AnswerGenerationController(AnswerGenerationService answerGenerationService, 
                                      BatchStateManager batchStateManager) {
        this.answerGenerationService = answerGenerationService;
        this.batchStateManager = batchStateManager;
    }
    
    @PostMapping("/batches")
    public ResponseEntity<AnswerGenerationBatchDTO> createBatch(@Valid @RequestBody AnswerGenerationBatchCreateRequest request) {
        logger.debug("创建回答生成批次: {}", request.getName());
        AnswerGenerationBatchDTO batch = answerGenerationService.createBatch(request);
        return ResponseEntity.ok(batch);
    }
    
    @PostMapping("/batches/{batchId}/start")
    public ResponseEntity<String> startBatch(@PathVariable Long batchId) {
        logger.debug("启动回答生成批次: {}", batchId);
        answerGenerationService.startBatch(batchId);
        return ResponseEntity.ok("批次启动成功");
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

        // 验证批次状态
        String currentStatus = batchStateManager.getBatchState(batchId);
        if (!"PAUSED".equals(currentStatus)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("batchId", batchId);
            response.put("status", currentStatus);
            response.put("message", "只能恢复PAUSED状态的批次，当前状态: " + currentStatus);
            return ResponseEntity.badRequest().body(response);
        }
        
        // 立即启动一个线程执行任务，不等待状态更新
        new Thread(() -> {
            try {
                logger.info("启动独立线程恢复批次{}的处理", batchId);
                
                // 更新状态为RESUMING，但不等待它完成
                batchStateManager.setBatchState(batchId, "RESUMING");
                
                // 清除中断标志
                batchStateManager.setInterruptFlag(batchId, false);
                
                // 直接调用服务恢复批次
                answerGenerationService.forceBatchResume(batchId);
                
                logger.info("批次{}恢复线程启动完成", batchId);
            } catch (Exception e) {
                logger.error("批次{}恢复启动失败: {}", batchId, e.getMessage(), e);
                
                // 发送错误通知
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("batchId", batchId);
                errorData.put("error", "恢复启动失败: " + e.getMessage());
                errorData.put("timestamp", System.currentTimeMillis());
                
                try {
                    answerGenerationService.sendErrorNotification(batchId, errorData);
                } catch (Exception ex) {
                    logger.error("发送错误通知失败", ex);
                }
            }
        }).start();
        
        // 并行开始数据库状态更新流程，但不阻塞响应
        boolean stateUpdateSuccess = batchStateManager.resumeBatch(batchId);
        
        // 立即返回响应，不等待任务执行结果
        Map<String, Object> response = new HashMap<>();
        response.put("success", true); // 任务已启动，所以设为true
        response.put("batchId", batchId);
        response.put("status", "RESUMING");  // 直接返回目标状态
        response.put("message", "批次恢复请求已接收并开始处理，任务已启动");
        
        return ResponseEntity.ok(response);
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
} 
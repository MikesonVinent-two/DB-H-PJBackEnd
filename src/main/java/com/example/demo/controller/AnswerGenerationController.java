package com.example.demo.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.AnswerGenerationBatchDTO;
import com.example.demo.dto.ModelAnswerRunDTO;
import com.example.demo.service.AnswerGenerationService;
import com.example.demo.service.AnswerGenerationService.AnswerGenerationBatchCreateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/answer-generation")
@CrossOrigin(origins = "*")
public class AnswerGenerationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AnswerGenerationController.class);
    
    private final AnswerGenerationService answerGenerationService;
    
    @Autowired
    public AnswerGenerationController(AnswerGenerationService answerGenerationService) {
        this.answerGenerationService = answerGenerationService;
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
    public ResponseEntity<String> pauseBatch(@PathVariable Long batchId, @RequestParam String reason) {
        logger.debug("暂停回答生成批次: {}, 原因: {}", batchId, reason);
        answerGenerationService.pauseBatch(batchId, reason);
        return ResponseEntity.ok("批次已暂停");
    }
    
    @PostMapping("/batches/{batchId}/resume")
    public ResponseEntity<String> resumeBatch(@PathVariable Long batchId) {
        logger.debug("恢复回答生成批次: {}", batchId);
        answerGenerationService.resumeBatch(batchId);
        return ResponseEntity.ok("批次已恢复");
    }
    
    @GetMapping("/batches/{batchId}")
    public ResponseEntity<AnswerGenerationBatchDTO> getBatchStatus(@PathVariable Long batchId) {
        logger.debug("获取回答生成批次状态: {}", batchId);
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
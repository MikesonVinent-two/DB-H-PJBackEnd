package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 创建评测运行请求DTO
 */
public class CreateEvaluationRunRequest {
    
    @NotNull(message = "模型回答运行ID不能为空")
    private Long modelAnswerRunId;
    
    @NotNull(message = "评测者ID不能为空")
    private Long evaluatorId;
    
    @NotNull(message = "运行名称不能为空")
    private String runName;
    
    private String runDescription;
    
    private String parameters;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    // Getters and Setters
    public Long getModelAnswerRunId() {
        return modelAnswerRunId;
    }
    
    public void setModelAnswerRunId(Long modelAnswerRunId) {
        this.modelAnswerRunId = modelAnswerRunId;
    }
    
    public Long getEvaluatorId() {
        return evaluatorId;
    }
    
    public void setEvaluatorId(Long evaluatorId) {
        this.evaluatorId = evaluatorId;
    }
    
    public String getRunName() {
        return runName;
    }
    
    public void setRunName(String runName) {
        this.runName = runName;
    }
    
    public String getRunDescription() {
        return runDescription;
    }
    
    public void setRunDescription(String runDescription) {
        this.runDescription = runDescription;
    }
    
    public String getParameters() {
        return parameters;
    }
    
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
} 
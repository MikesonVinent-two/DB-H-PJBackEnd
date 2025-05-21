package com.example.demo.dto;

import com.example.demo.entity.DifficultyLevel;
import com.example.demo.entity.QuestionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StandardQuestionDTO {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    private Long originalRawQuestionId;
    
    @NotBlank(message = "问题文本不能为空")
    private String questionText;
    
    @NotNull(message = "问题类型不能为空")
    private QuestionType questionType;
    
    private DifficultyLevel difficulty;
    
    private Long parentStandardQuestionId;
    
    private String commitMessage;
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getOriginalRawQuestionId() {
        return originalRawQuestionId;
    }
    
    public void setOriginalRawQuestionId(Long originalRawQuestionId) {
        this.originalRawQuestionId = originalRawQuestionId;
    }
    
    public String getQuestionText() {
        return questionText;
    }
    
    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }
    
    public QuestionType getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }
    
    public DifficultyLevel getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }
    
    public Long getParentStandardQuestionId() {
        return parentStandardQuestionId;
    }
    
    public void setParentStandardQuestionId(Long parentStandardQuestionId) {
        this.parentStandardQuestionId = parentStandardQuestionId;
    }
    
    public String getCommitMessage() {
        return commitMessage;
    }
    
    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
} 
package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "evaluation_subjective_prompts")
public class EvaluationSubjectivePrompt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "prompt_template", nullable = false, columnDefinition = "TEXT")
    private String promptTemplate;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "evaluation_criteria_focus", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> evaluationCriteriaFocus;
    
    @Column(name = "scoring_instruction", columnDefinition = "TEXT")
    private String scoringInstruction;
    
    @Column(name = "output_format_instruction", columnDefinition = "TEXT")
    private String outputFormatInstruction;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column
    private String version;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @ManyToOne
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;
    
    @ManyToOne
    @JoinColumn(name = "parent_prompt_id")
    private EvaluationSubjectivePrompt parentPrompt;
    
    @ManyToOne
    @JoinColumn(name = "created_change_log_id")
    private ChangeLog createdChangeLog;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getEvaluationCriteriaFocus() {
        return evaluationCriteriaFocus;
    }

    public void setEvaluationCriteriaFocus(Map<String, Object> evaluationCriteriaFocus) {
        this.evaluationCriteriaFocus = evaluationCriteriaFocus;
    }

    public String getScoringInstruction() {
        return scoringInstruction;
    }

    public void setScoringInstruction(String scoringInstruction) {
        this.scoringInstruction = scoringInstruction;
    }

    public String getOutputFormatInstruction() {
        return outputFormatInstruction;
    }

    public void setOutputFormatInstruction(String outputFormatInstruction) {
        this.outputFormatInstruction = outputFormatInstruction;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public EvaluationSubjectivePrompt getParentPrompt() {
        return parentPrompt;
    }

    public void setParentPrompt(EvaluationSubjectivePrompt parentPrompt) {
        this.parentPrompt = parentPrompt;
    }

    public ChangeLog getCreatedChangeLog() {
        return createdChangeLog;
    }

    public void setCreatedChangeLog(ChangeLog createdChangeLog) {
        this.createdChangeLog = createdChangeLog;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
} 
package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 评测者实体类，包括人类评测者和AI评测者
 */
@Entity
@Table(name = "evaluators")
public class Evaluator {
    
    /**
     * 评测者类型枚举
     */
    public enum EvaluatorType {
        HUMAN,    // 人类评测者
        AI_MODEL  // AI模型评测者
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;

    @Column(name = "evaluator_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EvaluatorType evaluatorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 如果是人类评测员，关联到用户表

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "llm_model_id")
    private LlmModel llmModel; // 如果是AI评测员，关联到模型表

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "expertise_areas", columnDefinition = "TEXT")
    private String expertiseAreas;
    
    @Column(name = "api_url")
    private String apiUrl;
    
    @Column(name = "api_key")
    private String apiKey;
    
    @Column(name = "api_type")
    private String apiType; // openai, azure_openai, openai_compatible
    
    @Column(name = "model_name")
    private String modelName;
    
    @Column(name = "model_parameters", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> modelParameters;
    
    @Column(name = "evaluation_prompt_template", columnDefinition = "TEXT")
    private String evaluationPromptTemplate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

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

    public EvaluatorType getEvaluatorType() {
        return evaluatorType;
    }

    public void setEvaluatorType(EvaluatorType evaluatorType) {
        this.evaluatorType = evaluatorType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LlmModel getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(LlmModel llmModel) {
        this.llmModel = llmModel;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExpertiseAreas() {
        return expertiseAreas;
    }

    public void setExpertiseAreas(String expertiseAreas) {
        this.expertiseAreas = expertiseAreas;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiType() {
        return apiType;
    }

    public void setApiType(String apiType) {
        this.apiType = apiType;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Map<String, Object> getModelParameters() {
        return modelParameters;
    }

    public void setModelParameters(Map<String, Object> modelParameters) {
        this.modelParameters = modelParameters;
    }

    public String getEvaluationPromptTemplate() {
        return evaluationPromptTemplate;
    }

    public void setEvaluationPromptTemplate(String evaluationPromptTemplate) {
        this.evaluationPromptTemplate = evaluationPromptTemplate;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
    
    /**
     * 判断是否为AI评测者
     * 
     * @return 是否为AI评测者
     */
    public boolean isAiEvaluator() {
        return evaluatorType == EvaluatorType.AI_MODEL;
    }
    
    /**
     * 判断是否为人类评测者
     * 
     * @return 是否为人类评测者
     */
    public boolean isHumanEvaluator() {
        return evaluatorType == EvaluatorType.HUMAN;
    }
}
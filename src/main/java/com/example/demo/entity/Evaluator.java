package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluators")
public class Evaluator {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluator_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EvaluatorType evaluatorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "llm_model_id")
    private LLMModel llmModel;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_change_log_id")
    private ChangeLog createdChangeLog;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 评测员类型枚举
    public enum EvaluatorType {
        HUMAN,      // 人类评测员
        AI_MODEL    // AI模型评测员
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LLMModel getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(LLMModel llmModel) {
        this.llmModel = llmModel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
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
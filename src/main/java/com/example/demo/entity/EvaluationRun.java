package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_runs")
public class EvaluationRun {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "llm_model_id", nullable = false)
    private LlmModel llmModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_version_id", nullable = false)
    private DatasetVersion datasetVersion;

    @Column(name = "run_time", nullable = false)
    private LocalDateTime runTime = LocalDateTime.now();

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RunStatus status = RunStatus.PENDING;

    @Column(name = "run_description", columnDefinition = "TEXT")
    private String runDescription;

    @Column(columnDefinition = "json")
    private String parameters;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    // 运行状态枚举
    public enum RunStatus {
        PENDING,                    // 等待中
        GENERATING_ANSWERS,         // 生成答案中
        ANSWER_GENERATION_FAILED,   // 答案生成失败
        READY_FOR_EVALUATION,       // 准备评测
        EVALUATING,                 // 评测中
        COMPLETED,                  // 已完成
        FAILED                      // 失败
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LlmModel getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(LlmModel llmModel) {
        this.llmModel = llmModel;
    }

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public LocalDateTime getRunTime() {
        return runTime;
    }

    public void setRunTime(LocalDateTime runTime) {
        this.runTime = runTime;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }
} 
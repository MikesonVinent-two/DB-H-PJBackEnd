package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "answer_generation_batches")
public class AnswerGenerationBatch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "dataset_version_id", nullable = false)
    private DatasetVersion datasetVersion;
    
    @Column(name = "creation_time", nullable = false)
    private LocalDateTime creationTime = LocalDateTime.now();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status = BatchStatus.PENDING;
    
    @ManyToOne
    @JoinColumn(name = "answer_assembly_config_id")
    private AnswerPromptAssemblyConfig answerAssemblyConfig;
    
    @ManyToOne
    @JoinColumn(name = "evaluation_assembly_config_id")
    private EvaluationPromptAssemblyConfig evaluationAssemblyConfig;
    
    @Column(name = "global_parameters", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> globalParameters;
    
    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "last_processed_question_id")
    private Long lastProcessedQuestionId;
    
    @Column(name = "last_processed_run_id")
    private Long lastProcessedRunId;
    
    @Column(name = "progress_percentage", precision = 5, scale = 2)
    private BigDecimal progressPercentage;
    
    @Column(name = "last_activity_time")
    private LocalDateTime lastActivityTime;
    
    @Column(name = "checkpoint_data", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> checkpointData;
    
    @Column(name = "resume_count", nullable = false)
    private Integer resumeCount = 0;
    
    @Column(name = "pause_time")
    private LocalDateTime pauseTime;
    
    @Column(name = "pause_reason")
    private String pauseReason;
    
    @Column(name = "answer_repeat_count", nullable = false)
    private Integer answerRepeatCount = 1;
    
    public enum BatchStatus {
        PENDING,        // 等待中
        IN_PROGRESS,    // 进行中
        COMPLETED,      // 已完成
        FAILED,         // 失败
        PAUSED,         // 已暂停
        RESUMING        // 正在恢复
    }
    
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public AnswerPromptAssemblyConfig getAnswerAssemblyConfig() {
        return answerAssemblyConfig;
    }

    public void setAnswerAssemblyConfig(AnswerPromptAssemblyConfig answerAssemblyConfig) {
        this.answerAssemblyConfig = answerAssemblyConfig;
    }

    public EvaluationPromptAssemblyConfig getEvaluationAssemblyConfig() {
        return evaluationAssemblyConfig;
    }

    public void setEvaluationAssemblyConfig(EvaluationPromptAssemblyConfig evaluationAssemblyConfig) {
        this.evaluationAssemblyConfig = evaluationAssemblyConfig;
    }

    public Map<String, Object> getGlobalParameters() {
        return globalParameters;
    }

    public void setGlobalParameters(Map<String, Object> globalParameters) {
        this.globalParameters = globalParameters;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getLastProcessedQuestionId() {
        return lastProcessedQuestionId;
    }

    public void setLastProcessedQuestionId(Long lastProcessedQuestionId) {
        this.lastProcessedQuestionId = lastProcessedQuestionId;
    }

    public Long getLastProcessedRunId() {
        return lastProcessedRunId;
    }

    public void setLastProcessedRunId(Long lastProcessedRunId) {
        this.lastProcessedRunId = lastProcessedRunId;
    }

    public BigDecimal getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(BigDecimal progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(LocalDateTime lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public Map<String, Object> getCheckpointData() {
        return checkpointData;
    }

    public void setCheckpointData(Map<String, Object> checkpointData) {
        this.checkpointData = checkpointData;
    }

    public Integer getResumeCount() {
        return resumeCount;
    }

    public void setResumeCount(Integer resumeCount) {
        this.resumeCount = resumeCount;
    }

    public LocalDateTime getPauseTime() {
        return pauseTime;
    }

    public void setPauseTime(LocalDateTime pauseTime) {
        this.pauseTime = pauseTime;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public void setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
    }

    public Integer getAnswerRepeatCount() {
        return answerRepeatCount;
    }

    public void setAnswerRepeatCount(Integer answerRepeatCount) {
        this.answerRepeatCount = answerRepeatCount;
    }
} 
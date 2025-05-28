package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "EVALUATION_RUNS")
public class EvaluationRun {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "MODEL_ANSWER_RUN_ID", nullable = false)
    private Long modelAnswerRunId;

    @Column(name = "EVALUATOR_ID", nullable = false)
    private Long evaluatorId;

    @Column(name = "RUN_NAME", nullable = false)
    private String runName;

    @Column(name = "RUN_DESCRIPTION")
    private String runDescription;

    @Column(name = "RUN_TIME", nullable = false)
    private LocalDateTime runTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private RunStatus status;

    @Column(name = "PARAMETERS", columnDefinition = "json")
    private String parameters;

    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @Column(name = "CREATED_BY_USER_ID")
    private Long createdByUserId;

    @Column(name = "LAST_PROCESSED_ANSWER_ID")
    private Long lastProcessedAnswerId;

    @Column(name = "PROGRESS_PERCENTAGE", precision = 5, scale = 2)
    private BigDecimal progressPercentage;

    @Column(name = "LAST_ACTIVITY_TIME")
    private LocalDateTime lastActivityTime;

    @Column(name = "COMPLETED_ANSWERS_COUNT", nullable = false)
    private Integer completedAnswersCount = 0;

    @Column(name = "TOTAL_ANSWERS_COUNT")
    private Integer totalAnswersCount;

    @Column(name = "FAILED_EVALUATIONS_COUNT", nullable = false)
    private Integer failedEvaluationsCount = 0;

    @Column(name = "RESUME_COUNT", nullable = false)
    private Integer resumeCount = 0;

    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;

    @Column(name = "LAST_CHECKPOINT_ID")
    private Long lastCheckpointId;

    @Column(name = "PAUSE_REASON")
    private String pauseReason;

    @Column(name = "PAUSE_TIME")
    private LocalDateTime pauseTime;

    @Column(name = "PAUSED_BY_USER_ID")
    private Long pausedByUserId;

    @Column(name = "TIMEOUT_SECONDS")
    private Integer timeoutSeconds = 3600;

    @Column(name = "IS_AUTO_RESUME", nullable = false)
    private Boolean isAutoResume = false;

    @Column(name = "AUTO_CHECKPOINT_INTERVAL")
    private Integer autoCheckpointInterval = 60;

    @Column(name = "CURRENT_BATCH_START_ID")
    private Long currentBatchStartId;

    @Column(name = "CURRENT_BATCH_END_ID")
    private Long currentBatchEndId;

    @Column(name = "BATCH_SIZE")
    private Integer batchSize = 50;

    @Column(name = "RETRY_COUNT", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "MAX_RETRIES")
    private Integer maxRetries = 3;

    @Column(name = "LAST_ERROR_TIME")
    private LocalDateTime lastErrorTime;

    @Column(name = "CONSECUTIVE_ERRORS", nullable = false)
    private Integer consecutiveErrors = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MODEL_ANSWER_RUN_ID", insertable = false, updatable = false)
    private ModelAnswerRun modelAnswerRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EVALUATOR_ID", insertable = false, updatable = false)
    private Evaluator evaluator;

    @Column(name = "START_TIME")
    private LocalDateTime startTime;

    @Column(name = "END_TIME")
    private LocalDateTime endTime;

    @Column(name = "LAST_UPDATED")
    private LocalDateTime lastUpdated;

    // 运行状态枚举
    public enum RunStatus {
        PENDING,      // 等待中
        IN_PROGRESS,  // 进行中
        COMPLETED,    // 已完成
        FAILED,       // 失败
        PAUSED,       // 暂停
        RESUMING      // 恢复中
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Long getLastProcessedAnswerId() {
        return lastProcessedAnswerId;
    }

    public void setLastProcessedAnswerId(Long lastProcessedAnswerId) {
        this.lastProcessedAnswerId = lastProcessedAnswerId;
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

    public Integer getCompletedAnswersCount() {
        return completedAnswersCount;
    }

    public void setCompletedAnswersCount(Integer completedAnswersCount) {
        this.completedAnswersCount = completedAnswersCount;
    }

    public Integer getTotalAnswersCount() {
        return totalAnswersCount;
    }

    public void setTotalAnswersCount(Integer totalAnswersCount) {
        this.totalAnswersCount = totalAnswersCount;
    }

    public Integer getFailedEvaluationsCount() {
        return failedEvaluationsCount;
    }

    public void setFailedEvaluationsCount(Integer failedEvaluationsCount) {
        this.failedEvaluationsCount = failedEvaluationsCount;
    }

    public Integer getResumeCount() {
        return resumeCount;
    }

    public void setResumeCount(Integer resumeCount) {
        this.resumeCount = resumeCount;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getLastCheckpointId() {
        return lastCheckpointId;
    }

    public void setLastCheckpointId(Long lastCheckpointId) {
        this.lastCheckpointId = lastCheckpointId;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public void setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
    }

    public LocalDateTime getPauseTime() {
        return pauseTime;
    }

    public void setPauseTime(LocalDateTime pauseTime) {
        this.pauseTime = pauseTime;
    }

    public Long getPausedByUserId() {
        return pausedByUserId;
    }

    public void setPausedByUserId(Long pausedByUserId) {
        this.pausedByUserId = pausedByUserId;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Boolean getIsAutoResume() {
        return isAutoResume;
    }

    public void setIsAutoResume(Boolean isAutoResume) {
        this.isAutoResume = isAutoResume;
    }

    public Integer getAutoCheckpointInterval() {
        return autoCheckpointInterval;
    }

    public void setAutoCheckpointInterval(Integer autoCheckpointInterval) {
        this.autoCheckpointInterval = autoCheckpointInterval;
    }

    public Long getCurrentBatchStartId() {
        return currentBatchStartId;
    }

    public void setCurrentBatchStartId(Long currentBatchStartId) {
        this.currentBatchStartId = currentBatchStartId;
    }

    public Long getCurrentBatchEndId() {
        return currentBatchEndId;
    }

    public void setCurrentBatchEndId(Long currentBatchEndId) {
        this.currentBatchEndId = currentBatchEndId;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public LocalDateTime getLastErrorTime() {
        return lastErrorTime;
    }

    public void setLastErrorTime(LocalDateTime lastErrorTime) {
        this.lastErrorTime = lastErrorTime;
    }

    public Integer getConsecutiveErrors() {
        return consecutiveErrors;
    }

    public void setConsecutiveErrors(Integer consecutiveErrors) {
        this.consecutiveErrors = consecutiveErrors;
    }

    public ModelAnswerRun getModelAnswerRun() {
        return modelAnswerRun;
    }

    public void setModelAnswerRun(ModelAnswerRun modelAnswerRun) {
        this.modelAnswerRun = modelAnswerRun;
        if (modelAnswerRun != null) {
            this.modelAnswerRunId = modelAnswerRun.getId();
        }
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
        if (evaluator != null) {
            this.evaluatorId = evaluator.getId();
        }
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getCreatedBy() {
        return createdByUserId;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdByUserId = createdBy;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
} 
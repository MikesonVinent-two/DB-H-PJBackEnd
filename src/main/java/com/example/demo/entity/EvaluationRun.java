package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_runs")
public class EvaluationRun {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_answer_run_id", nullable = false)
    private ModelAnswerRun modelAnswerRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private Evaluator evaluator;

    @Column(name = "run_name", nullable = false)
    private String runName;

    @Column(name = "run_description", columnDefinition = "TEXT")
    private String runDescription;

    @Column(name = "run_time", nullable = false)
    private LocalDateTime runTime = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RunStatus status = RunStatus.PENDING;

    @Column(name = "parameters", columnDefinition = "JSON")
    private String parameters;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(name = "last_processed_answer_id")
    private Long lastProcessedAnswerId;

    @Column(name = "progress_percentage", precision = 5, scale = 2)
    private BigDecimal progressPercentage;

    @Column(name = "last_activity_time")
    private LocalDateTime lastActivityTime;

    @Column(name = "completed_answers_count", nullable = false)
    private Integer completedAnswersCount = 0;

    @Column(name = "total_answers_count")
    private Integer totalAnswersCount;

    @Column(name = "failed_evaluations_count", nullable = false)
    private Integer failedEvaluationsCount = 0;

    @Column(name = "resume_count", nullable = false)
    private Integer resumeCount = 0;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "creation_time")
    private LocalDateTime creationTime;

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

    public ModelAnswerRun getModelAnswerRun() {
        return modelAnswerRun;
    }

    public void setModelAnswerRun(ModelAnswerRun modelAnswerRun) {
        this.modelAnswerRun = modelAnswerRun;
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
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

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
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

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public Long getCreatedBy() {
        return createdByUser != null ? createdByUser.getId() : null;
    }

    public void setCreatedBy(Long userId) {
        if (this.createdByUser == null) {
            this.createdByUser = new User();
        }
        this.createdByUser.setId(userId);
    }
} 
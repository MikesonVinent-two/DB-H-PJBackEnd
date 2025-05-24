package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "model_answer_runs")
public class ModelAnswerRun {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "answer_generation_batch_id", nullable = false)
    private AnswerGenerationBatch answerGenerationBatch;
    
    @ManyToOne
    @JoinColumn(name = "llm_model_id", nullable = false)
    private LlmModel llmModel;
    
    @Column(name = "run_name", nullable = false)
    private String runName;
    
    @Column(name = "run_description")
    private String runDescription;
    
    @Column(name = "run_index", nullable = false)
    private Integer runIndex = 0;
    
    @Column(name = "run_time", nullable = false)
    private LocalDateTime runTime = LocalDateTime.now();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status = RunStatus.PENDING;
    
    @Column(columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> parameters;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;
    
    @Column(name = "last_processed_question_id")
    private Long lastProcessedQuestionId;
    
    @Column(name = "last_processed_question_index")
    private Integer lastProcessedQuestionIndex;
    
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
    
    @Column(name = "completed_questions_count", nullable = false)
    private Integer completedQuestionsCount = 0;
    
    @Column(name = "total_questions_count")
    private Integer totalQuestionsCount;
    
    @Column(name = "failed_questions_count", nullable = false)
    private Integer failedQuestionsCount = 0;
    
    @Column(name = "failed_questions_ids", columnDefinition = "json")
    private Long[] failedQuestionsIds;
    
    @Column(name = "total_questions_count")
    private Integer totalQuestions;
    
    public enum RunStatus {
        PENDING,                  // 等待中
        GENERATING_ANSWERS,       // 生成答案中
        ANSWER_GENERATION_FAILED, // 答案生成失败
        READY_FOR_EVALUATION,     // 准备评测
        EVALUATING,               // 评测中
        COMPLETED,                // 已完成
        FAILED,                   // 失败
        PAUSED,                   // 已暂停
        RESUMING                  // 正在恢复
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AnswerGenerationBatch getAnswerGenerationBatch() {
        return answerGenerationBatch;
    }

    public void setAnswerGenerationBatch(AnswerGenerationBatch answerGenerationBatch) {
        this.answerGenerationBatch = answerGenerationBatch;
    }

    public LlmModel getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(LlmModel llmModel) {
        this.llmModel = llmModel;
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

    public Integer getRunIndex() {
        return runIndex;
    }

    public void setRunIndex(Integer runIndex) {
        this.runIndex = runIndex;
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

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
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

    public Long getLastProcessedQuestionId() {
        return lastProcessedQuestionId;
    }

    public void setLastProcessedQuestionId(Long lastProcessedQuestionId) {
        this.lastProcessedQuestionId = lastProcessedQuestionId;
    }

    public Integer getLastProcessedQuestionIndex() {
        return lastProcessedQuestionIndex;
    }

    public void setLastProcessedQuestionIndex(Integer lastProcessedQuestionIndex) {
        this.lastProcessedQuestionIndex = lastProcessedQuestionIndex;
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

    public Integer getCompletedQuestionsCount() {
        return completedQuestionsCount;
    }

    public void setCompletedQuestionsCount(Integer completedQuestionsCount) {
        this.completedQuestionsCount = completedQuestionsCount;
    }

    public Integer getTotalQuestionsCount() {
        return totalQuestionsCount;
    }

    public void setTotalQuestionsCount(Integer totalQuestionsCount) {
        this.totalQuestionsCount = totalQuestionsCount;
    }

    public Integer getFailedQuestionsCount() {
        return failedQuestionsCount;
    }

    public void setFailedQuestionsCount(Integer failedQuestionsCount) {
        this.failedQuestionsCount = failedQuestionsCount;
    }

    public Long[] getFailedQuestionsIds() {
        return failedQuestionsIds;
    }

    public void setFailedQuestionsIds(Long[] failedQuestionsIds) {
        this.failedQuestionsIds = failedQuestionsIds;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }
} 
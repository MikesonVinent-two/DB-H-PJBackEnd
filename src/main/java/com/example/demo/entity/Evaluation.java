package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluations")
public class Evaluation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "llm_answer_id", nullable = false)
    private LlmAnswer llmAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private Evaluator evaluator;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_run_id")
    private EvaluationRun evaluationRun;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "evaluation_time", nullable = false)
    private LocalDateTime evaluationTime = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", nullable = false)
    private EvaluationStatus evaluationStatus = EvaluationStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "evaluation_results", columnDefinition = "JSON")
    private String evaluationResults;

    @Column(name = "prompt_used", columnDefinition = "TEXT")
    private String promptUsed;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "raw_evaluator_response", columnDefinition = "TEXT")
    private String rawEvaluatorResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_change_log_id")
    private ChangeLog createdChangeLog;

    // 评测状态枚举
    public enum EvaluationStatus {
        SUCCESS,    // 评测成功
        FAILED,     // 评测失败
        PENDING     // 待评测
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LlmAnswer getLlmAnswer() {
        return llmAnswer;
    }

    public void setLlmAnswer(LlmAnswer llmAnswer) {
        this.llmAnswer = llmAnswer;
    }

    public Evaluator getEvaluator() {
        return evaluator;
    }

    public void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }
    
    public EvaluationRun getEvaluationRun() {
        return evaluationRun;
    }

    public void setEvaluationRun(EvaluationRun evaluationRun) {
        this.evaluationRun = evaluationRun;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }

    public LocalDateTime getEvaluationTime() {
        return evaluationTime;
    }

    public void setEvaluationTime(LocalDateTime evaluationTime) {
        this.evaluationTime = evaluationTime;
    }

    public EvaluationStatus getEvaluationStatus() {
        return evaluationStatus;
    }

    public void setEvaluationStatus(EvaluationStatus evaluationStatus) {
        this.evaluationStatus = evaluationStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getEvaluationResults() {
        return evaluationResults;
    }

    public void setEvaluationResults(String evaluationResults) {
        this.evaluationResults = evaluationResults;
    }

    public String getPromptUsed() {
        return promptUsed;
    }

    public void setPromptUsed(String promptUsed) {
        this.promptUsed = promptUsed;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getRawEvaluatorResponse() {
        return rawEvaluatorResponse;
    }

    public void setRawEvaluatorResponse(String rawEvaluatorResponse) {
        this.rawEvaluatorResponse = rawEvaluatorResponse;
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
    
    /**
     * 获取关联的问题
     * 
     * @return 标准问题实体
     */
    public StandardQuestion getQuestion() {
        if (llmAnswer != null && llmAnswer.getDatasetQuestionMapping() != null) {
            return llmAnswer.getDatasetQuestionMapping().getStandardQuestion();
        }
        return null;
    }
    
    /**
     * 获取回答文本
     * 
     * @return 回答文本
     */
    public String getAnswerText() {
        if (llmAnswer != null) {
            return llmAnswer.getAnswerText();
        }
        return null;
    }
} 
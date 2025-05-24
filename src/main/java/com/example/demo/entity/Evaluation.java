package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "evaluations")
public class Evaluation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "llm_answer_id", nullable = false)
    private LlmAnswer llmAnswer;

    @ManyToOne
    @JoinColumn(name = "evaluator_id", nullable = false)
    private Evaluator evaluator;
    
    @ManyToOne
    @JoinColumn(name = "evaluation_run_id")
    private EvaluationRun evaluationRun;

    @Column(name = "overall_score")
    private BigDecimal score;

    @Column(name = "evaluation_time", nullable = false)
    private LocalDateTime evaluationTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", nullable = false)
    private EvaluationStatus status = EvaluationStatus.PENDING;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "evaluation_results", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> evaluationResults;

    @Column(name = "prompt_used")
    private String promptUsed;

    @Column(name = "comments")
    private String comments;

    @Column(name = "raw_evaluator_response")
    private String rawEvaluatorResponse;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_change_log_id")
    private ChangeLog createdChangeLog;

    @Column(name = "creation_time")
    private LocalDateTime creationTime;

    @Column(name = "completion_time")
    private LocalDateTime completionTime;

    // 评测状态枚举
    public enum EvaluationStatus {
        SUCCESS,        // 评测成功
        FAILED,         // 评测失败
        PENDING        // 待评测
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

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public LocalDateTime getEvaluationTime() {
        return evaluationTime;
    }

    public void setEvaluationTime(LocalDateTime evaluationTime) {
        this.evaluationTime = evaluationTime;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public void setStatus(EvaluationStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Map<String, Object> getEvaluationResults() {
        return evaluationResults;
    }

    public void setEvaluationResults(Map<String, Object> evaluationResults) {
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
    
    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public LocalDateTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(LocalDateTime completionTime) {
        this.completionTime = completionTime;
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
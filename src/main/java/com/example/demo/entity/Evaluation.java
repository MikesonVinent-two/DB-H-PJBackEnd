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

    @Column(name = "evaluation_time", nullable = false)
    private LocalDateTime evaluationTime;

    @Column(name = "overall_score", precision = 10, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EvaluationStatus status = EvaluationStatus.COMPLETED;

    @Column(name = "raw_evaluator_output", columnDefinition = "json")
    private String rawEvaluatorOutput;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_prompt_id_used")
    private AiEvaluationPrompt aiPromptUsed;

    @Column(name = "prompt_text_rendered", columnDefinition = "TEXT")
    private String promptTextRendered;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    // 评测状态枚举
    public enum EvaluationStatus {
        PENDING,        // 待评测
        IN_PROGRESS,    // 评测中
        COMPLETED,      // 已完成
        FAILED,         // 失败
        NEEDS_REVIEW    // 需要复核
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

    public LocalDateTime getEvaluationTime() {
        return evaluationTime;
    }

    public void setEvaluationTime(LocalDateTime evaluationTime) {
        this.evaluationTime = evaluationTime;
    }

    public BigDecimal getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }

    public String getFeedbackText() {
        return feedbackText;
    }

    public void setFeedbackText(String feedbackText) {
        this.feedbackText = feedbackText;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public void setStatus(EvaluationStatus status) {
        this.status = status;
    }

    public String getRawEvaluatorOutput() {
        return rawEvaluatorOutput;
    }

    public void setRawEvaluatorOutput(String rawEvaluatorOutput) {
        this.rawEvaluatorOutput = rawEvaluatorOutput;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public AiEvaluationPrompt getAiPromptUsed() {
        return aiPromptUsed;
    }

    public void setAiPromptUsed(AiEvaluationPrompt aiPromptUsed) {
        this.aiPromptUsed = aiPromptUsed;
    }

    public String getPromptTextRendered() {
        return promptTextRendered;
    }

    public void setPromptTextRendered(String promptTextRendered) {
        this.promptTextRendered = promptTextRendered;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }
} 
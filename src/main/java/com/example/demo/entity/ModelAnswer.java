package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 模型回答实体类
 */
@Entity
@Table(name = "model_answers")
public class ModelAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "model_answer_run_id", nullable = false)
    private ModelAnswerRun modelAnswerRun;
    
    @ManyToOne
    @JoinColumn(name = "standard_question_id", nullable = false)
    private StandardQuestion standardQuestion;
    
    @Column(name = "answer_text", columnDefinition = "text", nullable = false)
    private String answerText;
    
    @Column(name = "repeat_index", nullable = false)
    private Integer repeatIndex;
    
    @Column(name = "generation_time", nullable = false)
    private LocalDateTime generationTime;
    
    @Column(name = "tokens_used")
    private Integer tokensUsed;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(name = "evaluation_score")
    private Double evaluationScore;
    
    @Column(name = "evaluation_details", columnDefinition = "json")
    private String evaluationDetails;
    
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

    public StandardQuestion getStandardQuestion() {
        return standardQuestion;
    }

    public void setStandardQuestion(StandardQuestion standardQuestion) {
        this.standardQuestion = standardQuestion;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public Integer getRepeatIndex() {
        return repeatIndex;
    }

    public void setRepeatIndex(Integer repeatIndex) {
        this.repeatIndex = repeatIndex;
    }

    public LocalDateTime getGenerationTime() {
        return generationTime;
    }

    public void setGenerationTime(LocalDateTime generationTime) {
        this.generationTime = generationTime;
    }

    public Integer getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(Integer tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Double getEvaluationScore() {
        return evaluationScore;
    }

    public void setEvaluationScore(Double evaluationScore) {
        this.evaluationScore = evaluationScore;
    }

    public String getEvaluationDetails() {
        return evaluationDetails;
    }

    public void setEvaluationDetails(String evaluationDetails) {
        this.evaluationDetails = evaluationDetails;
    }
} 
package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

/**
 * 回答分数实体类
 */
@Entity
@Table(name = "ANSWER_SCORES")
@NoArgsConstructor
public class AnswerScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LLM_ANSWER_ID", nullable = false)
    private LlmAnswer llmAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EVALUATOR_ID", nullable = false)
    private Evaluator evaluator;

    @Column(name = "RAW_SCORE", nullable = false, precision = 10, scale = 2)
    private BigDecimal rawScore;

    @Column(name = "NORMALIZED_SCORE", precision = 10, scale = 2)
    private BigDecimal normalizedScore;

    @Column(name = "WEIGHTED_SCORE", precision = 10, scale = 2)
    private BigDecimal weightedScore;

    @Column(name = "SCORE_TYPE", nullable = false, length = 50)
    private String scoreType;

    @Column(name = "SCORING_METHOD", length = 100)
    private String scoringMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EVALUATION_ID")
    private Evaluation evaluation;

    @Column(name = "SCORING_TIME", nullable = false)
    private LocalDateTime scoringTime = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREATED_BY_USER_ID")
    private User createdByUser;

    @Column(name = "COMMENTS")
    private String comments;
    
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

    public BigDecimal getRawScore() {
        return rawScore;
    }

    public void setRawScore(BigDecimal rawScore) {
        this.rawScore = rawScore;
    }

    public BigDecimal getNormalizedScore() {
        return normalizedScore;
    }

    public void setNormalizedScore(BigDecimal normalizedScore) {
        this.normalizedScore = normalizedScore;
    }

    public BigDecimal getWeightedScore() {
        return weightedScore;
    }

    public void setWeightedScore(BigDecimal weightedScore) {
        this.weightedScore = weightedScore;
    }

    public String getScoreType() {
        return scoreType;
    }

    public void setScoreType(String scoreType) {
        this.scoreType = scoreType;
    }

    public String getScoringMethod() {
        return scoringMethod;
    }

    public void setScoringMethod(String scoringMethod) {
        this.scoringMethod = scoringMethod;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public LocalDateTime getScoringTime() {
        return scoringTime;
    }

    public void setScoringTime(LocalDateTime scoringTime) {
        this.scoringTime = scoringTime;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
} 
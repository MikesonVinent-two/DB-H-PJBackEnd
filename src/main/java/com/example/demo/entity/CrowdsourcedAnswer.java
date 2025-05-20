package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "crowdsourced_answers")
public class CrowdsourcedAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_question_id", nullable = false)
    private StandardQuestion standardQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "submission_time", nullable = false)
    private LocalDateTime submissionTime = LocalDateTime.now();

    @Column(name = "task_batch_id")
    private Integer taskBatchId;

    @Column(name = "quality_review_status")
    @Enumerated(EnumType.STRING)
    private QualityReviewStatus qualityReviewStatus = QualityReviewStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedByUser;

    @Column(name = "review_time")
    private LocalDateTime reviewTime;

    @Column(name = "review_feedback")
    private String reviewFeedback;

    @Column(name = "other_metadata", columnDefinition = "json")
    private String otherMetadata;

    // 质量审核状态枚举
    public enum QualityReviewStatus {
        PENDING,    // 待审核
        ACCEPTED,   // 已接受
        REJECTED,   // 已拒绝
        FLAGGED     // 已标记
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StandardQuestion getStandardQuestion() {
        return standardQuestion;
    }

    public void setStandardQuestion(StandardQuestion standardQuestion) {
        this.standardQuestion = standardQuestion;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public LocalDateTime getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(LocalDateTime submissionTime) {
        this.submissionTime = submissionTime;
    }

    public Integer getTaskBatchId() {
        return taskBatchId;
    }

    public void setTaskBatchId(Integer taskBatchId) {
        this.taskBatchId = taskBatchId;
    }

    public QualityReviewStatus getQualityReviewStatus() {
        return qualityReviewStatus;
    }

    public void setQualityReviewStatus(QualityReviewStatus qualityReviewStatus) {
        this.qualityReviewStatus = qualityReviewStatus;
    }

    public User getReviewedByUser() {
        return reviewedByUser;
    }

    public void setReviewedByUser(User reviewedByUser) {
        this.reviewedByUser = reviewedByUser;
    }

    public LocalDateTime getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(LocalDateTime reviewTime) {
        this.reviewTime = reviewTime;
    }

    public String getReviewFeedback() {
        return reviewFeedback;
    }

    public void setReviewFeedback(String reviewFeedback) {
        this.reviewFeedback = reviewFeedback;
    }

    public String getOtherMetadata() {
        return otherMetadata;
    }

    public void setOtherMetadata(String otherMetadata) {
        this.otherMetadata = otherMetadata;
    }
} 
package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "expert_candidate_answers")
public class ExpertCandidateAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_question_id", nullable = false)
    private StandardQuestion standardQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "candidate_answer_text", nullable = false, columnDefinition = "TEXT")
    private String candidateAnswerText;

    @Column(name = "submission_time", nullable = false)
    private LocalDateTime submissionTime = LocalDateTime.now();

    @Column(name = "quality_score")
    private Integer qualityScore;

    private String feedback;

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

    public String getCandidateAnswerText() {
        return candidateAnswerText;
    }

    public void setCandidateAnswerText(String candidateAnswerText) {
        this.candidateAnswerText = candidateAnswerText;
    }

    public LocalDateTime getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(LocalDateTime submissionTime) {
        this.submissionTime = submissionTime;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
} 
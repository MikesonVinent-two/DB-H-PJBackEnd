package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "standard_subjective_answers")
public class StandardSubjectiveAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_question_id", nullable = false, unique = true)
    private StandardQuestion standardQuestion;

    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "scoring_guidance", columnDefinition = "TEXT")
    private String scoringGuidance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "determined_by_user_id", nullable = false)
    private User determinedByUser;

    @Column(name = "determined_time", nullable = false)
    private LocalDateTime determinedTime = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_change_log_id")
    private ChangeLog createdChangeLog;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public String getScoringGuidance() {
        return scoringGuidance;
    }

    public void setScoringGuidance(String scoringGuidance) {
        this.scoringGuidance = scoringGuidance;
    }

    public User getDeterminedByUser() {
        return determinedByUser;
    }

    public void setDeterminedByUser(User determinedByUser) {
        this.determinedByUser = determinedByUser;
    }

    public LocalDateTime getDeterminedTime() {
        return determinedTime;
    }

    public void setDeterminedTime(LocalDateTime determinedTime) {
        this.determinedTime = determinedTime;
    }

    public ChangeLog getCreatedChangeLog() {
        return createdChangeLog;
    }

    public void setCreatedChangeLog(ChangeLog createdChangeLog) {
        this.createdChangeLog = createdChangeLog;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
} 
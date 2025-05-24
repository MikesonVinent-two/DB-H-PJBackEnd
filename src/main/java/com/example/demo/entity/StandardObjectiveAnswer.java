package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "standard_objective_answers")
public class StandardObjectiveAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_question_id", nullable = false, unique = true)
    private StandardQuestion standardQuestion;

    @Column(name = "options", nullable = false, columnDefinition = "json")
    private String options;

    @Column(name = "correct_ids", nullable = false, columnDefinition = "json")
    private String correctOptionIds;

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

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getCorrectOptionIds() {
        return correctOptionIds;
    }

    public void setCorrectOptionIds(String correctOptionIds) {
        this.correctOptionIds = correctOptionIds;
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
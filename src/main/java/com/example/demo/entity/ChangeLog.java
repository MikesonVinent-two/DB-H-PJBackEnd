package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "change_log")
public class ChangeLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "change_time", nullable = false)
    private LocalDateTime changeTime = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedByUser;

    @Column(name = "change_type", nullable = false)
    private String changeType;

    @Column(name = "commit_message", columnDefinition = "TEXT")
    private String commitMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "associated_standard_question_id")
    private StandardQuestion associatedStandardQuestion;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(LocalDateTime changeTime) {
        this.changeTime = changeTime;
    }

    public User getChangedByUser() {
        return changedByUser;
    }

    public void setChangedByUser(User changedByUser) {
        this.changedByUser = changedByUser;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public StandardQuestion getAssociatedStandardQuestion() {
        return associatedStandardQuestion;
    }

    public void setAssociatedStandardQuestion(StandardQuestion associatedStandardQuestion) {
        this.associatedStandardQuestion = associatedStandardQuestion;
    }
} 
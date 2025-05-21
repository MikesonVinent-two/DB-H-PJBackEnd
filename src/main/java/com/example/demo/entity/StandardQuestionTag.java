package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "standard_question_tags")
public class StandardQuestionTag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_question_id", nullable = false)
    @JsonBackReference
    private StandardQuestion standardQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_change_log_id")
    private ChangeLog createdChangeLog;

    // 构造函数
    public StandardQuestionTag() {
    }
    
    public StandardQuestionTag(StandardQuestion standardQuestion, Tag tag, User user) {
        this.standardQuestion = standardQuestion;
        this.tag = tag;
        this.createdByUser = user;
        this.createdAt = LocalDateTime.now();
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

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
} 
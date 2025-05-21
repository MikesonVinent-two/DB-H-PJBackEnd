package com.example.demo.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "raw_question_tags",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"raw_question_id", "tag_id"},
        name = "uk_raw_question_tag"
    )
)
public class RawQuestionTag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_question_id", nullable = false)
    @JsonBackReference
    private RawQuestion rawQuestion;
    
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
    public RawQuestionTag() {
    }
    
    public RawQuestionTag(RawQuestion rawQuestion, Tag tag) {
        this.rawQuestion = rawQuestion;
        this.tag = tag;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public RawQuestion getRawQuestion() {
        return rawQuestion;
    }
    
    public void setRawQuestion(RawQuestion rawQuestion) {
        this.rawQuestion = rawQuestion;
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
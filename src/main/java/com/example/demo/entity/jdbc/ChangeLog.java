package com.example.demo.entity.jdbc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 变更日志实体类 - JDBC版本
 * 对应数据库表: change_log
 */
public class ChangeLog {
    // 表名常量
    public static final String TABLE_NAME = "change_log";
    
    // 列名常量
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CHANGE_TYPE = "change_type";
    public static final String COLUMN_CHANGED_BY_USER_ID = "changed_by_user_id";
    public static final String COLUMN_CHANGE_TIME = "change_time";
    public static final String COLUMN_COMMIT_MESSAGE = "commit_message";
    
    private Long id;
    private ChangeType changeType;
    private User changedByUser;
    private LocalDateTime changeTime = LocalDateTime.now();
    private String commitMessage;
    
    // 关联到各种实体的关系
    private StandardQuestion associatedStandardQuestion;
    private StandardObjectiveAnswer associatedObjectiveAnswer;
    private StandardSimpleAnswer associatedSimpleAnswer;
    private StandardSubjectiveAnswer associatedSubjectiveAnswer;
    private List<ChangeLogDetail> details = new ArrayList<>();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public User getChangedByUser() {
        return changedByUser;
    }

    public void setChangedByUser(User changedByUser) {
        this.changedByUser = changedByUser;
    }

    public LocalDateTime getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(LocalDateTime changeTime) {
        this.changeTime = changeTime;
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

    public StandardObjectiveAnswer getAssociatedObjectiveAnswer() {
        return associatedObjectiveAnswer;
    }

    public void setAssociatedObjectiveAnswer(StandardObjectiveAnswer associatedObjectiveAnswer) {
        this.associatedObjectiveAnswer = associatedObjectiveAnswer;
    }

    public StandardSimpleAnswer getAssociatedSimpleAnswer() {
        return associatedSimpleAnswer;
    }

    public void setAssociatedSimpleAnswer(StandardSimpleAnswer associatedSimpleAnswer) {
        this.associatedSimpleAnswer = associatedSimpleAnswer;
    }

    public StandardSubjectiveAnswer getAssociatedSubjectiveAnswer() {
        return associatedSubjectiveAnswer;
    }

    public void setAssociatedSubjectiveAnswer(StandardSubjectiveAnswer associatedSubjectiveAnswer) {
        this.associatedSubjectiveAnswer = associatedSubjectiveAnswer;
    }

    public List<ChangeLogDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ChangeLogDetail> details) {
        this.details = details;
    }

    // 辅助方法
    public void addDetail(ChangeLogDetail detail) {
        details.add(detail);
        detail.setChangeLog(this);
    }

    public void removeDetail(ChangeLogDetail detail) {
        details.remove(detail);
        detail.setChangeLog(null);
    }
} 
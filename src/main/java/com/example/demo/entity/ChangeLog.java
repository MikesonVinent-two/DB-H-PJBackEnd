package com.example.demo.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Convert;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;

@Data
@Entity
@Table(name = "change_log")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class ChangeLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = com.example.demo.converter.ChangeTypeConverter.class)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedByUser;

    @Column(name = "change_time", nullable = false)
    private LocalDateTime changeTime = LocalDateTime.now();

    @Column(name = "commit_message")
    private String commitMessage;

    // 关联到各种实体的关系
    @OneToOne(mappedBy = "createdChangeLog")
    @JsonIgnoreProperties("createdChangeLog")
    private StandardQuestion associatedStandardQuestion;

    @OneToOne(mappedBy = "createdChangeLog")
    @JsonIgnoreProperties("createdChangeLog")
    private StandardObjectiveAnswer associatedObjectiveAnswer;

    @OneToOne(mappedBy = "createdChangeLog")
    @JsonIgnoreProperties("createdChangeLog")
    private StandardSimpleAnswer associatedSimpleAnswer;

    @OneToOne(mappedBy = "createdChangeLog")
    @JsonIgnoreProperties("createdChangeLog")
    private StandardSubjectiveAnswer associatedSubjectiveAnswer;

    @OneToMany(mappedBy = "changeLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("changeLog")
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
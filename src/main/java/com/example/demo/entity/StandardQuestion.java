package com.example.demo.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonManagedReference;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Convert;

@Entity
@Table(name = "standard_questions")
public class StandardQuestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_raw_question_id")
    private RawQuestion originalRawQuestion;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Convert(converter = com.example.demo.converter.QuestionTypeConverter.class)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Convert(converter = com.example.demo.converter.DifficultyLevelConverter.class)
    @Column(name = "difficulty")
    private DifficultyLevel difficulty;

    @Column(name = "creation_time", nullable = false)
    private LocalDateTime creationTime = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_standard_question_id")
    private StandardQuestion parentStandardQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_change_log_id")
    private ChangeLog createdChangeLog;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @OneToMany(mappedBy = "standardQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<StandardQuestionTag> questionTags = new ArrayList<>();

    @OneToMany(mappedBy = "standardQuestion", cascade = CascadeType.ALL)
    private List<DatasetQuestionMapping> datasetMappings = new ArrayList<>();

    // 添加标签关联
    public void addTag(StandardQuestionTag tag) {
        questionTags.add(tag);
        tag.setStandardQuestion(this);
    }
    
    // 移除标签关联
    public void removeTag(StandardQuestionTag tag) {
        questionTags.remove(tag);
        tag.setStandardQuestion(null);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RawQuestion getOriginalRawQuestion() {
        return originalRawQuestion;
    }

    public void setOriginalRawQuestion(RawQuestion originalRawQuestion) {
        this.originalRawQuestion = originalRawQuestion;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public StandardQuestion getParentStandardQuestion() {
        return parentStandardQuestion;
    }

    public void setParentStandardQuestion(StandardQuestion parentStandardQuestion) {
        this.parentStandardQuestion = parentStandardQuestion;
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
    
    public List<StandardQuestionTag> getQuestionTags() {
        return questionTags;
    }

    public void setQuestionTags(List<StandardQuestionTag> questionTags) {
        this.questionTags = questionTags;
    }
    
    /**
     * 获取问题的标签列表
     * @return 标签列表
     */
    public List<Tag> getTags() {
        if (questionTags == null) {
            return new ArrayList<>();
        }
        return questionTags.stream()
                .map(StandardQuestionTag::getTag)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取数据集映射列表
     * @return 数据集映射列表
     */
    public List<DatasetQuestionMapping> getDatasetMappings() {
        return datasetMappings;
    }
    
    public void setDatasetMappings(List<DatasetQuestionMapping> datasetMappings) {
        this.datasetMappings = datasetMappings;
    }
} 
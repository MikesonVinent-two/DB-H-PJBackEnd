package com.example.demo.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;

@Entity
@Table(name = "raw_questions")
public class RawQuestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_url", unique = true, nullable = false)
    private String sourceUrl;

    @Column(name = "source_site")
    private String sourceSite;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "crawl_time", nullable = false)
    private LocalDateTime crawlTime;

    @Column(name = "tags", columnDefinition = "json")
    private String tags;

    @Column(name = "other_metadata", columnDefinition = "json")
    private String otherMetadata;
    
    @OneToMany(mappedBy = "rawQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<RawQuestionTag> questionTags = new ArrayList<>();

    // 构造函数
    public RawQuestion() {
        this.crawlTime = LocalDateTime.now();
    }

    // 添加标签关联
    public void addTag(RawQuestionTag tag) {
        questionTags.add(tag);
        tag.setRawQuestion(this);
    }
    
    // 移除标签关联
    public void removeTag(RawQuestionTag tag) {
        questionTags.remove(tag);
        tag.setRawQuestion(null);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceSite() {
        return sourceSite;
    }

    public void setSourceSite(String sourceSite) {
        this.sourceSite = sourceSite;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCrawlTime() {
        return crawlTime;
    }

    public void setCrawlTime(LocalDateTime crawlTime) {
        this.crawlTime = crawlTime;
    }
    
    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getOtherMetadata() {
        return otherMetadata;
    }

    public void setOtherMetadata(String otherMetadata) {
        this.otherMetadata = otherMetadata;
    }
    
    public List<RawQuestionTag> getQuestionTags() {
        return questionTags;
    }

    public void setQuestionTags(List<RawQuestionTag> questionTags) {
        this.questionTags = questionTags;
    }
} 
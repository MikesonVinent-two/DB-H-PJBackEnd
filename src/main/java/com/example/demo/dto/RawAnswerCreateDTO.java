package com.example.demo.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

public class RawAnswerCreateDTO {
    
    private String authorInfo;
    
    @NotBlank(message = "回答内容不能为空")
    private String content;
    
    private LocalDateTime publishTime;
    
    private Integer upvotes;
    
    private Boolean isAccepted;
    
    private String otherMetadata;
    
    // 构造函数
    public RawAnswerCreateDTO() {
    }
    
    // Getters and Setters
    public String getAuthorInfo() {
        return authorInfo;
    }
    
    public void setAuthorInfo(String authorInfo) {
        this.authorInfo = authorInfo;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getPublishTime() {
        return publishTime;
    }
    
    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }
    
    public Integer getUpvotes() {
        return upvotes;
    }
    
    public void setUpvotes(Integer upvotes) {
        this.upvotes = upvotes;
    }
    
    public Boolean getIsAccepted() {
        return isAccepted;
    }
    
    public void setIsAccepted(Boolean isAccepted) {
        this.isAccepted = isAccepted;
    }
    
    public String getOtherMetadata() {
        return otherMetadata;
    }
    
    public void setOtherMetadata(String otherMetadata) {
        this.otherMetadata = otherMetadata;
    }
} 
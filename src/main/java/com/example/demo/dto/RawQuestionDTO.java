package com.example.demo.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RawQuestionDTO {
    
    @NotBlank(message = "来源URL不能为空")
    private String sourceUrl;
    
    private String sourceSite;
    
    @NotBlank(message = "标题不能为空")
    @Size(max = 512, message = "标题长度不能超过512个字符")
    private String title;
    
    @NotBlank(message = "内容不能为空")
    private String content;
    
    private LocalDateTime crawlTime;
    
    private String otherMetadata;
    
    // 构造函数
    public RawQuestionDTO() {
    }
    
    // Getters and Setters
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
    
    public String getOtherMetadata() {
        return otherMetadata;
    }
    
    public void setOtherMetadata(String otherMetadata) {
        this.otherMetadata = otherMetadata;
    }
} 
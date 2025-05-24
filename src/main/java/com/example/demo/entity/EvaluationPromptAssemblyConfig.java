package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 评测场景的prompt组装配置实体类
 */
@Entity
@Table(name = "evaluation_prompt_assembly_configs")
public class EvaluationPromptAssemblyConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column
    private String description;
    
    @Column(name = "base_system_prompt", columnDefinition = "text")
    private String baseSystemPrompt;
    
    @Column(name = "tag_prompts_section_header")
    private String tagPromptsSectionHeader = "## 专业评测标准";
    
    @Column(name = "subjective_section_header")
    private String subjectiveSectionHeader = "## 主观题评测要求";
    
    @Column(name = "tag_prompt_separator")
    private String tagPromptSeparator = "\n\n";
    
    @Column(name = "section_separator")
    private String sectionSeparator = "\n\n";
    
    @Column(name = "final_instruction", columnDefinition = "text")
    private String finalInstruction;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @ManyToOne
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;
    
    @ManyToOne
    @JoinColumn(name = "created_change_log_id")
    private ChangeLog createdChangeLog;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBaseSystemPrompt() {
        return baseSystemPrompt;
    }

    public void setBaseSystemPrompt(String baseSystemPrompt) {
        this.baseSystemPrompt = baseSystemPrompt;
    }

    public String getTagPromptsSectionHeader() {
        return tagPromptsSectionHeader;
    }

    public void setTagPromptsSectionHeader(String tagPromptsSectionHeader) {
        this.tagPromptsSectionHeader = tagPromptsSectionHeader;
    }

    public String getSubjectiveSectionHeader() {
        return subjectiveSectionHeader;
    }

    public void setSubjectiveSectionHeader(String subjectiveSectionHeader) {
        this.subjectiveSectionHeader = subjectiveSectionHeader;
    }

    public String getTagPromptSeparator() {
        return tagPromptSeparator;
    }

    public void setTagPromptSeparator(String tagPromptSeparator) {
        this.tagPromptSeparator = tagPromptSeparator;
    }

    public String getSectionSeparator() {
        return sectionSeparator;
    }

    public void setSectionSeparator(String sectionSeparator) {
        this.sectionSeparator = sectionSeparator;
    }

    public String getFinalInstruction() {
        return finalInstruction;
    }

    public void setFinalInstruction(String finalInstruction) {
        this.finalInstruction = finalInstruction;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
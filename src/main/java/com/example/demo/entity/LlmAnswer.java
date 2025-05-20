package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "llm_answers")
public class LlmAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_run_id", nullable = false)
    private EvaluationRun evaluationRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_question_mapping_id", nullable = false)
    private DatasetQuestionMapping datasetQuestionMapping;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "generation_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private GenerationStatus generationStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "generation_time")
    private LocalDateTime generationTime;

    @Column(name = "prompt_used", columnDefinition = "TEXT")
    private String promptUsed;

    @Column(name = "other_metadata", columnDefinition = "json")
    private String otherMetadata;

    // 生成状态枚举
    public enum GenerationStatus {
        SUCCESS,    // 生成成功
        FAILED      // 生成失败
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EvaluationRun getEvaluationRun() {
        return evaluationRun;
    }

    public void setEvaluationRun(EvaluationRun evaluationRun) {
        this.evaluationRun = evaluationRun;
    }

    public DatasetQuestionMapping getDatasetQuestionMapping() {
        return datasetQuestionMapping;
    }

    public void setDatasetQuestionMapping(DatasetQuestionMapping datasetQuestionMapping) {
        this.datasetQuestionMapping = datasetQuestionMapping;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public GenerationStatus getGenerationStatus() {
        return generationStatus;
    }

    public void setGenerationStatus(GenerationStatus generationStatus) {
        this.generationStatus = generationStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getGenerationTime() {
        return generationTime;
    }

    public void setGenerationTime(LocalDateTime generationTime) {
        this.generationTime = generationTime;
    }

    public String getPromptUsed() {
        return promptUsed;
    }

    public void setPromptUsed(String promptUsed) {
        this.promptUsed = promptUsed;
    }

    public String getOtherMetadata() {
        return otherMetadata;
    }

    public void setOtherMetadata(String otherMetadata) {
        this.otherMetadata = otherMetadata;
    }
} 
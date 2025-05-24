package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "llm_answers")
public class LlmAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "model_answer_run_id", nullable = false)
    private ModelAnswerRun modelAnswerRun;

    @ManyToOne
    @JoinColumn(name = "dataset_question_mapping_id", nullable = false)
    private DatasetQuestionMapping datasetQuestionMapping;

    @Column(name = "answer_text", columnDefinition = "text")
    private String answerText;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false)
    private GenerationStatus generationStatus;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "generation_time")
    private LocalDateTime generationTime;

    @Column(name = "prompt_used", columnDefinition = "text")
    private String promptUsed;

    @Column(name = "raw_model_response", columnDefinition = "text")
    private String rawModelResponse;

    @Column(name = "other_metadata", columnDefinition = "json")
    private String otherMetadata;

    @Column(name = "repeat_index", nullable = false)
    private Integer repeatIndex = 0;

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

    public ModelAnswerRun getModelAnswerRun() {
        return modelAnswerRun;
    }

    public void setModelAnswerRun(ModelAnswerRun modelAnswerRun) {
        this.modelAnswerRun = modelAnswerRun;
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

    public String getRawModelResponse() {
        return rawModelResponse;
    }

    public void setRawModelResponse(String rawModelResponse) {
        this.rawModelResponse = rawModelResponse;
    }

    public String getOtherMetadata() {
        return otherMetadata;
    }

    public void setOtherMetadata(String otherMetadata) {
        this.otherMetadata = otherMetadata;
    }

    public Integer getRepeatIndex() {
        return repeatIndex;
    }

    public void setRepeatIndex(Integer repeatIndex) {
        this.repeatIndex = repeatIndex;
    }
} 
package com.example.demo.repository;

import com.example.demo.entity.AnswerGenerationBatch;
import com.example.demo.entity.AnswerGenerationBatch.BatchStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerGenerationBatchRepository extends JpaRepository<AnswerGenerationBatch, Long> {
    List<AnswerGenerationBatch> findByStatus(BatchStatus status);
    List<AnswerGenerationBatch> findByCreatedByUserId(Long userId);
    List<AnswerGenerationBatch> findByDatasetVersionId(Long datasetVersionId);
    long countByStatus(BatchStatus status);
} 
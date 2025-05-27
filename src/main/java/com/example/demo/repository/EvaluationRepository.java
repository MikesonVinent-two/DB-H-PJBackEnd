package com.example.demo.repository;

import com.example.demo.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 评测仓库接口
 */
@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    
    /**
     * 根据LLM回答ID查找评测
     * 
     * @param llmAnswerId LLM回答ID
     * @return 评测列表
     */
    List<Evaluation> findByLlmAnswerId(Long llmAnswerId);
    
    /**
     * 根据评测运行ID查找评测
     * 
     * @param evaluationRunId 评测运行ID
     * @return 评测列表
     */
    List<Evaluation> findByEvaluationRunId(Long evaluationRunId);
    
    /**
     * 根据评测者ID查找评测
     * 
     * @param evaluatorId 评测者ID
     * @return 评测列表
     */
    List<Evaluation> findByEvaluatorId(Long evaluatorId);
    
    /**
     * 统计评测运行中已完成的评测数量
     * 
     * @param evaluationRunId 评测运行ID
     * @return 已完成评测数量
     */
    @Query("SELECT COUNT(e) FROM Evaluation e WHERE e.evaluationRun.id = :evaluationRunId AND e.status = 'SUCCESS'")
    int countCompletedByEvaluationRunId(@Param("evaluationRunId") Long evaluationRunId);
    
    /**
     * 统计评测运行中失败的评测数量
     * 
     * @param evaluationRunId 评测运行ID
     * @return 失败评测数量
     */
    @Query("SELECT COUNT(e) FROM Evaluation e WHERE e.evaluationRun.id = :evaluationRunId AND e.status = 'FAILED'")
    int countFailedByEvaluationRunId(@Param("evaluationRunId") Long evaluationRunId);
    
    /**
     * 查找特定回答生成批次的所有评测
     * 
     * @param batchId 回答生成批次ID
     * @return 评测列表
     */
    @Query("SELECT e FROM Evaluation e WHERE e.llmAnswer.modelAnswerRun.answerGenerationBatch.id = :batchId")
    List<Evaluation> findByAnswerGenerationBatchId(@Param("batchId") Long batchId);

    int countByEvaluationRunId(Long evaluationRunId);
    boolean existsByLlmAnswerIdAndEvaluatorId(Long llmAnswerId, Long evaluatorId);
    boolean existsByLlmAnswerIdAndEvaluationRunId(Long llmAnswerId, Long evaluationRunId);
    
    List<Evaluation> findByLlmAnswerIdAndEvaluatorId(Long llmAnswerId, Long evaluatorId);
} 
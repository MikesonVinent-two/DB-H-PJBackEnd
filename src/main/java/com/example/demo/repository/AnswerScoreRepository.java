package com.example.demo.repository;

import com.example.demo.entity.AnswerScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerScoreRepository extends JpaRepository<AnswerScore, Long> {
    
    /**
     * 根据回答ID和评测者ID查找分数记录
     */
    List<AnswerScore> findByLlmAnswerIdAndEvaluatorId(Long llmAnswerId, Long evaluatorId);
    
    /**
     * 根据回答ID、评测者ID和分数类型查找分数记录
     */
    Optional<AnswerScore> findByLlmAnswerIdAndEvaluatorIdAndScoreType(Long llmAnswerId, Long evaluatorId, String scoreType);
    
    /**
     * 检查指定回答、评测者和分数类型的分数记录是否存在
     */
    boolean existsByLlmAnswerIdAndEvaluatorIdAndScoreType(Long llmAnswerId, Long evaluatorId, String scoreType);
    
    /**
     * 根据评测ID查找所有分数记录
     */
    List<AnswerScore> findByEvaluationId(Long evaluationId);
    
    /**
     * 根据回答ID查找所有分数记录
     */
    List<AnswerScore> findByLlmAnswerId(Long llmAnswerId);
    
    /**
     * 根据回答ID和分数类型查找所有分数记录
     */
    List<AnswerScore> findByLlmAnswerIdAndScoreType(Long llmAnswerId, String scoreType);
    
    /**
     * 查询指定回答的平均分数
     */
    @Query("SELECT AVG(a.normalizedScore) FROM AnswerScore a WHERE a.llmAnswer.id = :llmAnswerId AND a.scoreType = :scoreType")
    Double findAverageScoreByLlmAnswerIdAndScoreType(@Param("llmAnswerId") Long llmAnswerId, @Param("scoreType") String scoreType);
    
    /**
     * 查询指定回答的最高分
     */
    @Query("SELECT MAX(a.normalizedScore) FROM AnswerScore a WHERE a.llmAnswer.id = :llmAnswerId AND a.scoreType = :scoreType")
    Double findMaxScoreByLlmAnswerIdAndScoreType(@Param("llmAnswerId") Long llmAnswerId, @Param("scoreType") String scoreType);
    
    /**
     * 查询指定回答的最低分
     */
    @Query("SELECT MIN(a.normalizedScore) FROM AnswerScore a WHERE a.llmAnswer.id = :llmAnswerId AND a.scoreType = :scoreType")
    Double findMinScoreByLlmAnswerIdAndScoreType(@Param("llmAnswerId") Long llmAnswerId, @Param("scoreType") String scoreType);
    
    /**
     * 根据分数类型查找所有分数记录
     */
    List<AnswerScore> findByScoreType(String scoreType);
    
    /**
     * 根据评测者ID查找所有分数记录
     */
    List<AnswerScore> findByEvaluatorId(Long evaluatorId);
    
    /**
     * 根据评测者ID和分数类型查找所有分数记录
     */
    List<AnswerScore> findByEvaluatorIdAndScoreType(Long evaluatorId, String scoreType);
    
    /**
     * 根据评测ID删除所有分数记录
     */
    void deleteByEvaluationId(Long evaluationId);
} 
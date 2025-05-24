package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.ModelAnswer;

import java.util.List;

/**
 * 模型回答仓库接口
 */
@Repository
public interface ModelAnswerRepository extends JpaRepository<ModelAnswer, Long> {
    
    /**
     * 根据运行ID查找回答
     * 
     * @param runId 运行ID
     * @return 回答列表
     */
    List<ModelAnswer> findByModelAnswerRunId(Long runId);
    
    /**
     * 根据问题ID查找回答
     * 
     * @param questionId 问题ID
     * @return 回答列表
     */
    List<ModelAnswer> findByStandardQuestionId(Long questionId);
    
    /**
     * 根据运行ID和问题ID查找回答
     * 
     * @param runId 运行ID
     * @param questionId 问题ID
     * @return 回答列表
     */
    List<ModelAnswer> findByModelAnswerRunIdAndStandardQuestionId(Long runId, Long questionId);
    
    /**
     * 根据运行ID和问题ID及重复索引查找回答
     * 
     * @param runId 运行ID
     * @param questionId 问题ID
     * @param repeatIndex 重复索引
     * @return 回答
     */
    ModelAnswer findByModelAnswerRunIdAndStandardQuestionIdAndRepeatIndex(Long runId, Long questionId, Integer repeatIndex);
    
    /**
     * 统计运行的已完成回答数量
     * 
     * @param runId 运行ID
     * @return 回答数量
     */
    @Query("SELECT COUNT(a) FROM ModelAnswer a WHERE a.modelAnswerRun.id = :runId")
    int countByRunId(@Param("runId") Long runId);
    
    /**
     * 按批次ID查找所有回答
     * 
     * @param batchId 批次ID
     * @return 回答列表
     */
    @Query("SELECT a FROM ModelAnswer a WHERE a.modelAnswerRun.answerGenerationBatch.id = :batchId")
    List<ModelAnswer> findByBatchId(@Param("batchId") Long batchId);
} 
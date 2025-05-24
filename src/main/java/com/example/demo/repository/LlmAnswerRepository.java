package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.LlmAnswer;

import java.util.List;

/**
 * LLM回答仓库接口
 */
@Repository
public interface LlmAnswerRepository extends JpaRepository<LlmAnswer, Long> {
    
    /**
     * 根据运行ID查找回答
     * 
     * @param modelAnswerRunId 运行ID
     * @return 回答列表
     */
    List<LlmAnswer> findByModelAnswerRunId(Long modelAnswerRunId);
    
    /**
     * 根据数据集映射问题ID查找回答
     * 
     * @param datasetQuestionMappingId 数据集映射问题ID
     * @return 回答列表
     */
    List<LlmAnswer> findByDatasetQuestionMappingId(Long datasetQuestionMappingId);
    
    /**
     * 根据运行ID和数据集映射问题ID查找回答
     * 
     * @param runId 运行ID
     * @param datasetQuestionMappingId 数据集映射问题ID
     * @return 回答列表
     */
    List<LlmAnswer> findByModelAnswerRunIdAndDatasetQuestionMappingId(Long runId, Long datasetQuestionMappingId);
    
    /**
     * 根据运行ID和数据集映射问题ID及重复索引查找回答
     * 
     * @param runId 运行ID
     * @param datasetQuestionMappingId 数据集映射问题ID
     * @param repeatIndex 重复索引
     * @return 回答
     */
    LlmAnswer findByModelAnswerRunIdAndDatasetQuestionMappingIdAndRepeatIndex(Long runId, Long datasetQuestionMappingId, Integer repeatIndex);
    
    /**
     * 统计运行的已完成回答数量
     * 
     * @param runId 运行ID
     * @return 回答数量
     */
    @Query("SELECT COUNT(a) FROM LlmAnswer a WHERE a.modelAnswerRun.id = :runId")
    int countByRunId(@Param("runId") Long runId);
    
    /**
     * 按批次ID查找所有回答
     * 
     * @param batchId 批次ID
     * @return 回答列表
     */
    @Query("SELECT a FROM LlmAnswer a WHERE a.modelAnswerRun.answerGenerationBatch.id = :batchId")
    List<LlmAnswer> findByBatchId(@Param("batchId") Long batchId);
    
    /**
     * 根据模型回答运行ID和回答ID查询大于指定ID的回答列表
     * 
     * @param modelAnswerRunId 模型回答运行ID
     * @param id 回答ID
     * @return 回答列表
     */
    List<LlmAnswer> findByModelAnswerRunIdAndIdGreaterThan(Long modelAnswerRunId, Long id);
} 
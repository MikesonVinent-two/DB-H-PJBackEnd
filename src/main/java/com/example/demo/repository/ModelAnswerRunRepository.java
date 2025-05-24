package com.example.demo.repository;

import com.example.demo.entity.ModelAnswerRun;
import com.example.demo.entity.ModelAnswerRun.RunStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模型回答运行仓库接口
 */
@Repository
public interface ModelAnswerRunRepository extends JpaRepository<ModelAnswerRun, Long> {
    
    /**
     * 根据批次ID查找运行
     * 
     * @param batchId 批次ID
     * @return 该批次的所有运行
     */
    List<ModelAnswerRun> findByAnswerGenerationBatchId(Long batchId);
    
    /**
     * 根据模型ID查找运行
     * 
     * @param modelId 模型ID
     * @return 该模型的所有运行
     */
    List<ModelAnswerRun> findByLlmModelId(Long modelId);
    
    /**
     * 根据状态查找运行
     * 
     * @param status 运行状态
     * @return 指定状态的所有运行
     */
    List<ModelAnswerRun> findByStatus(RunStatus status);
    
    /**
     * 根据批次ID和模型ID查找运行
     * 
     * @param batchId 批次ID
     * @param modelId 模型ID
     * @return 匹配的运行列表
     */
    List<ModelAnswerRun> findByAnswerGenerationBatchIdAndLlmModelId(Long batchId, Long modelId);
    
    /**
     * 根据批次ID和状态查找运行
     * 
     * @param batchId 批次ID
     * @param status 运行状态
     * @return 匹配的运行列表
     */
    List<ModelAnswerRun> findByAnswerGenerationBatchIdAndStatus(Long batchId, RunStatus status);
    
    /**
     * 统计批次中各状态的运行数量
     * 
     * @param batchId 批次ID
     * @param status 运行状态
     * @return 符合条件的运行数量
     */
    long countByAnswerGenerationBatchIdAndStatus(Long batchId, RunStatus status);
    
    @Query("SELECT r FROM ModelAnswerRun r WHERE r.answerGenerationBatch.id = :batchId AND r.llmModel.id = :modelId AND r.runIndex = :runIndex")
    ModelAnswerRun findByBatchModelAndRunIndex(@Param("batchId") Long batchId,
                                              @Param("modelId") Long modelId,
                                              @Param("runIndex") Integer runIndex);
    
    @Query("SELECT r FROM ModelAnswerRun r JOIN r.answerGenerationBatch b WHERE b.createdByUser.id = :userId ORDER BY r.runTime DESC")
    List<ModelAnswerRun> findByUserId(@Param("userId") Long userId);
} 
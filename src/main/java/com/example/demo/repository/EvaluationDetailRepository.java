package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.EvaluationDetail;
import com.example.demo.entity.Evaluation;

import java.util.List;

/**
 * 评测详情仓库接口
 */
@Repository
public interface EvaluationDetailRepository extends JpaRepository<EvaluationDetail, Long> {
    
    /**
     * 根据评测ID查找评测详情
     * 
     * @param evaluationId 评测ID
     * @return 评测详情列表
     */
    List<EvaluationDetail> findByEvaluationId(Long evaluationId);
    
    /**
     * 根据评测实体查找评测详情
     * 
     * @param evaluation 评测实体
     * @return 评测详情列表
     */
    List<EvaluationDetail> findByEvaluation(Evaluation evaluation);
    
    /**
     * 根据评测运行ID查找评测详情
     * 
     * @param evaluationRunId 评测运行ID
     * @return 评测详情列表
     */
    @Query("SELECT ed FROM EvaluationDetail ed WHERE ed.evaluation.evaluationRun.id = :evaluationRunId")
    List<EvaluationDetail> findByEvaluationRunId(@Param("evaluationRunId") Long evaluationRunId);
    
    /**
     * 删除指定评测的所有评测详情
     * 
     * @param evaluationId 评测ID
     */
    void deleteByEvaluationId(Long evaluationId);
} 
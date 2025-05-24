package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.EvaluationCriterion;
import com.example.demo.entity.QuestionType;

import java.util.List;
import java.util.Optional;

/**
 * 评测标准仓库接口
 */
@Repository
public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, Long> {
    
    /**
     * 根据问题类型查找评测标准
     * 
     * @param questionType 问题类型
     * @return 评测标准列表
     */
    List<EvaluationCriterion> findByQuestionTypeAndDeletedAtIsNull(QuestionType questionType);
    
    /**
     * 根据问题类型查找激活的评测标准，按顺序排序
     * 
     * @param questionType 问题类型
     * @return 评测标准列表
     */
    @Query("SELECT ec FROM EvaluationCriterion ec WHERE ec.questionType = :questionType AND ec.deletedAt IS NULL ORDER BY ec.orderIndex ASC")
    List<EvaluationCriterion> findActiveByQuestionTypeOrderByOrderIndex(@Param("questionType") QuestionType questionType);
    
    /**
     * 根据名称查找评测标准
     * 
     * @param name 标准名称
     * @return 评测标准
     */
    Optional<EvaluationCriterion> findByNameAndDeletedAtIsNull(String name);
    
    /**
     * 根据ID和名称查找评测标准
     * 
     * @param id 标准ID
     * @param name 标准名称
     * @return 评测标准
     */
    Optional<EvaluationCriterion> findByIdAndNameAndDeletedAtIsNull(Long id, String name);

    List<EvaluationCriterion> findByQuestionType(QuestionType questionType);
} 
package com.example.demo.repository;

import com.example.demo.entity.StandardObjectiveAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 标准客观题答案仓库接口
 */
@Repository
public interface StandardObjectiveAnswerRepository extends JpaRepository<StandardObjectiveAnswer, Long> {
    
    /**
     * 根据标准问题ID查找未删除的标准客观题答案
     * 
     * @param standardQuestionId 标准问题ID
     * @return 标准客观题答案
     */
    Optional<StandardObjectiveAnswer> findByStandardQuestionIdAndDeletedAtIsNull(Long standardQuestionId);
    
    /**
     * 根据标准问题ID查找所有标准客观题答案（包括已删除的）
     * 
     * @param standardQuestionId 标准问题ID
     * @return 标准客观题答案列表
     */
    Optional<StandardObjectiveAnswer> findByStandardQuestionId(Long standardQuestionId);
} 
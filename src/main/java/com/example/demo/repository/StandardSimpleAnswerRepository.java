package com.example.demo.repository;

import com.example.demo.entity.StandardSimpleAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 标准简单事实题答案仓库接口
 */
@Repository
public interface StandardSimpleAnswerRepository extends JpaRepository<StandardSimpleAnswer, Long> {
    
    /**
     * 根据标准问题ID查找未删除的标准简单事实题答案
     * 
     * @param standardQuestionId 标准问题ID
     * @return 标准简单事实题答案
     */
    Optional<StandardSimpleAnswer> findByStandardQuestionIdAndDeletedAtIsNull(Long standardQuestionId);
    
    /**
     * 根据标准问题ID查找所有标准简单事实题答案（包括已删除的）
     * 
     * @param standardQuestionId 标准问题ID
     * @return 标准简单事实题答案列表
     */
    Optional<StandardSimpleAnswer> findByStandardQuestionId(Long standardQuestionId);
} 
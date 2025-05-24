package com.example.demo.repository;

import com.example.demo.entity.Evaluator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 评测者仓库接口
 */
@Repository
public interface EvaluatorRepository extends JpaRepository<Evaluator, Long> {
    
    /**
     * 根据名称查找评测者
     * 
     * @param name 评测者名称
     * @return 评测者
     */
    Optional<Evaluator> findByName(String name);
    
    /**
     * 查找所有激活状态的评测者
     * 
     * @return 评测者列表
     */
    List<Evaluator> findByIsActiveTrue();
    
    /**
     * 根据类型查找评测者
     * 
     * @param evaluatorType 评测者类型
     * @return 评测者列表
     */
    List<Evaluator> findByEvaluatorType(Evaluator.EvaluatorType evaluatorType);
    
    /**
     * 根据类型和激活状态查找评测者
     * 
     * @param evaluatorType 评测者类型
     * @param isActive 是否激活
     * @return 评测者列表
     */
    List<Evaluator> findByEvaluatorTypeAndIsActive(Evaluator.EvaluatorType evaluatorType, boolean isActive);
} 
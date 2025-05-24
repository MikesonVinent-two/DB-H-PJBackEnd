package com.example.demo.repository;

import com.example.demo.entity.EvaluationSubjectivePrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationSubjectivePromptRepository extends JpaRepository<EvaluationSubjectivePrompt, Long> {
    
    /**
     * 查找所有激活状态的主观题评测提示词
     */
    List<EvaluationSubjectivePrompt> findByIsActiveTrueAndDeletedAtIsNull();
    
    /**
     * 按版本号查询主观题评测提示词
     */
    Optional<EvaluationSubjectivePrompt> findByVersionAndDeletedAtIsNull(String version);
    
    /**
     * 查找所有未删除的主观题评测提示词
     */
    List<EvaluationSubjectivePrompt> findByDeletedAtIsNull();
    
    /**
     * 查找最新版本的主观题评测提示词
     */
    @Query("SELECT esp FROM EvaluationSubjectivePrompt esp WHERE esp.deletedAt IS NULL ORDER BY esp.createdAt DESC")
    List<EvaluationSubjectivePrompt> findLatestPrompts();
    
    /**
     * 按名称查找主观题评测提示词
     */
    List<EvaluationSubjectivePrompt> findByNameContainingAndDeletedAtIsNull(String name);
} 
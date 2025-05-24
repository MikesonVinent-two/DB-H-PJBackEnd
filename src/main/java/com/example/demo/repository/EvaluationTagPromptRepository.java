package com.example.demo.repository;

import com.example.demo.entity.EvaluationTagPrompt;
import com.example.demo.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationTagPromptRepository extends JpaRepository<EvaluationTagPrompt, Long> {
    
    /**
     * 查找指定标签的所有激活状态的评测提示词，按优先级排序
     */
    List<EvaluationTagPrompt> findByTagAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(Tag tag);
    
    /**
     * 按标签ID查询激活状态的评测提示词
     */
    List<EvaluationTagPrompt> findByTagIdAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(Long tagId);
    
    /**
     * 按版本号查询特定标签的评测提示词
     */
    Optional<EvaluationTagPrompt> findByTagIdAndVersionAndDeletedAtIsNull(Long tagId, String version);
    
    /**
     * 查找所有未删除的标签评测提示词
     */
    List<EvaluationTagPrompt> findByDeletedAtIsNull();
    
    /**
     * 查找最新版本的标签评测提示词
     */
    @Query("SELECT etp FROM EvaluationTagPrompt etp WHERE etp.tag.id = :tagId AND etp.deletedAt IS NULL ORDER BY etp.createdAt DESC")
    List<EvaluationTagPrompt> findLatestByTagId(Long tagId);
} 
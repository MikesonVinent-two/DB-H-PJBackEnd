package com.example.demo.repository;

import com.example.demo.entity.AnswerTagPrompt;
import com.example.demo.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerTagPromptRepository extends JpaRepository<AnswerTagPrompt, Long> {
    
    /**
     * 查找指定标签的所有激活状态的提示词，按优先级排序
     */
    List<AnswerTagPrompt> findByTagAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(Tag tag);
    
    /**
     * 按标签ID查询激活状态的提示词
     */
    List<AnswerTagPrompt> findByTagIdAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(Long tagId);
    
    /**
     * 按版本号查询特定标签的提示词
     */
    Optional<AnswerTagPrompt> findByTagIdAndVersionAndDeletedAtIsNull(Long tagId, String version);
    
    /**
     * 查找所有未删除的标签提示词
     */
    List<AnswerTagPrompt> findByDeletedAtIsNull();
    
    /**
     * 查找最新版本的标签提示词
     */
    @Query("SELECT atp FROM AnswerTagPrompt atp WHERE atp.tag.id = :tagId AND atp.deletedAt IS NULL ORDER BY atp.createdAt DESC")
    List<AnswerTagPrompt> findLatestByTagId(Long tagId);
} 
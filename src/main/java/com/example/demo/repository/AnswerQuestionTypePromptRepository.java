package com.example.demo.repository;

import com.example.demo.entity.AnswerQuestionTypePrompt;
import com.example.demo.entity.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerQuestionTypePromptRepository extends JpaRepository<AnswerQuestionTypePrompt, Long> {
    
    /**
     * 按题型查找激活状态的提示词
     */
    List<AnswerQuestionTypePrompt> findByQuestionTypeAndIsActiveTrueAndDeletedAtIsNull(QuestionType questionType);
    
    /**
     * 按版本号查询特定题型的提示词
     */
    Optional<AnswerQuestionTypePrompt> findByQuestionTypeAndVersionAndDeletedAtIsNull(QuestionType questionType, String version);
    
    /**
     * 查找所有未删除的题型提示词
     */
    List<AnswerQuestionTypePrompt> findByDeletedAtIsNull();
    
    /**
     * 查找最新版本的题型提示词
     */
    @Query("SELECT aqtp FROM AnswerQuestionTypePrompt aqtp WHERE aqtp.questionType = :questionType AND aqtp.deletedAt IS NULL ORDER BY aqtp.createdAt DESC")
    List<AnswerQuestionTypePrompt> findLatestByQuestionType(QuestionType questionType);
    
    /**
     * 查找所有已启用的题型提示词，按题型分组
     */
    @Query("SELECT aqtp FROM AnswerQuestionTypePrompt aqtp WHERE aqtp.isActive = true AND aqtp.deletedAt IS NULL ORDER BY aqtp.questionType, aqtp.createdAt DESC")
    List<AnswerQuestionTypePrompt> findAllActiveGroupByQuestionType();
} 
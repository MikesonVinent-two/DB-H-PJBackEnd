package com.example.demo.service;

import com.example.demo.dto.AnswerQuestionTypePromptDTO;
import com.example.demo.dto.AnswerTagPromptDTO;
import com.example.demo.entity.AnswerQuestionTypePrompt;
import com.example.demo.entity.AnswerTagPrompt;
import com.example.demo.entity.QuestionType;

import java.util.List;
import java.util.Optional;

public interface PromptService {
    
    /**
     * 创建标签提示词
     */
    AnswerTagPrompt createAnswerTagPrompt(AnswerTagPromptDTO dto, Long userId);
    
    /**
     * 更新标签提示词
     */
    AnswerTagPrompt updateAnswerTagPrompt(Long id, AnswerTagPromptDTO dto, Long userId);
    
    /**
     * 获取标签提示词详情
     */
    Optional<AnswerTagPrompt> getAnswerTagPromptById(Long id);
    
    /**
     * 按标签ID获取激活状态的提示词
     */
    List<AnswerTagPrompt> getActiveAnswerTagPromptsByTagId(Long tagId);
    
    /**
     * 获取所有标签提示词
     */
    List<AnswerTagPrompt> getAllAnswerTagPrompts();
    
    /**
     * 删除标签提示词（软删除）
     */
    void deleteAnswerTagPrompt(Long id, Long userId);
    
    /**
     * 创建题型提示词
     */
    AnswerQuestionTypePrompt createAnswerQuestionTypePrompt(AnswerQuestionTypePromptDTO dto, Long userId);
    
    /**
     * 更新题型提示词
     */
    AnswerQuestionTypePrompt updateAnswerQuestionTypePrompt(Long id, AnswerQuestionTypePromptDTO dto, Long userId);
    
    /**
     * 获取题型提示词详情
     */
    Optional<AnswerQuestionTypePrompt> getAnswerQuestionTypePromptById(Long id);
    
    /**
     * 按题型获取激活状态的提示词
     */
    List<AnswerQuestionTypePrompt> getActiveAnswerQuestionTypePromptsByType(QuestionType questionType);
    
    /**
     * 获取所有题型提示词
     */
    List<AnswerQuestionTypePrompt> getAllAnswerQuestionTypePrompts();
    
    /**
     * 删除题型提示词（软删除）
     */
    void deleteAnswerQuestionTypePrompt(Long id, Long userId);
} 
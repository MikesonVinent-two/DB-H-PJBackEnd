package com.example.demo.service;

import java.util.List;
import com.example.demo.dto.QuestionHistoryDTO;
import com.example.demo.dto.StandardQuestionDTO;

public interface StandardQuestionService {
    /**
     * 创建标准问题
     * @param questionDTO 问题数据传输对象
     * @param userId 创建用户ID
     * @return 创建后的问题DTO
     */
    StandardQuestionDTO createStandardQuestion(StandardQuestionDTO questionDTO, Long userId);

    /**
     * 更新标准问题
     * @param questionId 要更新的问题ID
     * @param questionDTO 更新的问题数据
     * @param userId 操作用户ID
     * @return 更新后的问题DTO
     */
    StandardQuestionDTO updateStandardQuestion(Long questionId, StandardQuestionDTO questionDTO, Long userId);

    /**
     * 获取问题的修改历史
     * @param questionId 问题ID
     * @return 问题的历史版本列表
     */
    List<QuestionHistoryDTO> getQuestionHistory(Long questionId);

    /**
     * 获取问题的版本树
     * @param questionId 问题ID
     * @return 问题的版本树结构
     */
    List<QuestionHistoryDTO> getVersionTree(Long questionId);
} 
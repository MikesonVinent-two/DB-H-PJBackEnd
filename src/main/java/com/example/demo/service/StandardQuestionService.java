package com.example.demo.service;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.demo.dto.BatchTagOperationsDTO;
import com.example.demo.dto.QuestionHistoryDTO;
import com.example.demo.dto.StandardQuestionDTO;
import com.example.demo.dto.TagOperationDTO;

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
    
    /**
     * 获取所有标准问题（分页）
     * @param pageable 分页参数
     * @return 标准问题分页列表
     */
    Page<StandardQuestionDTO> findAllStandardQuestions(Pageable pageable);
    
    /**
     * 获取所有最新版本的标准问题（分页）
     * 只返回版本树中的叶子节点，即没有子版本的问题
     * @param pageable 分页参数
     * @return 最新版本的标准问题分页列表
     */
    Page<StandardQuestionDTO> findLatestStandardQuestions(Pageable pageable);
    
    /**
     * 操作单个标准问题的标签
     * @param operationDTO 标签操作请求
     * @return 更新后的标准问题DTO
     */
    StandardQuestionDTO updateQuestionTags(TagOperationDTO operationDTO);
    
    /**
     * 批量操作多个标准问题的标签
     * @param batchOperationsDTO 批量标签操作请求
     * @return 操作结果，key为问题ID，value为操作成功与否
     */
    Map<Long, Boolean> batchUpdateQuestionTags(BatchTagOperationsDTO batchOperationsDTO);
} 
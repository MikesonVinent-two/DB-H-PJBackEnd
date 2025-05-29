package com.example.demo.service;

import com.example.demo.dto.RawAnswerDTO;
import com.example.demo.dto.RawQuestionDTO;
import com.example.demo.dto.RawQuestionWithAnswersDTO;
import com.example.demo.entity.jdbc.RawQuestion;
import com.example.demo.entity.jdbc.RawAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.demo.dto.RawQuestionDisplayDTO;
import java.util.List;

public interface RawDataService {
    RawQuestion createQuestion(RawQuestion question);
    RawQuestion createQuestionFromDTO(RawQuestionDTO questionDTO);
    RawAnswer createAnswer(RawAnswerDTO answerDTO);
    RawQuestion createQuestionWithAnswers(RawQuestionWithAnswersDTO dto);
    
    // 分页查询原始问题
    Page<RawQuestionDisplayDTO> findAllRawQuestions(Pageable pageable);
    
    // 根据标准化状态分页查询
    Page<RawQuestionDisplayDTO> findRawQuestionsByStandardizedStatus(boolean isStandardized, Pageable pageable);
    
    // 根据来源网站分页查询
    Page<RawQuestionDisplayDTO> findRawQuestionsBySourceSite(String sourceSite, Pageable pageable);
    
    // 根据标题或内容模糊搜索
    Page<RawQuestionDisplayDTO> searchRawQuestions(String keyword, Pageable pageable);
    
    /**
     * 根据多个标签查询问题
     * @param tags 标签列表
     * @param pageable 分页参数
     * @return 满足所有标签的问题列表
     */
    Page<RawQuestionDisplayDTO> findQuestionsByTags(List<String> tags, Pageable pageable);
    
    /**
     * 删除原始问题
     * @param questionId 问题ID
     * @return 是否删除成功
     * @throws IllegalArgumentException 如果问题不存在
     * @throws IllegalStateException 如果问题已被标准化，不能删除
     */
    boolean deleteRawQuestion(Long questionId);
    
    /**
     * 删除原始回答
     * @param answerId 回答ID
     * @return 是否删除成功
     * @throws IllegalArgumentException 如果回答不存在
     */
    boolean deleteRawAnswer(Long answerId);
} 
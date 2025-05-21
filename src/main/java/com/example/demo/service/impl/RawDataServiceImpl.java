package com.example.demo.service.impl;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.dto.RawAnswerDTO;
import com.example.demo.dto.RawQuestionWithAnswersDTO;
import com.example.demo.entity.RawAnswer;
import com.example.demo.entity.RawQuestion;
import com.example.demo.repository.RawAnswerRepository;
import com.example.demo.repository.RawQuestionRepository;
import com.example.demo.service.RawDataService;

@Service
public class RawDataServiceImpl implements RawDataService {

    @Autowired
    private RawQuestionRepository questionRepository;

    @Autowired
    private RawAnswerRepository answerRepository;

    @Override
    public RawQuestion createQuestion(RawQuestion question) {
        // 验证必填字段
        if (question == null) {
            throw new IllegalArgumentException("问题对象不能为空");
        }
        if (!StringUtils.hasText(question.getSourceUrl())) {
            throw new IllegalArgumentException("来源URL不能为空");
        }
        if (!StringUtils.hasText(question.getTitle())) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (!StringUtils.hasText(question.getContent())) {
            throw new IllegalArgumentException("内容不能为空");
        }
        
        // 确保crawlTime有值
        if (question.getCrawlTime() == null) {
            question.setCrawlTime(LocalDateTime.now());
        }
        
        // 检查URL是否已存在
        if (questionRepository.existsBySourceUrl(question.getSourceUrl())) {
            throw new IllegalArgumentException("该来源URL已存在");
        }
        
        return questionRepository.save(question);
    }

    @Override
    @Transactional
    public RawAnswer createAnswer(RawAnswerDTO answerDTO) {
        // 验证必填字段
        if (answerDTO == null) {
            throw new IllegalArgumentException("回答对象不能为空");
        }
        if (answerDTO.getRawQuestionId() == null) {
            throw new IllegalArgumentException("问题ID不能为空");
        }
        if (!StringUtils.hasText(answerDTO.getContent())) {
            throw new IllegalArgumentException("回答内容不能为空");
        }
        
        // 查找问题
        RawQuestion question = questionRepository.findById(answerDTO.getRawQuestionId())
            .orElseThrow(() -> new IllegalArgumentException("问题不存在"));
        
        // 创建回答实体
        RawAnswer answer = new RawAnswer();
        answer.setRawQuestion(question);
        answer.setAuthorInfo(answerDTO.getAuthorInfo());
        answer.setContent(answerDTO.getContent());
        answer.setPublishTime(answerDTO.getPublishTime() != null ? 
            answerDTO.getPublishTime() : LocalDateTime.now());
        answer.setUpvotes(answerDTO.getUpvotes() != null ? 
            answerDTO.getUpvotes() : 0);
        answer.setIsAccepted(answerDTO.getIsAccepted());
        answer.setOtherMetadata(answerDTO.getOtherMetadata());
        
        return answerRepository.save(answer);
    }

    @Override
    @Transactional
    public RawQuestion createQuestionWithAnswers(RawQuestionWithAnswersDTO dto) {
        // 验证DTO对象
        if (dto == null) {
            throw new IllegalArgumentException("请求数据不能为空");
        }
        if (dto.getQuestion() == null) {
            throw new IllegalArgumentException("问题信息不能为空");
        }
        if (!StringUtils.hasText(dto.getQuestion().getSourceUrl())) {
            throw new IllegalArgumentException("来源URL不能为空");
        }
        if (!StringUtils.hasText(dto.getQuestion().getTitle())) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (!StringUtils.hasText(dto.getQuestion().getContent())) {
            throw new IllegalArgumentException("内容不能为空");
        }
        
        // 检查URL是否已存在
        if (questionRepository.existsBySourceUrl(dto.getQuestion().getSourceUrl())) {
            throw new IllegalArgumentException("该来源URL已存在");
        }
        
        // 创建问题
        RawQuestion question = new RawQuestion();
        question.setSourceUrl(dto.getQuestion().getSourceUrl());
        question.setSourceSite(dto.getQuestion().getSourceSite());
        question.setTitle(dto.getQuestion().getTitle());
        question.setContent(dto.getQuestion().getContent());
        // 如果DTO中的crawlTime为null，则使用当前时间
        question.setCrawlTime(dto.getQuestion().getCrawlTime() != null ? 
            dto.getQuestion().getCrawlTime() : LocalDateTime.now());
        question.setOtherMetadata(dto.getQuestion().getOtherMetadata());

        // 保存问题
        question = questionRepository.save(question);

        // 创建并保存答案
        if (dto.getAnswers() != null) {
            for (var answerDto : dto.getAnswers()) {
                if (answerDto == null) {
                    continue;
                }
                if (!StringUtils.hasText(answerDto.getContent())) {
                    throw new IllegalArgumentException("回答内容不能为空");
                }
                
                RawAnswer answer = new RawAnswer();
                answer.setRawQuestion(question);
                answer.setAuthorInfo(answerDto.getAuthorInfo());
                answer.setContent(answerDto.getContent());
                answer.setPublishTime(answerDto.getPublishTime());
                answer.setUpvotes(answerDto.getUpvotes());
                answer.setIsAccepted(answerDto.getIsAccepted());
                answer.setOtherMetadata(answerDto.getOtherMetadata());
                
                answerRepository.save(answer);
            }
        }

        return question;
    }
} 
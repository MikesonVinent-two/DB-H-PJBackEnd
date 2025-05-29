package com.example.demo.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.dto.RawAnswerDTO;
import com.example.demo.dto.RawQuestionDTO;
import com.example.demo.util.MetadataUtils;
import com.example.demo.dto.RawQuestionWithAnswersDTO;
import com.example.demo.entity.jdbc.RawAnswer;
import com.example.demo.entity.jdbc.RawQuestion;
import com.example.demo.entity.jdbc.RawQuestionTag;
import com.example.demo.entity.jdbc.Tag;
import com.example.demo.repository.jdbc.RawAnswerRepository;
import com.example.demo.repository.jdbc.RawQuestionRepository;
import com.example.demo.repository.jdbc.RawQuestionTagRepository;
import com.example.demo.repository.jdbc.TagRepository;
import com.example.demo.service.RawDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.dto.RawQuestionDisplayDTO;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.repository.jdbc.StandardQuestionRepository;

@Service
public class RawDataServiceImpl implements RawDataService {

    private static final Logger logger = LoggerFactory.getLogger(RawDataServiceImpl.class);

    @Autowired
    private RawQuestionRepository questionRepository;

    @Autowired
    private RawAnswerRepository answerRepository;
    
    @Autowired
    private TagRepository tagRepository;
    
    @Autowired
    private RawQuestionTagRepository rawQuestionTagRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RawQuestionRepository rawQuestionRepository;
    
    @Autowired
    private StandardQuestionRepository standardQuestionRepository;

    @Override
    @Transactional
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
        
        // 保存问题
        question = questionRepository.save(question);
        
        return question;
    }
    
    @Override
    @Transactional
    public RawQuestion createQuestionFromDTO(RawQuestionDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("问题数据不能为空");
        }
        
        // 创建问题实体
        RawQuestion question = new RawQuestion();
        question.setSourceUrl(dto.getSourceUrl());
        question.setSourceSite(dto.getSourceSite());
        question.setTitle(dto.getTitle());
        question.setContent(dto.getContent());
        question.setCrawlTime(dto.getCrawlTime() != null ? dto.getCrawlTime() : LocalDateTime.now());
        question.setOtherMetadata(MetadataUtils.normalizeMetadata(dto.getOtherMetadata()));
        
        // 处理标签
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            try {
                // 将标签列表转换为JSON字符串
                question.setTags(objectMapper.writeValueAsString(dto.getTags()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("处理标签数据失败", e);
            }
        }
        
        // 保存问题
        question = createQuestion(question);
        
        // 处理标签关联
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            processQuestionTags(question, dto.getTags());
        }
        
        return question;
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
        answer.setOtherMetadata(MetadataUtils.normalizeMetadata(answerDTO.getOtherMetadata()));
        
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
        question.setOtherMetadata(MetadataUtils.normalizeMetadata(dto.getQuestion().getOtherMetadata()));
        
        // 处理标签
        if (dto.getQuestion().getTags() != null && !dto.getQuestion().getTags().isEmpty()) {
            try {
                // 将标签列表转换为JSON字符串存储在raw_questions表的tags字段中
                question.setTags(objectMapper.writeValueAsString(dto.getQuestion().getTags()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("处理标签数据失败", e);
            }
        }

        // 保存问题
        question = questionRepository.save(question);
        
        // 处理标签关联
        if (dto.getQuestion().getTags() != null && !dto.getQuestion().getTags().isEmpty()) {
            processQuestionTags(question, dto.getQuestion().getTags());
        }

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
                answer.setOtherMetadata(MetadataUtils.normalizeMetadata(answerDto.getOtherMetadata()));
                
                answerRepository.save(answer);
            }
        }

        return question;
    }
    
    /**
     * 处理问题的标签关联
     * @param question 已保存的问题实体
     * @param tagNames 标签名称列表
     */
    @Transactional
    protected void processQuestionTags(RawQuestion question, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        
        List<RawQuestionTag> questionTags = new ArrayList<>();
        
        for (String tagName : tagNames) {
            if (!StringUtils.hasText(tagName)) {
                continue;
            }
            
            // 查找或创建标签
            Tag tag = tagRepository.findByTagName(tagName.trim())
                    .orElseGet(() -> {
                        Tag newTag = new Tag(tagName.trim());
                        return tagRepository.save(newTag);
                    });
            
            // 如果该问题与标签的关联不存在，则创建关联
            if (!rawQuestionTagRepository.existsByRawQuestionAndTag(question, tag)) {
                RawQuestionTag questionTag = new RawQuestionTag(question, tag);
                questionTags.add(questionTag);
                question.addTag(questionTag);
            }
        }
        
        if (!questionTags.isEmpty()) {
            rawQuestionTagRepository.saveAll(questionTags);
        }
    }

    @Override
    public Page<RawQuestionDisplayDTO> findAllRawQuestions(Pageable pageable) {
        logger.debug("开始查询原始问题，页码：{}，每页大小：{}", pageable.getPageNumber(), pageable.getPageSize());
        Page<RawQuestion> rawQuestions = rawQuestionRepository.findAll(pageable);
        logger.debug("查询到 {} 条记录", rawQuestions.getTotalElements());
        
        if (rawQuestions.isEmpty()) {
            logger.warn("未查询到任何原始问题数据");
        }
        
        return rawQuestions.map(question -> {
            logger.debug("转换问题数据，ID：{}", question.getId());
            return convertToDisplayDTO(question);
        });
    }

    @Override
    public Page<RawQuestionDisplayDTO> findRawQuestionsByStandardizedStatus(boolean isStandardized, Pageable pageable) {
        if (isStandardized) {
            return rawQuestionRepository.findByIdInOrderByIdDesc(
                standardQuestionRepository.findDistinctOriginalRawQuestionIds(), 
                pageable
            ).map(this::convertToDisplayDTO);
        } else {
            return rawQuestionRepository.findByIdNotInOrderByIdDesc(
                standardQuestionRepository.findDistinctOriginalRawQuestionIds(), 
                pageable
            ).map(this::convertToDisplayDTO);
        }
    }

    @Override
    public Page<RawQuestionDisplayDTO> findRawQuestionsBySourceSite(String sourceSite, Pageable pageable) {
        return rawQuestionRepository.findBySourceSiteContainingIgnoreCase(sourceSite, pageable)
                .map(this::convertToDisplayDTO);
    }

    @Override
    public Page<RawQuestionDisplayDTO> searchRawQuestions(String keyword, Pageable pageable) {
        return rawQuestionRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
                keyword, keyword, pageable)
                .map(this::convertToDisplayDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RawQuestionDisplayDTO> findQuestionsByTags(List<String> tags, Pageable pageable) {
        if (tags == null || tags.isEmpty()) {
            return findAllRawQuestions(pageable);
        }
        
        return questionRepository.findByTagNames(tags, (long)tags.size(), pageable)
                .map(this::convertToDisplayDTO);
    }

    @Override
    @Transactional
    public boolean deleteRawQuestion(Long questionId) {
        logger.debug("开始删除原始问题 - ID: {}", questionId);
        
        // 验证问题ID
        if (questionId == null) {
            throw new IllegalArgumentException("问题ID不能为空");
        }
        
        // 检查问题是否存在
        RawQuestion question = questionRepository.findById(questionId)
            .orElseThrow(() -> {
                logger.error("删除原始问题失败 - 问题不存在: {}", questionId);
                return new IllegalArgumentException("问题不存在");
            });
        
        // 检查是否被标准化
        List<StandardQuestion> standardQuestions = standardQuestionRepository.findByOriginalRawQuestionId(questionId);
        if (standardQuestions != null && !standardQuestions.isEmpty()) {
            String errorMsg = String.format("问题(ID:%d)已被标准化，不能删除", questionId);
            logger.error("删除原始问题失败 - {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        try {
            // 删除问题前先删除关联的回答和标签
            // 注意：如果使用了级联删除，这一步可能不是必须的
            // 但为了确保清理干净，我们手动处理
            List<RawAnswer> answers = answerRepository.findByRawQuestionId(questionId);
            if (answers != null && !answers.isEmpty()) {
                answerRepository.deleteAll(answers);
                logger.debug("已删除问题关联的{}个回答", answers.size());
            }
            
            List<RawQuestionTag> tags = rawQuestionTagRepository.findByRawQuestionId(questionId);
            if (tags != null && !tags.isEmpty()) {
                rawQuestionTagRepository.deleteAll(tags);
                logger.debug("已删除问题关联的{}个标签", tags.size());
            }
            
            // 删除问题
            questionRepository.delete(question);
            logger.info("原始问题删除成功 - ID: {}", questionId);
            return true;
        } catch (Exception e) {
            logger.error("删除原始问题失败", e);
            throw new RuntimeException("删除原始问题失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public boolean deleteRawAnswer(Long answerId) {
        logger.debug("开始删除原始回答 - ID: {}", answerId);
        
        // 验证回答ID
        if (answerId == null) {
            throw new IllegalArgumentException("回答ID不能为空");
        }
        
        // 检查回答是否存在
        RawAnswer answer = answerRepository.findById(answerId)
            .orElseThrow(() -> {
                logger.error("删除原始回答失败 - 回答不存在: {}", answerId);
                return new IllegalArgumentException("回答不存在");
            });
        
        try {
            // 删除回答
            answerRepository.delete(answer);
            logger.info("原始回答删除成功 - ID: {}, 问题ID: {}", answerId, answer.getRawQuestion().getId());
            return true;
        } catch (Exception e) {
            logger.error("删除原始回答失败", e);
            throw new RuntimeException("删除原始回答失败: " + e.getMessage());
        }
    }

    // 转换为展示DTO的辅助方法
    private RawQuestionDisplayDTO convertToDisplayDTO(RawQuestion rawQuestion) {
        logger.debug("开始转换问题 ID：{} 的数据", rawQuestion.getId());
        RawQuestionDisplayDTO dto = new RawQuestionDisplayDTO();
        dto.setId(rawQuestion.getId());
        dto.setSourceUrl(rawQuestion.getSourceUrl());
        dto.setSourceSite(rawQuestion.getSourceSite());
        dto.setTitle(rawQuestion.getTitle());
        dto.setContent(rawQuestion.getContent());
        dto.setCrawlTime(rawQuestion.getCrawlTime());
        
        // 处理标签
        if (rawQuestion.getQuestionTags() != null && !rawQuestion.getQuestionTags().isEmpty()) {
            logger.debug("问题 ID：{} 有 {} 个标签", rawQuestion.getId(), rawQuestion.getQuestionTags().size());
            dto.setTags(rawQuestion.getQuestionTags().stream()
                    .map(tag -> tag.getTag().getTagName())
                    .collect(Collectors.toList()));
        }
        
        // 检查是否已标准化
        Optional<StandardQuestion> standardQuestion = standardQuestionRepository
                .findFirstByOriginalRawQuestionIdOrderByCreationTimeDesc(rawQuestion.getId());
        
        dto.setStandardized(standardQuestion.isPresent());
        standardQuestion.ifPresent(sq -> {
            dto.setStandardQuestionId(sq.getId());
            logger.debug("问题 ID：{} 已标准化，标准问题 ID：{}", rawQuestion.getId(), sq.getId());
        });
        
        return dto;
    }
} 
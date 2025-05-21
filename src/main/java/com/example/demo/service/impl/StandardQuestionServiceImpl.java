package com.example.demo.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.dto.StandardQuestionDTO;
import com.example.demo.entity.ChangeLog;
import com.example.demo.entity.ChangeType;
import com.example.demo.entity.ChangeLogDetail;
import com.example.demo.entity.EntityType;
import com.example.demo.entity.RawQuestion;
import com.example.demo.entity.StandardQuestion;
import com.example.demo.entity.StandardQuestionTag;
import com.example.demo.entity.Tag;
import com.example.demo.entity.User;
import com.example.demo.repository.ChangeLogDetailRepository;
import com.example.demo.repository.ChangeLogRepository;
import com.example.demo.repository.RawQuestionRepository;
import com.example.demo.repository.StandardQuestionRepository;
import com.example.demo.repository.StandardQuestionTagRepository;
import com.example.demo.repository.TagRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.StandardQuestionService;
import com.example.demo.util.ChangeLogUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class StandardQuestionServiceImpl implements StandardQuestionService {

    private static final Logger logger = LoggerFactory.getLogger(StandardQuestionServiceImpl.class);

    @Autowired
    private StandardQuestionRepository standardQuestionRepository;
    
    @Autowired
    private RawQuestionRepository rawQuestionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChangeLogRepository changeLogRepository;
    
    @Autowired
    private ChangeLogDetailRepository changeLogDetailRepository;
    
    @Autowired
    private TagRepository tagRepository;
    
    @Autowired
    private StandardQuestionTagRepository standardQuestionTagRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public StandardQuestion createStandardQuestion(StandardQuestionDTO questionDTO, Long userId) {
        logger.debug("开始创建标准问题 - 用户ID: {}, 问题文本: {}", userId, questionDTO.getQuestionText());
        
        // 验证用户ID
        if (userId == null) {
            logger.error("创建标准问题失败 - 用户ID为空");
            throw new IllegalArgumentException("用户ID不能为空");
        }

        try {
            // 获取用户信息
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("创建标准问题失败 - 找不到用户ID: {}", userId);
                    return new IllegalArgumentException("找不到指定的用户（ID: " + userId + "）");
                });
            
            // 创建标准问题实体
            StandardQuestion standardQuestion = new StandardQuestion();
            standardQuestion.setQuestionText(questionDTO.getQuestionText());
            standardQuestion.setQuestionType(questionDTO.getQuestionType());
            standardQuestion.setDifficulty(questionDTO.getDifficulty());
            standardQuestion.setCreatedByUser(user);
            
            // 如果有原始问题ID，设置关联
            if (questionDTO.getOriginalRawQuestionId() != null) {
                RawQuestion rawQuestion = rawQuestionRepository.findById(questionDTO.getOriginalRawQuestionId())
                    .orElseThrow(() -> {
                        logger.error("创建标准问题失败 - 找不到原始问题ID: {}", questionDTO.getOriginalRawQuestionId());
                        return new IllegalArgumentException("找不到指定的原始问题（ID: " + questionDTO.getOriginalRawQuestionId() + "）");
                    });
                standardQuestion.setOriginalRawQuestion(rawQuestion);
            }
            
            // 如果有父标准问题ID，设置关联
            if (questionDTO.getParentStandardQuestionId() != null) {
                StandardQuestion parentQuestion = standardQuestionRepository.findById(questionDTO.getParentStandardQuestionId())
                    .orElseThrow(() -> {
                        logger.error("创建标准问题失败 - 找不到父标准问题ID: {}", questionDTO.getParentStandardQuestionId());
                        return new IllegalArgumentException("找不到指定的父标准问题（ID: " + questionDTO.getParentStandardQuestionId() + "）");
                    });
                standardQuestion.setParentStandardQuestion(parentQuestion);
            }
            
            // 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(questionDTO.getParentStandardQuestionId() != null ? 
                ChangeType.UPDATE_STANDARD_QUESTION : ChangeType.CREATE_STANDARD_QUESTION);
            changeLog.setChangedByUser(user);
            changeLog.setCommitMessage(questionDTO.getCommitMessage());
            
            // 先保存变更日志
            try {
                changeLog = changeLogRepository.save(changeLog);
            } catch (Exception e) {
                logger.error("创建标准问题失败 - 保存变更日志时出错", e);
                throw new RuntimeException("保存变更日志时出错: " + e.getMessage());
            }
            
            // 设置变更日志关联
            standardQuestion.setCreatedChangeLog(changeLog);
            
            // 保存标准问题
            try {
                standardQuestion = standardQuestionRepository.save(standardQuestion);
            } catch (Exception e) {
                logger.error("创建标准问题失败 - 保存标准问题时出错", e);
                throw new RuntimeException("保存标准问题时出错: " + e.getMessage());
            }
            
            // 处理标签关联
            if (questionDTO.getTags() != null && !questionDTO.getTags().isEmpty()) {
                processQuestionTags(standardQuestion, questionDTO.getTags(), user, changeLog);
            }
            
            // 更新变更日志的关联问题
            changeLog.setAssociatedStandardQuestion(standardQuestion);
            try {
                changeLogRepository.save(changeLog);
            } catch (Exception e) {
                logger.error("创建标准问题失败 - 更新变更日志关联时出错", e);
                throw new RuntimeException("更新变更日志关联时出错: " + e.getMessage());
            }
            
            // 现在标准问题已经有ID了，可以创建变更详情
            try {
                if (questionDTO.getParentStandardQuestionId() != null) {
                    StandardQuestion parentQuestion = standardQuestion.getParentStandardQuestion();
                    List<ChangeLogDetail> details = ChangeLogUtils.compareAndCreateDetails(
                        changeLog,
                        EntityType.STANDARD_QUESTION,
                        standardQuestion.getId(), // 使用新创建的问题ID
                        parentQuestion,
                        standardQuestion,
                        "questionText", "questionType", "difficulty"
                    );
                    
                    // 保存所有变更详情
                    for (ChangeLogDetail detail : details) {
                        changeLogDetailRepository.save(detail);
                    }
                } else {
                    // 如果是全新创建的问题，记录所有字段为新增
                    ChangeLogDetail textDetail = ChangeLogUtils.createDetail(
                        changeLog,
                        EntityType.STANDARD_QUESTION,
                        standardQuestion.getId(), // 使用新创建的问题ID
                        "questionText",
                        null,
                        standardQuestion.getQuestionText()
                    );
                    changeLogDetailRepository.save(textDetail);
                    
                    ChangeLogDetail typeDetail = ChangeLogUtils.createDetail(
                        changeLog,
                        EntityType.STANDARD_QUESTION,
                        standardQuestion.getId(), // 使用新创建的问题ID
                        "questionType",
                        null,
                        standardQuestion.getQuestionType()
                    );
                    changeLogDetailRepository.save(typeDetail);
                    
                    if (standardQuestion.getDifficulty() != null) {
                        ChangeLogDetail difficultyDetail = ChangeLogUtils.createDetail(
                            changeLog,
                            EntityType.STANDARD_QUESTION,
                            standardQuestion.getId(), // 使用新创建的问题ID
                            "difficulty",
                            null,
                            standardQuestion.getDifficulty()
                        );
                        changeLogDetailRepository.save(difficultyDetail);
                    }
                }
            } catch (Exception e) {
                logger.error("创建标准问题失败 - 保存变更详情时出错", e);
                throw new RuntimeException("保存变更详情时出错: " + e.getMessage());
            }
            
            logger.info("成功创建标准问题 - ID: {}, 用户ID: {}", standardQuestion.getId(), userId);
            return standardQuestion;
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("创建标准问题时发生未预期的错误", e);
            throw new RuntimeException("创建标准问题时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 处理标准问题的标签关联
     * @param standardQuestion 已保存的标准问题实体
     * @param tagNames 标签名称列表
     * @param user 当前用户
     * @param changeLog 变更日志
     */
    @Transactional
    protected void processQuestionTags(StandardQuestion standardQuestion, List<String> tagNames, 
                                      User user, ChangeLog changeLog) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        
        List<StandardQuestionTag> questionTags = new ArrayList<>();
        
        for (String tagName : tagNames) {
            if (!StringUtils.hasText(tagName)) {
                continue;
            }
            
            // 查找或创建标签
            Tag tag = tagRepository.findByTagName(tagName.trim())
                    .orElseGet(() -> {
                        Tag newTag = new Tag(tagName.trim());
                        newTag.setCreatedByUser(user);
                        newTag.setCreatedChangeLog(changeLog);
                        return tagRepository.save(newTag);
                    });
            
            // 如果该问题与标签的关联不存在，则创建关联
            if (!standardQuestionTagRepository.existsByStandardQuestionAndTag(standardQuestion, tag)) {
                StandardQuestionTag questionTag = new StandardQuestionTag(standardQuestion, tag, user);
                questionTag.setCreatedChangeLog(changeLog);
                questionTags.add(questionTag);
                standardQuestion.addTag(questionTag);
                
                // 记录变更日志详情
                ChangeLogDetail tagDetail = ChangeLogUtils.createDetail(
                    changeLog,
                    EntityType.STANDARD_QUESTION_TAGS,
                    standardQuestion.getId(),
                    "tag_id",
                    null,
                    tag.getId()
                );
                changeLogDetailRepository.save(tagDetail);
            }
        }
        
        if (!questionTags.isEmpty()) {
            standardQuestionTagRepository.saveAll(questionTags);
        }
    }
} 
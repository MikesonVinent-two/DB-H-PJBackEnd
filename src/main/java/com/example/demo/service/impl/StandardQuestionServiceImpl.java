package com.example.demo.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.dto.BatchTagOperationsDTO;
import com.example.demo.dto.BatchTagOperationsDTO.TagOperation;
import com.example.demo.dto.ChangeDetailDTO;
import com.example.demo.dto.QuestionHistoryDTO;
import com.example.demo.dto.StandardQuestionDTO;
import com.example.demo.dto.TagOperationDTO;
import com.example.demo.entity.jdbc.ChangeLog;
import com.example.demo.entity.jdbc.ChangeLogDetail;
import com.example.demo.entity.jdbc.ChangeType;
import com.example.demo.entity.jdbc.EntityType;
import com.example.demo.entity.jdbc.QuestionType;
import com.example.demo.entity.jdbc.RawAnswer;
import com.example.demo.entity.jdbc.RawQuestion;
import com.example.demo.entity.jdbc.StandardQuestion;
import com.example.demo.entity.jdbc.StandardQuestionTag;
import com.example.demo.entity.jdbc.Tag;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.ChangeLogDetailRepository;
import com.example.demo.repository.jdbc.ChangeLogRepository;
import com.example.demo.repository.jdbc.CrowdsourcedAnswerRepository;
import com.example.demo.repository.jdbc.ExpertCandidateAnswerRepository;
import com.example.demo.repository.jdbc.RawQuestionRepository;
import com.example.demo.repository.jdbc.StandardObjectiveAnswerRepository;
import com.example.demo.repository.jdbc.StandardQuestionRepository;
import com.example.demo.repository.jdbc.StandardQuestionTagRepository;
import com.example.demo.repository.jdbc.StandardSimpleAnswerRepository;
import com.example.demo.repository.jdbc.StandardSubjectiveAnswerRepository;
import com.example.demo.repository.jdbc.TagRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.StandardQuestionService;
import com.example.demo.util.ChangeLogUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class StandardQuestionServiceImpl implements StandardQuestionService {

    private static final Logger logger = LoggerFactory.getLogger(StandardQuestionServiceImpl.class);

    private final StandardQuestionRepository standardQuestionRepository;
    private final RawQuestionRepository rawQuestionRepository;
    private final UserRepository userRepository;
    private final ChangeLogRepository changeLogRepository;
    private final ChangeLogDetailRepository changeLogDetailRepository;
    private final TagRepository tagRepository;
    private final StandardQuestionTagRepository standardQuestionTagRepository;
    private final ObjectMapper objectMapper;
    private final CrowdsourcedAnswerRepository crowdsourcedAnswerRepository;
    private final ExpertCandidateAnswerRepository expertCandidateAnswerRepository;
    private final StandardObjectiveAnswerRepository standardObjectiveAnswerRepository;
    private final StandardSimpleAnswerRepository standardSimpleAnswerRepository;
    private final StandardSubjectiveAnswerRepository standardSubjectiveAnswerRepository;
    
    // 显式构造函数
    public StandardQuestionServiceImpl(
            StandardQuestionRepository standardQuestionRepository,
            RawQuestionRepository rawQuestionRepository,
            UserRepository userRepository,
            ChangeLogRepository changeLogRepository,
            ChangeLogDetailRepository changeLogDetailRepository,
            TagRepository tagRepository,
            StandardQuestionTagRepository standardQuestionTagRepository,
            ObjectMapper objectMapper,
            CrowdsourcedAnswerRepository crowdsourcedAnswerRepository,
            ExpertCandidateAnswerRepository expertCandidateAnswerRepository,
            StandardObjectiveAnswerRepository standardObjectiveAnswerRepository,
            StandardSimpleAnswerRepository standardSimpleAnswerRepository,
            StandardSubjectiveAnswerRepository standardSubjectiveAnswerRepository) {
        this.standardQuestionRepository = standardQuestionRepository;
        this.rawQuestionRepository = rawQuestionRepository;
        this.userRepository = userRepository;
        this.changeLogRepository = changeLogRepository;
        this.changeLogDetailRepository = changeLogDetailRepository;
        this.tagRepository = tagRepository;
        this.standardQuestionTagRepository = standardQuestionTagRepository;
        this.objectMapper = objectMapper;
        this.crowdsourcedAnswerRepository = crowdsourcedAnswerRepository;
        this.expertCandidateAnswerRepository = expertCandidateAnswerRepository;
        this.standardObjectiveAnswerRepository = standardObjectiveAnswerRepository;
        this.standardSimpleAnswerRepository = standardSimpleAnswerRepository;
        this.standardSubjectiveAnswerRepository = standardSubjectiveAnswerRepository;
    }

    @Override
    @Transactional
    public StandardQuestionDTO createStandardQuestion(StandardQuestionDTO questionDTO, Long userId) {
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
            
            // 如果指定了父问题，则需要先查询
            if (questionDTO.getParentStandardQuestionId() != null) {
                StandardQuestion parentQuestion = standardQuestionRepository.findById(questionDTO.getParentStandardQuestionId())
                    .orElseThrow(() -> {
                        logger.error("创建标准问题失败 - 找不到父标准问题ID: {}", questionDTO.getParentStandardQuestionId());
                        return new IllegalArgumentException("找不到指定的父标准问题（ID: " + questionDTO.getParentStandardQuestionId() + "）");
                    });
                standardQuestion.setParentStandardQuestion(parentQuestion);
            }
            
            // 先保存标准问题，获取ID
            try {
                standardQuestion = standardQuestionRepository.save(standardQuestion);
                logger.debug("已保存标准问题 - ID: {}", standardQuestion.getId());
            } catch (Exception e) {
                logger.error("创建标准问题失败 - 保存标准问题时出错", e);
                throw new RuntimeException("保存标准问题时出错: " + e.getMessage());
            }
            
            // 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(questionDTO.getParentStandardQuestionId() != null ? 
                ChangeType.UPDATE_STANDARD_QUESTION : ChangeType.CREATE_STANDARD_QUESTION);
            changeLog.setUser(user);
            changeLog.setCommitMessage(questionDTO.getCommitMessage());
            // 设置关联的标准问题（此时标准问题已有ID）
            changeLog.setAssociatedStandardQuestion(standardQuestion);
            
            // 保存变更日志
            try {
                changeLog = changeLogRepository.save(changeLog);
            } catch (Exception e) {
                logger.error("创建标准问题失败 - 保存变更日志时出错", e);
                throw new RuntimeException("保存变更日志时出错: " + e.getMessage());
            }
            
            // 设置变更日志关联并更新标准问题
            standardQuestion.setCreatedChangeLog(changeLog);
            try {
                standardQuestion = standardQuestionRepository.save(standardQuestion);
            } catch (Exception e) {
                logger.error("创建标准问题失败 - 更新标准问题变更日志关联时出错", e);
                throw new RuntimeException("更新标准问题变更日志关联时出错: " + e.getMessage());
            }
            
            // 处理标签关联
            if (questionDTO.getTags() != null && !questionDTO.getTags().isEmpty()) {
                processQuestionTags(standardQuestion, questionDTO.getTags(), user, changeLog);
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
            return convertToDTO(standardQuestion);
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("创建标准问题时发生未预期的错误", e);
            throw new RuntimeException("创建标准问题时发生错误: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public StandardQuestionDTO updateStandardQuestion(Long questionId, StandardQuestionDTO questionDTO, Long userId) {
        logger.debug("开始修改标准问题 - 问题ID: {}, 用户ID: {}", questionId, userId);
        
        // 验证参数
        if (questionId == null || userId == null) {
            logger.error("修改标准问题失败 - 问题ID或用户ID为空");
            throw new IllegalArgumentException("问题ID和用户ID不能为空");
        }

        try {
            // 获取原问题
            StandardQuestion originalQuestion = standardQuestionRepository.findById(questionId)
                .orElseThrow(() -> {
                    logger.error("修改标准问题失败 - 找不到问题ID: {}", questionId);
                    return new IllegalArgumentException("找不到指定的标准问题（ID: " + questionId + "）");
                });

            // 获取用户信息
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("修改标准问题失败 - 找不到用户ID: {}", userId);
                    return new IllegalArgumentException("找不到指定的用户（ID: " + userId + "）");
                });

            // 创建新版本
            StandardQuestion newVersion = new StandardQuestion();
            newVersion.setQuestionText(questionDTO.getQuestionText());
            newVersion.setQuestionType(questionDTO.getQuestionType());
            newVersion.setDifficulty(questionDTO.getDifficulty());
            newVersion.setCreatedByUser(user);
            newVersion.setParentStandardQuestion(originalQuestion);
            newVersion.setOriginalRawQuestion(originalQuestion.getOriginalRawQuestion());

            // 先保存新版本标准问题
            try {
                newVersion = standardQuestionRepository.save(newVersion);
                logger.debug("已保存标准问题新版本 - ID: {}", newVersion.getId());
            } catch (Exception e) {
                logger.error("修改标准问题失败 - 保存新版本时出错", e);
                throw new RuntimeException("保存新版本时出错: " + e.getMessage());
            }

            // 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(ChangeType.UPDATE_STANDARD_QUESTION);
            changeLog.setUser(user);
            changeLog.setCommitMessage(questionDTO.getCommitMessage());
            // 设置关联的标准问题（此时标准问题已有ID）
            changeLog.setAssociatedStandardQuestion(newVersion);
            
            // 保存变更日志
            try {
                changeLog = changeLogRepository.save(changeLog);
            } catch (Exception e) {
                logger.error("修改标准问题失败 - 保存变更日志时出错", e);
                throw new RuntimeException("保存变更日志时出错: " + e.getMessage());
            }

            // 设置变更日志关联并更新标准问题
            newVersion.setCreatedChangeLog(changeLog);
            try {
                newVersion = standardQuestionRepository.save(newVersion);
            } catch (Exception e) {
                logger.error("修改标准问题失败 - 更新标准问题变更日志关联时出错", e);
                throw new RuntimeException("更新标准问题变更日志关联时出错: " + e.getMessage());
            }

            // 处理标签关联
            if (questionDTO.getTags() != null && !questionDTO.getTags().isEmpty()) {
                processQuestionTags(newVersion, questionDTO.getTags(), user, changeLog);
            }

            // 创建变更详情
            List<ChangeLogDetail> details = ChangeLogUtils.compareAndCreateDetails(
                changeLog,
                EntityType.STANDARD_QUESTION,
                newVersion.getId(),
                originalQuestion,
                newVersion,
                "questionText", "questionType", "difficulty"
            );

            // 保存所有变更详情
            for (ChangeLogDetail detail : details) {
                changeLogDetailRepository.save(detail);
            }

            logger.info("成功修改标准问题 - 原问题ID: {}, 新版本ID: {}, 用户ID: {}", 
                questionId, newVersion.getId(), userId);
            return convertToDTO(newVersion);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("修改标准问题时发生未预期的错误", e);
            throw new RuntimeException("修改标准问题时发生错误: " + e.getMessage());
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

    private StandardQuestionDTO convertToDTO(StandardQuestion question) {
        StandardQuestionDTO dto = new StandardQuestionDTO();
        dto.setId(question.getId());
        dto.setQuestionText(question.getQuestionText());
        dto.setQuestionType(question.getQuestionType());
        dto.setDifficulty(question.getDifficulty());
        dto.setUserId(question.getCreatedByUser().getId());
        
        if (question.getParentStandardQuestion() != null) {
            dto.setParentStandardQuestionId(question.getParentStandardQuestion().getId());
        }
        
        if (question.getOriginalRawQuestion() != null) {
            dto.setOriginalRawQuestionId(question.getOriginalRawQuestion().getId());
        }
        
        List<String> tags = question.getQuestionTags().stream()
            .map(tag -> tag.getTag().getTagName())
            .collect(Collectors.toList());
        dto.setTags(tags);
        
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionHistoryDTO> getQuestionHistory(Long questionId) {
        logger.debug("开始获取问题修改历史 - 问题ID: {}", questionId);
        
        StandardQuestion currentQuestion = standardQuestionRepository.findById(questionId)
            .orElseThrow(() -> new IllegalArgumentException("问题不存在"));

        List<QuestionHistoryDTO> history = new ArrayList<>();
        collectQuestionHistory(currentQuestion, history);
        
        history.sort((a, b) -> b.getCreationTime().compareTo(a.getCreationTime()));
        
        logger.debug("成功获取问题修改历史 - 问题ID: {}, 版本数量: {}", questionId, history.size());
        return history;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionHistoryDTO> getVersionTree(Long questionId) {
        logger.debug("开始获取问题版本树 - 问题ID: {}", questionId);
        
        StandardQuestion currentQuestion = standardQuestionRepository.findById(questionId)
            .orElseThrow(() -> new IllegalArgumentException("问题不存在"));

        StandardQuestion rootQuestion = findRootQuestion(currentQuestion);
        List<QuestionHistoryDTO> versionTree = new ArrayList<>();
        buildVersionTree(rootQuestion, versionTree);
        
        logger.debug("成功获取问题版本树 - 问题ID: {}", questionId);
        return versionTree;
    }

    private void collectQuestionHistory(StandardQuestion question, List<QuestionHistoryDTO> history) {
        if (question == null) {
            return;
        }
        
        QuestionHistoryDTO historyDTO = convertToHistoryDTO(question);
        history.add(historyDTO);
        
        if (question.getParentStandardQuestion() != null) {
            collectQuestionHistory(question.getParentStandardQuestion(), history);
        }
    }

    private StandardQuestion findRootQuestion(StandardQuestion question) {
        StandardQuestion current = question;
        while (current.getParentStandardQuestion() != null) {
            current = current.getParentStandardQuestion();
        }
        return current;
    }

    private void buildVersionTree(StandardQuestion question, List<QuestionHistoryDTO> versionTree) {
        if (question == null) {
            return;
        }

        QuestionHistoryDTO node = convertToHistoryDTO(question);
        versionTree.add(node);

        List<StandardQuestion> children = standardQuestionRepository.findByParentStandardQuestionId(question.getId());
        for (StandardQuestion child : children) {
            buildVersionTree(child, versionTree);
        }
    }

    private QuestionHistoryDTO convertToHistoryDTO(StandardQuestion question) {
        QuestionHistoryDTO dto = new QuestionHistoryDTO();
        dto.setId(question.getId());
        dto.setQuestionText(question.getQuestionText());
        dto.setQuestionType(question.getQuestionType().toString());
        dto.setDifficulty(question.getDifficulty().toString());
        dto.setCreationTime(question.getCreationTime());
        dto.setCreatedByUserId(question.getCreatedByUser().getId());
        
        // 添加用户详细信息
        User createdByUser = question.getCreatedByUser();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", createdByUser.getId());
        userInfo.put("username", createdByUser.getUsername());
        userInfo.put("name", createdByUser.getName());
        userInfo.put("role", createdByUser.getRole());
        userInfo.put("contactInfo", createdByUser.getContactInfo());
        dto.setCreatedByUser(userInfo);
        
        if (question.getParentStandardQuestion() != null) {
            dto.setParentQuestionId(question.getParentStandardQuestion().getId());
        }
        
        List<String> tags = question.getQuestionTags().stream()
            .map(tag -> tag.getTag().getTagName())
            .collect(Collectors.toList());
        dto.setTags(tags);
        
        // 获取变更日志
        ChangeLog changeLog = changeLogRepository.findByAssociatedStandardQuestionId(question.getId());
        if (changeLog != null) {
            dto.setCommitMessage(changeLog.getCommitMessage());
            
            // 获取变更详情
            List<ChangeDetailDTO> changes = changeLogDetailRepository.findByChangeLogId(changeLog.getId())
                .stream()
                .map(this::convertToChangeDetailDTO)
                .collect(Collectors.toList());
            dto.setChanges(changes);
        }
        
        return dto;
    }

    private ChangeDetailDTO convertToChangeDetailDTO(ChangeLogDetail detail) {
        ChangeDetailDTO dto = new ChangeDetailDTO();
        dto.setAttributeName(detail.getAttributeName());
        dto.setOldValue(detail.getOldValue() != null ? detail.getOldValue().toString() : null);
        dto.setNewValue(detail.getNewValue() != null ? detail.getNewValue().toString() : null);
        
        if (detail.getOldValue() == null && detail.getNewValue() != null) {
            dto.setChangeType("ADD");
        } else if (detail.getOldValue() != null && detail.getNewValue() == null) {
            dto.setChangeType("DELETE");
        } else {
            dto.setChangeType("MODIFY");
        }
        
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StandardQuestionDTO> findAllStandardQuestions(Pageable pageable) {
        logger.debug("获取所有标准问题 - 页码: {}, 每页大小: {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<StandardQuestion> standardQuestions = standardQuestionRepository.findAll(pageable);
            return standardQuestions.map(this::convertToDTO);
        } catch (Exception e) {
            logger.error("获取所有标准问题失败", e);
            throw new RuntimeException("获取标准问题列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StandardQuestionDTO> findLatestStandardQuestions(Pageable pageable) {
        logger.debug("获取所有最新版本的标准问题 - 页码: {}, 每页大小: {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<StandardQuestion> latestQuestions = standardQuestionRepository.findLatestVersions(pageable);
            logger.info("成功获取最新版本标准问题 - 总数: {}", latestQuestions.getTotalElements());
            return latestQuestions.map(this::convertToDTO);
        } catch (Exception e) {
            logger.error("获取最新版本标准问题失败", e);
            throw new RuntimeException("获取最新版本标准问题列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public StandardQuestionDTO updateQuestionTags(TagOperationDTO operationDTO) {
        logger.debug("开始更新标准问题标签 - 问题ID: {}, 操作类型: {}", 
            operationDTO.getQuestionId(), operationDTO.getOperationType());
        
        if (operationDTO.getQuestionId() == null || operationDTO.getUserId() == null) {
            throw new IllegalArgumentException("问题ID和用户ID不能为空");
        }
        
        try {
            // 获取标准问题
            StandardQuestion question = standardQuestionRepository.findById(operationDTO.getQuestionId())
                .orElseThrow(() -> {
                    logger.error("更新标签失败 - 找不到问题ID: {}", operationDTO.getQuestionId());
                    return new IllegalArgumentException("找不到指定的标准问题");
                });
            
            // 获取用户
            User user = userRepository.findById(operationDTO.getUserId())
                .orElseThrow(() -> {
                    logger.error("更新标签失败 - 找不到用户ID: {}", operationDTO.getUserId());
                    return new IllegalArgumentException("找不到指定的用户");
                });
            
            // 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(ChangeType.UPDATE_STANDARD_QUESTION_TAGS);
            changeLog.setUser(user);
            changeLog.setCommitMessage(operationDTO.getCommitMessage());
            changeLog.setAssociatedStandardQuestion(question);
            changeLogRepository.save(changeLog);
            
            // 获取当前标签
            Set<String> currentTags = question.getQuestionTags().stream()
                .map(tag -> tag.getTag().getTagName())
                .collect(Collectors.toSet());
            
            // 新标签集合
            Set<String> newTags = new HashSet<>();
            
            // 根据操作类型处理标签
            switch (operationDTO.getOperationType()) {
                case ADD:
                    // 添加标签
                    if (operationDTO.getTags() != null) {
                        newTags.addAll(currentTags);  // 保留现有标签
                        newTags.addAll(operationDTO.getTags());  // 添加新标签
                    }
                    break;
                    
                case REMOVE:
                    // 移除标签
                    if (operationDTO.getTags() != null) {
                        newTags.addAll(currentTags);  // 复制现有标签
                        newTags.removeAll(operationDTO.getTags());  // 移除指定标签
                    }
                    break;
                    
                case REPLACE:
                    // 替换所有标签
                    if (operationDTO.getTags() != null) {
                        newTags.addAll(operationDTO.getTags());  // 使用新标签集合
                    }
                    break;
                    
                default:
                    throw new IllegalArgumentException("不支持的操作类型: " + operationDTO.getOperationType());
            }
            
            // 清除现有标签关联
            List<StandardQuestionTag> existingTags = new ArrayList<>(question.getQuestionTags());
            for (StandardQuestionTag tagLink : existingTags) {
                question.removeTag(tagLink);
                standardQuestionTagRepository.delete(tagLink);
                
                // 记录变更日志详情
                ChangeLogDetail removeDetail = ChangeLogUtils.createDetail(
                    changeLog,
                    EntityType.STANDARD_QUESTION_TAGS,
                    question.getId(),
                    "tag_id",
                    tagLink.getTag().getId(),
                    null
                );
                changeLogDetailRepository.save(removeDetail);
            }
            
            // 添加新标签
            for (String tagName : newTags) {
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
                
                // 创建关联
                StandardQuestionTag questionTag = new StandardQuestionTag(question, tag, user);
                questionTag.setCreatedChangeLog(changeLog);
                standardQuestionTagRepository.save(questionTag);
                question.addTag(questionTag);
                
                // 记录变更日志详情
                ChangeLogDetail addDetail = ChangeLogUtils.createDetail(
                    changeLog,
                    EntityType.STANDARD_QUESTION_TAGS,
                    question.getId(),
                    "tag_id",
                    null,
                    tag.getId()
                );
                changeLogDetailRepository.save(addDetail);
            }
            
            // 保存问题
            question = standardQuestionRepository.save(question);
            
            logger.info("成功更新标准问题标签 - 问题ID: {}, 标签数量: {}", 
                question.getId(), newTags.size());
            
            return convertToDTO(question);
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("更新标准问题标签时发生错误", e);
            throw new RuntimeException("更新标准问题标签时发生错误: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<Long, Boolean> batchUpdateQuestionTags(BatchTagOperationsDTO batchOperationsDTO) {
        logger.debug("开始批量更新标准问题标签 - 操作数量: {}", 
            batchOperationsDTO.getOperations() != null ? batchOperationsDTO.getOperations().size() : 0);
        
        if (batchOperationsDTO.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        if (batchOperationsDTO.getOperations() == null || batchOperationsDTO.getOperations().isEmpty()) {
            throw new IllegalArgumentException("操作列表不能为空");
        }
        
        // 操作结果
        Map<Long, Boolean> results = new HashMap<>();
        
        try {
            // 获取用户
            User user = userRepository.findById(batchOperationsDTO.getUserId())
                .orElseThrow(() -> {
                    logger.error("批量更新标签失败 - 找不到用户ID: {}", batchOperationsDTO.getUserId());
                    return new IllegalArgumentException("找不到指定的用户");
                });
            
            // 逐个处理操作
            for (TagOperation operation : batchOperationsDTO.getOperations()) {
                try {
                    // 构建单个操作DTO
                    TagOperationDTO singleOperation = new TagOperationDTO();
                    singleOperation.setQuestionId(operation.getQuestionId());
                    singleOperation.setUserId(batchOperationsDTO.getUserId());
                    singleOperation.setOperationType(convertOperationType(operation.getOperationType()));
                    singleOperation.setTags(operation.getTags());
                    singleOperation.setCommitMessage(batchOperationsDTO.getCommitMessage());
                    
                    // 执行单个操作
                    updateQuestionTags(singleOperation);
                    results.put(operation.getQuestionId(), true);
                } catch (Exception e) {
                    logger.error("批量更新标签时处理问题ID: {} 失败", operation.getQuestionId(), e);
                    results.put(operation.getQuestionId(), false);
                }
            }
            
            logger.info("批量更新标准问题标签完成 - 总数: {}, 成功: {}", 
                results.size(), results.values().stream().filter(v -> v).count());
            
            return results;
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("批量更新标准问题标签时发生错误", e);
            throw new RuntimeException("批量更新标准问题标签时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 转换操作类型
     */
    private TagOperationDTO.OperationType convertOperationType(BatchTagOperationsDTO.TagOperation.OperationType type) {
        switch (type) {
            case ADD:
                return TagOperationDTO.OperationType.ADD;
            case REMOVE:
                return TagOperationDTO.OperationType.REMOVE;
            case REPLACE:
                return TagOperationDTO.OperationType.REPLACE;
            default:
                throw new IllegalArgumentException("不支持的操作类型: " + type);
        }
    }

    @Override
    public Map<String, Object> searchQuestions(List<String> tags, String keyword, Long userId, Pageable pageable) {
        logger.info("搜索标准问题 - 标签: {}, 关键词: {}, 用户ID: {}", tags, keyword, userId);
        
        // 1. 根据条件获取问题列表
        List<StandardQuestion> questions = new ArrayList<>();
        
        // 如果同时有标签和关键词，先按标签过滤，再按关键词过滤
        if (tags != null && !tags.isEmpty() && keyword != null && !keyword.trim().isEmpty()) {
            // 获取包含所有指定标签的问题
            List<StandardQuestion> tagFilteredQuestions = getQuestionsByTags(tags);
            
            // 在标签过滤结果中进一步按关键词过滤
            for (StandardQuestion question : tagFilteredQuestions) {
                if (question.getQuestionText().toLowerCase().contains(keyword.toLowerCase())) {
                    questions.add(question);
                }
            }
        }
        // 只有标签
        else if (tags != null && !tags.isEmpty()) {
            questions = getQuestionsByTags(tags);
        }
        // 只有关键词
        else if (keyword != null && !keyword.trim().isEmpty()) {
            questions = standardQuestionRepository.findByQuestionTextContaining("%" + keyword + "%");
        }
        // 没有搜索条件，返回所有问题（而不仅是最新版本）
        else {
            questions = standardQuestionRepository.findAll();
        }
        
        // 去重处理，确保每个问题ID只出现一次
        Map<Long, StandardQuestion> uniqueQuestionsMap = new HashMap<>();
        for (StandardQuestion question : questions) {
            uniqueQuestionsMap.put(question.getId(), question);
        }
        questions = new ArrayList<>(uniqueQuestionsMap.values());
        
        // 2. 分页处理
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), questions.size());
        
        List<StandardQuestion> pagedQuestions;
        if (start <= end) {
            pagedQuestions = questions.subList(start, end);
        } else {
            pagedQuestions = new ArrayList<>();
        }
        
        // 3. 转换为DTO并添加额外信息
        List<Map<String, Object>> questionDTOs = new ArrayList<>();
        
        for (StandardQuestion question : pagedQuestions) {
            Map<String, Object> questionDTO = new HashMap<>();
            
            // 基本信息
            questionDTO.put("id", question.getId());
            questionDTO.put("questionText", question.getQuestionText());
            questionDTO.put("questionType", question.getQuestionType());
            questionDTO.put("difficulty", question.getDifficulty());
            questionDTO.put("creationTime", question.getCreationTime());
            
            // 标签信息
            List<String> questionTags = question.getQuestionTags().stream()
                    .map(tag -> tag.getTag().getTagName())
                    .collect(Collectors.toList());
            questionDTO.put("tags", questionTags);
            
            // 检查是否有标准回答
            boolean hasStandardAnswer = false;
            
            if (question.getQuestionType() == QuestionType.SINGLE_CHOICE || question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                hasStandardAnswer = standardObjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
            } else if (question.getQuestionType() == QuestionType.SIMPLE_FACT) {
                hasStandardAnswer = standardSimpleAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
            } else if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
                hasStandardAnswer = standardSubjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
            }
            
            questionDTO.put("hasStandardAnswer", hasStandardAnswer);
            
            // 如果提供了用户ID，检查用户是否已回答
            if (userId != null) {
                // 检查是否有众包回答
                boolean hasCrowdsourcedAnswer = crowdsourcedAnswerRepository.existsByStandardQuestionIdAndUserIdAndTaskBatchId(
                        question.getId(), userId, null);
                questionDTO.put("hasCrowdsourcedAnswer", hasCrowdsourcedAnswer);
                
                // 检查是否有专家回答
                boolean hasExpertAnswer = expertCandidateAnswerRepository.findByStandardQuestionIdAndUserId(
                        question.getId(), userId).isPresent();
                questionDTO.put("hasExpertAnswer", hasExpertAnswer);
            } else {
                questionDTO.put("hasCrowdsourcedAnswer", false);
                questionDTO.put("hasExpertAnswer", false);
            }
            
            questionDTOs.add(questionDTO);
        }
        
        // 4. 构建响应
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("questions", questionDTOs);
        result.put("total", questions.size());
        result.put("page", pageable.getPageNumber());
        result.put("size", pageable.getPageSize());
        result.put("totalPages", (int) Math.ceil((double) questions.size() / pageable.getPageSize()));
        
        return result;
    }
    
    /**
     * 根据标签列表获取包含所有指定标签的问题
     * 
     * @param tags 标签列表
     * @return 包含所有指定标签的问题列表
     */
    private List<StandardQuestion> getQuestionsByTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取所有标准问题
        List<StandardQuestion> allQuestions = standardQuestionRepository.findAll();
        
        // 过滤出包含所有指定标签的问题
        return allQuestions.stream()
                .filter(question -> {
                    List<String> questionTagNames = question.getTags().stream()
                            .map(tag -> tag.getTagName().toLowerCase())
                            .collect(Collectors.toList());
                    
                    // 检查问题是否包含所有指定标签
                    return tags.stream()
                            .map(String::toLowerCase)
                            .allMatch(tag -> questionTagNames.contains(tag));
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getOriginalQuestionAndAnswers(Long questionId, Pageable pageable) {
        logger.debug("开始获取原始问题和回答 - 标准问题ID: {}, 页码: {}, 每页大小: {}", 
            questionId, pageable.getPageNumber(), pageable.getPageSize());
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取标准问题
            StandardQuestion standardQuestion = standardQuestionRepository.findById(questionId)
                .orElseThrow(() -> {
                    logger.error("获取原始问题和回答失败 - 找不到标准问题ID: {}", questionId);
                    return new IllegalArgumentException("找不到指定的标准问题（ID: " + questionId + "）");
                });
            
            // 检查是否有关联的原始问题
            if (standardQuestion.getOriginalRawQuestion() == null) {
                logger.warn("标准问题没有关联的原始问题 - 标准问题ID: {}", questionId);
                return result;
            }
            
            // 获取原始问题
            RawQuestion rawQuestion = standardQuestion.getOriginalRawQuestion();
            
            // 转换原始问题为DTO
            Map<String, Object> rawQuestionDTO = new HashMap<>();
            rawQuestionDTO.put("id", rawQuestion.getId());
            rawQuestionDTO.put("questionText", rawQuestion.getTitle());
            rawQuestionDTO.put("source", rawQuestion.getSourceSite());
            rawQuestionDTO.put("collectionTime", rawQuestion.getCrawlTime());
            
            // 获取原始回答列表
            List<RawAnswer> rawAnswers = rawQuestionRepository.findRawAnswersByQuestionId(rawQuestion.getId());
            
            // 分页处理回答列表
            int totalAnswers = rawAnswers.size();
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), totalAnswers);
            
            List<RawAnswer> pagedAnswers;
            if (start <= end && totalAnswers > 0) {
                pagedAnswers = rawAnswers.subList(start, end);
            } else {
                pagedAnswers = new ArrayList<>();
            }
            
            List<Map<String, Object>> rawAnswersDTO = new ArrayList<>();
            for (RawAnswer answer : pagedAnswers) {
                Map<String, Object> answerDTO = new HashMap<>();
                answerDTO.put("id", answer.getId());
                answerDTO.put("answerText", answer.getContent());
                answerDTO.put("respondent", answer.getAuthorInfo());
                answerDTO.put("answerTime", answer.getPublishTime());
                answerDTO.put("score", answer.getUpvotes());
                
                rawAnswersDTO.add(answerDTO);
            }
            
            // 构建结果
            result.put("standardQuestion", convertToDTO(standardQuestion));
            result.put("rawQuestion", rawQuestionDTO);
            result.put("rawAnswers", rawAnswersDTO);
            result.put("total", totalAnswers);
            result.put("page", pageable.getPageNumber());
            result.put("size", pageable.getPageSize());
            result.put("totalPages", (int) Math.ceil((double) totalAnswers / pageable.getPageSize()));
            
            logger.info("成功获取原始问题和回答 - 标准问题ID: {}, 原始问题ID: {}, 原始回答总数: {}, 当前页回答数: {}", 
                questionId, rawQuestion.getId(), totalAnswers, rawAnswersDTO.size());
            
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("获取原始问题和回答时发生错误", e);
            throw new RuntimeException("获取原始问题和回答时发生错误: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> findQuestionsWithoutStandardAnswers(Pageable pageable) {
        logger.debug("开始查找无标准回答的问题 - 页码: {}, 每页大小: {}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            // 获取所有标准问题
            List<StandardQuestion> allQuestions = standardQuestionRepository.findAll();
            
            // 过滤出没有标准回答的问题
            List<StandardQuestion> questionsWithoutAnswers = allQuestions.stream()
                .filter(question -> {
                    // 检查不同类型问题的标准回答
                    if (question.getQuestionType() == QuestionType.SINGLE_CHOICE || 
                        question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                        return !standardObjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                    } else if (question.getQuestionType() == QuestionType.SIMPLE_FACT) {
                        return !standardSimpleAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                    } else if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
                        return !standardSubjectiveAnswerRepository.findByStandardQuestionId(question.getId()).isPresent();
                    }
                    return true;  // 未知类型，默认视为没有回答
                })
                .collect(Collectors.toList());
            
            // 去重处理
            Map<Long, StandardQuestion> uniqueQuestionsMap = new HashMap<>();
            for (StandardQuestion question : questionsWithoutAnswers) {
                uniqueQuestionsMap.put(question.getId(), question);
            }
            questionsWithoutAnswers = new ArrayList<>(uniqueQuestionsMap.values());
            
            // 分页处理
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), questionsWithoutAnswers.size());
            
            List<StandardQuestion> pagedQuestions;
            if (start <= end) {
                pagedQuestions = questionsWithoutAnswers.subList(start, end);
            } else {
                pagedQuestions = new ArrayList<>();
            }
            
            // 转换为DTO
            List<Map<String, Object>> questionDTOs = pagedQuestions.stream()
                .map(this::convertToDTOWithTags)
                .collect(Collectors.toList());
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("questions", questionDTOs);
            result.put("total", questionsWithoutAnswers.size());
            result.put("page", pageable.getPageNumber());
            result.put("size", pageable.getPageSize());
            result.put("totalPages", (int) Math.ceil((double) questionsWithoutAnswers.size() / pageable.getPageSize()));
            
            logger.info("成功获取无标准回答问题 - 总数: {}, 当前页: {}", 
                questionsWithoutAnswers.size(), questionDTOs.size());
            
            return result;
        } catch (Exception e) {
            logger.error("查找无标准回答问题时发生错误", e);
            throw new RuntimeException("查找无标准回答问题时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 转换标准问题为DTO，包含标签信息
     */
    private Map<String, Object> convertToDTOWithTags(StandardQuestion question) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", question.getId());
        dto.put("questionText", question.getQuestionText());
        dto.put("questionType", question.getQuestionType());
        dto.put("difficulty", question.getDifficulty());
        dto.put("creationTime", question.getCreationTime());
        
        if (question.getCreatedByUser() != null) {
            dto.put("createdByUserId", question.getCreatedByUser().getId());
        }
        
        if (question.getParentStandardQuestion() != null) {
            dto.put("parentQuestionId", question.getParentStandardQuestion().getId());
        }
        
        if (question.getOriginalRawQuestion() != null) {
            dto.put("originalRawQuestionId", question.getOriginalRawQuestion().getId());
        }
        
        // 添加标签信息
        List<String> tags = question.getQuestionTags().stream()
            .map(tag -> tag.getTag().getTagName())
            .collect(Collectors.toList());
        dto.put("tags", tags);
        
        return dto;
    }
} 
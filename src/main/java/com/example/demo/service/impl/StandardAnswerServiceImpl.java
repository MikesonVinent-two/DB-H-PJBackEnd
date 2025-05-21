package com.example.demo.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.StandardAnswerDTO;
import com.example.demo.entity.ChangeLog;
import com.example.demo.entity.ChangeType;
import com.example.demo.entity.ChangeLogDetail;
import com.example.demo.entity.EntityType;
import com.example.demo.entity.QuestionType;
import com.example.demo.entity.StandardObjectiveAnswer;
import com.example.demo.entity.StandardQuestion;
import com.example.demo.entity.StandardSimpleAnswer;
import com.example.demo.entity.StandardSubjectiveAnswer;
import com.example.demo.entity.User;
import com.example.demo.repository.ChangeLogDetailRepository;
import com.example.demo.repository.ChangeLogRepository;
import com.example.demo.repository.StandardObjectiveAnswerRepository;
import com.example.demo.repository.StandardQuestionRepository;
import com.example.demo.repository.StandardSimpleAnswerRepository;
import com.example.demo.repository.StandardSubjectiveAnswerRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.StandardAnswerService;
import com.example.demo.util.ChangeLogUtils;

@Service
public class StandardAnswerServiceImpl implements StandardAnswerService {
    
    private static final Logger logger = LoggerFactory.getLogger(StandardAnswerServiceImpl.class);
    
    @Autowired
    private StandardQuestionRepository standardQuestionRepository;
    
    @Autowired
    private StandardObjectiveAnswerRepository objectiveAnswerRepository;
    
    @Autowired
    private StandardSimpleAnswerRepository simpleAnswerRepository;
    
    @Autowired
    private StandardSubjectiveAnswerRepository subjectiveAnswerRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChangeLogRepository changeLogRepository;
    
    @Autowired
    private ChangeLogDetailRepository changeLogDetailRepository;
    
    @Override
    @Transactional
    public Object createOrUpdateStandardAnswer(StandardAnswerDTO answerDTO, Long userId) {
        logger.debug("开始创建/更新标准答案 - 标准问题ID: {}, 用户ID: {}", answerDTO.getStandardQuestionId(), userId);
        
        // 验证基本参数
        if (answerDTO.getStandardQuestionId() == null || userId == null || answerDTO.getQuestionType() == null) {
            logger.error("创建/更新标准答案失败 - 标准问题ID、用户ID或问题类型为空");
            throw new IllegalArgumentException("标准问题ID、用户ID和问题类型不能为空");
        }
        
        // 验证答案文本
        if (answerDTO.getAnswerText() == null || answerDTO.getAnswerText().trim().isEmpty()) {
            logger.error("创建/更新标准答案失败 - 答案文本为空");
            throw new IllegalArgumentException("答案文本不能为空");
        }
        
        try {
            // 获取用户信息
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("创建/更新标准答案失败 - 找不到用户ID: {}", userId);
                    return new IllegalArgumentException("找不到指定的用户（ID: " + userId + "）");
                });
            
            // 获取标准问题
            StandardQuestion standardQuestion = standardQuestionRepository.findById(answerDTO.getStandardQuestionId())
                .orElseThrow(() -> {
                    logger.error("创建/更新标准答案失败 - 找不到标准问题ID: {}", answerDTO.getStandardQuestionId());
                    return new IllegalArgumentException("找不到指定的标准问题（ID: " + answerDTO.getStandardQuestionId() + "）");
                });
            
            // 验证问题类型是否匹配
            if (standardQuestion.getQuestionType() != answerDTO.getQuestionType()) {
                logger.error("创建/更新标准答案失败 - 问题类型不匹配，期望: {}, 实际: {}", 
                    standardQuestion.getQuestionType(), answerDTO.getQuestionType());
                throw new IllegalArgumentException("问题类型不匹配");
            }
            
            // 根据问题类型验证特定字段
            validateAnswerFields(answerDTO);
            
            // 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(ChangeType.UPDATE_STANDARD_ANSWER);
            changeLog.setChangedByUser(user);
            changeLog.setCommitMessage(answerDTO.getCommitMessage());
            changeLog.setAssociatedStandardQuestion(standardQuestion);
            changeLog = changeLogRepository.save(changeLog);
            
            // 根据问题类型创建或更新相应的答案
            Object result = null;
            switch (answerDTO.getQuestionType()) {
                case SINGLE_CHOICE:
                case MULTIPLE_CHOICE:
                    result = createOrUpdateObjectiveAnswer(standardQuestion, answerDTO, user, changeLog);
                    break;
                case SIMPLE_FACT:
                    result = createOrUpdateSimpleAnswer(standardQuestion, answerDTO, user, changeLog);
                    break;
                case SUBJECTIVE:
                    result = createOrUpdateSubjectiveAnswer(standardQuestion, answerDTO, user, changeLog);
                    break;
                default:
                    logger.error("创建/更新标准答案失败 - 不支持的问题类型: {}", answerDTO.getQuestionType());
                    throw new IllegalArgumentException("不支持的问题类型: " + answerDTO.getQuestionType());
            }
            
            logger.info("成功创建/更新标准答案 - 标准问题ID: {}, 用户ID: {}", answerDTO.getStandardQuestionId(), userId);
            return result;
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("创建/更新标准答案时发生未预期的错误", e);
            throw new RuntimeException("创建/更新标准答案时发生错误: " + e.getMessage());
        }
    }
    
    private void validateAnswerFields(StandardAnswerDTO answerDTO) {
        switch (answerDTO.getQuestionType()) {
            case SINGLE_CHOICE:
            case MULTIPLE_CHOICE:
                if (answerDTO.getOptions() == null || answerDTO.getOptions().trim().isEmpty()) {
                    throw new IllegalArgumentException("客观题选项不能为空");
                }
                if (answerDTO.getCorrectIds() == null || answerDTO.getCorrectIds().trim().isEmpty()) {
                    throw new IllegalArgumentException("客观题正确答案不能为空");
                }
                break;
            case SIMPLE_FACT:
                // alternativeAnswers是可选的，不需要验证
                break;
            case SUBJECTIVE:
                if (answerDTO.getScoringGuidance() == null || answerDTO.getScoringGuidance().trim().isEmpty()) {
                    throw new IllegalArgumentException("主观题评分指导不能为空");
                }
                break;
        }
    }
    
    private StandardObjectiveAnswer createOrUpdateObjectiveAnswer(
            StandardQuestion standardQuestion,
            StandardAnswerDTO answerDTO,
            User user,
            ChangeLog changeLog) {
        
        // 查找现有答案
        StandardObjectiveAnswer existingAnswer = objectiveAnswerRepository
            .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestion.getId());
        
        StandardObjectiveAnswer answer = existingAnswer != null ? existingAnswer : new StandardObjectiveAnswer();
        answer.setStandardQuestion(standardQuestion);
        answer.setOptions(answerDTO.getOptions());
        answer.setCorrectIds(answerDTO.getCorrectIds());
        answer.setDeterminedByUser(user);
        answer.setDeterminedTime(LocalDateTime.now());
        answer.setCreatedChangeLog(changeLog);
        
        answer = objectiveAnswerRepository.save(answer);
        
        // 记录变更详情
        if (existingAnswer != null) {
            List<ChangeLogDetail> details = ChangeLogUtils.compareAndCreateDetails(
                changeLog,
                EntityType.STANDARD_OBJECTIVE_ANSWER,
                answer.getId(),
                existingAnswer,
                answer,
                "options", "correctIds"
            );
            
            for (ChangeLogDetail detail : details) {
                changeLogDetailRepository.save(detail);
            }
        } else {
            ChangeLogUtils.createAndSaveNewEntityDetails(
                changeLogDetailRepository,
                changeLog,
                EntityType.STANDARD_OBJECTIVE_ANSWER,
                answer.getId(),
                answer,
                "options", "correctIds"
            );
        }
        
        return answer;
    }
    
    private StandardSimpleAnswer createOrUpdateSimpleAnswer(
            StandardQuestion standardQuestion,
            StandardAnswerDTO answerDTO,
            User user,
            ChangeLog changeLog) {
        
        // 查找现有答案
        StandardSimpleAnswer existingAnswer = simpleAnswerRepository
            .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestion.getId());
        
        StandardSimpleAnswer answer = existingAnswer != null ? existingAnswer : new StandardSimpleAnswer();
        answer.setStandardQuestion(standardQuestion);
        answer.setAnswerText(answerDTO.getAnswerText());
        answer.setAlternativeAnswers(answerDTO.getAlternativeAnswers());
        answer.setDeterminedByUser(user);
        answer.setDeterminedTime(LocalDateTime.now());
        answer.setCreatedChangeLog(changeLog);
        
        answer = simpleAnswerRepository.save(answer);
        
        // 记录变更详情
        if (existingAnswer != null) {
            List<ChangeLogDetail> details = ChangeLogUtils.compareAndCreateDetails(
                changeLog,
                EntityType.STANDARD_SIMPLE_ANSWER,
                answer.getId(),
                existingAnswer,
                answer,
                "answerText", "alternativeAnswers"
            );
            
            for (ChangeLogDetail detail : details) {
                changeLogDetailRepository.save(detail);
            }
        } else {
            ChangeLogUtils.createAndSaveNewEntityDetails(
                changeLogDetailRepository,
                changeLog,
                EntityType.STANDARD_SIMPLE_ANSWER,
                answer.getId(),
                answer,
                "answerText", "alternativeAnswers"
            );
        }
        
        return answer;
    }
    
    private StandardSubjectiveAnswer createOrUpdateSubjectiveAnswer(
            StandardQuestion standardQuestion,
            StandardAnswerDTO answerDTO,
            User user,
            ChangeLog changeLog) {
        
        // 查找现有答案
        StandardSubjectiveAnswer existingAnswer = subjectiveAnswerRepository
            .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestion.getId());
        
        StandardSubjectiveAnswer answer = existingAnswer != null ? existingAnswer : new StandardSubjectiveAnswer();
        answer.setStandardQuestion(standardQuestion);
        answer.setAnswerText(answerDTO.getAnswerText());
        answer.setScoringGuidance(answerDTO.getScoringGuidance());
        answer.setDeterminedByUser(user);
        answer.setDeterminedTime(LocalDateTime.now());
        answer.setCreatedChangeLog(changeLog);
        
        answer = subjectiveAnswerRepository.save(answer);
        
        // 记录变更详情
        if (existingAnswer != null) {
            List<ChangeLogDetail> details = ChangeLogUtils.compareAndCreateDetails(
                changeLog,
                EntityType.STANDARD_SUBJECTIVE_ANSWER,
                answer.getId(),
                existingAnswer,
                answer,
                "answerText", "scoringGuidance"
            );
            
            for (ChangeLogDetail detail : details) {
                changeLogDetailRepository.save(detail);
            }
        } else {
            ChangeLogUtils.createAndSaveNewEntityDetails(
                changeLogDetailRepository,
                changeLog,
                EntityType.STANDARD_SUBJECTIVE_ANSWER,
                answer.getId(),
                answer,
                "answerText", "scoringGuidance"
            );
        }
        
        return answer;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Object getStandardAnswer(Long standardQuestionId) {
        logger.debug("开始获取标准答案 - 标准问题ID: {}", standardQuestionId);
        
        if (standardQuestionId == null) {
            logger.error("获取标准答案失败 - 标准问题ID为空");
            throw new IllegalArgumentException("标准问题ID不能为空");
        }
        
        try {
            // 获取标准问题
            StandardQuestion standardQuestion = standardQuestionRepository.findById(standardQuestionId)
                .orElseThrow(() -> {
                    logger.error("获取标准答案失败 - 找不到标准问题ID: {}", standardQuestionId);
                    return new IllegalArgumentException("找不到指定的标准问题（ID: " + standardQuestionId + "）");
                });
            
            // 根据问题类型获取相应的答案
            Object answer = null;
            switch (standardQuestion.getQuestionType()) {
                case SINGLE_CHOICE:
                case MULTIPLE_CHOICE:
                    answer = objectiveAnswerRepository.findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId);
                    break;
                case SIMPLE_FACT:
                    answer = simpleAnswerRepository.findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId);
                    break;
                case SUBJECTIVE:
                    answer = subjectiveAnswerRepository.findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId);
                    break;
                default:
                    logger.error("获取标准答案失败 - 不支持的问题类型: {}", standardQuestion.getQuestionType());
                    throw new IllegalArgumentException("不支持的问题类型: " + standardQuestion.getQuestionType());
            }
            
            logger.info("成功获取标准答案 - 标准问题ID: {}", standardQuestionId);
            return answer;
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("获取标准答案时发生未预期的错误", e);
            throw new RuntimeException("获取标准答案时发生错误: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void deleteStandardAnswer(Long standardQuestionId, Long userId) {
        logger.debug("开始删除标准答案 - 标准问题ID: {}, 用户ID: {}", standardQuestionId, userId);
        
        if (standardQuestionId == null || userId == null) {
            logger.error("删除标准答案失败 - 标准问题ID或用户ID为空");
            throw new IllegalArgumentException("标准问题ID和用户ID不能为空");
        }
        
        try {
            // 获取用户信息
            User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.error("删除标准答案失败 - 找不到用户ID: {}", userId);
                    return new IllegalArgumentException("找不到指定的用户（ID: " + userId + "）");
                });
            
            // 获取标准问题
            StandardQuestion standardQuestion = standardQuestionRepository.findById(standardQuestionId)
                .orElseThrow(() -> {
                    logger.error("删除标准答案失败 - 找不到标准问题ID: {}", standardQuestionId);
                    return new IllegalArgumentException("找不到指定的标准问题（ID: " + standardQuestionId + "）");
                });
            
            // 创建变更日志
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChangeType(ChangeType.DELETE_STANDARD_ANSWER);
            changeLog.setChangedByUser(user);
            changeLog.setCommitMessage("删除标准答案");
            changeLog.setAssociatedStandardQuestion(standardQuestion);
            changeLog = changeLogRepository.save(changeLog);
            
            LocalDateTime now = LocalDateTime.now();
            
            // 根据问题类型删除相应的答案
            switch (standardQuestion.getQuestionType()) {
                case SINGLE_CHOICE:
                case MULTIPLE_CHOICE:
                    StandardObjectiveAnswer objectiveAnswer = objectiveAnswerRepository
                        .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId);
                    if (objectiveAnswer != null) {
                        objectiveAnswer.setDeletedAt(now);
                        objectiveAnswerRepository.save(objectiveAnswer);
                        recordDeletionDetails(changeLog, EntityType.STANDARD_OBJECTIVE_ANSWER, objectiveAnswer.getId());
                    }
                    break;
                case SIMPLE_FACT:
                    StandardSimpleAnswer simpleAnswer = simpleAnswerRepository
                        .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId);
                    if (simpleAnswer != null) {
                        simpleAnswer.setDeletedAt(now);
                        simpleAnswerRepository.save(simpleAnswer);
                        recordDeletionDetails(changeLog, EntityType.STANDARD_SIMPLE_ANSWER, simpleAnswer.getId());
                    }
                    break;
                case SUBJECTIVE:
                    StandardSubjectiveAnswer subjectiveAnswer = subjectiveAnswerRepository
                        .findByStandardQuestionIdAndDeletedAtIsNull(standardQuestionId);
                    if (subjectiveAnswer != null) {
                        subjectiveAnswer.setDeletedAt(now);
                        subjectiveAnswerRepository.save(subjectiveAnswer);
                        recordDeletionDetails(changeLog, EntityType.STANDARD_SUBJECTIVE_ANSWER, subjectiveAnswer.getId());
                    }
                    break;
                default:
                    logger.error("删除标准答案失败 - 不支持的问题类型: {}", standardQuestion.getQuestionType());
                    throw new IllegalArgumentException("不支持的问题类型: " + standardQuestion.getQuestionType());
            }
            
            logger.info("成功删除标准答案 - 标准问题ID: {}, 用户ID: {}", standardQuestionId, userId);
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("删除标准答案时发生未预期的错误", e);
            throw new RuntimeException("删除标准答案时发生错误: " + e.getMessage());
        }
    }
    
    private void recordDeletionDetails(ChangeLog changeLog, EntityType entityType, Long entityId) {
        ChangeLogDetail detail = new ChangeLogDetail();
        detail.setChangeLog(changeLog);
        detail.setEntityType(entityType);
        detail.setEntityId(entityId);
        detail.setAttributeName("deleted_at");
        detail.setOldValue(null);
        detail.setNewValue(LocalDateTime.now().toString());
        changeLogDetailRepository.save(detail);
    }
} 
package com.example.demo.task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.WebSocketMessage.MessageType;
import com.example.demo.entity.AnswerGenerationBatch;
import com.example.demo.entity.AnswerGenerationBatch.BatchStatus;
import com.example.demo.entity.AnswerPromptAssemblyConfig;
import com.example.demo.entity.AnswerQuestionTypePrompt;
import com.example.demo.entity.AnswerTagPrompt;
import com.example.demo.entity.DatasetQuestionMapping;
import com.example.demo.entity.LlmAnswer;
import com.example.demo.entity.LlmModel;
import com.example.demo.entity.ModelAnswerRun;
import com.example.demo.entity.ModelAnswerRun.RunStatus;
import com.example.demo.entity.QuestionType;
import com.example.demo.entity.StandardQuestion;
import com.example.demo.entity.Tag;
import com.example.demo.manager.BatchStateManager;
import com.example.demo.repository.AnswerGenerationBatchRepository;
import com.example.demo.repository.AnswerQuestionTypePromptRepository;
import com.example.demo.repository.AnswerTagPromptRepository;
import com.example.demo.repository.LlmAnswerRepository;
import com.example.demo.repository.ModelAnswerRunRepository;
import com.example.demo.repository.StandardQuestionRepository;
import com.example.demo.service.LlmApiService;
import com.example.demo.service.WebSocketService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;



/**
 * 回答生成异步任务
 */
@Component
public class AnswerGenerationTask {
    
    private static final Logger logger = LoggerFactory.getLogger(AnswerGenerationTask.class);
    
    @Autowired
    private EntityManager entityManager;
    
    private final AnswerGenerationBatchRepository batchRepository;
    private final ModelAnswerRunRepository runRepository;
    private final StandardQuestionRepository questionRepository;
    private final LlmAnswerRepository answerRepository;
    private final WebSocketService webSocketService;
    private final LlmApiService llmApiService;
    private final AnswerTagPromptRepository answerTagPromptRepository;
    private final AnswerQuestionTypePromptRepository answerQuestionTypePromptRepository;
    private final JdbcTemplate jdbcTemplate;
    private BatchStateManager batchStateManager;
    
    // 添加中断控制器
    private final ConcurrentHashMap<Long, AtomicBoolean> interruptionFlags = new ConcurrentHashMap<>();
    private final ScheduledExecutorService interruptionMonitor = Executors.newScheduledThreadPool(1);
    
    // 添加中断标志来源跟踪
    private final ConcurrentHashMap<Long, String> interruptionSource = new ConcurrentHashMap<>();
    
    @Autowired
    public AnswerGenerationTask(
            AnswerGenerationBatchRepository batchRepository,
            ModelAnswerRunRepository runRepository,
            StandardQuestionRepository questionRepository,
            LlmAnswerRepository answerRepository,
            WebSocketService webSocketService,
            LlmApiService llmApiService,
            AnswerTagPromptRepository answerTagPromptRepository,
            AnswerQuestionTypePromptRepository answerQuestionTypePromptRepository,
            JdbcTemplate jdbcTemplate) {
        this.batchRepository = batchRepository;
        this.runRepository = runRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.webSocketService = webSocketService;
        this.llmApiService = llmApiService;
        this.answerTagPromptRepository = answerTagPromptRepository;
        this.answerQuestionTypePromptRepository = answerQuestionTypePromptRepository;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Autowired
    @Lazy
    public void setBatchStateManager(BatchStateManager batchStateManager) {
        this.batchStateManager = batchStateManager;
    }
    
    /**
     * 初始化方法，启动中断监控
     */
    @PostConstruct
    public void init() {
        logger.info("初始化回答生成任务管理器");
        
        // 启动定期检查Redis中断标志的任务
        interruptionMonitor.scheduleAtFixedRate(() -> {
            try {
                // 检查所有有中断标志的批次
                for (Long batchId : interruptionFlags.keySet()) {
                    try {
                        // 检查Redis中的中断标志
                        if (batchStateManager != null) {
                            boolean redisInterruptFlag = batchStateManager.isInterrupted(batchId);
                            boolean memoryInterruptFlag = interruptionFlags.get(batchId).get();
                            
                            // 同步Redis和内存中的中断标志
                            if (redisInterruptFlag && !memoryInterruptFlag) {
                                logger.info("批次{}在Redis中有中断标志，同步到内存", batchId);
                                markForInterruption(batchId, "REDIS_SYNC");
                            } else if (!redisInterruptFlag && memoryInterruptFlag) {
                                // 检查是否是手动暂停
                                String source = interruptionSource.getOrDefault(batchId, "UNKNOWN");
                                if (!"MANUAL_PAUSE".equals(source)) {
                                    logger.info("批次{}在Redis中无中断标志，清除内存中的中断标志", batchId);
                                    clearInterruptionFlag(batchId);
                                } else {
                                    logger.info("批次{}有手动暂停标志，保持中断状态", batchId);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("检查批次{}中断状态时出错", batchId, e);
                    }
                }
            } catch (Exception e) {
                logger.error("中断监控任务出错", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }
    
    /**
     * 销毁方法，关闭中断监控
     */
    @PreDestroy
    public void destroy() {
        logger.info("关闭回答生成任务管理器");
        interruptionMonitor.shutdownNow();
    }
    
    /**
     * 标记批次需要中断
     */
    public void markForInterruption(Long batchId) {
        logger.info("批次{}已被标记为需要中断", batchId);
        interruptionFlags.computeIfAbsent(batchId, k -> new AtomicBoolean(false)).set(true);
        interruptionSource.put(batchId, "TASK_MANAGER");
    }
    
    /**
     * 标记批次需要中断（带来源）
     */
    public void markForInterruption(Long batchId, String source) {
        logger.info("批次{}已被标记为需要中断，来源: {}", batchId, source);
        interruptionFlags.computeIfAbsent(batchId, k -> new AtomicBoolean(false)).set(true);
        interruptionSource.put(batchId, source);
    }
    
    /**
     * 清除批次中断标志
     */
    public void clearInterruptionFlag(Long batchId) {
        logger.info("批次{}的中断标志已清除", batchId);
        AtomicBoolean flag = interruptionFlags.get(batchId);
        if (flag != null) {
            flag.set(false);
        }
        interruptionSource.remove(batchId);
    }
    
    /**
     * 检查批次是否应该中断
     */
    public boolean shouldInterrupt(Long batchId) {
        // 只检查内存中的中断标志
        AtomicBoolean flag = interruptionFlags.get(batchId);
        if (flag != null && flag.get()) {
            logger.debug("批次{}有内存中断标志，需要中断", batchId);
            return true;
        }
        
        // 如果Redis中有中断标志，也应该中断
        if (batchStateManager != null && batchStateManager.isInterrupted(batchId)) {
            logger.debug("批次{}在Redis中有中断标志，需要中断", batchId);
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查批次是否应该暂停
     */
    public boolean shouldPauseBatch(Long batchId) {
        // 直接调用shouldInterrupt方法
        return shouldInterrupt(batchId);
    }
    
    /**
     * 开始处理单个批次
     */
    public void startBatchAnswerGeneration(Long batchId) {
        logger.info("开始处理批次: {}", batchId);
        
        try {
            // 获取当前状态但不用于判断是否处理
            String currentStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM answer_generation_batches WHERE id = ?", 
                String.class, batchId);
                
            logger.info("批次{}当前状态为{}，开始处理", batchId, currentStatus);
            
            // 直接更新状态为GENERATING_ANSWERS，不做状态检查
            int updated = jdbcTemplate.update(
                "UPDATE answer_generation_batches SET status = 'GENERATING_ANSWERS', last_activity_time = ?, " + 
                "last_check_time = ?, processing_instance = ? WHERE id = ?",
                LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID().toString(), batchId);
                    
            logger.info("已将批次{}状态更新为GENERATING_ANSWERS并获取处理权", batchId);
            
            // 同步Redis状态
            if (batchStateManager != null) {
                batchStateManager.setBatchState(batchId, "GENERATING_ANSWERS");
                batchStateManager.setInterruptFlag(batchId, false);
            }
            
            // 清除内存中断标志
            clearInterruptionFlag(batchId);
            
            // 获取批次信息
            AnswerGenerationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的批次: " + batchId));
            
            // 获取批次关联的所有运行
            List<ModelAnswerRun> runs = runRepository.findByAnswerGenerationBatchId(batchId);
            if (runs.isEmpty()) {
                logger.warn("批次{}没有关联的运行，无法启动处理", batchId);
                return;
            }
            logger.info("批次{}共有{}个运行", batchId, runs.size());
            
            // 获取批次关联的所有问题
            List<StandardQuestion> questions = questionRepository.findByDatasetVersionId(batch.getDatasetVersion().getId());
            if (questions.isEmpty()) {
                logger.warn("批次{}关联的数据集版本没有问题，无法启动处理", batchId);
                return;
            }
            logger.info("批次{}关联的数据集共有{}个问题", batchId, questions.size());
            
            // 预加载问题ID
            List<Long> questionIds = questions.stream().map(StandardQuestion::getId).collect(java.util.stream.Collectors.toList());
            
            // 预加载数据集映射（不会覆盖已加载的标签）
            if (!questionIds.isEmpty()) {
                logger.info("预加载批次{}的问题映射关系", batchId);
                List<StandardQuestion> questionsWithMappings = questionRepository.findByIdsWithDatasetMappings(questionIds);
                
                // 创建ID到预加载问题的映射，用于替换原始列表中的问题
                Map<Long, StandardQuestion> questionMap = new HashMap<>();
                for (StandardQuestion q : questionsWithMappings) {
                    questionMap.put(q.getId(), q);
                }
                
                // 使用预加载的问题替换原始列表中的问题
                for (int i = 0; i < questions.size(); i++) {
                    Long id = questions.get(i).getId();
                    if (questionMap.containsKey(id)) {
                        questions.set(i, questionMap.get(id));
                    }
                }
                logger.info("批次{}问题映射关系加载完成", batchId);
            }
            
            // 更新批次的总问题数
            int totalQuestions = questions.size() * runs.size() * batch.getAnswerRepeatCount();
            logger.info("批次{}总问题数: {}", batchId, totalQuestions);
            
            // 启动每个运行的处理，不跳过任何运行
            for (ModelAnswerRun run : runs) {
                Long runId = run.getId();
                
                // 获取当前状态但不用于判断是否处理
                String runStatus = jdbcTemplate.queryForObject(
                    "SELECT status FROM model_answer_runs WHERE id = ?", 
                    String.class, runId);
                
                logger.info("开始处理批次{}的运行: {}，模型: {}, 当前状态: {}", 
                        batchId, runId, run.getLlmModel().getName(), runStatus);
                
                // 直接更新运行状态为GENERATING_ANSWERS
                jdbcTemplate.update(
                    "UPDATE model_answer_runs SET status = 'GENERATING_ANSWERS', last_activity_time = ? WHERE id = ?",
                    LocalDateTime.now(), runId);
                    
                logger.info("运行{}状态已更新为GENERATING_ANSWERS", runId);
                
                // 检查是否有断点信息
                Long lastProcessedQuestionId = run.getLastProcessedQuestionId();
                Integer lastProcessedQuestionIndex = run.getLastProcessedQuestionIndex();
                
                if (lastProcessedQuestionId != null && lastProcessedQuestionIndex != null && lastProcessedQuestionIndex >= 0) {
                    logger.info("运行{}有断点信息，将从断点处继续: 问题ID={}, 索引={}", 
                                runId, lastProcessedQuestionId, lastProcessedQuestionIndex);
                    
                    // 从断点处继续处理
                    startRunAnswerGenerationFromCheckpoint(run, questions, batch.getAnswerRepeatCount(), new AtomicBoolean(false));
                } else {
                    logger.info("运行{}没有断点信息，将从头开始处理", runId);
                    
                    // 从头开始处理
                    startRunAnswerGeneration(run, questions, batch.getAnswerRepeatCount(), new AtomicBoolean(false));
                }
                
                logger.info("批次{}的运行{}处理已启动", batchId, runId);
            }
            
            // 批次处理完成后，检查所有运行状态并更新批次状态
            checkAndUpdateBatchCompletion(batch);
            
            // 处理完成后，清除处理标记
            jdbcTemplate.update(
                "UPDATE answer_generation_batches SET processing_instance = NULL WHERE id = ?",
                batchId);
                
            logger.info("批次{}处理完成，已清除处理标记", batchId);
        } catch (Exception e) {
            logger.error("处理批次{}失败: {}", batchId, e.getMessage(), e);
            
            try {
                // 更新批次状态为失败
                jdbcTemplate.update(
                    "UPDATE answer_generation_batches SET status = 'FAILED', error_message = ?, " +
                    "last_activity_time = ?, processing_instance = NULL WHERE id = ?",
                    e.getMessage(), LocalDateTime.now(), batchId);
                
                if (batchStateManager != null) {
                    batchStateManager.setBatchState(batchId, "FAILED");
                }
                
                // 发送错误通知
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("batchId", batchId);
                errorData.put("error", "批次处理失败: " + e.getMessage());
                errorData.put("timestamp", System.currentTimeMillis());
                webSocketService.sendBatchMessage(batchId, MessageType.ERROR, errorData);
            } catch (Exception ex) {
                logger.error("更新批次{}失败状态时出错", batchId, ex);
            }
        }
    }
    
    /**
     * 从断点处开始处理单个运行的回答生成
     */
    private void startRunAnswerGenerationFromCheckpoint(ModelAnswerRun run, List<StandardQuestion> questions, int repeatCount, AtomicBoolean shouldStop) {
        Long runId = run.getId();
        Long batchId = run.getAnswerGenerationBatch().getId();
        logger.info("从断点处恢复运行: {}, 模型: {}", runId, run.getLlmModel().getName());
        
        // 获取批次状态但不用于判断是否处理
        String batchStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM answer_generation_batches WHERE id = ?", 
            String.class, batchId);
        
        logger.info("断点恢复前批次{}状态: {}，继续处理", batchId, batchStatus);
        
        try {
            // 直接更新运行状态为GENERATING_ANSWERS，不做状态检查
            jdbcTemplate.update(
                "UPDATE model_answer_runs SET status = 'GENERATING_ANSWERS', last_activity_time = ? WHERE id = ?",
                LocalDateTime.now(), runId);
            
            logger.info("运行{}状态已更新为GENERATING_ANSWERS", runId);
            
            // 初始化计数器
            int totalQuestions = questions.size() * repeatCount;
            int completedQuestions = run.getCompletedQuestionsCount();
            int failedQuestions = run.getFailedQuestionsCount();
            
            logger.info("运行{}已完成的问题数: {}, 失败的问题数: {}, 总问题数: {}", 
                runId, completedQuestions, failedQuestions, totalQuestions);
            
            // 查找上次处理的位置
            int lastProcessedIndex = run.getLastProcessedQuestionIndex();
            Long lastProcessedId = run.getLastProcessedQuestionId();
            
            logger.info("运行{}上次处理位置: 问题ID={}, 索引={}", runId, lastProcessedId, lastProcessedIndex);
            
            // 确定开始处理的重复索引和问题索引
            int startRepeatIndex = 0;
            int startQuestionIndex = 0;
            
            if (lastProcessedIndex >= 0) {
                // 计算重复索引和问题索引
                startRepeatIndex = lastProcessedIndex / questions.size();
                startQuestionIndex = lastProcessedIndex % questions.size();
                
                logger.info("运行{}从断点处恢复：重复索引={}, 问题索引={}", runId, startRepeatIndex, startQuestionIndex);
            } else {
                logger.info("运行{}没有有效的断点位置，从头开始处理", runId);
            }
            
            long startTime = System.currentTimeMillis();
            logger.info("开始从断点处恢复运行{}的处理", runId);
            
            // 处理每个问题（考虑重复次数），从断点处开始
            for (int r = startRepeatIndex; r < repeatCount; r++) {
                for (int q = (r == startRepeatIndex ? startQuestionIndex : 0); q < questions.size(); q++) {
                    StandardQuestion question = questions.get(q);
                    
                    logger.debug("运行{}处理问题: ID={}, 重复索引={}, 问题索引={}", 
                        runId, question.getId(), r, q);
                    
                    // 每次处理一个问题前检查批次是否应该中断
                    if (shouldInterrupt(batchId) || shouldStop.get()) {
                        logger.info("检测到批次{}已标记为中断，停止处理运行{}中的新问题", batchId, runId);
                        
                        // 保存当前处理位置
                        jdbcTemplate.update(
                            "UPDATE model_answer_runs SET status = 'PAUSED', last_activity_time = ?, " +
                            "last_processed_question_id = ?, last_processed_question_index = ? WHERE id = ?",
                            LocalDateTime.now(), question.getId(), (r * questions.size() + q), runId);
                            
                        logger.info("运行{}已暂停，当前处理位置已保存: 问题ID={}, 索引={}", 
                            runId, question.getId(), (r * questions.size() + q));
                            
                        // 发送状态变更通知
                        Map<String, Object> statusData = new HashMap<>();
                        statusData.put("runId", runId);
                        statusData.put("status", "PAUSED");
                        statusData.put("completedQuestions", completedQuestions);
                        statusData.put("failedQuestions", failedQuestions);
                        statusData.put("totalQuestions", totalQuestions);
                        statusData.put("message", "运行已暂停");
                        
                        webSocketService.sendRunMessage(runId, MessageType.STATUS_CHANGE, statusData);
                        
                        return;
                    }
                    
                    // 处理单个问题
                    logger.debug("运行{}开始处理问题: ID={}", runId, question.getId());
                    boolean success = processQuestion(run, question, r);
                    
                    // 更新计数器
                    if (success) {
                        completedQuestions++;
                        logger.debug("运行{}问题处理成功: ID={}, 已完成问题数={}", 
                            runId, question.getId(), completedQuestions);
                    } else {
                        failedQuestions++;
                        logger.debug("运行{}问题处理失败: ID={}, 失败问题数={}", 
                            runId, question.getId(), failedQuestions);
                    }
                    
                    // 更新处理位置
                    jdbcTemplate.update(
                        "UPDATE model_answer_runs SET last_processed_question_id = ?, last_processed_question_index = ? WHERE id = ?",
                        question.getId(), (r * questions.size() + q), runId);
                    
                    logger.debug("运行{}处理位置已更新: 问题ID={}, 索引={}", 
                        runId, question.getId(), (r * questions.size() + q));
                    
                    // 更新进度
                    updateRunProgress(run, completedQuestions, failedQuestions, totalQuestions);
                    
                    // 每处理5个问题后，再次检查中断状态
                    if ((completedQuestions + failedQuestions) % 5 == 0) {
                        if (shouldInterrupt(batchId) || shouldStop.get()) {
                            logger.info("批量处理过程中检测到批次{}已标记为中断，停止处理", batchId);
                            return;
                        }
                    }
                }
            }
            
            long endTime = System.currentTimeMillis();
            logger.info("运行{}处理完成，总耗时: {}毫秒", runId, (endTime - startTime));
            
            // 更新运行状态为COMPLETED
            updateRunStatus(run, RunStatus.COMPLETED, null);
            
            // 检查批次是否所有运行都已完成
            checkAndUpdateBatchCompletion(run.getAnswerGenerationBatch());
            
        } catch (Exception e) {
            logger.error("处理运行{}失败: {}", runId, e.getMessage(), e);
            
            // 更新运行状态为FAILED
            updateRunStatus(run, RunStatus.FAILED, e.getMessage());
        }
    }
    
    /**
     * 开始处理单个运行的回答生成
     */
    private void startRunAnswerGeneration(ModelAnswerRun run, List<StandardQuestion> questions, int repeatCount, AtomicBoolean shouldStop) {
        Long runId = run.getId();
        Long batchId = run.getAnswerGenerationBatch().getId();
        logger.info("开始处理运行: {}, 模型: {}", runId, run.getLlmModel().getName());
        
        // 获取批次状态但不用于判断是否处理
        String batchStatus = jdbcTemplate.queryForObject(
            "SELECT status FROM answer_generation_batches WHERE id = ?", 
            String.class, batchId);
        
        logger.info("批次{}当前状态为{}，继续处理运行{}", batchId, batchStatus, runId);
        
        try {
            // 直接更新运行状态为GENERATING_ANSWERS，不做状态检查
            jdbcTemplate.update(
                "UPDATE model_answer_runs SET status = 'GENERATING_ANSWERS', last_activity_time = ? WHERE id = ?",
                LocalDateTime.now(), runId);
            
            // 初始化计数器
            int totalQuestions = questions.size() * repeatCount;
            int completedQuestions = 0;
            int failedQuestions = 0;
            
            long startTime = System.currentTimeMillis();
            
            // 处理每个问题（考虑重复次数）
            for (int r = 0; r < repeatCount; r++) {
                for (int q = 0; q < questions.size(); q++) {
                    StandardQuestion question = questions.get(q);
                    
                    // 每次处理一个问题前检查批次是否应该中断
                    if (shouldInterrupt(batchId) || shouldStop.get()) {
                        logger.info("检测到批次{}已标记为中断，停止处理运行{}中的新问题", batchId, runId);
                        
                        // 更新运行状态为PAUSED，同时保存当前处理位置
                        jdbcTemplate.update(
                            "UPDATE model_answer_runs SET status = 'PAUSED', last_activity_time = ?, " +
                            "last_processed_question_id = ?, last_processed_question_index = ? WHERE id = ?",
                            LocalDateTime.now(), question.getId(), (r * questions.size() + q), runId);
                            
                        // 发送状态变更通知
                        Map<String, Object> statusData = new HashMap<>();
                        statusData.put("runId", runId);
                        statusData.put("status", "PAUSED");
                        statusData.put("completedQuestions", completedQuestions);
                        statusData.put("failedQuestions", failedQuestions);
                        statusData.put("totalQuestions", totalQuestions);
                        statusData.put("message", "运行已暂停");
                        
                        webSocketService.sendRunMessage(runId, MessageType.STATUS_CHANGE, statusData);
                        
                        return;
                    }
                    
                    // 处理单个问题
                    boolean success = processQuestion(run, question, r);
                    
                    // 更新计数器
                    if (success) {
                        completedQuestions++;
                    } else {
                        failedQuestions++;
                    }
                    
                    // 更新处理位置
                    jdbcTemplate.update(
                        "UPDATE model_answer_runs SET last_processed_question_id = ?, last_processed_question_index = ? WHERE id = ?",
                        question.getId(), (r * questions.size() + q), runId);
                    
                    // 更新进度
                    updateRunProgress(run, completedQuestions, failedQuestions, totalQuestions);
                    
                    // 每处理5个问题后，再次检查中断状态
                    if ((completedQuestions + failedQuestions) % 5 == 0) {
                        if (shouldInterrupt(batchId) || shouldStop.get()) {
                            logger.info("批量处理过程中检测到批次{}已标记为中断，停止处理", batchId);
                            return;
                        }
                    }
                }
            }
            
            long endTime = System.currentTimeMillis();
            logger.info("运行{}处理完成，总耗时: {}毫秒", runId, (endTime - startTime));
            
            // 更新运行状态为COMPLETED
            updateRunStatus(run, RunStatus.COMPLETED, null);
            
            // 检查批次是否所有运行都已完成
            checkAndUpdateBatchCompletion(run.getAnswerGenerationBatch());
            
        } catch (Exception e) {
            logger.error("处理运行{}失败: {}", runId, e.getMessage(), e);
            
            // 更新运行状态为FAILED
            updateRunStatus(run, RunStatus.FAILED, e.getMessage());
        }
    }
    
    /**
     * 处理单个问题
     */
    @Transactional
    public boolean processQuestion(ModelAnswerRun run, StandardQuestion question, int repeatIndex) {
        Long runId = run.getId();
        Long questionId = question.getId();
        Long batchId = run.getAnswerGenerationBatch().getId();
        
        logger.info("开始处理问题: 运行={}, 问题ID={}, 重复索引={}", runId, questionId, repeatIndex);
        
        // 每次处理问题前检查中断标志
        if (shouldInterrupt(batchId)) {
            logger.info("检测到批次{}的中断信号，跳过问题处理", batchId);
            return false; // 立即返回，不处理当前问题
        }
        
        logger.debug("处理问题: 运行={}, 问题={}, 重复索引={}", runId, questionId, repeatIndex);
        
        try {
            // 在问题处理过程中定期检查中断标志
            if (shouldInterrupt(batchId)) {
                logger.info("问题处理前检测到批次{}的中断信号，中止处理", batchId);
                return false;
            }
            
            // 发送问题开始处理通知
            sendQuestionStartedNotification(run, question, repeatIndex);
            logger.debug("问题处理开始通知已发送: 运行={}, 问题ID={}", runId, questionId);
            
            // 组装Prompt
            logger.debug("开始组装问题Prompt: 运行={}, 问题ID={}", runId, questionId);
            String prompt = assemblePrompt(run, question);
            logger.debug("问题Prompt组装完成: 运行={}, 问题ID={}, Prompt长度={}", runId, questionId, prompt.length());
            
            // 调用LLM API生成回答，并支持中断检查
            logger.info("开始调用LLM API生成回答: 运行={}, 问题ID={}, 模型={}", 
                runId, questionId, run.getLlmModel().getName());
            String answer = generateAnswerWithInterruptCheck(run, prompt, new AtomicBoolean(false));
            
            // 如果已被中断，则不继续处理
            if (answer == null) {
                logger.info("批次{}在生成回答过程中被中断，不保存结果: 运行={}, 问题={}", batchId, runId, questionId);
                jdbcTemplate.update(
                    "UPDATE model_answer_runs SET status = 'PAUSED', last_activity_time = ? WHERE id = ?",
                    LocalDateTime.now(), runId);
                return false;
            }
            
            logger.info("LLM API生成回答成功: 运行={}, 问题ID={}, 回答长度={}", 
                runId, questionId, answer.length());
            
            // 保存回答结果
            logger.debug("开始保存回答结果: 运行={}, 问题ID={}", runId, questionId);
            saveModelAnswer(run, question, answer, repeatIndex);
            logger.info("回答结果保存成功: 运行={}, 问题ID={}", runId, questionId);
            
            // 发送问题完成处理通知
            sendQuestionCompletedNotification(run, question, repeatIndex);
            logger.debug("问题处理完成通知已发送: 运行={}, 问题ID={}", runId, questionId);
            
            return true;
        } catch (Exception e) {
            logger.error("处理问题失败: 运行={}, 问题={}, 错误={}", runId, questionId, e.getMessage(), e);
            
            // 记录失败信息
            recordFailedQuestion(run, questionId);
            
            // 发送问题处理失败通知
            sendQuestionFailedNotification(run, question, repeatIndex, e.getMessage());
            
            return false;
        }
    }
    
    /**
     * 组装Prompt
     */
    private String assemblePrompt(ModelAnswerRun run, StandardQuestion question) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 获取回答Prompt组装配置
        AnswerPromptAssemblyConfig config = run.getAnswerGenerationBatch().getAnswerAssemblyConfig();
        
        if (config != null) {
            // 添加基础系统提示
            if (config.getBaseSystemPrompt() != null) {
                promptBuilder.append(config.getBaseSystemPrompt());
                promptBuilder.append(config.getSectionSeparator());
            }
            
            // 预先查询标签，解决懒加载问题
            List<Tag> tags = new ArrayList<>();
            try {
                // 使用JPA查询获取标签，避免使用懒加载关系
                List<Tag> fetchedTags = entityManager.createQuery(
                    "SELECT t FROM Tag t JOIN StandardQuestionTag sqt ON t.id = sqt.tag.id " +
                    "WHERE sqt.standardQuestion.id = :questionId AND t.deletedAt IS NULL",
                    Tag.class)
                    .setParameter("questionId", question.getId())
                    .getResultList();
                    
                tags.addAll(fetchedTags);
                logger.debug("为问题ID{}成功加载了{}个标签", question.getId(), tags.size());
            } catch (Exception e) {
                logger.warn("加载问题标签时出错，问题ID: {}", question.getId(), e);
                // 使用空列表继续处理
            }
            
            // 添加标签提示（如果问题有标签）
            if (!tags.isEmpty() && config.getTagPromptsSectionHeader() != null) {
                
                // 先收集有效的标签提示词，如果没有任何有效提示词则跳过整个标签部分
                boolean hasAnyTagPrompt = false;
                
                StringBuilder tagPromptsBuilder = new StringBuilder();
                
                for (Tag tag : tags) {
                    try {
                        // 跳过没有初始化的标签
                        if (tag == null || tag.getTagName() == null) {
                            logger.warn("标签未初始化或标签名为空");
                            continue;
                        }
                        
                        // 获取该标签的激活状态提示词
                        List<AnswerTagPrompt> tagPrompts = answerTagPromptRepository
                            .findByTagIdAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(tag.getId());
                        
                        if (!tagPrompts.isEmpty()) {
                            // 使用优先级最高的提示词（列表已按优先级排序）
                            AnswerTagPrompt prompt = tagPrompts.get(0);
                            tagPromptsBuilder.append("【").append(tag.getTagName()).append("】: ");
                            tagPromptsBuilder.append(prompt.getPromptTemplate());
                            tagPromptsBuilder.append(config.getTagPromptSeparator());
                            hasAnyTagPrompt = true;
                        }
                        // 如果标签没有提示词，则跳过该标签，不添加到prompt中
                    } catch (Exception e) {
                        // 捕获并记录错误，但不中断处理
                        logger.warn("获取标签提示词失败，标签ID: {}，继续处理其他标签", 
                                    tag != null ? tag.getId() : "未知", e);
                    }
                }
                
                // 只有当至少有一个标签有提示词时，才添加标签部分
                if (hasAnyTagPrompt) {
                    promptBuilder.append(config.getTagPromptsSectionHeader());
                    promptBuilder.append("\n");
                    promptBuilder.append(tagPromptsBuilder);
                    promptBuilder.append(config.getSectionSeparator());
                }
            }
            
            // 添加问题类型要求
            if (config.getQuestionTypeSectionHeader() != null) {
                promptBuilder.append(config.getQuestionTypeSectionHeader());
                promptBuilder.append("\n");
                
                // 根据问题类型添加特定要求
                QuestionType questionType = question.getQuestionType();
                if (questionType != null) {
                    try {
                        // 获取该问题类型的激活状态提示词
                        List<AnswerQuestionTypePrompt> typePrompts = answerQuestionTypePromptRepository
                            .findByQuestionTypeAndIsActiveTrueAndDeletedAtIsNull(questionType);
                        
                        if (!typePrompts.isEmpty()) {
                            // 使用最新创建的提示词（假设列表已按创建时间排序）
                            AnswerQuestionTypePrompt prompt = typePrompts.get(0);
                            promptBuilder.append(prompt.getPromptTemplate());
                            
                            // 添加回答格式要求（如果有）
                            if (prompt.getResponseFormatInstruction() != null && !prompt.getResponseFormatInstruction().isEmpty()) {
                                promptBuilder.append("\n\n回答格式要求:\n");
                                promptBuilder.append(prompt.getResponseFormatInstruction());
                            }
                            
                            // 添加回答示例（如果有）
                            if (prompt.getResponseExample() != null && !prompt.getResponseExample().isEmpty()) {
                                promptBuilder.append("\n\n回答示例:\n");
                                promptBuilder.append(prompt.getResponseExample());
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("获取问题类型提示词失败，问题类型: {}", questionType, e);
                    }
                }
                
                promptBuilder.append(config.getSectionSeparator());
            }
            
            // 添加最终指示
            if (config.getFinalInstruction() != null) {
                promptBuilder.append(config.getFinalInstruction());
                promptBuilder.append(config.getSectionSeparator());
            }
        }
        
        // 添加问题内容
        promptBuilder.append("问题: ");
        promptBuilder.append(question.getQuestionText());
        
        return promptBuilder.toString();
    }
    
    /**
     * 调用LLM API生成回答，并支持中断检查
     */
    private String generateAnswerWithInterruptCheck(ModelAnswerRun run, String prompt, AtomicBoolean interrupted) {
        Long batchId = run.getAnswerGenerationBatch().getId();
        LlmModel model = run.getLlmModel();
        
        // API调用前检查中断状态
        if (shouldInterrupt(batchId)) {
            logger.info("API调用前检测到批次{}的中断信号，不执行API调用", batchId);
            return null;
        }
        
        // 如果外部已经设置中断标志，也直接返回
        if (interrupted.get()) {
            logger.info("检测到外部中断标志，不执行API调用");
            return null;
        }
        
        try {
            // 获取上下文变量
            Map<String, Object> contextVariables = getContextVariables(run);
            
            // 使用LlmApiService生成回答，同时传入中断检查回调
            return llmApiService.generateModelAnswer(model, prompt, contextVariables);
        } catch (Exception e) {
            logger.error("生成模型回答失败: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 获取上下文变量，包括批次全局参数和运行特定参数
     */
    private Map<String, Object> getContextVariables(ModelAnswerRun run) {
        Map<String, Object> parameters = new HashMap<>();
        
        // 添加批次全局参数
        if (run.getAnswerGenerationBatch().getGlobalParameters() != null) {
            parameters.putAll(run.getAnswerGenerationBatch().getGlobalParameters());
        }
        
        // 添加运行特定参数（优先级最高）
        if (run.getParameters() != null) {
            parameters.putAll(run.getParameters());
        }
        
        return parameters;
    }
    
    /**
     * 保存模型回答
     */
    @Transactional
    public void saveModelAnswer(ModelAnswerRun run, StandardQuestion question, String answerText, int repeatIndex) {
        LlmAnswer answer = new LlmAnswer();
        answer.setModelAnswerRun(run);
        
        try {
            // 需要从StandardQuestion获取对应的DatasetQuestionMapping
            // 直接通过ID查询，避免使用懒加载的集合
            Long datasetVersionId = run.getAnswerGenerationBatch().getDatasetVersion().getId();
            
            // 使用EntityManager直接查询，确保在当前事务中
            DatasetQuestionMapping mapping = entityManager.createQuery(
                "SELECT dqm FROM DatasetQuestionMapping dqm " +
                "WHERE dqm.standardQuestion.id = :questionId " +
                "AND dqm.datasetVersion.id = :versionId", 
                DatasetQuestionMapping.class)
                .setParameter("questionId", question.getId())
                .setParameter("versionId", datasetVersionId)
                .getSingleResult();
            
            answer.setDatasetQuestionMapping(mapping);
            answer.setAnswerText(answerText);
            answer.setRepeatIndex(repeatIndex);
            answer.setGenerationTime(LocalDateTime.now());
            answer.setGenerationStatus(LlmAnswer.GenerationStatus.SUCCESS);
            
            // 可以添加其他字段
            answer.setPromptUsed(assemblePrompt(run, question));
            
            answerRepository.save(answer);
        } catch (Exception e) {
            logger.error("保存模型回答失败: questionId={}, runId={}", question.getId(), run.getId(), e);
            throw e;
        }
    }
    
    /**
     * 记录失败的问题
     */
    @Transactional
    public void recordFailedQuestion(ModelAnswerRun run, Long questionId) {
        run.setFailedQuestionsCount(run.getFailedQuestionsCount() + 1);
        
        // 添加到失败问题ID列表
        List<Long> failedIds = run.getFailedQuestionsIds();
        if (failedIds == null) {
            failedIds = new ArrayList<>();
        }
        failedIds.add(questionId);
        run.setFailedQuestionsIds(failedIds);
        
        runRepository.save(run);
    }
    
    /**
     * 刷新运行状态
     */
    private ModelAnswerRun refreshRunStatus(Long runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("运行不存在: " + runId));
    }
    
    /**
     * 更新运行进度
     */
    @Transactional
    public void updateRunProgress(ModelAnswerRun run, int completedQuestions, int failedQuestions, int totalQuestions) {
        // 更新运行进度信息
        run.setLastProcessedQuestionId(null);
        run.setLastProcessedQuestionIndex(-1);
        run.setLastActivityTime(LocalDateTime.now());
        
        run.setCompletedQuestionsCount(completedQuestions);
        run.setFailedQuestionsCount(failedQuestions);
        
        // 计算进度百分比
        BigDecimal progressPercentage = BigDecimal.valueOf((double) completedQuestions / totalQuestions * 100)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        run.setProgressPercentage(progressPercentage);
        
        runRepository.save(run);
        
        // 发送WebSocket进度更新通知
        sendRunProgressNotification(run, progressPercentage.doubleValue(), 
                "已处理 " + completedQuestions + "/" + totalQuestions + " 个问题");
    }
    
    /**
     * 更新运行状态
     */
    @Transactional
    public void updateRunStatus(ModelAnswerRun run, RunStatus status, String errorMessage) {
        run.setStatus(status);
        run.setLastActivityTime(LocalDateTime.now());
        
        if (errorMessage != null) {
            run.setErrorMessage(errorMessage);
        }
        
        if (status == RunStatus.COMPLETED) {
            run.setProgressPercentage(BigDecimal.valueOf(100));
        }
        
        runRepository.saveAndFlush(run);
        entityManager.clear(); // 清除一级缓存，确保后续查询能获取最新状态
        
        // 发送状态变更通知
        webSocketService.sendStatusChangeMessage(run.getId(), status.name(), 
                "运行状态变更为: " + status.name());
    }
    
    /**
     * 更新批次状态
     */
    @Transactional
    public void updateBatchStatus(AnswerGenerationBatch batch, BatchStatus status, String errorMessage) {
        batch.setStatus(status);
        batch.setLastActivityTime(LocalDateTime.now());
        
        if (errorMessage != null) {
            batch.setErrorMessage(errorMessage);
        }
        
        if (status == BatchStatus.COMPLETED) {
            batch.setCompletedAt(LocalDateTime.now());
            batch.setProgressPercentage(BigDecimal.valueOf(100));
        }
        
        batchRepository.saveAndFlush(batch);
        entityManager.clear(); // 清除一级缓存，确保后续查询能获取最新状态
        
        // 同步状态到Redis
        if (batchStateManager != null) {
            batchStateManager.setBatchState(batch.getId(), status.name());
            
            // 根据状态设置中断标志
            if (status == BatchStatus.PAUSED) {
                batchStateManager.setInterruptFlag(batch.getId(), true);
            } else {
                batchStateManager.setInterruptFlag(batch.getId(), false);
            }
            
            logger.debug("批次{}状态已同步到Redis: {}", batch.getId(), status);
        }
        
        // 发送批次状态变更通知
        Map<String, Object> payload = new HashMap<>();
        payload.put("batchId", batch.getId());
        payload.put("status", status.name());
        payload.put("timestamp", System.currentTimeMillis());
        
        if (errorMessage != null) {
            payload.put("error", errorMessage);
        }
        
        MessageType messageType = (status == BatchStatus.COMPLETED) ? 
                MessageType.TASK_COMPLETED : MessageType.STATUS_CHANGE;
        
        webSocketService.sendBatchMessage(batch.getId(), messageType, payload);
        
        logger.info("批次{}状态已更新为: {}", batch.getId(), status);
    }
    
    /**
     * 检查并更新批次完成状态
     */
    @Transactional
    public boolean checkAndUpdateBatchCompletion(AnswerGenerationBatch batch) {
        List<ModelAnswerRun> runs = runRepository.findByAnswerGenerationBatchId(batch.getId());
        
        // 检查所有运行是否完成
        boolean allCompleted = runs.stream()
                .allMatch(run -> run.getStatus() == RunStatus.COMPLETED || run.getStatus() == RunStatus.FAILED);
        
        if (allCompleted) {
            // 计算批次总体进度
            BigDecimal totalProgress = runs.stream()
                    .map(ModelAnswerRun::getProgressPercentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(runs.size()), 2, java.math.RoundingMode.HALF_UP);
            
            batch.setProgressPercentage(totalProgress);
            
            // 检查是否存在失败的运行
            boolean hasFailed = runs.stream().anyMatch(run -> run.getStatus() == RunStatus.FAILED);
            
            // 检查当前批次状态，避免从PENDING直接变为COMPLETED
            if (batch.getStatus() == BatchStatus.PENDING) {
                logger.warn("批次{}状态异常: 从PENDING直接尝试变为COMPLETED", batch.getId());
                updateBatchStatus(batch, BatchStatus.GENERATING_ANSWERS, null);
                
                // 添加延迟确保状态能被正确更新
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 重新获取批次状态
                batch = batchRepository.findById(batch.getId()).orElse(batch);
            }
            
            if (hasFailed) {
                updateBatchStatus(batch, BatchStatus.FAILED, "部分运行失败");
            } else {
                updateBatchStatus(batch, BatchStatus.COMPLETED, null);
            }
        }
        
        return allCompleted;
    }
    
    /**
     * 发送运行开始通知
     */
    private void sendRunStartNotification(ModelAnswerRun run) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("runId", run.getId());
        payload.put("runName", run.getRunName());
        payload.put("modelName", run.getLlmModel().getName());
        payload.put("startTime", LocalDateTime.now());
        
        webSocketService.sendRunMessage(run.getId(), MessageType.TASK_STARTED, payload);
    }
    
    /**
     * 发送运行完成通知
     */
    private void sendRunCompletionNotification(ModelAnswerRun run) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("runId", run.getId());
        payload.put("runName", run.getRunName());
        payload.put("modelName", run.getLlmModel().getName());
        payload.put("completedTime", LocalDateTime.now());
        payload.put("completedCount", run.getCompletedQuestionsCount());
        payload.put("failedCount", run.getFailedQuestionsCount());
        
        webSocketService.sendRunMessage(run.getId(), MessageType.TASK_COMPLETED, payload);
    }
    
    /**
     * 发送运行进度通知
     */
    private void sendRunProgressNotification(ModelAnswerRun run, double progress, String message) {
        webSocketService.sendRunProgressMessage(run.getId(), progress, message);
    }
    
    /**
     * 发送问题开始处理通知
     */
    private void sendQuestionStartedNotification(ModelAnswerRun run, StandardQuestion question, int repeatIndex) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("runId", run.getId());
        payload.put("questionId", question.getId());
        payload.put("questionText", question.getQuestionText());
        payload.put("repeatIndex", repeatIndex);
        
        webSocketService.sendRunMessage(run.getId(), MessageType.QUESTION_STARTED, payload);
    }
    
    /**
     * 发送问题完成处理通知
     */
    private void sendQuestionCompletedNotification(ModelAnswerRun run, StandardQuestion question, int repeatIndex) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("runId", run.getId());
        payload.put("questionId", question.getId());
        payload.put("questionText", question.getQuestionText());
        payload.put("repeatIndex", repeatIndex);
        payload.put("completedCount", run.getCompletedQuestionsCount());
        
        webSocketService.sendRunMessage(run.getId(), MessageType.QUESTION_COMPLETED, payload);
    }
    
    /**
     * 发送问题处理失败通知
     */
    private void sendQuestionFailedNotification(ModelAnswerRun run, StandardQuestion question, int repeatIndex, String error) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("runId", run.getId());
        payload.put("questionId", question.getId());
        payload.put("questionText", question.getQuestionText());
        payload.put("repeatIndex", repeatIndex);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("error", error);
        
        webSocketService.sendRunMessage(run.getId(), MessageType.QUESTION_FAILED, payload);
    }

    /**
     * 发送批次完成通知
     */
    private void sendBatchCompletedNotification(AnswerGenerationBatch batch) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("batchId", batch.getId());
        payload.put("status", "COMPLETED");
        payload.put("timestamp", System.currentTimeMillis());
        
        webSocketService.sendBatchMessage(batch.getId(), MessageType.TASK_COMPLETED, payload);
    }

    /**
     * 更新批次内存状态
     * @param batchId 批次ID
     * @param status 状态
     */
    public void updateBatchMemoryState(Long batchId, String status) {
        logger.info("更新批次{}内存状态为: {}", batchId, status);
        
        // 根据状态设置中断标志
        if ("PAUSED".equals(status)) {
            markForInterruption(batchId, "MEMORY_STATE_UPDATE");
        } else if ("GENERATING_ANSWERS".equals(status) || "RESUMING".equals(status)) {
            clearInterruptionFlag(batchId);
        }
        
        // 如果batchStateManager未初始化，跳过Redis操作
        if (batchStateManager == null) {
            logger.warn("batchStateManager尚未注入，跳过Redis状态同步");
            return;
        }
        
        // 同步Redis状态
        batchStateManager.setBatchState(batchId, status);
        
        if ("PAUSED".equals(status)) {
            batchStateManager.setInterruptFlag(batchId, true);
        } else {
            batchStateManager.setInterruptFlag(batchId, false);
        }
    }
} 
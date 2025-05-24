package com.example.demo.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.WebSocketMessage.MessageType;
import com.example.demo.entity.AnswerGenerationBatch;
import com.example.demo.entity.ModelAnswerRun;
import com.example.demo.entity.StandardQuestion;
import com.example.demo.entity.ModelAnswer;
import com.example.demo.entity.LlmModel;
import com.example.demo.entity.AnswerPromptAssemblyConfig;
import com.example.demo.entity.AnswerGenerationBatch.BatchStatus;
import com.example.demo.entity.ModelAnswerRun.RunStatus;
import com.example.demo.repository.AnswerGenerationBatchRepository;
import com.example.demo.repository.ModelAnswerRunRepository;
import com.example.demo.repository.StandardQuestionRepository;
import com.example.demo.repository.ModelAnswerRepository;
import com.example.demo.service.WebSocketService;
import com.example.demo.service.LlmApiService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 回答生成异步任务
 */
@Component
public class AnswerGenerationTask {
    
    private static final Logger logger = LoggerFactory.getLogger(AnswerGenerationTask.class);
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private final AnswerGenerationBatchRepository batchRepository;
    private final ModelAnswerRunRepository runRepository;
    private final StandardQuestionRepository questionRepository;
    private final ModelAnswerRepository answerRepository;
    private final WebSocketService webSocketService;
    private final LlmApiService llmApiService;
    
    public AnswerGenerationTask(
            AnswerGenerationBatchRepository batchRepository,
            ModelAnswerRunRepository runRepository,
            StandardQuestionRepository questionRepository,
            ModelAnswerRepository answerRepository,
            WebSocketService webSocketService,
            LlmApiService llmApiService) {
        this.batchRepository = batchRepository;
        this.runRepository = runRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.webSocketService = webSocketService;
        this.llmApiService = llmApiService;
    }
    
    /**
     * 启动批次的回答生成过程
     */
    @Async
    public void startBatchAnswerGeneration(Long batchId) {
        logger.info("开始异步处理批次: {}", batchId);
        
        try {
            // 获取批次信息
            AnswerGenerationBatch batch = batchRepository.findById(batchId)
                    .orElseThrow(() -> new IllegalArgumentException("批次不存在: " + batchId));
            
            // 获取批次的所有运行
            List<ModelAnswerRun> runs = runRepository.findByAnswerGenerationBatchId(batchId);
            if (runs.isEmpty()) {
                logger.error("批次没有关联的运行: {}", batchId);
                updateBatchStatus(batch, BatchStatus.FAILED, "批次没有关联的运行");
                return;
            }
            
            // 获取数据集版本中的所有问题
            List<StandardQuestion> questions = questionRepository
                    .findByDatasetVersionId(batch.getDatasetVersion().getId());
            if (questions.isEmpty()) {
                logger.error("数据集版本没有问题: {}", batch.getDatasetVersion().getId());
                updateBatchStatus(batch, BatchStatus.FAILED, "数据集版本没有问题");
                return;
            }
            
            // 更新批次的总问题数
            int totalQuestions = questions.size() * runs.size() * batch.getAnswerRepeatCount();
            
            // 启动每个运行的处理
            for (ModelAnswerRun run : runs) {
                startRunAnswerGeneration(run, questions, batch.getAnswerRepeatCount());
            }
            
            // 定期检查所有运行是否完成，完成后更新批次状态
            boolean allCompleted = checkAndUpdateBatchCompletion(batch);
            
            if (allCompleted) {
                logger.info("批次所有运行已完成: {}", batchId);
            }
            
        } catch (Exception e) {
            logger.error("批次处理失败: {}", batchId, e);
            // 更新批次状态为失败
            try {
                AnswerGenerationBatch batch = batchRepository.findById(batchId).orElse(null);
                if (batch != null) {
                    updateBatchStatus(batch, BatchStatus.FAILED, e.getMessage());
                }
            } catch (Exception ex) {
                logger.error("更新批次状态失败", ex);
            }
        }
    }
    
    /**
     * 启动单个运行的回答生成过程
     */
    @Async
    public void startRunAnswerGeneration(ModelAnswerRun run, List<StandardQuestion> questions, int repeatCount) {
        Long runId = run.getId();
        logger.info("开始处理运行: {}, 模型: {}", runId, run.getLlmModel().getName());
        
        try {
            // 准备处理的问题和重复次数
            int totalQuestionsToProcess = questions.size() * repeatCount;
            run.setTotalQuestionsCount(totalQuestionsToProcess);
            runRepository.save(run);
            
            // 发送运行开始通知
            sendRunStartNotification(run);
            
            // 开始处理每个问题
            AtomicInteger processedCount = new AtomicInteger(0);
            
            for (int r = 0; r < repeatCount; r++) {
                int repeatIndex = r;
                
                for (int i = 0; i < questions.size(); i++) {
                    StandardQuestion question = questions.get(i);
                    
                    // 检查运行状态，如果已暂停或失败则退出
                    run = refreshRunStatus(runId);
                    if (run.getStatus() == RunStatus.PAUSED || run.getStatus() == RunStatus.FAILED) {
                        logger.info("运行已{}，停止处理: {}", run.getStatus(), runId);
                        return;
                    }
                    
                    // 处理单个问题
                    boolean success = processQuestion(run, question, repeatIndex, i);
                    
                    // 更新进度
                    int currentProcessed = processedCount.incrementAndGet();
                    updateRunProgress(run, question.getId(), i, currentProcessed, totalQuestionsToProcess, success);
                    
                    // 确保数据已保存并且实体已分离，避免大事务
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            
            // 完成所有问题后，更新运行状态
            run = refreshRunStatus(runId);
            updateRunStatus(run, RunStatus.COMPLETED, null);
            
            // 发送运行完成通知
            sendRunCompletionNotification(run);
            
        } catch (Exception e) {
            logger.error("运行处理失败: {}", runId, e);
            try {
                ModelAnswerRun failedRun = runRepository.findById(runId).orElse(null);
                if (failedRun != null) {
                    updateRunStatus(failedRun, RunStatus.FAILED, e.getMessage());
                }
            } catch (Exception ex) {
                logger.error("更新运行状态失败", ex);
            }
        }
    }
    
    /**
     * 处理单个问题
     */
    @Transactional
    public boolean processQuestion(ModelAnswerRun run, StandardQuestion question, int repeatIndex, int questionIndex) {
        Long runId = run.getId();
        Long questionId = question.getId();
        logger.debug("处理问题: 运行={}, 问题={}, 重复索引={}", runId, questionId, repeatIndex);
        
        try {
            // 发送问题开始处理通知
            sendQuestionStartedNotification(run, question, repeatIndex);
            
            // 组装Prompt
            String prompt = assemblePrompt(run, question);
            
            // 调用LLM API生成回答
            String answer = generateAnswer(run, prompt);
            
            // 保存回答结果
            saveModelAnswer(run, question, answer, repeatIndex);
            
            // 发送问题完成处理通知
            sendQuestionCompletedNotification(run, question, repeatIndex);
            
            return true;
        } catch (Exception e) {
            logger.error("处理问题失败: 运行={}, 问题={}", runId, questionId, e);
            
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
            
            // 添加标签提示（如果问题有标签）
            if (question.getTags() != null && !question.getTags().isEmpty() && 
                config.getTagPromptsSectionHeader() != null) {
                promptBuilder.append(config.getTagPromptsSectionHeader());
                promptBuilder.append("\n");
                
                // TODO: 根据标签获取相应的提示
                // 这里需要根据具体业务逻辑添加标签相关的提示
                
                promptBuilder.append(config.getSectionSeparator());
            }
            
            // 添加问题类型要求
            if (config.getQuestionTypeSectionHeader() != null) {
                promptBuilder.append(config.getQuestionTypeSectionHeader());
                promptBuilder.append("\n");
                
                // TODO: 根据问题类型添加特定要求
                // 这里需要根据具体业务逻辑添加问题类型相关的要求
                
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
     * 调用LLM API生成回答
     */
    private String generateAnswer(ModelAnswerRun run, String prompt) {
        LlmModel model = run.getLlmModel();
        
        // 获取模型参数
        Map<String, Object> parameters = new HashMap<>();
        
        // 添加批次全局参数
        if (run.getAnswerGenerationBatch().getGlobalParameters() != null) {
            parameters.putAll(run.getAnswerGenerationBatch().getGlobalParameters());
        }
        
        // 添加模型默认参数
        if (model.getModelParameters() != null) {
            parameters.putAll(model.getModelParameters());
        }
        
        // 添加运行特定参数（优先级最高）
        if (run.getParameters() != null) {
            parameters.putAll(run.getParameters());
        }
        
        // 调用LLM API服务
        return llmApiService.generateAnswer(
            model.getApiUrl(),
            model.getApiKey(),
            model.getApiType(),
            prompt,
            parameters
        );
    }
    
    /**
     * 保存模型回答
     */
    @Transactional
    public void saveModelAnswer(ModelAnswerRun run, StandardQuestion question, String answerText, int repeatIndex) {
        ModelAnswer answer = new ModelAnswer();
        answer.setModelAnswerRun(run);
        answer.setStandardQuestion(question);
        answer.setAnswerText(answerText);
        answer.setRepeatIndex(repeatIndex);
        answer.setGenerationTime(LocalDateTime.now());
        
        // 可以添加其他字段，如令牌使用量等
        
        answerRepository.save(answer);
    }
    
    /**
     * 记录失败的问题
     */
    @Transactional
    public void recordFailedQuestion(ModelAnswerRun run, Long questionId) {
        run.setFailedQuestionsCount(run.getFailedQuestionsCount() + 1);
        
        // 添加到失败问题ID列表
        Long[] failedIds = run.getFailedQuestionsIds();
        if (failedIds == null) {
            failedIds = new Long[]{questionId};
        } else {
            Long[] newFailedIds = new Long[failedIds.length + 1];
            System.arraycopy(failedIds, 0, newFailedIds, 0, failedIds.length);
            newFailedIds[failedIds.length] = questionId;
            failedIds = newFailedIds;
        }
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
    public void updateRunProgress(ModelAnswerRun run, Long questionId, int questionIndex, 
                                 int processedCount, int totalCount, boolean success) {
        // 更新运行进度信息
        run.setLastProcessedQuestionId(questionId);
        run.setLastProcessedQuestionIndex(questionIndex);
        run.setLastActivityTime(LocalDateTime.now());
        
        if (success) {
            run.setCompletedQuestionsCount(run.getCompletedQuestionsCount() + 1);
        }
        
        // 计算进度百分比
        BigDecimal progressPercentage = BigDecimal.valueOf((double) processedCount / totalCount * 100)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        run.setProgressPercentage(progressPercentage);
        
        runRepository.save(run);
        
        // 发送WebSocket进度更新通知
        sendRunProgressNotification(run, progressPercentage.doubleValue(), 
                "已处理 " + processedCount + "/" + totalCount + " 个问题");
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
        
        runRepository.save(run);
        
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
        
        if (status == BatchStatus.COMPLETED) {
            batch.setCompletedAt(LocalDateTime.now());
            batch.setProgressPercentage(BigDecimal.valueOf(100));
        }
        
        batchRepository.save(batch);
        
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
        payload.put("error", error);
        payload.put("failedCount", run.getFailedQuestionsCount());
        
        webSocketService.sendRunMessage(run.getId(), MessageType.QUESTION_FAILED, payload);
    }
} 
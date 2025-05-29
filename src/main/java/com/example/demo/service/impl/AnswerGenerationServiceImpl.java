package com.example.demo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.web.client.RestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.demo.dto.AnswerGenerationBatchDTO;
import com.example.demo.dto.ModelAnswerRunDTO;
import com.example.demo.dto.WebSocketMessage.MessageType;
import com.example.demo.entity.jdbc.AnswerGenerationBatch;
import com.example.demo.entity.jdbc.AnswerGenerationBatch.BatchStatus;
import com.example.demo.entity.jdbc.AnswerPromptAssemblyConfig;
import com.example.demo.entity.jdbc.AnswerQuestionTypePrompt;
import com.example.demo.entity.jdbc.DatasetVersion;
import com.example.demo.entity.jdbc.EvaluationPromptAssemblyConfig;
import com.example.demo.entity.jdbc.LlmModel;
import com.example.demo.entity.jdbc.ModelAnswerRun;
import com.example.demo.entity.jdbc.ModelAnswerRun.RunStatus;
import com.example.demo.entity.jdbc.QuestionType;
import com.example.demo.entity.jdbc.User;
import com.example.demo.repository.jdbc.AnswerGenerationBatchRepository;
import com.example.demo.repository.jdbc.AnswerPromptAssemblyConfigRepository;
import com.example.demo.repository.jdbc.AnswerQuestionTypePromptRepository;
import com.example.demo.repository.jdbc.DatasetVersionRepository;
import com.example.demo.repository.jdbc.EvaluationPromptAssemblyConfigRepository;
import com.example.demo.repository.jdbc.LlmModelRepository;
import com.example.demo.repository.jdbc.ModelAnswerRunRepository;
import com.example.demo.repository.jdbc.UserRepository;
import com.example.demo.service.AnswerGenerationService;
import com.example.demo.service.WebSocketService;
import com.example.demo.task.AnswerGenerationTask;
import com.example.demo.service.LlmApiService;
import com.example.demo.manager.BatchStateManager;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

@Service
public class AnswerGenerationServiceImpl implements AnswerGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnswerGenerationServiceImpl.class);
    
    private final AnswerGenerationBatchRepository batchRepository;
    private final ModelAnswerRunRepository runRepository;
    private final DatasetVersionRepository datasetVersionRepository;
    private final LlmModelRepository llmModelRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final AnswerPromptAssemblyConfigRepository answerConfigRepository;
    private final EvaluationPromptAssemblyConfigRepository evalConfigRepository;
    private final AnswerQuestionTypePromptRepository answerQuestionTypePromptRepository;
    private final AnswerGenerationTask answerGenerationTask;
    private final LlmApiService llmApiService;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final BatchStateManager batchStateManager;
    private final ExecutorService taskExecutor;
    
    @Autowired
    public AnswerGenerationServiceImpl(
            AnswerGenerationBatchRepository batchRepository,
            ModelAnswerRunRepository runRepository,
            DatasetVersionRepository datasetVersionRepository,
            LlmModelRepository llmModelRepository,
            UserRepository userRepository,
            WebSocketService webSocketService,
            AnswerPromptAssemblyConfigRepository answerConfigRepository,
            EvaluationPromptAssemblyConfigRepository evalConfigRepository,
            AnswerQuestionTypePromptRepository answerQuestionTypePromptRepository,
            AnswerGenerationTask answerGenerationTask,
            LlmApiService llmApiService,
            EntityManager entityManager,
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            BatchStateManager batchStateManager) {
        this.batchRepository = batchRepository;
        this.runRepository = runRepository;
        this.datasetVersionRepository = datasetVersionRepository;
        this.llmModelRepository = llmModelRepository;
        this.userRepository = userRepository;
        this.webSocketService = webSocketService;
        this.answerConfigRepository = answerConfigRepository;
        this.evalConfigRepository = evalConfigRepository;
        this.answerQuestionTypePromptRepository = answerQuestionTypePromptRepository;
        this.answerGenerationTask = answerGenerationTask;
        this.llmApiService = llmApiService;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionManager = transactionManager;
        this.batchStateManager = batchStateManager;
        this.taskExecutor = Executors.newFixedThreadPool(5);
    }
    
    // 实现接口方法
    @Override
    @Transactional
    public AnswerGenerationBatchDTO createBatch(AnswerGenerationBatchCreateRequest request) {
        logger.info("创建回答生成批次: {}", request.getName());
        
        // 验证数据集版本
        DatasetVersion datasetVersion = datasetVersionRepository.findById(request.getDatasetVersionId())
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的数据集版本(ID: " + request.getDatasetVersionId() + ")"));
        
        // 验证用户
        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的用户(ID: " + request.getUserId() + ")"));
        }
        
        // 验证Prompt组装配置
        AnswerPromptAssemblyConfig answerConfig = null;
        if (request.getAnswerAssemblyConfigId() != null) {
            answerConfig = answerConfigRepository.findById(request.getAnswerAssemblyConfigId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的回答Prompt组装配置(ID: " + request.getAnswerAssemblyConfigId() + ")"));
        }
        
        EvaluationPromptAssemblyConfig evalConfig = null;
        if (request.getEvaluationAssemblyConfigId() != null) {
            evalConfig = evalConfigRepository.findById(request.getEvaluationAssemblyConfigId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测Prompt组装配置(ID: " + request.getEvaluationAssemblyConfigId() + ")"));
        }
        
        // 验证题型prompt
        AnswerQuestionTypePrompt singleChoicePrompt = null;
        if (request.getSingleChoicePromptId() != null) {
            singleChoicePrompt = answerQuestionTypePromptRepository.findById(request.getSingleChoicePromptId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的单选题Prompt(ID: " + request.getSingleChoicePromptId() + ")"));
            if (singleChoicePrompt.getQuestionType() != QuestionType.SINGLE_CHOICE) {
                throw new IllegalArgumentException("指定的Prompt不是单选题类型");
            }
        }
        
        AnswerQuestionTypePrompt multipleChoicePrompt = null;
        if (request.getMultipleChoicePromptId() != null) {
            multipleChoicePrompt = answerQuestionTypePromptRepository.findById(request.getMultipleChoicePromptId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的多选题Prompt(ID: " + request.getMultipleChoicePromptId() + ")"));
            if (multipleChoicePrompt.getQuestionType() != QuestionType.MULTIPLE_CHOICE) {
                throw new IllegalArgumentException("指定的Prompt不是多选题类型");
            }
        }
        
        AnswerQuestionTypePrompt simpleFactPrompt = null;
        if (request.getSimpleFactPromptId() != null) {
            simpleFactPrompt = answerQuestionTypePromptRepository.findById(request.getSimpleFactPromptId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的简单事实题Prompt(ID: " + request.getSimpleFactPromptId() + ")"));
            if (simpleFactPrompt.getQuestionType() != QuestionType.SIMPLE_FACT) {
                throw new IllegalArgumentException("指定的Prompt不是简单事实题类型");
            }
        }
        
        AnswerQuestionTypePrompt subjectivePrompt = null;
        if (request.getSubjectivePromptId() != null) {
            subjectivePrompt = answerQuestionTypePromptRepository.findById(request.getSubjectivePromptId())
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的主观题Prompt(ID: " + request.getSubjectivePromptId() + ")"));
            if (subjectivePrompt.getQuestionType() != QuestionType.SUBJECTIVE) {
                throw new IllegalArgumentException("指定的Prompt不是主观题类型");
            }
        }
        
        // 创建批次
        AnswerGenerationBatch batch = new AnswerGenerationBatch();
        batch.setName(request.getName());
        batch.setDescription(request.getDescription());
        batch.setDatasetVersion(datasetVersion);
        batch.setCreationTime(LocalDateTime.now());
        batch.setStatus(BatchStatus.PENDING);
        batch.setAnswerAssemblyConfig(answerConfig);
        batch.setEvaluationAssemblyConfig(evalConfig);
        batch.setGlobalParameters(request.getGlobalParameters());
        batch.setCreatedByUser(user);
        batch.setProgressPercentage(BigDecimal.ZERO);
        batch.setLastActivityTime(LocalDateTime.now());
        
        // 设置题型prompt
        batch.setSingleChoicePrompt(singleChoicePrompt);
        batch.setMultipleChoicePrompt(multipleChoicePrompt);
        batch.setSimpleFactPrompt(simpleFactPrompt);
        batch.setSubjectivePrompt(subjectivePrompt);
        
        // 设置答案重复次数
        if (request.getAnswerRepeatCount() != null && request.getAnswerRepeatCount() > 0) {
            batch.setAnswerRepeatCount(request.getAnswerRepeatCount());
        } else {
            batch.setAnswerRepeatCount(1); // 默认值
        }
        
        // 保存批次
        AnswerGenerationBatch savedBatch = batchRepository.save(batch);
        logger.debug("批次已创建: ID={}, 名称={}", savedBatch.getId(), savedBatch.getName());
        
        // 验证模型并创建运行
        List<ModelAnswerRun> runs = new ArrayList<>();
        if (request.getLlmModelIds() != null && !request.getLlmModelIds().isEmpty()) {
            for (Long modelId : request.getLlmModelIds()) {
                LlmModel model = llmModelRepository.findById(modelId)
                        .orElseThrow(() -> new EntityNotFoundException("找不到指定的LLM模型(ID: " + modelId + ")"));
                
                ModelAnswerRun run = new ModelAnswerRun();
                run.setAnswerGenerationBatch(savedBatch);
                run.setLlmModel(model);
                run.setRunName(model.getName() + " - " + savedBatch.getName());
                run.setRunDescription("使用" + model.getName() + "(" + model.getProvider() + ")模型运行批次" + savedBatch.getName());
                run.setRunIndex(0); // 首次运行
                run.setRunTime(LocalDateTime.now());
                run.setStatus(RunStatus.PENDING);
                
                // 设置模型特定参数
                if (request.getModelSpecificParameters() != null && 
                    request.getModelSpecificParameters().containsKey(modelId)) {
                    run.setParameters(request.getModelSpecificParameters().get(modelId));
                }
                
                run.setCreatedByUser(user);
                run.setProgressPercentage(BigDecimal.ZERO);
                run.setCompletedQuestionsCount(0);
                run.setFailedQuestionsCount(0);
                
                // 保存运行
                ModelAnswerRun savedRun = runRepository.save(run);
                runs.add(savedRun);
                logger.debug("为批次{}创建运行: ID={}, 模型={}", 
                    savedBatch.getId(), savedRun.getId(), model.getName());
            }
        }
        
        // 通过WebSocket发送通知
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("batchId", savedBatch.getId());
        notificationData.put("batchName", savedBatch.getName());
        notificationData.put("datasetName", datasetVersion.getName());
        notificationData.put("datasetVersionNumber", datasetVersion.getVersionNumber());
        notificationData.put("status", savedBatch.getStatus().name());
        notificationData.put("runsCount", runs.size());
        
        if (user != null) {
            webSocketService.sendUserMessage(user.getId(), MessageType.TASK_STARTED, notificationData);
        }
        webSocketService.sendGlobalMessage(MessageType.TASK_STARTED, notificationData);
        
        logger.info("批次{}创建完成，包含{}个运行", savedBatch.getId(), runs.size());
        
        // 转换为DTO并返回
        AnswerGenerationBatchDTO batchDTO = convertToDTO(savedBatch);
        batchDTO.setTotalRuns(runs.size());
        batchDTO.setPendingRuns(runs.size());
        batchDTO.setCompletedRuns(0);
        batchDTO.setFailedRuns(0);
        
        return batchDTO;
    }
    
    @Override
    @Transactional
    public void startBatch(Long batchId) {
        logger.info("启动回答生成批次: {}", batchId);
        AnswerGenerationBatch batch = null;
        
        try {
            // 获取批次
            batch = batchRepository.findById(batchId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的批次(ID: " + batchId + ")"));
            
            // 删除状态检查，允许任何状态的批次都能被启动
            logger.info("批次{}当前状态为{}，开始启动处理", batchId, batch.getStatus());
            
            // 获取批次的所有运行
            List<ModelAnswerRun> runs = runRepository.findByAnswerGenerationBatchId(batchId);
            if (runs.isEmpty()) {
                String errorMsg = String.format("批次(ID: %d)没有关联的运行", batchId);
                logger.error(errorMsg);
                webSocketService.sendErrorMessage(batchId, errorMsg, null);
                throw new IllegalStateException(errorMsg);
            }
            
            // 测试所有模型的连通性
            logger.info("执行模型连通性测试...");
            List<String> failedModels = testModelsConnectivity(runs);
            
            if (!failedModels.isEmpty()) {
                String errorMsg = "以下模型连接失败: " + String.join(", ", failedModels);
                logger.error("批次(ID: {})启动失败: {}", batchId, errorMsg);
                
                // 更新批次状态为失败
                batch.setStatus(BatchStatus.FAILED);
                batch.setLastActivityTime(LocalDateTime.now());
                batch.setErrorMessage(errorMsg);
                batchRepository.saveAndFlush(batch);
                
                // 通过WebSocket发送错误通知
                Map<String, Object> payload = new HashMap<>();
                payload.put("batchId", batch.getId());
                payload.put("status", batch.getStatus().name());
                payload.put("error", errorMsg);
                payload.put("failedModels", failedModels);
                
                webSocketService.sendBatchMessage(batchId, MessageType.ERROR, payload);
                
                throw new IllegalStateException(errorMsg);
            }
            
            // 所有模型都能正常连接，继续启动批次
            logger.info("所有模型连通性测试通过，更新批次状态为GENERATING_ANSWERS");
            
            // 直接使用JDBC更新批次状态和时间
            jdbcTemplate.update(
                "UPDATE answer_generation_batches SET status = 'GENERATING_ANSWERS', last_activity_time = ?, last_check_time = ? WHERE id = ?",
                LocalDateTime.now(), LocalDateTime.now(), batch.getId());
            
            logger.info("批次状态已更新为: GENERATING_ANSWERS");
            
            // 立即同步Redis状态 - 简单记录，不作为决策依据
            if (batchStateManager != null) {
                batchStateManager.setBatchState(batchId, "GENERATING_ANSWERS");
                batchStateManager.setInterruptFlag(batchId, false);
                logger.info("已同步批次{}的Redis状态为GENERATING_ANSWERS", batchId);
            }
            
            // 更新运行状态
            for (ModelAnswerRun run : runs) {
                jdbcTemplate.update(
                    "UPDATE model_answer_runs SET status = 'GENERATING_ANSWERS', last_activity_time = ? WHERE id = ?",
                    LocalDateTime.now(), run.getId()
                );
                
                // 通过WebSocket发送状态变更通知
                webSocketService.sendStatusChangeMessage(run.getId(), RunStatus.GENERATING_ANSWERS.name(), 
                    "开始生成回答");
            }
            
            // 通过WebSocket发送批次启动通知
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("batchId", batch.getId());
            notificationData.put("batchName", batch.getName());
            notificationData.put("status", "GENERATING_ANSWERS");
            notificationData.put("startTime", LocalDateTime.now());
            notificationData.put("runsCount", runs.size());
            
            webSocketService.sendBatchMessage(batchId, MessageType.TASK_STARTED, notificationData);
            
        } catch (Exception e) {
            logger.error("启动批次{}时发生异常: {}", batchId, e.getMessage(), e);
            
            // 确保发生错误时批次状态被正确更新
            if (batch != null && batch.getStatus() == BatchStatus.PENDING) {
                batch.setStatus(BatchStatus.FAILED);
                batch.setLastActivityTime(LocalDateTime.now());
                batch.setErrorMessage(e.getMessage());
                batchRepository.saveAndFlush(batch);
                
                // 通过WebSocket发送错误通知
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("batchId", batchId);
                errorData.put("error", e.getMessage());
                errorData.put("timestamp", System.currentTimeMillis());
                webSocketService.sendBatchMessage(batchId, MessageType.ERROR, errorData);
            }
            
            throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException("启动批次失败", e);
        }
        
        // 直接提交任务
        logger.info("直接启动批次{}处理任务", batchId);
        taskExecutor.submit(() -> {
            try {
                answerGenerationTask.startBatchAnswerGeneration(batchId);
                logger.info("批次{}处理任务完成", batchId);
            } catch (Exception e) {
                logger.error("批次{}处理任务执行失败: {}", batchId, e.getMessage(), e);
            }
        });
    }
    
    /**
     * 在新事务中启动批次处理任务
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startBatchProcessingTask(Long batchId) {
        try {
            // 获取当前状态但不用于判断是否处理
            String currentStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM answer_generation_batches WHERE id = ?", 
                String.class, batchId);
            
            logger.info("批次{}当前状态为：{}，准备开始处理", batchId, currentStatus);
            
            // 检查状态是否有异常值
            if (currentStatus != null && currentStatus.contains("bootRun")) {
                // 修正状态值异常
                String correctedStatus = currentStatus.replace("bootRun", "");
                logger.warn("批次{}状态值异常: {}，修正为: {}", batchId, currentStatus, correctedStatus);
                
                jdbcTemplate.update(
                    "UPDATE answer_generation_batches SET status = ? WHERE id = ?",
                    correctedStatus, batchId);
                
                currentStatus = correctedStatus;
            }
            
            // 直接更新状态为GENERATING_ANSWERS，不做状态检查
            jdbcTemplate.update(
                "UPDATE answer_generation_batches SET status = 'GENERATING_ANSWERS', last_activity_time = ? WHERE id = ?",
                LocalDateTime.now(), batchId);
            
            logger.info("批次{}状态已更新为GENERATING_ANSWERS", batchId);
            
            // 确保批次状态为GENERATING_ANSWERS
            // 这是关键部分：使用AtomicBoolean确保任务只执行一次
            final AtomicBoolean taskStarted = new AtomicBoolean(false);
            
            // 启动状态同步线程，确保状态正确
            Thread statusSyncThread = new Thread(() -> {
                try {
                    int checkCount = 0;
                    while (checkCount < 10) { // 最多检查10次，避免无限循环
                        // 每秒检查一次批次状态
                        try {
                            String status = jdbcTemplate.queryForObject(
                                "SELECT status FROM answer_generation_batches WHERE id = ?", 
                                String.class, batchId);
                            
                            logger.debug("状态同步线程: 批次{}当前状态为{}", batchId, status);
                            
                            // 如果状态是GENERATING_ANSWERS，确保内存状态一致并清除中断标志
                            if ("GENERATING_ANSWERS".equals(status) || "RESUMING".equals(status)) {
                                answerGenerationTask.updateBatchMemoryState(batchId, status);
                                answerGenerationTask.clearInterruptionFlag(batchId);
                                logger.debug("状态同步线程: 批次{}处于活动状态{}，已同步内存状态并清除中断标志", batchId, status);
                                
                                // 只在第一次检查时启动任务
                                if (!taskStarted.getAndSet(true)) {
                                    logger.info("状态同步线程: 批次{}准备开始处理任务", batchId);
                                    // 通过新线程启动任务，避免在当前线程中阻塞
                                    new Thread(() -> {
                                        try {
                                            logger.info("开始同步处理批次: {}", batchId);
                                            answerGenerationTask.startBatchAnswerGeneration(batchId);
                                            logger.info("批次{}处理完成", batchId);
                                        } catch (Exception e) {
                                            logger.error("批次{}处理失败: {}", batchId, e.getMessage(), e);
                                        }
                                    }).start();
                                }
                            }
                            // 如果状态被手动改为暂停，则同步到所有运行
                            else if ("PAUSED".equals(status)) {
                                jdbcTemplate.update(
                                    "UPDATE model_answer_runs SET status = 'PAUSED' " + 
                                    "WHERE answer_generation_batch_id = ? AND status = 'GENERATING_ANSWERS'",
                                    batchId);
                                
                                logger.info("批次{}状态为暂停，已同步到运行状态", batchId);
                            }
                            
                            // 如果批次已完成或失败，退出监控
                            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                                logger.info("批次{}已{}，状态同步线程退出", batchId, status);
                                break;
                            }
                        } catch (Exception e) {
                            logger.error("状态同步线程异常", e);
                        }
                        
                        // 等待1秒
                        Thread.sleep(1000);
                        checkCount++;
                    }
                    
                    // 如果经过10次检查后任务还未启动，强制启动一次
                    if (!taskStarted.get()) {
                        logger.warn("批次{}经过多次检查后仍未启动任务，强制启动", batchId);
                        answerGenerationTask.startBatchAnswerGeneration(batchId);
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.info("状态同步线程被中断");
                }
            });
            statusSyncThread.setDaemon(true);
            statusSyncThread.start();
            
            // 等待状态同步线程启动任务
            try {
                // 最多等待5秒
                for (int i = 0; i < 5; i++) {
                    if (taskStarted.get()) {
                        logger.info("批次{}任务已成功启动", batchId);
                        break;
                    }
                    Thread.sleep(1000);
                }
                
                // 如果5秒后任务仍未启动，直接在主线程启动
                if (!taskStarted.get()) {
                    logger.warn("批次{}等待5秒后任务仍未启动，在主线程中启动", batchId);
                    answerGenerationTask.startBatchAnswerGeneration(batchId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
        } catch (Exception e) {
            logger.error("批次{}处理失败: {}", batchId, e.getMessage(), e);
            // 批次处理失败时发送通知
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("batchId", batchId);
            errorData.put("error", e.getMessage());
            errorData.put("timestamp", System.currentTimeMillis());
            webSocketService.sendBatchMessage(batchId, MessageType.ERROR, errorData);
            
            // 更新批次状态
            AnswerGenerationBatch batch = batchRepository.findById(batchId).orElseThrow();
            batch.setStatus(BatchStatus.FAILED);
            batch.setLastActivityTime(LocalDateTime.now());
            batch.setErrorMessage(e.getMessage());
            batchRepository.saveAndFlush(batch);
            
            throw new RuntimeException("批次处理失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 测试所有模型的连通性
     * @param runs 运行列表
     * @return 连接失败的模型名称列表
     */
    private List<String> testModelsConnectivity(List<ModelAnswerRun> runs) {
        List<String> failedModels = new ArrayList<>();
        
        logger.info("开始测试{}个模型的连通性", runs.size());
        
        // 创建临时线程池并行测试所有模型
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(runs.size(), 5));
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (ModelAnswerRun run : runs) {
            LlmModel model = run.getLlmModel();
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    logger.info("测试模型连通性: {}", model.getName());
                    // 使用LlmApiService中的方法进行测试，传入模型名称
                    return llmApiService.testModelConnectivity(
                        model.getApiUrl(), 
                        model.getApiKey(), 
                        model.getApiType(),
                        model.getName());  // 传入模型表中的模型名称
                } catch (Exception e) {
                    logger.error("模型 {} 连通性测试失败: {}", model.getName(), e.getMessage());
                    return false;
                }
            }, executor);
            
            futures.add(future);
        }
        
        // 收集结果
        for (int i = 0; i < runs.size(); i++) {
            try {
                LlmModel model = runs.get(i).getLlmModel();
                // 根据模型类型设置不同的超时时间
                int timeoutSeconds = getModelTimeoutSeconds(model);
                
                Boolean success = futures.get(i).get(timeoutSeconds, TimeUnit.SECONDS);
                if (!success) {
                    failedModels.add(model.getName() + " (" + model.getProvider() + ")");
                }
            } catch (Exception e) {
                LlmModel model = runs.get(i).getLlmModel();
                logger.error("模型 {} 连通性测试超时或出错", model.getName(), e);
                failedModels.add(model.getName() + " (" + model.getProvider() + ")");
            }
        }
        
        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(120, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        if (failedModels.isEmpty()) {
            logger.info("所有模型连通性测试通过");
        } else {
            logger.error("部分模型连通性测试失败: {}", String.join(", ", failedModels));
        }
        
        return failedModels;
    }
    
    /**
     * 根据模型类型获取适当的超时时间（秒）
     * @param model 模型
     * @return 超时时间（秒）
     */
    private int getModelTimeoutSeconds(LlmModel model) {
        // 默认超时时间为60秒
        int timeout = 600;
        
        if (model == null || model.getName() == null) {
            return timeout;
        }
        
        String modelName = model.getName().toLowerCase();
        String provider = model.getProvider() != null ? model.getProvider().toLowerCase() : "";
        
        // 根据模型名称和提供商设置不同的超时时间
        if (modelName.contains("gpt-4") || modelName.contains("gpt4")) {
            // GPT-4系列模型超时时间较长
            timeout = 900;
        } else if (modelName.contains("claude")) {
            // Claude模型超时时间
            timeout = 750;
        } else if (modelName.contains("gpt-3.5") || modelName.contains("gpt3")) {
            // GPT-3.5模型
            timeout = 450;
        } else if (provider.contains("anthropic")) {
            // Anthropic其他模型
            timeout = 750;
        } else if (provider.contains("openai")) {
            // OpenAI其他模型
            timeout = 600;
        } else if (modelName.contains("gemini") || modelName.contains("palm")) {
            // Google模型
            timeout = 600;
        } else if (modelName.contains("llama") || modelName.contains("mixtral")) {
            // 开源大型模型
            timeout = 900;
        }
        
        // 可以从配置中读取更精确的超时设置
        // TODO: 从配置文件中读取模型特定的超时时间
        
        logger.debug("模型 {} 设置的测试超时时间为 {}秒", model.getName(), timeout);
        return timeout;
    }
    
    @Override
    @Transactional
    public void pauseBatch(Long batchId, String reason) {
        logger.info("委托BatchStateManager暂停批次: {}, 原因: {}", batchId, reason);
        boolean success = batchStateManager.pauseBatch(batchId, reason);
        if (!success) {
            throw new IllegalStateException("无法暂停批次 " + batchId);
        }
    }
    
    @Override
    @Transactional
    public void resumeBatch(Long batchId) {
        logger.info("恢复批次: {}", batchId);
        
        // 使用事务模板确保状态更新完成
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        
        // 创建一个直接启动任务的回调函数
        Runnable startTaskCallback = () -> {
            try {
                // 使用新事务直接启动任务处理
                transactionTemplate.execute(status -> {
                    try {
                        // 获取批次关联的所有运行
                        logger.info("获取批次{}关联的所有运行", batchId);
                        List<ModelAnswerRun> runs = runRepository.findByAnswerGenerationBatchId(batchId);
                        if (runs.isEmpty()) {
                            String errorMsg = String.format("批次(ID: %d)没有关联的运行", batchId);
                            logger.error(errorMsg);
                            throw new IllegalStateException(errorMsg);
                        }
                        
                        // 测试所有模型的连通性
                        logger.info("执行恢复前的模型连通性测试...");
                        List<String> failedModels = testModelsConnectivity(runs);
                        
                        if (!failedModels.isEmpty()) {
                            String errorMsg = "恢复失败，以下模型连接测试失败: " + String.join(", ", failedModels);
                            logger.error("批次(ID: {})恢复失败: {}", batchId, errorMsg);
                            
                            // 更新批次状态为失败
                            AnswerGenerationBatch batch = batchRepository.findById(batchId).orElseThrow();
                            batch.setStatus(BatchStatus.FAILED);
                            batch.setLastActivityTime(LocalDateTime.now());
                            batch.setErrorMessage(errorMsg);
                            batchRepository.saveAndFlush(batch);
                            
                            // 通过WebSocket发送错误通知
                            Map<String, Object> payload = new HashMap<>();
                            payload.put("batchId", batch.getId());
                            payload.put("status", batch.getStatus().name());
                            payload.put("error", errorMsg);
                            payload.put("failedModels", failedModels);
                            
                            webSocketService.sendBatchMessage(batchId, MessageType.ERROR, payload);
                            
                            throw new IllegalStateException(errorMsg);
                        }
                        
                        logger.info("所有模型连通性测试通过，立即启动批次处理");
                        
                        // 更新批次的恢复计数
                        jdbcTemplate.update(
                            "UPDATE answer_generation_batches SET resume_count = resume_count + 1 WHERE id = ?", 
                            batchId);
                        
                        // 通过WebSocket发送批次恢复通知
                        Map<String, Object> notificationData = new HashMap<>();
                        notificationData.put("batchId", batchId);
                        notificationData.put("status", "IN_PROGRESS");
                        notificationData.put("resumeTime", LocalDateTime.now());
                        webSocketService.sendBatchMessage(batchId, MessageType.STATUS_CHANGE, notificationData);
                        
                        // 立即更新状态为IN_PROGRESS
                        jdbcTemplate.update(
                            "UPDATE answer_generation_batches SET status = 'IN_PROGRESS', last_activity_time = ? WHERE id = ?",
                            LocalDateTime.now(), batchId);
                        
                        if (batchStateManager != null) {
                            batchStateManager.setBatchState(batchId, "IN_PROGRESS");
                        }
                        
                        // 更新运行状态为GENERATING_ANSWERS
                        jdbcTemplate.update(
                            "UPDATE model_answer_runs SET status = 'GENERATING_ANSWERS', last_activity_time = ? " +
                            "WHERE answer_generation_batch_id = ? AND status = 'PAUSED' OR status = 'RESUMING'",
                            LocalDateTime.now(), batchId);
                        
                        // 直接启动批次处理，不再依赖调度器
                        logger.info("立即启动批次{}的处理任务", batchId);
                        answerGenerationTask.startBatchAnswerGeneration(batchId);
                        
                        return null;
                    } catch (Exception e) {
                        logger.error("恢复批次{}处理准备阶段失败", batchId, e);
                        
                        // 发送错误通知
                        Map<String, Object> errorData = new HashMap<>();
                        errorData.put("batchId", batchId);
                        errorData.put("error", "恢复准备失败: " + e.getMessage());
                        errorData.put("timestamp", System.currentTimeMillis());
                        webSocketService.sendBatchMessage(batchId, MessageType.ERROR, errorData);
                        
                        throw new RuntimeException("恢复准备失败: " + e.getMessage(), e);
                    }
                });
            } catch (Exception e) {
                logger.error("启动恢复任务失败", e);
            }
        };
        
        // 先更新状态，状态更新成功后再执行任务启动回调
        boolean success = batchStateManager.resumeBatch(batchId);
        if (success) {
            // 状态更新成功后，执行回调启动任务
            startTaskCallback.run();
        } else {
            throw new IllegalStateException("无法恢复批次 " + batchId);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public AnswerGenerationBatchDTO getBatchStatus(Long batchId) {
        // 待实现
        return null;
    }
    
    @Override
    @Transactional(readOnly = true)
    public ModelAnswerRunDTO getRunStatus(Long runId) {
        // 待实现
        return null;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AnswerGenerationBatchDTO> getBatchesByUserId(Long userId) {
        // 待实现
        return null;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ModelAnswerRunDTO> getRunsByBatchId(Long batchId) {
        // 待实现
        return null;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ModelAnswerRunDTO> getRunsByModelId(Long modelId) {
        // 待实现
        return null;
    }
    
    @Override
    @Transactional
    public void resetFailedBatch(Long batchId) {
        logger.info("重置失败的批次状态: {}", batchId);
        
        // 获取批次
        AnswerGenerationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的批次(ID: " + batchId + ")"));
        
        // 验证批次状态是否为FAILED
        if (batch.getStatus() != BatchStatus.FAILED) {
            throw new IllegalStateException("只能重置FAILED状态的批次，当前状态: " + batch.getStatus());
        }
        
        // 更新批次状态为PAUSED
        batch.setStatus(BatchStatus.PAUSED);
        batch.setLastActivityTime(LocalDateTime.now());
        batch.setPauseTime(LocalDateTime.now());
        batch.setPauseReason("从FAILED状态手动重置");
        batch.setErrorMessage(null); // 清除错误信息
        batchRepository.saveAndFlush(batch);
        
        // 更新所有失败和运行中的批次运行状态为PAUSED
        List<ModelAnswerRun> runs = runRepository.findByAnswerGenerationBatchId(batchId);
        for (ModelAnswerRun run : runs) {
            if (run.getStatus() == RunStatus.FAILED || 
                run.getStatus() == RunStatus.GENERATING_ANSWERS) {
                
                run.setStatus(RunStatus.PAUSED);
                run.setLastActivityTime(LocalDateTime.now());
                run.setPauseTime(LocalDateTime.now());
                run.setPauseReason("批次从FAILED状态手动重置");
                runRepository.save(run);
            }
        }
        
        // 发送状态变更通知
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("batchId", batchId);
        notificationData.put("status", "PAUSED");
        notificationData.put("message", "批次已从FAILED状态重置为PAUSED");
        notificationData.put("timestamp", System.currentTimeMillis());
        
        webSocketService.sendBatchMessage(batchId, MessageType.STATUS_CHANGE, notificationData);
        
        logger.info("批次{}状态已从FAILED重置为PAUSED", batchId);
    }
    
    @Override
    public void sendErrorNotification(Long batchId, Map<String, Object> errorData) {
        webSocketService.sendBatchMessage(batchId, MessageType.ERROR, errorData);
    }
    
    @Override
    public void forceBatchResume(Long batchId) {
        logger.info("强制恢复批次执行: {}", batchId);
        
        try {
            // 无论如何，先清除Redis和内存中的中断标志
            // 这是最关键的改动，确保即使数据库更新失败，中断标志也会被清除
            if (batchStateManager != null) {
                logger.info("批次{}：清除Redis中的中断标志", batchId);
                batchStateManager.setInterruptFlag(batchId, false);
                batchStateManager.setBatchState(batchId, "GENERATING_ANSWERS");
            }
            
            // 通知任务清除内存中的中断标志
            if (answerGenerationTask != null) {
                logger.info("批次{}：清除任务内存中的中断标志", batchId);
                answerGenerationTask.clearInterruptionFlag(batchId);
                answerGenerationTask.updateBatchMemoryState(batchId, "GENERATING_ANSWERS");
            }
            
            // 首先检查批次当前状态，避免重复恢复
            String currentStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM answer_generation_batches WHERE id = ?", 
                String.class, batchId);
                
            // 只有PAUSED状态的批次才需要恢复数据库状态
            if (!"PAUSED".equals(currentStatus)) {
                logger.info("批次{}当前状态为{}，无需在数据库中恢复", batchId, currentStatus);
                return;
            }
            
            // 使用乐观锁和处理实例标记，确保只有一个进程能处理该批次
            String processingInstance = UUID.randomUUID().toString();
            int updated = jdbcTemplate.update(
                "UPDATE answer_generation_batches SET status = 'GENERATING_ANSWERS', last_activity_time = ?, " +
                "processing_instance = ? WHERE id = ? AND status = 'PAUSED' AND " + 
                "(processing_instance IS NULL OR processing_instance = '')",
                LocalDateTime.now(), processingInstance, batchId);
                
            if (updated == 0) {
                logger.info("批次{}数据库状态更新失败，可能已被其他进程处理，但中断标志已清除", batchId);
                return;
            }
            
            logger.info("批次{}状态已更新为GENERATING_ANSWERS并获取处理权", batchId);
            
            // 更新所有PAUSED状态的运行为GENERATING_ANSWERS
            jdbcTemplate.update(
                "UPDATE model_answer_runs SET status = 'GENERATING_ANSWERS', last_activity_time = ? " +
                "WHERE answer_generation_batch_id = ? AND status = 'PAUSED'",
                LocalDateTime.now(), batchId);
            
            // 通过WebSocket发送批次恢复通知
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("batchId", batchId);
            notificationData.put("status", "GENERATING_ANSWERS");
            notificationData.put("resumeTime", LocalDateTime.now());
            webSocketService.sendBatchMessage(batchId, MessageType.STATUS_CHANGE, notificationData);
            
            try {
                // 启动一个新线程执行批次处理任务
                taskExecutor.submit(() -> {
                    try {
                        answerGenerationTask.startBatchAnswerGeneration(batchId);
                        logger.info("批次{}处理任务完成", batchId);
                    } catch (Exception e) {
                        logger.error("批次{}处理任务执行失败: {}", batchId, e.getMessage(), e);
                    } finally {
                        // 任务结束后清除处理标记
                        try {
                            jdbcTemplate.update(
                                "UPDATE answer_generation_batches SET processing_instance = NULL WHERE id = ? AND processing_instance = ?",
                                batchId, processingInstance);
                            logger.info("批次{}处理完成，已清除处理标记", batchId);
                        } catch (Exception ex) {
                            logger.error("清除批次{}处理标记时出错", batchId, ex);
                        }
                    }
                });
            } catch (Exception e) {
                // 如果无法提交任务，清除处理标记
                jdbcTemplate.update(
                    "UPDATE answer_generation_batches SET processing_instance = NULL WHERE id = ? AND processing_instance = ?",
                    batchId, processingInstance);
                throw e;
            }
            
        } catch (Exception e) {
            logger.error("强制恢复批次{}失败: {}", batchId, e.getMessage(), e);
            throw new RuntimeException("强制恢复批次失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> testSystemConnectivity() {
        logger.info("测试系统所有模型连通性");
        long startTime = System.currentTimeMillis();
        
        // 获取所有已配置的模型
        List<LlmModel> allModels = llmModelRepository.findAll();
        if (allModels.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "系统中未配置任何模型");
            result.put("timestamp", System.currentTimeMillis());
            return result;
        }
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> modelResults = new ArrayList<>();
        int passedCount = 0;
        int failedCount = 0;
        
        // 创建线程池并行测试所有模型
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(allModels.size(), 5));
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
        
        // 提交所有测试任务
        for (LlmModel model : allModels) {
            CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> {
                Map<String, Object> modelResult = new HashMap<>();
                modelResult.put("modelId", model.getId());
                modelResult.put("modelName", model.getName());
                modelResult.put("provider", model.getProvider());
                modelResult.put("apiEndpoint", model.getApiUrl());
                
                long modelStartTime = System.currentTimeMillis();
                try {
                    // 测试模型连通性，传入模型名称
                    boolean connected = llmApiService.testModelConnectivity(
                        model.getApiUrl(),
                        model.getApiKey(),
                        model.getApiType(),
                        model.getName()  // 传入模型表中的模型名称
                    );
                    
                    long responseTime = System.currentTimeMillis() - modelStartTime;
                    modelResult.put("connected", connected);
                    modelResult.put("responseTime", responseTime);
                    
                    if (!connected) {
                        modelResult.put("error", "API测试失败");
                    }
                    
                    return modelResult;
                } catch (Exception e) {
                    long responseTime = System.currentTimeMillis() - modelStartTime;
                    modelResult.put("connected", false);
                    modelResult.put("responseTime", responseTime);
                    modelResult.put("error", e.getMessage());
                    return modelResult;
                }
            }, executor);
            
            futures.add(future);
        }
        
        // 收集所有测试结果
        for (int i = 0; i < allModels.size(); i++) {
            try {
                // 设置合理的超时时间
                int timeoutSeconds = getModelTimeoutSeconds(allModels.get(i));
                Map<String, Object> modelResult = futures.get(i).get(timeoutSeconds, TimeUnit.SECONDS);
                modelResults.add(modelResult);
                
                if ((Boolean)modelResult.get("connected")) {
                    passedCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                Map<String, Object> modelResult = new HashMap<>();
                modelResult.put("modelId", allModels.get(i).getId());
                modelResult.put("modelName", allModels.get(i).getName());
                modelResult.put("provider", allModels.get(i).getProvider());
                modelResult.put("connected", false);
                modelResult.put("error", "测试超时或异常: " + e.getMessage());
                modelResults.add(modelResult);
                failedCount++;
            }
        }
        
        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(120, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 组装最终结果
        long testDuration = System.currentTimeMillis() - startTime;
        result.put("success", true);
        result.put("timestamp", System.currentTimeMillis());
        result.put("totalModels", allModels.size());
        result.put("passedModels", passedCount);
        result.put("failedModels", failedCount);
        result.put("testDuration", testDuration);
        result.put("modelResults", modelResults);
        
        return result;
    }
    
    @Override
    public Map<String, Object> testModelConnectivity(Long modelId) {
        logger.info("测试模型{}连通性", modelId);
        long startTime = System.currentTimeMillis();
        
        // 获取指定模型
        LlmModel model = llmModelRepository.findById(modelId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的模型(ID: " + modelId + ")"));
        
        Map<String, Object> result = new HashMap<>();
        result.put("modelId", model.getId());
        result.put("modelName", model.getName());
        result.put("provider", model.getProvider());
        result.put("apiEndpoint", model.getApiUrl());
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            // 测试模型连通性，传入模型名称
            boolean connected = llmApiService.testModelConnectivity(
                model.getApiUrl(),
                model.getApiKey(),
                model.getApiType(),
                model.getName()  // 传入模型表中的模型名称
            );
            
            long responseTime = System.currentTimeMillis() - startTime;
            result.put("connected", connected);
            result.put("responseTime", responseTime);
            result.put("success", connected);
            
            if (!connected) {
                result.put("error", "API测试失败");
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            result.put("connected", false);
            result.put("responseTime", responseTime);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> testBatchModelsConnectivity(Long batchId) {
        logger.info("测试批次{}关联的所有模型连通性", batchId);
        long startTime = System.currentTimeMillis();
        
        // 获取批次
        AnswerGenerationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的批次(ID: " + batchId + ")"));
        
        // 获取批次关联的所有运行
        List<ModelAnswerRun> runs = runRepository.findByAnswerGenerationBatchId(batchId);
        if (runs.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("batchId", batchId);
            result.put("batchName", batch.getName());
            result.put("error", "批次未关联任何运行");
            result.put("timestamp", System.currentTimeMillis());
            return result;
        }
        
        // 提取运行中使用的所有模型（去重）
        Map<Long, LlmModel> modelsMap = new HashMap<>();
        for (ModelAnswerRun run : runs) {
            LlmModel model = run.getLlmModel();
            if (!modelsMap.containsKey(model.getId())) {
                modelsMap.put(model.getId(), model);
            }
        }
        
        List<LlmModel> models = new ArrayList<>(modelsMap.values());
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> modelResults = new ArrayList<>();
        int passedCount = 0;
        int failedCount = 0;
        
        // 创建线程池并行测试所有模型
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(models.size(), 5));
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
        
        // 提交所有测试任务
        for (LlmModel model : models) {
            CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> {
                Map<String, Object> modelResult = new HashMap<>();
                modelResult.put("modelId", model.getId());
                modelResult.put("modelName", model.getName());
                modelResult.put("provider", model.getProvider());
                modelResult.put("apiEndpoint", model.getApiUrl());
                
                long modelStartTime = System.currentTimeMillis();
                try {
                    // 测试模型连通性，传入模型名称
                    boolean connected = llmApiService.testModelConnectivity(
                        model.getApiUrl(),
                        model.getApiKey(),
                        model.getApiType(),
                        model.getName()  // 传入模型表中的模型名称
                    );
                    
                    long responseTime = System.currentTimeMillis() - modelStartTime;
                    modelResult.put("connected", connected);
                    modelResult.put("responseTime", responseTime);
                    
                    if (!connected) {
                        modelResult.put("error", "API测试失败");
                    }
                    
                    // 查找使用该模型的运行
                    List<ModelAnswerRun> modelRuns = runs.stream()
                        .filter(run -> run.getLlmModel().getId().equals(model.getId()))
                        .collect(Collectors.toList());
                    
                    List<Map<String, Object>> runInfos = new ArrayList<>();
                    for (ModelAnswerRun run : modelRuns) {
                        Map<String, Object> runInfo = new HashMap<>();
                        runInfo.put("runId", run.getId());
                        runInfo.put("runName", run.getRunName());
                        runInfo.put("status", run.getStatus());
                        runInfos.add(runInfo);
                    }
                    modelResult.put("runs", runInfos);
                    
                    return modelResult;
                } catch (Exception e) {
                    long responseTime = System.currentTimeMillis() - modelStartTime;
                    modelResult.put("connected", false);
                    modelResult.put("responseTime", responseTime);
                    modelResult.put("error", e.getMessage());
                    return modelResult;
                }
            }, executor);
            
            futures.add(future);
        }
        
        // 收集所有测试结果
        for (int i = 0; i < models.size(); i++) {
            try {
                // 设置合理的超时时间
                int timeoutSeconds = getModelTimeoutSeconds(models.get(i));
                Map<String, Object> modelResult = futures.get(i).get(timeoutSeconds, TimeUnit.SECONDS);
                modelResults.add(modelResult);
                
                if ((Boolean)modelResult.get("connected")) {
                    passedCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                Map<String, Object> modelResult = new HashMap<>();
                modelResult.put("modelId", models.get(i).getId());
                modelResult.put("modelName", models.get(i).getName());
                modelResult.put("provider", models.get(i).getProvider());
                modelResult.put("connected", false);
                modelResult.put("error", "测试超时或异常: " + e.getMessage());
                modelResults.add(modelResult);
                failedCount++;
            }
        }
        
        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(120, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 组装最终结果
        long testDuration = System.currentTimeMillis() - startTime;
        result.put("success", true);
        result.put("batchId", batchId);
        result.put("batchName", batch.getName());
        result.put("batchStatus", batch.getStatus());
        result.put("timestamp", System.currentTimeMillis());
        result.put("totalModels", models.size());
        result.put("passedModels", passedCount);
        result.put("failedModels", failedCount);
        result.put("testDuration", testDuration);
        result.put("modelResults", modelResults);
        
        return result;
    }
    
    // 辅助方法
    private AnswerGenerationBatchDTO convertToDTO(AnswerGenerationBatch batch) {
        if (batch == null) {
            return null;
        }
        
        AnswerGenerationBatchDTO dto = new AnswerGenerationBatchDTO();
        dto.setId(batch.getId());
        dto.setName(batch.getName());
        dto.setDescription(batch.getDescription());
        dto.setDatasetVersionId(batch.getDatasetVersion().getId());
        dto.setDatasetVersionName(batch.getDatasetVersion().getName() + " " + batch.getDatasetVersion().getVersionNumber());
        
        // 将jdbc包中的BatchStatus转换为entity包中的BatchStatus
        switch (batch.getStatus()) {
            case PENDING:
                dto.setStatus(BatchStatus.PENDING);
                break;
            case GENERATING_ANSWERS:
                dto.setStatus(BatchStatus.GENERATING_ANSWERS);
                break;
            case COMPLETED:
                dto.setStatus(BatchStatus.COMPLETED);
                break;
            case FAILED:
                dto.setStatus(BatchStatus.FAILED);
                break;
            case PAUSED:
                dto.setStatus(BatchStatus.PAUSED);
                break;
            case RESUMING:
                dto.setStatus(BatchStatus.RESUMING);
                break;
            default:
                dto.setStatus(BatchStatus.PENDING);
                break;
        }
        
        dto.setCreationTime(batch.getCreationTime());
        
        if (batch.getAnswerAssemblyConfig() != null) {
            dto.setAnswerAssemblyConfigId(batch.getAnswerAssemblyConfig().getId());
        }
        
        if (batch.getEvaluationAssemblyConfig() != null) {
            dto.setEvaluationAssemblyConfigId(batch.getEvaluationAssemblyConfig().getId());
        }
        
        // 转换题型prompt信息
        if (batch.getSingleChoicePrompt() != null) {
            dto.setSingleChoicePromptId(batch.getSingleChoicePrompt().getId());
            dto.setSingleChoicePromptName(batch.getSingleChoicePrompt().getName());
        }
        
        if (batch.getMultipleChoicePrompt() != null) {
            dto.setMultipleChoicePromptId(batch.getMultipleChoicePrompt().getId());
            dto.setMultipleChoicePromptName(batch.getMultipleChoicePrompt().getName());
        }
        
        if (batch.getSimpleFactPrompt() != null) {
            dto.setSimpleFactPromptId(batch.getSimpleFactPrompt().getId());
            dto.setSimpleFactPromptName(batch.getSimpleFactPrompt().getName());
        }
        
        if (batch.getSubjectivePrompt() != null) {
            dto.setSubjectivePromptId(batch.getSubjectivePrompt().getId());
            dto.setSubjectivePromptName(batch.getSubjectivePrompt().getName());
        }
        
        dto.setGlobalParameters(batch.getGlobalParameters());
        
        if (batch.getCreatedByUser() != null) {
            dto.setCreatedByUserId(batch.getCreatedByUser().getId());
            dto.setCreatedByUsername(batch.getCreatedByUser().getUsername());
        }
        
        dto.setCompletedAt(batch.getCompletedAt());
        dto.setProgressPercentage(batch.getProgressPercentage());
        dto.setLastActivityTime(batch.getLastActivityTime());
        dto.setResumeCount(batch.getResumeCount());
        dto.setPauseTime(batch.getPauseTime());
        dto.setPauseReason(batch.getPauseReason());
        dto.setAnswerRepeatCount(batch.getAnswerRepeatCount());
        
        // 添加上次处理的运行ID
        if (batch.getLastProcessedRunId() != null) {
            dto.setLastProcessedRunId(batch.getLastProcessedRunId());
        }
        
        return dto;
    }
    
    private ModelAnswerRunDTO convertToDTO(ModelAnswerRun run) {
        // 待实现
        return null;
    }
}
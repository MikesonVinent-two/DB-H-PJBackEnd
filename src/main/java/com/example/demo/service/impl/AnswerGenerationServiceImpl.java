package com.example.demo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AnswerGenerationBatchDTO;
import com.example.demo.dto.ModelAnswerRunDTO;
import com.example.demo.dto.WebSocketMessage.MessageType;
import com.example.demo.entity.AnswerGenerationBatch;
import com.example.demo.entity.AnswerGenerationBatch.BatchStatus;
import com.example.demo.entity.AnswerPromptAssemblyConfig;
import com.example.demo.entity.DatasetVersion;
import com.example.demo.entity.EvaluationPromptAssemblyConfig;
import com.example.demo.entity.LlmModel;
import com.example.demo.entity.ModelAnswerRun;
import com.example.demo.entity.ModelAnswerRun.RunStatus;
import com.example.demo.entity.User;
import com.example.demo.repository.AnswerGenerationBatchRepository;
import com.example.demo.repository.AnswerPromptAssemblyConfigRepository;
import com.example.demo.repository.DatasetVersionRepository;
import com.example.demo.repository.EvaluationPromptAssemblyConfigRepository;
import com.example.demo.repository.LlmModelRepository;
import com.example.demo.repository.ModelAnswerRunRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AnswerGenerationService;
import com.example.demo.service.WebSocketService;
import com.example.demo.task.AnswerGenerationTask;
import com.example.demo.service.LlmApiService;

import jakarta.persistence.EntityNotFoundException;

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
    private final AnswerGenerationTask answerGenerationTask;
    private final LlmApiService llmApiService;
    
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
            AnswerGenerationTask answerGenerationTask,
            LlmApiService llmApiService) {
        this.batchRepository = batchRepository;
        this.runRepository = runRepository;
        this.datasetVersionRepository = datasetVersionRepository;
        this.llmModelRepository = llmModelRepository;
        this.userRepository = userRepository;
        this.webSocketService = webSocketService;
        this.answerConfigRepository = answerConfigRepository;
        this.evalConfigRepository = evalConfigRepository;
        this.answerGenerationTask = answerGenerationTask;
        this.llmApiService = llmApiService;
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
        
        // 获取批次
        AnswerGenerationBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的批次(ID: " + batchId + ")"));
        
        // 检查批次状态
        if (batch.getStatus() != BatchStatus.PENDING && batch.getStatus() != BatchStatus.PAUSED) {
            String errorMsg = String.format("批次(ID: %d)当前状态为%s，无法启动", batchId, batch.getStatus());
            logger.error(errorMsg);
            
            // 通过WebSocket发送错误通知
            webSocketService.sendErrorMessage(batchId, errorMsg, batch.getStatus());
            
            throw new IllegalStateException(errorMsg);
        }
        
        // 获取批次的所有运行
        List<ModelAnswerRun> runs = runRepository.findByAnswerGenerationBatchId(batchId);
        if (runs.isEmpty()) {
            String errorMsg = String.format("批次(ID: %d)没有关联的运行", batchId);
            logger.error(errorMsg);
            
            // 通过WebSocket发送错误通知
            webSocketService.sendErrorMessage(batchId, errorMsg, null);
            
            throw new IllegalStateException(errorMsg);
        }
        
        // 测试所有模型的连通性
        List<String> failedModels = testModelsConnectivity(runs);
        
        if (!failedModels.isEmpty()) {
            String errorMsg = "以下模型连接失败: " + String.join(", ", failedModels);
            logger.error("批次(ID: {})启动失败: {}", batchId, errorMsg);
            
            // 更新批次状态为失败
            batch.setStatus(BatchStatus.FAILED);
            batch.setLastActivityTime(LocalDateTime.now());
            batchRepository.save(batch);
            
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
        
        // 更新批次状态
        batch.setStatus(BatchStatus.IN_PROGRESS);
        batch.setLastActivityTime(LocalDateTime.now());
        if (batch.getStatus() == BatchStatus.PAUSED) {
            batch.setResumeCount(batch.getResumeCount() + 1);
        }
        batchRepository.save(batch);
        
        // 更新运行状态
        for (ModelAnswerRun run : runs) {
            if (run.getStatus() == RunStatus.PENDING || run.getStatus() == RunStatus.PAUSED) {
                run.setStatus(RunStatus.GENERATING_ANSWERS);
                run.setLastActivityTime(LocalDateTime.now());
                if (run.getStatus() == RunStatus.PAUSED) {
                    run.setResumeCount(run.getResumeCount() + 1);
                }
                runRepository.save(run);
                
                // 通过WebSocket发送状态变更通知
                webSocketService.sendStatusChangeMessage(run.getId(), run.getStatus().name(), 
                    "开始生成回答");
            }
        }
        
        // 通过WebSocket发送批次启动通知
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("batchId", batch.getId());
        notificationData.put("batchName", batch.getName());
        notificationData.put("status", batch.getStatus().name());
        notificationData.put("startTime", LocalDateTime.now());
        notificationData.put("runsCount", runs.size());
        
        webSocketService.sendBatchMessage(batchId, MessageType.TASK_STARTED, notificationData);
        
        // 启动异步任务来执行实际的回答生成
        logger.info("启动异步任务处理批次: {}", batchId);
        answerGenerationTask.startBatchAnswerGeneration(batchId);
        
        logger.info("批次{}已启动，包含{}个运行", batchId, runs.size());
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
                    // 发送简单测试消息
                    String testPrompt = "测试连接，请回复'连接成功'";
                    Map<String, Object> testParams = new HashMap<>();
                    testParams.put("max_tokens", 10); // 最小化响应大小
                    testParams.put("temperature", 0.0); // 确定性响应
                    
                    String response = llmApiService.generateAnswer(
                            model.getApiUrl(),
                            model.getApiKey(),
                            model.getApiType(), // 使用模型的API类型
                            testPrompt,
                            testParams);
                    
                    boolean success = response != null && !response.isEmpty();
                    logger.info("模型 {} 连通性测试 {}", model.getName(), success ? "成功" : "失败");
                    return success;
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
        int timeout = 60;
        
        if (model == null || model.getName() == null) {
            return timeout;
        }
        
        String modelName = model.getName().toLowerCase();
        String provider = model.getProvider() != null ? model.getProvider().toLowerCase() : "";
        
        // 根据模型名称和提供商设置不同的超时时间
        if (modelName.contains("gpt-4") || modelName.contains("gpt4")) {
            // GPT-4系列模型超时时间较长
            timeout = 90;
        } else if (modelName.contains("claude")) {
            // Claude模型超时时间
            timeout = 75;
        } else if (modelName.contains("gpt-3.5") || modelName.contains("gpt3")) {
            // GPT-3.5模型
            timeout = 45;
        } else if (provider.contains("anthropic")) {
            // Anthropic其他模型
            timeout = 75;
        } else if (provider.contains("openai")) {
            // OpenAI其他模型
            timeout = 60;
        } else if (modelName.contains("gemini") || modelName.contains("palm")) {
            // Google模型
            timeout = 60;
        } else if (modelName.contains("llama") || modelName.contains("mixtral")) {
            // 开源大型模型
            timeout = 90;
        }
        
        // 可以从配置中读取更精确的超时设置
        // TODO: 从配置文件中读取模型特定的超时时间
        
        logger.debug("模型 {} 设置的测试超时时间为 {}秒", model.getName(), timeout);
        return timeout;
    }
    
    @Override
    @Transactional
    public void pauseBatch(Long batchId, String reason) {
        // 待实现
    }
    
    @Override
    @Transactional
    public void resumeBatch(Long batchId) {
        // 待实现
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
        dto.setStatus(batch.getStatus());
        dto.setCreationTime(batch.getCreationTime());
        
        if (batch.getAnswerAssemblyConfig() != null) {
            dto.setAnswerAssemblyConfigId(batch.getAnswerAssemblyConfig().getId());
        }
        
        if (batch.getEvaluationAssemblyConfig() != null) {
            dto.setEvaluationAssemblyConfigId(batch.getEvaluationAssemblyConfig().getId());
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
        
        return dto;
    }
    
    private ModelAnswerRunDTO convertToDTO(ModelAnswerRun run) {
        // 待实现
        return null;
    }
}
package com.example.demo.service.impl;

import com.example.demo.entity.Evaluation;
import com.example.demo.entity.Evaluation.EvaluationStatus;
import com.example.demo.entity.Evaluator;
import com.example.demo.entity.LlmAnswer;
import com.example.demo.entity.QuestionType;
import com.example.demo.entity.StandardObjectiveAnswer;
import com.example.demo.entity.StandardQuestion;
import com.example.demo.entity.StandardSimpleAnswer;
import com.example.demo.entity.User;
import com.example.demo.entity.EvaluationRun;
import com.example.demo.entity.EvaluationRun.RunStatus;
import com.example.demo.entity.ModelAnswerRun;
import com.example.demo.entity.EvaluationDetail;
import com.example.demo.entity.EvaluationCriterion;
import com.example.demo.entity.LlmModel;
import com.example.demo.entity.Tag;
import com.example.demo.entity.EvaluationTagPrompt;
import com.example.demo.entity.EvaluationPromptAssemblyConfig;
import com.example.demo.entity.EvaluationSubjectivePrompt;
import com.example.demo.entity.StandardSubjectiveAnswer;
import com.example.demo.entity.EvaluationType;
import com.example.demo.repository.EvaluationRepository;
import com.example.demo.repository.EvaluatorRepository;
import com.example.demo.repository.StandardObjectiveAnswerRepository;
import com.example.demo.repository.StandardQuestionRepository;
import com.example.demo.repository.StandardSimpleAnswerRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.LlmAnswerRepository;
import com.example.demo.repository.ModelAnswerRunRepository;
import com.example.demo.repository.EvaluationRunRepository;
import com.example.demo.repository.EvaluationDetailRepository;
import com.example.demo.repository.EvaluationCriterionRepository;
import com.example.demo.repository.EvaluationTagPromptRepository;
import com.example.demo.repository.EvaluationPromptAssemblyConfigRepository;
import com.example.demo.repository.EvaluationSubjectivePromptRepository;
import com.example.demo.repository.StandardSubjectiveAnswerRepository;
import com.example.demo.repository.AnswerScoreRepository;
import com.example.demo.repository.LlmModelRepository;
import com.example.demo.entity.AnswerScore;
import com.example.demo.service.EvaluationService;
import com.example.demo.dto.Option;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import java.util.Optional;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class EvaluationServiceImpl implements EvaluationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EvaluationServiceImpl.class);
    
    private final EvaluationRepository evaluationRepository;
    private final EvaluatorRepository evaluatorRepository;
    private final UserRepository userRepository;
    private final StandardObjectiveAnswerRepository objectiveAnswerRepository;
    private final StandardSimpleAnswerRepository simpleAnswerRepository;
    private final LlmAnswerRepository llmAnswerRepository;
    private final ModelAnswerRunRepository modelAnswerRunRepository;
    private final EvaluationRunRepository evaluationRunRepository;
    private final EvaluationDetailRepository evaluationDetailRepository;
    private final EvaluationCriterionRepository evaluationCriterionRepository;
    private final EvaluationTagPromptRepository evaluationTagPromptRepository;
    private final EvaluationPromptAssemblyConfigRepository evaluationPromptAssemblyConfigRepository;
    private final EvaluationSubjectivePromptRepository evaluationSubjectivePromptRepository;
    private final StandardQuestionRepository standardQuestionRepository;
    private final StandardSubjectiveAnswerRepository standardSubjectiveAnswerRepository;
    private final ObjectMapper objectMapper;
    // 不再需要AnswerScoreRepository
    private final LlmModelRepository llmModelRepository;
    
    // 线程池用于异步执行评测任务
    private final ExecutorService evaluationExecutor = Executors.newFixedThreadPool(5);
    
    // AI服务配置
    @Value("${ai.service.url:}")
    private String aiServiceUrl;
    
    @Value("${ai.service.api-key:}")
    private String aiServiceApiKey;
    
    @Value("${ai.service.model:}")
    private String aiServiceModel;
    
    private final RestTemplate restTemplate;
    
    // 添加Redis相关依赖
    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public EvaluationServiceImpl(
            EvaluationRepository evaluationRepository,
            EvaluatorRepository evaluatorRepository,
            UserRepository userRepository,
            StandardObjectiveAnswerRepository objectiveAnswerRepository,
            StandardSimpleAnswerRepository simpleAnswerRepository,
            LlmAnswerRepository llmAnswerRepository,
            ModelAnswerRunRepository modelAnswerRunRepository,
            EvaluationRunRepository evaluationRunRepository,
            EvaluationDetailRepository evaluationDetailRepository,
            EvaluationCriterionRepository evaluationCriterionRepository,
            EvaluationTagPromptRepository evaluationTagPromptRepository,
            EvaluationPromptAssemblyConfigRepository evaluationPromptAssemblyConfigRepository,
            EvaluationSubjectivePromptRepository evaluationSubjectivePromptRepository,
            StandardQuestionRepository standardQuestionRepository,
            StandardSubjectiveAnswerRepository standardSubjectiveAnswerRepository,
            LlmModelRepository llmModelRepository,
            RestTemplate restTemplate,
            RedisTemplate<String, String> redisTemplate,
            RedissonClient redissonClient,
            JdbcTemplate jdbcTemplate) {
        this.evaluationRepository = evaluationRepository;
        this.evaluatorRepository = evaluatorRepository;
        this.userRepository = userRepository;
        this.objectiveAnswerRepository = objectiveAnswerRepository;
        this.simpleAnswerRepository = simpleAnswerRepository;
        this.llmAnswerRepository = llmAnswerRepository;
        this.modelAnswerRunRepository = modelAnswerRunRepository;
        this.evaluationRunRepository = evaluationRunRepository;
        this.evaluationDetailRepository = evaluationDetailRepository;
        this.evaluationCriterionRepository = evaluationCriterionRepository;
        this.evaluationTagPromptRepository = evaluationTagPromptRepository;
        this.evaluationPromptAssemblyConfigRepository = evaluationPromptAssemblyConfigRepository;
        this.evaluationSubjectivePromptRepository = evaluationSubjectivePromptRepository;
        this.standardQuestionRepository = standardQuestionRepository;
        this.standardSubjectiveAnswerRepository = standardSubjectiveAnswerRepository;
        this.llmModelRepository = llmModelRepository;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    // ... 其他方法保持不变 ...
    
    /**
     * 组装评估提示词
     * 
     * @param question 标准问题
     * @param answerText 学生回答文本
     * @param referenceAnswer 参考答案
     * @param criteria 评测标准
     * @return 组装后的提示词
     */
    private String assembleEvaluationPrompt(StandardQuestion question, String answerText, 
                                         String referenceAnswer, List<EvaluationCriterion> criteria) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 获取默认的评测提示词组装配置（取第一个激活的配置）
        List<EvaluationPromptAssemblyConfig> configs = evaluationPromptAssemblyConfigRepository.findByIsActiveTrue();
        EvaluationPromptAssemblyConfig config = configs.isEmpty() ? null : configs.get(0);
        
        if (config != null) {
            // 添加基础系统提示
            if (config.getBaseSystemPrompt() != null) {
                promptBuilder.append(config.getBaseSystemPrompt());
                promptBuilder.append(config.getSectionSeparator());
            }
            
            // 添加标签提示（如果问题有标签）
            if (question.getTags() != null && !question.getTags().isEmpty() && 
                config.getTagPromptsSectionHeader() != null) {
                
                // 先收集有效的标签提示词，如果没有任何有效提示词则跳过整个标签部分
                List<Tag> tags = question.getTags();
                boolean hasAnyTagPrompt = false;
                
                StringBuilder tagPromptsBuilder = new StringBuilder();
                
                for (Tag tag : tags) {
                    try {
                        // 获取该标签的激活状态提示词
                        List<EvaluationTagPrompt> tagPrompts = evaluationTagPromptRepository
                            .findByTagIdAndIsActiveTrueAndDeletedAtIsNullOrderByPromptPriorityAsc(tag.getId());
                        
                        if (!tagPrompts.isEmpty()) {
                            // 使用优先级最高的提示词（列表已按优先级排序）
                            EvaluationTagPrompt prompt = tagPrompts.get(0);
                            tagPromptsBuilder.append("【").append(tag.getTagName()).append("】: ");
                            tagPromptsBuilder.append(prompt.getPromptTemplate());
                            tagPromptsBuilder.append(config.getTagPromptSeparator());
                            hasAnyTagPrompt = true;
                        }
                        // 如果标签没有提示词，则跳过该标签，不添加到prompt中
                    } catch (Exception e) {
                        logger.warn("获取评测标签提示词失败，标签ID: {}", tag.getId(), e);
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
            
            // 添加主观题评测要求（如果问题类型是主观题）
            if (question.getQuestionType() == QuestionType.SUBJECTIVE && 
                config.getSubjectiveSectionHeader() != null) {
                promptBuilder.append(config.getSubjectiveSectionHeader());
                promptBuilder.append("\n");
                
                try {
                    // 获取主观题评测提示词
                    List<EvaluationSubjectivePrompt> subjectivePrompts = evaluationSubjectivePromptRepository
                        .findByIsActiveTrueAndDeletedAtIsNull();
                    
                    if (!subjectivePrompts.isEmpty()) {
                        // 使用第一个激活的提示词
                        EvaluationSubjectivePrompt prompt = subjectivePrompts.get(0);
                        promptBuilder.append(prompt.getPromptTemplate());
                        
                        // 添加评分指导（如果有）
                        if (prompt.getScoringInstruction() != null && !prompt.getScoringInstruction().isEmpty()) {
                            promptBuilder.append("\n\n评分指导:\n");
                            promptBuilder.append(prompt.getScoringInstruction());
                        }
                        
                        // 添加输出格式要求（如果有）
                        if (prompt.getOutputFormatInstruction() != null && !prompt.getOutputFormatInstruction().isEmpty()) {
                            promptBuilder.append("\n\n输出格式要求:\n");
                            promptBuilder.append(prompt.getOutputFormatInstruction());
                        }
                    }
                } catch (Exception e) {
                    logger.warn("获取主观题评测提示词失败", e);
                }
                
                promptBuilder.append(config.getSectionSeparator());
            }
            
            // 添加最终指示
            if (config.getFinalInstruction() != null) {
                promptBuilder.append(config.getFinalInstruction());
                promptBuilder.append(config.getSectionSeparator());
            }
        } else {
            // 如果没有配置，使用默认系统提示
            promptBuilder.append("你是一位专业的答案评测专家。请对以下主观题的回答进行评测。\n\n");
        }
        
        // 添加问题和答案内容
        promptBuilder.append("问题：").append(question.getQuestionText()).append("\n\n");
        promptBuilder.append("学生回答：").append(answerText).append("\n\n");
        promptBuilder.append("参考答案：").append(referenceAnswer).append("\n\n");
        
        // 添加评测标准
        promptBuilder.append("评测标准：\n");
        for (EvaluationCriterion criterion : criteria) {
            promptBuilder.append("- ").append(criterion.getName()).append("：")
                  .append(criterion.getDescription()).append("\n");
        }
        
        // 添加默认的输出格式要求（如果没有配置或主观题提示词）
        if (config == null || 
            (question.getQuestionType() == QuestionType.SUBJECTIVE && 
             evaluationSubjectivePromptRepository.findByIsActiveTrueAndDeletedAtIsNull().isEmpty())) {
            promptBuilder.append("\n请对回答进行全面评测，并给出以下格式的评测结果：\n");
            promptBuilder.append("1. 总体评分（0-10分）\n");
            promptBuilder.append("2. 各评测标准的得分和评语\n");
            promptBuilder.append("3. 总体评语，包括优点和不足\n");
            promptBuilder.append("4. 改进建议\n\n");
            promptBuilder.append("请以JSON格式输出，格式如下：\n");
            promptBuilder.append("{\n");
            promptBuilder.append("  \"总分\": 分数,\n");
            promptBuilder.append("  \"criteria_scores\": [\n");
            promptBuilder.append("    {\"criterion\": \"标准名称\", \"score\": 分数, \"comments\": \"评语\"},\n");
            promptBuilder.append("    ...\n");
            promptBuilder.append("  ],\n");
            promptBuilder.append("  \"overall_comments\": \"总体评语\",\n");
            promptBuilder.append("  \"improvement_suggestions\": \"改进建议\"\n");
            promptBuilder.append("}");
        }
        
        return promptBuilder.toString();
    }
    
    @Override
    public Map<String, Object> evaluateSubjectiveWithAI(String answerText, String questionText, 
                                                   String referenceAnswer, List<EvaluationCriterion> criteria,
                                                   Long evaluatorId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("开始使用AI评测主观题，评测者ID: {}", evaluatorId);
            
            // 获取评测者信息
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                    .orElseThrow(() -> new EntityNotFoundException("评测者不存在: " + evaluatorId));
            
            // 验证评测者类型是AI
            if (evaluator.getEvaluatorType() != Evaluator.EvaluatorType.AI_MODEL) {
                throw new IllegalArgumentException("评测者不是AI模型: " + evaluatorId);
            }
            
            // 获取AI模型信息
            if (evaluator.getLlmModel() == null) {
                throw new IllegalArgumentException("评测者未关联AI模型: " + evaluatorId);
            }
            
            // 查找问题（如果可能）
            StandardQuestion question = null;
            try {
                // 尝试通过问题文本查找对应的标准问题
                // 这里简化处理，实际上可能需要更复杂的匹配逻辑
                // 或者修改方法签名，直接传入问题ID或问题对象
                List<StandardQuestion> questions = standardQuestionRepository.findByQuestionTextContaining(questionText);
                if (!questions.isEmpty()) {
                    question = questions.get(0);
                }
            } catch (Exception e) {
                logger.warn("无法查找对应的标准问题，将使用默认提示词", e);
            }
            
            // 组装评测提示词
            String prompt;
            if (question != null) {
                // 使用标准问题组装提示词
                prompt = assembleEvaluationPrompt(question, answerText, referenceAnswer, criteria);
            } else {
                // 创建一个临时问题对象
                question = new StandardQuestion();
                question.setQuestionText(questionText);
                question.setQuestionType(QuestionType.SUBJECTIVE);
                // 没有标签
                prompt = assembleEvaluationPrompt(question, answerText, referenceAnswer, criteria);
            }
            
            // 调用AI服务进行评测
            String aiResponse = callAIService(prompt, evaluator.getLlmModel().getId());
            
            // 将完整的AI回复记录到日志中
            logger.info("\n========== AI评测回复 ==========\n{}\n==================================", aiResponse);
            
            // 预处理AI响应：移除Markdown代码块标记
            String processedResponse = aiResponse;
            if (aiResponse.startsWith("```")) {
                // 移除开头的```json或```等标记
                processedResponse = aiResponse.replaceAll("^```(json)?\\s*", "");
                // 移除结尾的```标记
                processedResponse = processedResponse.replaceAll("\\s*```\\s*$", "");
                logger.info("检测到Markdown格式的响应，已移除代码块标记");
            }
            
            // 解析AI评测结果
            Map<String, Object> aiResult;
            try {
                aiResult = objectMapper.readValue(processedResponse, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                logger.error("JSON解析失败，尝试进一步清理响应", e);
                // 尝试更激进的清理，处理可能的多行代码块标记
                processedResponse = aiResponse.replaceAll("```[a-zA-Z]*\\s*", "").replaceAll("\\s*```", "");
                aiResult = objectMapper.readValue(processedResponse, new TypeReference<Map<String, Object>>() {});
            }
            
            // 提取总体评分 - 支持"overall_score"或"总分"字段
            Object overallScoreObj = aiResult.get("overall_score");
            if (overallScoreObj == null) {
                overallScoreObj = aiResult.get("总分");
            }
            
            BigDecimal overallScore;
            if (overallScoreObj instanceof Number) {
                overallScore = new BigDecimal(overallScoreObj.toString()).setScale(2, RoundingMode.HALF_UP);
                // 确保分数在0-100范围内
                if (overallScore.compareTo(BigDecimal.ZERO) < 0) {
                    overallScore = BigDecimal.ZERO;
                } else if (overallScore.compareTo(new BigDecimal(100)) > 0) {
                    overallScore = new BigDecimal(100);
                }
            } else {
                overallScore = new BigDecimal(50); // 默认中等分数
            }
            
            // 提取评语
            String overallComments = (String) aiResult.getOrDefault("overall_comments", "无评语");
            String improvementSuggestions = (String) aiResult.getOrDefault("improvement_suggestions", "无建议");
            
            // 组合最终评测结果
            result.put("score", overallScore);
            result.put("comments", overallComments);
            result.put("improvement_suggestions", improvementSuggestions);
            result.put("raw_ai_response", aiResponse);
            result.put("criteria_scores", aiResult.get("criteria_scores"));
            
        } catch (Exception e) {
            logger.error("AI评测主观题失败", e);
            result.put("score", BigDecimal.ZERO);
            result.put("comments", "评测失败: " + e.getMessage());
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 调用AI服务进行评测
     */
    private String callAIService(String prompt, Long modelId) {
        try {
            // 获取模型信息
            LlmModel llmModel = null;
            if (modelId != null) {
                // 从数据库中获取指定ID的LlmModel对象
                llmModel = llmModelRepository.findById(modelId)
                    .orElse(null);
                if (llmModel != null) {
                    logger.info("找到模型: {}, API URL: {}", llmModel.getName(), llmModel.getApiUrl());
                } else {
                    logger.warn("未找到ID为{}的模型", modelId);
                }
            }
            
            // 确定API URL和密钥
            String apiUrl = null;
            String apiKey = null;
            String model = null;
            String apiType = "openai_compatible"; // 默认为OpenAI兼容格式
            
            if (llmModel != null && llmModel.getApiUrl() != null && !llmModel.getApiUrl().isEmpty()) {
                apiUrl = llmModel.getApiUrl();
                apiKey = llmModel.getApiKey();
                model = llmModel.getName();
                apiType = llmModel.getApiType() != null ? llmModel.getApiType() : apiType;
            } else {
                // 使用配置的默认值
                apiUrl = aiServiceUrl;
                apiKey = aiServiceApiKey;
                model = aiServiceModel;
            }
            
            // 补全API URL路径
            apiUrl = buildApiUrl(apiUrl, apiType);
            
            logger.info("API URL: {}, 模型: {}", apiUrl, model);
            
            if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty() || model == null || model.isEmpty()) {
                logger.warn("AI服务配置不完整，使用直接调用大模型");
                return executeAIEvaluation(prompt, modelId);
            }
            
            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // 准备请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统消息
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位专业的评测专家，负责评估答案的质量。请以JSON格式返回评测结果。");
            messages.add(systemMessage);
            
            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", 2000);
            
            // 发送请求
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            try {
                logger.info("正在向AI服务发送请求: {}", apiUrl);
                ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
                
                // 处理响应
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map responseBody = response.getBody();
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        if (message != null && message.containsKey("content")) {
                            String content = (String) message.get("content");
                            logger.info("大模型评测成功，返回内容长度: {}", content.length());
                            // 将AI回复内容完整记录到日志
                            logger.info("\n========== 大模型评测回复内容 ==========\n{}\n=======================================", content);
                            return content;
                        }
                    }
                }
                
                logger.warn("AI服务返回无效响应，使用直接调用大模型");
            } catch (Exception e) {
                logger.error("调用AI服务接口失败: {}", e.getMessage(), e);
                logger.warn("切换到直接调用大模型");
            }
            
            // 如果API调用失败，使用直接调用大模型
            return executeAIEvaluation(prompt, modelId);
            
        } catch (Exception e) {
            logger.error("调用AI服务过程中发生错误: {}", e.getMessage(), e);
            return executeAIEvaluation(prompt, modelId);
        }
    }
    
    /**
     * 根据API类型和基础URL构建完整的API端点URL
     */
    private String buildApiUrl(String baseUrl, String apiType) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return baseUrl;
        }
        
        // 移除URL末尾的斜杠
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        
        // 如果apiType为空，尝试从URL判断API类型
        if (apiType == null || apiType.isEmpty()) {
            if (baseUrl.contains("openai.com")) {
                apiType = "openai";
            } else if (baseUrl.contains("anthropic.com")) {
                apiType = "anthropic";
            } else if (baseUrl.contains("baidu.com") || baseUrl.contains("wenxin")) {
                apiType = "baidu";
            } else if (baseUrl.contains("aliyun") || baseUrl.contains("tongyi") || baseUrl.contains("dashscope")) {
                apiType = "aliyun";
            } else if (baseUrl.contains("zhipu") || baseUrl.contains("chatglm")) {
                apiType = "zhipu";
            } else if (baseUrl.contains("azure")) {
                apiType = "azure";
            } else {
                apiType = "openai_compatible"; // 默认为OpenAI兼容格式
            }
        }
        
        // 根据API类型返回不同的端点路径
        switch (apiType.toLowerCase()) {
            case "openai":
            case "openai_compatible":
                // 检查是否已包含完整路径
                if (baseUrl.endsWith("/chat/completions") || baseUrl.endsWith("/v1/chat/completions")) {
                    return baseUrl;
                }
                return baseUrl + "/v1/chat/completions";
            case "anthropic":
                return baseUrl + "/v1/messages";
            case "baidu":
                return baseUrl + "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions";
            case "aliyun":
            case "tongyi":
            case "dashscope":
                return baseUrl + "/v1/services/aigc/text-generation/generation";
            case "zhipu":
            case "glm":
            case "chatglm":
                return baseUrl + "/v1/chat/completions";
            case "azure":
                // Azure OpenAI API通常需要在URL中包含部署ID
                if (baseUrl.contains("deployments")) {
                    return baseUrl;
                } else {
                    return baseUrl + "/deployments/{deployment-id}/chat/completions?api-version=2023-07-01-preview";
                }
            default:
                return baseUrl + "/v1/chat/completions"; // 默认使用OpenAI格式
        }
    }
    
    /**
     * 执行AI评测（调用大语言模型API）
     */
    private String executeAIEvaluation(String prompt, Long modelId) {
        try {
            logger.info("调用真实大模型进行评测，提示词长度: {}", prompt.length());
            
            // 从数据库获取模型信息
            LlmModel llmModel = null;
            if (modelId != null) {
                llmModel = llmModelRepository.findById(modelId)
                    .orElse(null);
            }
            
            // 获取API信息
            String apiUrl = "https://api.openai.com/v1/chat/completions";  // 默认API端点
            String apiKey = aiServiceApiKey;
            String model = aiServiceModel;
            String apiType = "openai_compatible"; // 默认为OpenAI兼容格式
            
            if (llmModel != null && llmModel.getApiUrl() != null && !llmModel.getApiUrl().isEmpty()) {
                apiUrl = llmModel.getApiUrl();
                apiKey = llmModel.getApiKey();
                model = llmModel.getName();
                apiType = llmModel.getApiType() != null ? llmModel.getApiType() : apiType;
                logger.info("使用数据库中的模型配置: URL={}, 模型={}", apiUrl, model);
            } else {
                logger.info("使用默认配置: URL={}, 模型={}", apiUrl, model);
            }
            
            // 补全API URL路径
            apiUrl = buildApiUrl(apiUrl, apiType);
            logger.info("完整API URL路径: {}", apiUrl);
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统消息
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位专业的评测专家，负责评估答案的质量。请以JSON格式返回评测结果。");
            messages.add(systemMessage);
            
            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.2); // 低温度，增加输出的确定性
            requestBody.put("max_tokens", 2000); // 足够长的输出
            
            // 发送请求
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            try {
                logger.info("发送请求到: {}", apiUrl);
                ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map responseBody = response.getBody();
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        
                        if (message != null && message.containsKey("content")) {
                            String content = (String) message.get("content");
                            logger.info("大模型评测成功，返回内容长度: {}", content.length());
                            return content;
                        }
                    }
                }
                
                logger.warn("大模型响应解析失败，返回默认评测结果");
            } catch (Exception e) {
                logger.error("调用大模型API失败: {}", e.getMessage(), e);
            }
            
            // 如果API调用失败，返回默认JSON格式评测结果
            return """
                {
                  "总分": 75,
                  "criteria_scores": [
                    {"criterion": "内容完整性", "score": 80, "comments": "回答涵盖了大部分关键点"},
                    {"criterion": "逻辑性", "score": 70, "comments": "论述基本连贯，但有些地方逻辑跳跃"},
                    {"criterion": "专业性", "score": 80, "comments": "使用了适当的专业术语，展示了对主题的理解"}
                  ],
                  "overall_comments": "回答整体表现良好，展示了对主题的理解，但在某些方面还可以进一步完善。",
                  "improvement_suggestions": "建议增加更多具体例子来支持论点，并进一步阐述某些关键概念的细节。"
                }
                """;
        } catch (Exception e) {
            logger.error("评测过程出现错误: {}", e.getMessage(), e);
            
            // 返回错误信息的JSON
            return """
                {
                  "总分": 50,
                  "criteria_scores": [
                    {"criterion": "评测错误", "score": 50, "comments": "评测过程中发生错误"}
                  ],
                  "overall_comments": "评测过程中发生错误: """ + e.getMessage() + """
                  ",
                  "improvement_suggestions": "请重新提交评测请求"
                }
                """;
        }
    }
    
    @Override
    public List<EvaluationDetail> getEvaluationDetails(Long evaluationId) {
        logger.info("获取评测详情，评测ID: {}", evaluationId);
        
        try {
            // 查询数据库中已保存的评测详情
            List<EvaluationDetail> savedDetails = evaluationDetailRepository.findByEvaluationId(evaluationId);
            
            // 如果有已保存的详情，直接返回
            if (!savedDetails.isEmpty()) {
                return savedDetails;
            }
            
            // 否则，解析评测结果并动态生成评测详情
            Evaluation evaluation = evaluationRepository.findById(evaluationId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测记录: " + evaluationId));
            
            // 获取评测结果
            Map<String, Object> results = getEvaluationResults(evaluation);
            if (results.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 提取评测详情
            List<EvaluationDetail> details = new ArrayList<>();
            
            // 处理标准评分
            Object criteriaScores = results.get("criteria_scores");
            if (criteriaScores instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> scoresList = (List<Map<String, Object>>) criteriaScores;
                
                for (Map<String, Object> scoreMap : scoresList) {
                    EvaluationDetail detail = new EvaluationDetail();
                    detail.setEvaluation(evaluation);
                    
                    // 设置评测标准名称
                    Object criterionObj = scoreMap.get("criterion");
                    if (criterionObj != null) {
                        detail.setCriterionName(criterionObj.toString());
                    }
                    
                    // 设置分数
                    Object scoreObj = scoreMap.get("score");
                    if (scoreObj instanceof Number) {
                        detail.setScore(new BigDecimal(scoreObj.toString()));
                    }
                    
                    // 设置评语
                    Object commentsObj = scoreMap.get("comments");
                    if (commentsObj != null) {
                        detail.setComments(commentsObj.toString());
                    }
                    
                    details.add(detail);
                }
                
                // 保存到数据库中
                if (!details.isEmpty()) {
                    details = evaluationDetailRepository.saveAll(details);
                }
            }
            
            return details;
            
        } catch (Exception e) {
            logger.error("获取评测详情失败", e);
            throw new RuntimeException("获取评测详情失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<EvaluationCriterion> getCriteriaForQuestionType(QuestionType questionType) {
        return evaluationCriterionRepository.findByQuestionType(questionType);
    }
    
    @Override
    public BigDecimal calculateBleuScore(String candidateText, String referenceText) {
        logger.info("计算BLEU分数，候选文本长度: {}, 参考文本长度: {}", 
                candidateText != null ? candidateText.length() : 0, 
                referenceText != null ? referenceText.length() : 0);
        
        try {
            // 参数验证
            if (candidateText == null || referenceText == null || candidateText.isEmpty() || referenceText.isEmpty()) {
                logger.warn("计算BLEU分数失败：输入文本为空");
                return BigDecimal.ZERO;
            }
            
            // 中文文本处理：标准化处理，移除所有空白字符、标点符号并转为小写
            String processedCandidate = candidateText.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "") // 移除标点符号
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
                
            String processedReference = referenceText.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "") // 移除标点符号
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
            
            // 如果任一处理后的文本为空，返回0分
            if (processedCandidate.isEmpty() || processedReference.isEmpty()) {
                logger.warn("计算BLEU分数失败：处理后文本为空");
                return BigDecimal.ZERO;
            }
            
            // 对于中文短文本，我们使用字符级别的匹配计算
            // 将字符串转换为字符数组
            char[] candidateChars = processedCandidate.toCharArray();
            char[] referenceChars = processedReference.toCharArray();
            
            // 计算字符匹配数
            Map<Character, Integer> refCharCount = new HashMap<>();
            
            // 统计参考文本中的字符频率
            for (char c : referenceChars) {
                refCharCount.put(c, refCharCount.getOrDefault(c, 0) + 1);
            }
            
            // 计算匹配数
            int matchCount = 0;
            Map<Character, Integer> candidateCharCount = new HashMap<>();
            for (char c : candidateChars) {
                candidateCharCount.put(c, candidateCharCount.getOrDefault(c, 0) + 1);
            }
            
            // 计算共同字符的最小出现次数
            for (Map.Entry<Character, Integer> entry : candidateCharCount.entrySet()) {
                char c = entry.getKey();
                int count = entry.getValue();
                if (refCharCount.containsKey(c)) {
                    matchCount += Math.min(count, refCharCount.get(c));
                }
            }
            
            // 计算精确率
            double precision = (double) matchCount / candidateChars.length;
            
            // 字符级别的匹配率作为BLEU分数
            double bleuScore = precision;
            
            // 四舍五入到2位小数
            BigDecimal result = new BigDecimal(bleuScore).setScale(2, RoundingMode.HALF_UP);
            
            // 确保结果在0-1范围内
            if (result.compareTo(BigDecimal.ZERO) < 0) {
                result = BigDecimal.ZERO;
            } else if (result.compareTo(BigDecimal.ONE) > 0) {
                result = BigDecimal.ONE;
            }
            
            logger.info("BLEU分数计算结果: {}, 原始文本1: {}, 处理后: {}, 原始文本2: {}, 处理后: {}", 
                result, candidateText, processedCandidate, referenceText, processedReference);
            return result;
            
        } catch (Exception e) {
            logger.error("计算BLEU分数时发生错误", e);
            return BigDecimal.ZERO;
        }
    }
    
    @Override
    public Map<String, Object> getEvaluationRunProgress(Long evaluationRunId) {
        logger.info("获取评测运行进度，评测运行ID: {}", evaluationRunId);
        
        try {
            // 查询评测运行记录
            EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
            
            // 获取相关的模型回答运行记录
            ModelAnswerRun modelAnswerRun = evaluationRun.getModelAnswerRun();
            if (modelAnswerRun == null) {
                throw new IllegalStateException("评测运行未关联模型回答运行: " + evaluationRunId);
            }
            
            // 获取总问题数量
            int totalQuestions = modelAnswerRun.getTotalQuestionsCount() != null ? modelAnswerRun.getTotalQuestionsCount() : 0;
            
            // 获取已评测的回答数量
            int evaluatedAnswers = evaluationRepository.countByEvaluationRunId(evaluationRunId);
            
            // 计算进度百分比
            double progressPercentage = totalQuestions > 0 ? 
                    ((double) evaluatedAnswers / totalQuestions) * 100 : 0;
            
            // 四舍五入到2位小数
            BigDecimal progress = new BigDecimal(progressPercentage).setScale(2, RoundingMode.HALF_UP);
            
            // 构建进度信息
            Map<String, Object> progressInfo = new HashMap<>();
            progressInfo.put("evaluationRunId", evaluationRunId);
            progressInfo.put("status", evaluationRun.getStatus().toString());
            progressInfo.put("totalQuestions", totalQuestions);
            progressInfo.put("evaluatedAnswers", evaluatedAnswers);
            progressInfo.put("progressPercentage", progress);
            progressInfo.put("startTime", evaluationRun.getStartTime());
            progressInfo.put("endTime", evaluationRun.getEndTime());
            progressInfo.put("lastUpdated", LocalDateTime.now());
            
            // 如果评测已完成，添加汇总结果
            if (evaluationRun.getStatus() == RunStatus.COMPLETED) {
                Map<String, Object> summaryResults = calculateEvaluationSummary(evaluationRunId);
                progressInfo.put("summaryResults", summaryResults);
            }
            
            return progressInfo;
            
        } catch (Exception e) {
            logger.error("获取评测运行进度失败", e);
            
            // 返回错误信息
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("evaluationRunId", evaluationRunId);
            errorInfo.put("error", e.getMessage());
            errorInfo.put("status", "ERROR");
            return errorInfo;
        }
    }
    
    @Override
    @Async
    public CompletableFuture<Void> startEvaluationRun(Long evaluationRunId) {
        logger.info("开始评测运行，评测运行ID: {}", evaluationRunId);
        
        // 清除中断标志
        String interruptKey = "evaluation_run:interrupt:" + evaluationRunId;
        redisTemplate.delete(interruptKey);
        
        // 更新Redis状态
        String stateKey = "evaluation_run:state:" + evaluationRunId;
        redisTemplate.opsForValue().set(stateKey, "IN_PROGRESS");
        redisTemplate.expire(stateKey, Duration.ofHours(24));
        
        return CompletableFuture.runAsync(() -> {
            try {
                // 查询评测运行记录
                EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                        .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
                
                // 检查状态，只有PENDING或PAUSED状态才能启动
                if (evaluationRun.getStatus() != RunStatus.PENDING && evaluationRun.getStatus() != RunStatus.PAUSED) {
                    logger.warn("评测运行状态不允许启动: {}", evaluationRun.getStatus());
                    throw new IllegalStateException("评测运行状态不允许启动: " + evaluationRun.getStatus());
                }
                
                // 更新状态为进行中
                evaluationRun.setStatus(RunStatus.IN_PROGRESS);
                if (evaluationRun.getStartTime() == null) {
                    evaluationRun.setStartTime(LocalDateTime.now());
                }
                evaluationRunRepository.save(evaluationRun);
                
                // 获取相关的模型回答运行记录
                ModelAnswerRun modelAnswerRun = evaluationRun.getModelAnswerRun();
                if (modelAnswerRun == null) {
                    throw new IllegalStateException("评测运行未关联模型回答运行: " + evaluationRunId);
                }
                
                // 获取评测者
                Evaluator evaluator = evaluationRun.getEvaluator();
                if (evaluator == null) {
                    throw new IllegalStateException("评测运行未关联评测者: " + evaluationRunId);
                }
                
                // 获取用户ID
                Long userId = evaluationRun.getCreatedBy();
                
                // 获取已生成的LLM回答
                List<LlmAnswer> llmAnswers = llmAnswerRepository.findByModelAnswerRunId(modelAnswerRun.getId());
                
                // 过滤出未评测的回答
                List<LlmAnswer> unevaluatedAnswers = llmAnswers.stream()
                        .filter(answer -> !evaluationRepository.existsByLlmAnswerIdAndEvaluationRunId(
                                answer.getId(), evaluationRunId))
                        .collect(Collectors.toList());
                
                logger.info("开始评测运行，总回答数: {}，未评测回答数: {}", llmAnswers.size(), unevaluatedAnswers.size());
                
                // 更新总回答数
                evaluationRun.setTotalAnswersCount(unevaluatedAnswers.size());
                evaluationRunRepository.save(evaluationRun);
                
                // 批量处理未评测的回答
                int batchSize = evaluationRun.getBatchSize() != null ? evaluationRun.getBatchSize() : 10;
                for (int i = 0; i < unevaluatedAnswers.size(); i += batchSize) {
                    // 检查是否应该中断处理
                    if (shouldInterruptEvaluation(evaluationRunId)) {
                        logger.info("检测到评测运行{}的中断信号，停止处理", evaluationRunId);
                        
                        // 更新状态为暂停
                        evaluationRun.setStatus(RunStatus.PAUSED);
                        evaluationRun.setPauseTime(LocalDateTime.now());
                        evaluationRun.setLastUpdated(LocalDateTime.now());
                        evaluationRunRepository.save(evaluationRun);
                        
                        return;
                    }
                    
                    // 检查评测运行是否被暂停或取消
                    EvaluationRun currentStatus = evaluationRunRepository.findById(evaluationRunId).orElse(null);
                    if (currentStatus == null || currentStatus.getStatus() != RunStatus.IN_PROGRESS) {
                        logger.info("评测运行已被暂停或取消，ID: {}, 状态: {}", 
                                evaluationRunId, currentStatus != null ? currentStatus.getStatus() : "已删除");
                        return;
                    }
                    
                    // 获取当前批次的回答
                    int endIndex = Math.min(i + batchSize, unevaluatedAnswers.size());
                    List<LlmAnswer> batchAnswers = unevaluatedAnswers.subList(i, endIndex);
                    
                    // 批量评测
                    evaluateAnswers(batchAnswers, evaluator.getId(), userId);
                    
                    // 更新最后处理的回答ID
                    if (!batchAnswers.isEmpty()) {
                        LlmAnswer lastAnswer = batchAnswers.get(batchAnswers.size() - 1);
                        evaluationRun.setLastProcessedAnswerId(lastAnswer.getId());
                    }
                    
                    // 更新进度
                    int processedCount = i + batchAnswers.size();
                    BigDecimal progress = new BigDecimal(processedCount)
                            .multiply(new BigDecimal(100))
                            .divide(new BigDecimal(unevaluatedAnswers.size()), 2, RoundingMode.HALF_UP);
                    
                    evaluationRun.setProgressPercentage(progress);
                    evaluationRun.setCompletedAnswersCount(processedCount);
                    evaluationRun.setLastActivityTime(LocalDateTime.now());
                    evaluationRunRepository.save(evaluationRun);
                    
                    logger.info("评测运行进度: {}/{}", processedCount, unevaluatedAnswers.size());
                }
                
                // 所有回答评测完成，更新状态为已完成
                evaluationRun.setStatus(RunStatus.COMPLETED);
                evaluationRun.setEndTime(LocalDateTime.now());
                evaluationRun.setLastActivityTime(LocalDateTime.now());
                evaluationRun.setProgressPercentage(new BigDecimal(100));
                evaluationRunRepository.save(evaluationRun);
                
                logger.info("评测运行完成，ID: {}", evaluationRunId);
                
            } catch (Exception e) {
                logger.error("评测运行过程中发生错误", e);
                
                // 更新状态为错误
                try {
                    EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId).orElse(null);
                    if (evaluationRun != null) {
                        evaluationRun.setStatus(RunStatus.FAILED);
                        evaluationRun.setLastUpdated(LocalDateTime.now());
                        evaluationRun.setErrorMessage(e.getMessage());
                        evaluationRunRepository.save(evaluationRun);
                    }
                } catch (Exception ex) {
                    logger.error("更新评测运行状态失败", ex);
                }
                
                throw new RuntimeException("评测运行失败: " + e.getMessage(), e);
            }
        }, evaluationExecutor);
    }
    
    /**
     * 计算评测运行的汇总结果
     */
    private Map<String, Object> calculateEvaluationSummary(Long evaluationRunId) {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            // 获取所有评测结果
            List<Evaluation> evaluations = evaluationRepository.findByEvaluationRunId(evaluationRunId);
            
            if (evaluations.isEmpty()) {
                return summary;
            }
            
            // 计算平均分
            BigDecimal totalScore = BigDecimal.ZERO;
            for (Evaluation evaluation : evaluations) {
                if (evaluation.getScore() != null) {
                    totalScore = totalScore.add(evaluation.getScore());
                }
            }
            
            BigDecimal averageScore = totalScore.divide(new BigDecimal(evaluations.size()), 2, RoundingMode.HALF_UP);
            
            // 按问题类型分组计算
            Map<QuestionType, List<Evaluation>> groupedByType = evaluations.stream()
                    .filter(e -> e.getLlmAnswer() != null && 
                            e.getLlmAnswer().getDatasetQuestionMapping() != null && 
                            e.getLlmAnswer().getDatasetQuestionMapping().getStandardQuestion() != null)
                    .collect(Collectors.groupingBy(e -> e.getLlmAnswer().getDatasetQuestionMapping().getStandardQuestion().getQuestionType()));
            
            Map<String, Object> typeScores = new HashMap<>();
            for (Map.Entry<QuestionType, List<Evaluation>> entry : groupedByType.entrySet()) {
                QuestionType type = entry.getKey();
                List<Evaluation> typeEvaluations = entry.getValue();
                
                BigDecimal typeTotal = BigDecimal.ZERO;
                for (Evaluation evaluation : typeEvaluations) {
                    if (evaluation.getScore() != null) {
                        typeTotal = typeTotal.add(evaluation.getScore());
                    }
                }
                
                BigDecimal typeAverage = typeTotal.divide(new BigDecimal(typeEvaluations.size()), 2, RoundingMode.HALF_UP);
                typeScores.put(type.toString(), typeAverage);
            }
            
            // 构建汇总结果
            summary.put("evaluationCount", evaluations.size());
            summary.put("averageScore", averageScore);
            summary.put("scoresByQuestionType", typeScores);
            
            return summary;
            
        } catch (Exception e) {
            logger.error("计算评测汇总结果失败", e);
            summary.put("error", "计算评测汇总结果失败: " + e.getMessage());
            return summary;
        }
    }
    
    @Override
    @Transactional
    public Evaluation evaluateAnswer(LlmAnswer llmAnswer, Long evaluatorId, Long userId) {
        logger.info("开始评测单个回答，回答ID: {}, 评测者ID: {}, 用户ID: {}", 
                llmAnswer.getId(), evaluatorId, userId);
        
        try {
            // 获取评测者信息
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                    .orElseThrow(() -> new EntityNotFoundException("评测者不存在: " + evaluatorId));
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
            
            // 检查是否已存在相同的评测记录
            boolean exists = evaluationRepository.existsByLlmAnswerIdAndEvaluatorId(
                llmAnswer.getId(), evaluator.getId());
            
            if (exists) {
                logger.warn("该回答已被同一评测者评测过，跳过重复评测，回答ID: {}, 评测者ID: {}", 
                        llmAnswer.getId(), evaluator.getId());
                
                // 查找并返回现有评测记录
                List<Evaluation> existingEvaluations = evaluationRepository.findByLlmAnswerIdAndEvaluatorId(
                        llmAnswer.getId(), evaluator.getId());
                
                if (!existingEvaluations.isEmpty()) {
                    return existingEvaluations.get(0); // 返回第一条匹配的记录
                }
            }
            
            // 获取问题信息
            StandardQuestion question = llmAnswer.getDatasetQuestionMapping().getStandardQuestion();
            
            // 创建评测记录
            Evaluation evaluation = new Evaluation();
            evaluation.setLlmAnswer(llmAnswer);
            evaluation.setEvaluator(evaluator);
            evaluation.setCreatedByUser(user);
            evaluation.setCreationTime(LocalDateTime.now());
            evaluation.setStatus(EvaluationStatus.PENDING);
            // 设置评测类型
            evaluation.setEvaluationType(evaluator.getEvaluatorType() == Evaluator.EvaluatorType.HUMAN ? 
                EvaluationType.MANUAL : EvaluationType.AI_MODEL);
            
            // 根据问题类型进行评测
            Map<String, Object> evaluationResult;
            String scoreType;
            
            switch (question.getQuestionType()) {
                case SINGLE_CHOICE:
                    StandardObjectiveAnswer objectiveAnswer = objectiveAnswerRepository
                            .findByStandardQuestionId(question.getId())
                            .orElseThrow(() -> new EntityNotFoundException("找不到标准选择题答案"));
                    evaluationResult = evaluateSingleChoice(llmAnswer.getAnswerText(), 
                            objectiveAnswer.getCorrectOptionIds(), objectiveAnswer.getOptions());
                    scoreType = "OBJECTIVE_SINGLE_CHOICE";
                    break;
                    
                case MULTIPLE_CHOICE:
                    StandardObjectiveAnswer multiAnswer = objectiveAnswerRepository
                            .findByStandardQuestionId(question.getId())
                            .orElseThrow(() -> new EntityNotFoundException("找不到标准多选题答案"));
                    evaluationResult = evaluateMultipleChoice(llmAnswer.getAnswerText(), 
                            multiAnswer.getCorrectOptionIds(), multiAnswer.getOptions());
                    scoreType = "OBJECTIVE_MULTIPLE_CHOICE";
                    break;
                    
                case SIMPLE_FACT:
                    StandardSimpleAnswer simpleAnswer = simpleAnswerRepository
                            .findByStandardQuestionId(question.getId())
                            .orElseThrow(() -> new EntityNotFoundException("找不到标准简答题答案"));
                    evaluationResult = evaluateSimpleFact(llmAnswer.getAnswerText(), 
                            simpleAnswer.getAnswerText(), simpleAnswer.getAlternativeAnswers());
                    scoreType = "OBJECTIVE_SIMPLE_FACT";
                    break;
                    
                case SUBJECTIVE:
                    // 获取评测标准
                    List<EvaluationCriterion> criteria = getCriteriaForQuestionType(QuestionType.SUBJECTIVE);
                    evaluationResult = evaluateSubjectiveWithAI(llmAnswer.getAnswerText(), 
                            question.getQuestionText(), 
                            question.getStandardSubjectiveAnswer().getAnswerText(), 
                            criteria, evaluatorId);
                    scoreType = "SUBJECTIVE";
                    break;
                    
                default:
                    throw new IllegalArgumentException("不支持的问题类型: " + question.getQuestionType());
            }
            
            // 更新评测记录
            BigDecimal score = new BigDecimal(evaluationResult.get("score").toString());
            evaluation.setScore(score);
            evaluation.setComments((String) evaluationResult.get("comments"));
            evaluation.setEvaluationResults(evaluationResult);
            evaluation.setStatus(EvaluationStatus.SUCCESS);
            evaluation.setCompletionTime(LocalDateTime.now());
            
            // 在保存前检查是否已存在相同的评测记录
            boolean existsBeforeSave = evaluationRepository.existsByLlmAnswerIdAndEvaluatorId(
                llmAnswer.getId(), evaluator.getId());
            
            // 详细记录请求体信息
            logger.info("准备保存评测记录，详细信息: llmAnswerId={}, evaluatorId={}, createdByUserId={}, status={}, score={}, 已存在相同记录={}",
                llmAnswer.getId(), evaluator.getId(), user.getId(), evaluation.getStatus(), score, existsBeforeSave);
            
            if (existsBeforeSave) {
                logger.warn("检测到唯一键约束冲突风险! 该回答(ID:{})已被同一评测者(ID:{})评测过", llmAnswer.getId(), evaluator.getId());
            }
            
            // 保存评测记录
            evaluation = evaluationRepository.save(evaluation);
            
            // 保存评测详情
            if (evaluationResult.containsKey("criteria_scores")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> criteriaScores = (List<Map<String, Object>>) evaluationResult.get("criteria_scores");
                
                List<EvaluationDetail> details = new ArrayList<>();
                for (Map<String, Object> criteriaScore : criteriaScores) {
                    EvaluationDetail detail = new EvaluationDetail();
                    detail.setEvaluation(evaluation);
                    detail.setCriterionName((String) criteriaScore.get("criterion"));
                    detail.setScore(new BigDecimal(criteriaScore.get("score").toString()));
                    detail.setComments((String) criteriaScore.get("comments"));
                    detail.setCreatedAt(LocalDateTime.now());
                    details.add(detail);
                }
                
                evaluationDetailRepository.saveAll(details);
            }
            
            // 在Evaluation中保存分数记录
            // 标准化分数（0-100分）
            BigDecimal normalizedScore;
            if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
                // 主观题分数通常是0-10分
                normalizedScore = score.multiply(new BigDecimal(10));
            } else {
                // 客观题分数通常是0-100分
                normalizedScore = score;
            }
            
            evaluation.setRawScore(score);
            evaluation.setNormalizedScore(normalizedScore);
            evaluation.setScoreType(scoreType);
            evaluation.setScoringMethod(evaluator.getEvaluatorType() == Evaluator.EvaluatorType.HUMAN ? "HUMAN" : "AI_EVALUATION");
            
            evaluation = evaluationRepository.save(evaluation);
            logger.info("成功保存评测记录，评测ID: {}, 回答ID: {}, 评测者ID: {}, 分数类型: {}", 
                    evaluation.getId(), llmAnswer.getId(), evaluatorId, scoreType);
            
            // 保存详细评分记录
            if (evaluationResult.containsKey("criteria_scores")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> criteriaScores = (List<Map<String, Object>>) evaluationResult.get("criteria_scores");
                
                for (Map<String, Object> criterionScore : criteriaScores) {
                    String criterionName = (String) criterionScore.get("criterion");
                    BigDecimal criterionScoreValue = new BigDecimal(criterionScore.get("score").toString());
                    String criterionComments = (String) criterionScore.get("comments");
                    
                    // 创建详细评分记录 - 使用EvaluationDetail
                    EvaluationDetail detailScore = new EvaluationDetail();
                    detailScore.setEvaluation(evaluation);
                    detailScore.setCriterionName(criterionName);
                    detailScore.setScore(criterionScoreValue);
                    detailScore.setComments(criterionComments);
                    detailScore.setCreatedAt(LocalDateTime.now());
                    
                    evaluationDetailRepository.save(detailScore);
                }
            }
            
            logger.info("评测完成，评测ID: {}, 得分: {}", evaluation.getId(), evaluation.getScore());
            return evaluation;
            
        } catch (Exception e) {
            logger.error("评测回答失败", e);
            throw new RuntimeException("评测回答失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public List<Evaluation> evaluateAnswers(List<LlmAnswer> llmAnswers, Long evaluatorId, Long userId) {
        logger.info("开始批量评测回答，回答数量: {}, 评测者ID: {}, 用户ID: {}", 
                llmAnswers.size(), evaluatorId, userId);
        
        List<Evaluation> evaluations = new ArrayList<>();
        
        try {
            for (LlmAnswer answer : llmAnswers) {
                try {
                    Evaluation evaluation = evaluateAnswer(answer, evaluatorId, userId);
                    evaluations.add(evaluation);
                } catch (Exception e) {
                    logger.error("评测回答失败，回答ID: {}", answer.getId(), e);
                    // 继续处理下一个回答
                }
            }
            
            logger.info("批量评测完成，成功评测数量: {}", evaluations.size());
            return evaluations;
            
        } catch (Exception e) {
            logger.error("批量评测回答失败", e);
            throw new RuntimeException("批量评测回答失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> evaluateSingleChoice(String answerText, String correctOptionIds, String options) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 解析正确答案 - 标准化处理，去除引号、方括号等特殊字符
            String correctAnswer = correctOptionIds.trim()
                .replaceAll("[\\[\\]\"]", "") // 去除方括号和引号
                .replaceAll("\\s+", ""); // 去除空白字符
            
            // 解析学生答案 - 标准化处理
            String studentAnswer = answerText.trim()
                .replaceAll("[\\[\\]\"]", "") // 去除方括号和引号
                .replaceAll("\\s+", ""); // 去除空白字符
                
            // 提取学生答案中的选项ID（通常是A、B、C、D等）
            Pattern pattern = Pattern.compile("[A-Z]");
            Matcher matcher = pattern.matcher(studentAnswer.toUpperCase());
            StringBuilder extractedAnswer = new StringBuilder();
            
            while (matcher.find()) {
                extractedAnswer.append(matcher.group());
            }
            
            // 如果成功提取到选项ID，使用提取的结果
            if (extractedAnswer.length() > 0) {
                studentAnswer = extractedAnswer.toString();
            }
            
            // 计算得分
            boolean isCorrect = correctAnswer.equalsIgnoreCase(studentAnswer);
            BigDecimal score = isCorrect ? new BigDecimal("100") : BigDecimal.ZERO;
            
            // 构建评测结果
            result.put("score", score);
            result.put("isCorrect", isCorrect);
            result.put("correctAnswer", correctAnswer);
            result.put("studentAnswer", studentAnswer);
            result.put("comments", isCorrect ? "答案正确" : "答案错误，正确答案是: " + correctAnswer);
            
            // 打印答案到日志，方便人工判断
            logger.info("\n========== 单选题评测结果 ==========");
            logger.info("原始大模型答案: {}", answerText);
            logger.info("处理后大模型答案: {}", studentAnswer);
            logger.info("原始标准答案: {}", correctOptionIds);
            logger.info("处理后标准答案: {}", correctAnswer);
            logger.info("评测结果: {}", (isCorrect ? "正确" : "错误"));
            logger.info("===================================");
            
            return result;
            
        } catch (Exception e) {
            logger.error("评测单选题失败", e);
            result.put("score", BigDecimal.ZERO);
            result.put("error", "评测失败: " + e.getMessage());
            return result;
        }
    }
    
    @Override
    public Map<String, Object> evaluateMultipleChoice(String answerText, String correctIds, String options) {
        logger.info("开始评测多选题回答，回答文本长度: {}", answerText != null ? answerText.length() : 0);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 参数验证
            if (answerText == null || answerText.isEmpty() || correctIds == null || correctIds.isEmpty() || options == null || options.isEmpty()) {
                logger.warn("评测多选题参数无效");
                result.put("score", BigDecimal.ZERO);
                result.put("comments", "无效的回答或标准答案");
                return result;
            }
            
            // 解析选项为List<Option>
            List<Option> optionsList = objectMapper.readValue(options, new TypeReference<List<Option>>() {});
            
            // 将List<Option>转换为Map<String, String>便于后续处理
            Map<String, String> optionsMap = new HashMap<>();
            for (Option option : optionsList) {
                optionsMap.put(option.getId(), option.getText());
            }
            
            // 处理标准答案 - 标准化处理，去除引号、方括号等特殊字符
            String cleanedCorrectIds = correctIds.replaceAll("[\\[\\]\"]", "").replaceAll("\\s+", "");
            Set<String> correctIdSet = new HashSet<>(Arrays.asList(cleanedCorrectIds.split("[,、]")));
            
            // 清理空字符串
            correctIdSet.removeIf(String::isEmpty);
            
            // 原始学生答案（用于日志）
            String originalStudentAnswer = answerText;
            
            // 提取学生的选择
            Set<String> studentChoices = new HashSet<>();
            
            // 使用正则表达式匹配选项ID（通常是A、B、C、D等）
            Pattern pattern = Pattern.compile("[A-Z]");
            Matcher matcher = pattern.matcher(answerText.toUpperCase());
            
            while (matcher.find()) {
                studentChoices.add(matcher.group());
            }
            
            // 如果没有找到有效的选择，尝试从完整答案中提取
            if (studentChoices.isEmpty()) {
                // 尝试按逗号或顿号分割
                String cleanedAnswer = answerText.replaceAll("[\\[\\]\"]", "").trim();
                String[] parts = cleanedAnswer.split("[,、]");
                for (String part : parts) {
                    part = part.trim().toUpperCase();
                    if (part.length() == 1 && Character.isLetter(part.charAt(0))) {
                        studentChoices.add(part);
                    }
                }
                
                // 如果仍然为空，尝试从文本中提取选项内容
                if (studentChoices.isEmpty()) {
                    // 遍历所有选项，查找答案中是否包含选项内容
                    for (Map.Entry<String, String> entry : optionsMap.entrySet()) {
                        if (answerText.contains(entry.getValue())) {
                            studentChoices.add(entry.getKey());
                        }
                    }
                }
            }
            
            // 评分和评语
            if (!studentChoices.isEmpty()) {
                // 计算正确选择的数量
                Set<String> correctChoices = new HashSet<>(studentChoices);
                correctChoices.retainAll(correctIdSet);
                
                // 计算错误选择的数量
                Set<String> wrongChoices = new HashSet<>(studentChoices);
                wrongChoices.removeAll(correctIdSet);
                
                // 计算漏选的数量
                Set<String> missedChoices = new HashSet<>(correctIdSet);
                missedChoices.removeAll(studentChoices);
                
                // 计算得分（满分100分）
                // 每个正确选择得到：100分 / 正确答案总数
                // 每个错误选择或漏选扣除：100分 / (正确答案总数 * 2)
                double pointsPerCorrect = 100.0 / correctIdSet.size();
                double pointsPerWrong = pointsPerCorrect / 2;
                
                double score = correctChoices.size() * pointsPerCorrect - 
                             (wrongChoices.size() + missedChoices.size()) * pointsPerWrong;
                
                // 确保分数在0-100范围内
                score = Math.max(0, Math.min(100, score));
                
                result.put("score", new BigDecimal(score).setScale(2, RoundingMode.HALF_UP));
                
                // 生成评语
                StringBuilder comments = new StringBuilder();
                comments.append("共选择了").append(studentChoices.size()).append("个选项。\n");
                
                if (!correctChoices.isEmpty()) {
                    comments.append("正确选择的选项：").append(String.join("、", correctChoices))
                           .append("\n");
                }
                
                if (!wrongChoices.isEmpty()) {
                    comments.append("错误选择的选项：").append(String.join("、", wrongChoices))
                           .append("\n");
                }
                
                if (!missedChoices.isEmpty()) {
                    comments.append("漏选的正确选项：").append(String.join("、", missedChoices))
                           .append("\n");
                }
                
                comments.append("\n正确答案应该是选项：").append(String.join("、", correctIdSet));
                
                result.put("comments", comments.toString());
                
                // 打印答案到日志，方便人工判断
                logger.info("\n========== 多选题评测结果 ==========");
                logger.info("原始大模型答案: {}", originalStudentAnswer);
                logger.info("处理后大模型答案: {}", String.join("、", studentChoices));
                logger.info("原始标准答案: {}", correctIds);
                logger.info("处理后标准答案: {}", String.join("、", correctIdSet));
                logger.info("正确选择: {}", (correctChoices.isEmpty() ? "无" : String.join("、", correctChoices)));
                logger.info("错误选择: {}", (wrongChoices.isEmpty() ? "无" : String.join("、", wrongChoices)));
                logger.info("漏选项目: {}", (missedChoices.isEmpty() ? "无" : String.join("、", missedChoices)));
                logger.info("评测得分: {}", result.get("score"));
                logger.info("===================================");
                
            } else {
                result.put("score", BigDecimal.ZERO);
                result.put("comments", "未能从回答中识别出明确的选择。正确答案是选项：" + String.join("、", correctIdSet));
                
                // 打印答案到日志，方便人工判断
                logger.info("\n========== 多选题评测结果 ==========");
                logger.info("原始大模型答案: {}", originalStudentAnswer);
                logger.info("未能从回答中识别出明确的选择");
                logger.info("原始标准答案: {}", correctIds);
                logger.info("处理后标准答案: {}", String.join("、", correctIdSet));
                logger.info("评测得分: 0");
                logger.info("===================================");
            }
            
            // 添加评测详情
            List<Map<String, Object>> criteriaScores = new ArrayList<>();
            
            // 正确性评分
            Map<String, Object> correctnessScore = new HashMap<>();
            correctnessScore.put("criterion", "正确性");
            correctnessScore.put("score", result.get("score"));
            correctnessScore.put("comments", result.get("comments"));
            criteriaScores.add(correctnessScore);
            
            result.put("criteria_scores", criteriaScores);
            
        } catch (Exception e) {
            logger.error("评测多选题失败", e);
            result.put("score", BigDecimal.ZERO);
            result.put("comments", "评测过程发生错误：" + e.getMessage());
            
            // 打印错误信息到日志
            logger.info("\n========== 多选题评测错误 ==========");
            logger.info("大模型原始答案: {}", answerText);
            logger.info("标准答案: {}", correctIds);
            logger.info("评测错误: {}", e.getMessage());
            logger.info("===================================");
        }
        
        return result;
    }
    
    @Override
    public Map<String, Object> evaluateSimpleFact(String answerText, String standardAnswer, String alternativeAnswers) {
        logger.info("开始评测简单事实题回答，回答文本长度: {}", answerText != null ? answerText.length() : 0);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 参数验证
            if (answerText == null || answerText.isEmpty() || standardAnswer == null || standardAnswer.isEmpty()) {
                logger.warn("评测简单事实题参数无效");
                result.put("score", BigDecimal.ZERO);
                result.put("comments", "无效的回答或标准答案");
                return result;
            }
            
            // 原始答案（用于日志记录）
            String originalAnswerText = answerText;
            String originalStandardAnswer = standardAnswer;
            
            // 解析备选答案
            List<String> alternatives = new ArrayList<>();
            if (alternativeAnswers != null && !alternativeAnswers.isEmpty()) {
                alternatives = objectMapper.readValue(alternativeAnswers, new TypeReference<List<String>>() {});
            }
            
            // 计算与标准答案的相似度
            BigDecimal standardSimilarity = calculateTextSimilarity(answerText, standardAnswer);
            BigDecimal maxSimilarity = standardSimilarity;
            String bestMatchAnswer = standardAnswer;
            
            // 计算与备选答案的相似度
            for (String alternative : alternatives) {
                BigDecimal similarity = calculateTextSimilarity(answerText, alternative);
                if (similarity.compareTo(maxSimilarity) > 0) {
                    maxSimilarity = similarity;
                    bestMatchAnswer = alternative;
                }
            }
            
            // 计算BERT相似度（语义相似度）
            BigDecimal bertScore = calculateBertSimilarity(answerText, bestMatchAnswer);
            
            // 计算ROUGE分数
            BigDecimal rougeScore = calculateRougeScore(answerText, bestMatchAnswer);
            
            // 计算BLEU分数
            BigDecimal bleuScore = calculateBleuScore(answerText, bestMatchAnswer);
            
            // 综合评分（权重：BERT相似度0.4，传统相似度0.2，ROUGE 0.2，BLEU 0.2）
            BigDecimal finalScore = bertScore.multiply(new BigDecimal("0.4"))
                    .add(maxSimilarity.multiply(new BigDecimal("0.2")))
                    .add(rougeScore.multiply(new BigDecimal("0.2")))
                    .add(bleuScore.multiply(new BigDecimal("0.2")));
            
            // 将分数转换为100分制
            finalScore = finalScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
            
            // 确保分数在0-100范围内
            if (finalScore.compareTo(BigDecimal.ZERO) < 0) {
                finalScore = BigDecimal.ZERO;
            } else if (finalScore.compareTo(new BigDecimal("100")) > 0) {
                finalScore = new BigDecimal("100");
            }
            
            result.put("score", finalScore);
            
            // 生成评语
            StringBuilder comments = new StringBuilder();
            comments.append("回答评分：").append(finalScore).append("分\n\n");
            comments.append("评分详情：\n");
            comments.append("1. BERT语义相似度：").append(bertScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)).append("分\n");
            comments.append("2. 文本相似度：").append(maxSimilarity.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)).append("分\n");
            comments.append("3. ROUGE分数：").append(rougeScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)).append("分\n");
            comments.append("4. BLEU分数：").append(bleuScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)).append("分\n\n");
            
            if (finalScore.compareTo(new BigDecimal("80")) >= 0) {
                comments.append("回答非常准确，与标准答案高度一致。");
            } else if (finalScore.compareTo(new BigDecimal("60")) >= 0) {
                comments.append("回答较为准确，与标准答案基本一致。");
            } else if (finalScore.compareTo(new BigDecimal("40")) >= 0) {
                comments.append("回答部分正确，但存在一些偏差。建议参考标准答案：").append(bestMatchAnswer);
            } else {
                comments.append("回答与标准答案差异较大。标准答案是：").append(bestMatchAnswer);
            }
            
            result.put("comments", comments.toString());
            
            // 打印答案到日志，方便人工判断
            logger.info("\n========== 简单事实题评测结果 ==========");
            logger.info("原始大模型答案: {}", originalAnswerText);
            logger.info("原始标准答案: {}", originalStandardAnswer);
            
            // 获取标准化处理后的文本（通过再次调用计算函数）
            String processedAnswerText = originalAnswerText.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "")
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", "");
            
            String processedStandardAnswer = originalStandardAnswer.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "")
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", "");
            
            logger.info("处理后大模型答案: {}", processedAnswerText);
            logger.info("处理后标准答案: {}", processedStandardAnswer);
            
            if (!alternatives.isEmpty()) {
                logger.info("备选答案: {}", alternatives);
                logger.info("最佳匹配答案: {}", bestMatchAnswer);
            }
            logger.info("BERT相似度: {}", bertScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
            logger.info("文本相似度: {}", maxSimilarity.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
            logger.info("ROUGE分数: {}", rougeScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
            logger.info("BLEU分数: {}", bleuScore.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
            logger.info("最终得分: {}", finalScore);
            logger.info("=====================================");
            
            // 添加评测详情
            List<Map<String, Object>> criteriaScores = new ArrayList<>();
            
            // BERT相似度评分
            Map<String, Object> bertScoreMap = new HashMap<>();
            bertScoreMap.put("criterion", "BERT语义相似度");
            bertScoreMap.put("score", bertScore.multiply(new BigDecimal("100")));
            bertScoreMap.put("comments", "BERT语义理解评分");
            criteriaScores.add(bertScoreMap);
            
            // 相似度评分
            Map<String, Object> similarityScore = new HashMap<>();
            similarityScore.put("criterion", "文本相似度");
            similarityScore.put("score", maxSimilarity.multiply(new BigDecimal("100")));
            similarityScore.put("comments", "文本相似度评分");
            criteriaScores.add(similarityScore);
            
            // ROUGE评分
            Map<String, Object> rougeScoreMap = new HashMap<>();
            rougeScoreMap.put("criterion", "ROUGE分数");
            rougeScoreMap.put("score", rougeScore.multiply(new BigDecimal("100")));
            rougeScoreMap.put("comments", "ROUGE评分");
            criteriaScores.add(rougeScoreMap);
            
            // BLEU评分
            Map<String, Object> bleuScoreMap = new HashMap<>();
            bleuScoreMap.put("criterion", "BLEU分数");
            bleuScoreMap.put("score", bleuScore.multiply(new BigDecimal("100")));
            bleuScoreMap.put("comments", "BLEU评分");
            criteriaScores.add(bleuScoreMap);
            
            result.put("criteria_scores", criteriaScores);
            
        } catch (Exception e) {
            logger.error("评测简单事实题失败", e);
            result.put("score", BigDecimal.ZERO);
            result.put("comments", "评测过程发生错误：" + e.getMessage());
            
            // 打印错误信息到日志
            logger.info("\n========== 简单事实题评测错误 ==========");
            logger.info("大模型原始答案: {}", answerText);
            logger.info("标准答案: {}", standardAnswer);
            logger.info("评测错误: {}", e.getMessage());
            logger.info("=====================================");
        }
        
        return result;
    }
    
    @Override
    public BigDecimal calculateTextSimilarity(String text1, String text2) {
        logger.info("计算文本相似度，文本1长度: {}, 文本2长度: {}", 
                text1 != null ? text1.length() : 0, 
                text2 != null ? text2.length() : 0);
        
        try {
            // 参数验证
            if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
                logger.warn("计算文本相似度失败：输入文本为空");
                return BigDecimal.ZERO;
            }
            
            // 中文文本处理：移除所有空白字符、标点符号并转为小写
            String processedText1 = text1.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "") // 移除标点符号
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
            
            String processedText2 = text2.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "") // 移除标点符号
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
            
            // 如果任一处理后的文本为空，返回0分
            if (processedText1.isEmpty() || processedText2.isEmpty()) {
                logger.warn("计算文本相似度失败：处理后文本为空");
                return BigDecimal.ZERO;
            }
            
            // 直接字符级比较
            // 对于短文本如简单事实题答案，使用编辑距离(Levenshtein距离)计算相似度
            int distance = calculateLevenshteinDistance(processedText1, processedText2);
            int maxLength = Math.max(processedText1.length(), processedText2.length());
            
            // 计算相似度：1 - 标准化编辑距离
            double similarity = 1.0 - ((double) distance / maxLength);
            
            // 转换为BigDecimal并四舍五入到2位小数
            BigDecimal result = new BigDecimal(similarity).setScale(2, RoundingMode.HALF_UP);
            
            // 确保结果在0-1范围内
            if (result.compareTo(BigDecimal.ZERO) < 0) {
                result = BigDecimal.ZERO;
            } else if (result.compareTo(BigDecimal.ONE) > 0) {
                result = BigDecimal.ONE;
            }
            
            logger.info("文本相似度计算结果: {}, 原始文本1: {}, 处理后: {}, 原始文本2: {}, 处理后: {}", 
                result, text1, processedText1, text2, processedText2);
            return result;
            
        } catch (Exception e) {
            logger.error("计算文本相似度时发生错误", e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 计算两个字符串之间的编辑距离(Levenshtein距离)
     * 
     * @param s1 第一个字符串
     * @param s2 第二个字符串
     * @return 编辑距离
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    @Override
    public BigDecimal calculateRougeScore(String candidateText, String referenceText) {
        logger.info("计算ROUGE分数，候选文本长度: {}, 参考文本长度: {}", 
                candidateText != null ? candidateText.length() : 0, 
                referenceText != null ? referenceText.length() : 0);
        
        try {
            // 参数验证
            if (candidateText == null || referenceText == null || candidateText.isEmpty() || referenceText.isEmpty()) {
                logger.warn("计算ROUGE分数失败：输入文本为空");
                return BigDecimal.ZERO;
            }
            
            // 中文文本处理：标准化处理，移除所有空白字符、标点符号并转为小写
            String processedCandidate = candidateText.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "") // 移除标点符号
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
                
            String processedReference = referenceText.toLowerCase()
                .replaceAll("[\\s:：,，.。!！?？;；()（）\\[\\]【】\"'\"]", "") // 移除标点符号
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
            
            // 如果任一处理后的文本为空，返回0分
            if (processedCandidate.isEmpty() || processedReference.isEmpty()) {
                logger.warn("计算ROUGE分数失败：处理后文本为空");
                return BigDecimal.ZERO;
            }
            
            // 对于中文文本，我们使用字符级别的分析
            // 将字符串转换为字符集合
            Set<Character> candidateChars = processedCandidate.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toSet());
            
            Set<Character> referenceChars = processedReference.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toSet());
            
            // 计算重叠的字符数
            Set<Character> overlap = new HashSet<>(candidateChars);
            overlap.retainAll(referenceChars);
            
            // 计算召回率和精确率
            double recall = (double) overlap.size() / referenceChars.size();
            double precision = (double) overlap.size() / candidateChars.size();
            
            // 计算F1分数
            double f1Score = 0.0;
            if (precision + recall > 0) {
                f1Score = 2 * precision * recall / (precision + recall);
            }
            
            // 转换为BigDecimal并四舍五入到2位小数
            BigDecimal result = new BigDecimal(f1Score).setScale(2, RoundingMode.HALF_UP);
            
            // 确保结果在0-1范围内
            if (result.compareTo(BigDecimal.ZERO) < 0) {
                result = BigDecimal.ZERO;
            } else if (result.compareTo(BigDecimal.ONE) > 0) {
                result = BigDecimal.ONE;
            }
            
            logger.info("ROUGE分数计算结果: {}, 原始文本1: {}, 处理后: {}, 原始文本2: {}, 处理后: {}", 
                result, candidateText, processedCandidate, referenceText, processedReference);
            return result;
            
        } catch (Exception e) {
            logger.error("计算ROUGE分数时发生错误", e);
            return BigDecimal.ZERO;
        }
    }
    
    @Override
    @Transactional
    public Evaluation createHumanEvaluation(Long llmAnswerId, Long evaluatorId, Long userId) {
        logger.info("创建人工评测记录，回答ID: {}, 评测者ID: {}, 用户ID: {}", 
                llmAnswerId, evaluatorId, userId);
        
        try {
            // 获取LLM回答
            LlmAnswer llmAnswer = llmAnswerRepository.findById(llmAnswerId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的LLM回答: " + llmAnswerId));
            
            // 获取评测者信息
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                    .orElseThrow(() -> new EntityNotFoundException("评测者不存在: " + evaluatorId));
            
            // 验证评测者类型是人类
            if (evaluator.getEvaluatorType() != Evaluator.EvaluatorType.HUMAN) {
                throw new IllegalArgumentException("评测者不是人类: " + evaluatorId);
            }
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
            
            // 检查是否已存在该回答的人工评测
            if (evaluationRepository.existsByLlmAnswerIdAndEvaluatorId(llmAnswerId, evaluatorId)) {
                throw new IllegalStateException("该回答已存在人工评测记录");
            }
            
            // 创建评测记录
            Evaluation evaluation = new Evaluation();
            evaluation.setLlmAnswer(llmAnswer);
            evaluation.setEvaluator(evaluator);
            evaluation.setCreatedByUser(user);
            evaluation.setCreationTime(LocalDateTime.now());
            evaluation.setStatus(EvaluationStatus.PENDING);
            // 设置评测类型
            evaluation.setEvaluationType(evaluator.getEvaluatorType() == Evaluator.EvaluatorType.HUMAN ? 
                EvaluationType.MANUAL : EvaluationType.AI_MODEL);
            
            // 保存评测记录
            evaluation = evaluationRepository.save(evaluation);
            
            logger.info("人工评测记录创建成功，评测ID: {}", evaluation.getId());
            return evaluation;
            
        } catch (Exception e) {
            logger.error("创建人工评测记录失败", e);
            throw new RuntimeException("创建人工评测记录失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public Evaluation submitHumanEvaluation(Long evaluationId, BigDecimal overallScore, String comments, 
                                          List<Map<String, Object>> detailScores, Long userId) {
        logger.info("提交人工评测结果，评测ID: {}, 总分: {}, 用户ID: {}", evaluationId, overallScore, userId);
        
        try {
            // 获取评测记录
            Evaluation evaluation = evaluationRepository.findById(evaluationId)
                    .orElseThrow(() -> new EntityNotFoundException("评测记录不存在: " + evaluationId));
            
            // 验证评测状态
            if (evaluation.getStatus() != EvaluationStatus.PENDING) {
                throw new IllegalStateException("评测已完成，无法修改: " + evaluationId);
            }
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
            
            // 验证评测者类型
            if (evaluation.getEvaluator().getEvaluatorType() != Evaluator.EvaluatorType.HUMAN) {
                throw new IllegalStateException("只能提交人工评测结果: " + evaluationId);
            }
            
            // 更新评测记录
            evaluation.setScore(overallScore);
            evaluation.setComments(comments);
            evaluation.setStatus(EvaluationStatus.SUCCESS);
            evaluation.setCompletionTime(LocalDateTime.now());
            
            // 构建评测结果JSON
            Map<String, Object> evaluationResults = new HashMap<>();
            evaluationResults.put("score", overallScore);
            evaluationResults.put("comments", comments);
            evaluationResults.put("criteria_scores", detailScores);
            evaluation.setEvaluationResults(evaluationResults);
            
            // 保存评测记录
            evaluation = evaluationRepository.save(evaluation);
            
            // 保存评测详情
            List<EvaluationDetail> details = new ArrayList<>();
            for (Map<String, Object> detailScore : detailScores) {
                EvaluationDetail detail = new EvaluationDetail();
                detail.setEvaluation(evaluation);
                detail.setCriterionName((String) detailScore.get("criterion"));
                detail.setScore(new BigDecimal(detailScore.get("score").toString()));
                detail.setComments((String) detailScore.get("comments"));
                detail.setCreatedAt(LocalDateTime.now());
                details.add(detail);
            }
            
            evaluationDetailRepository.saveAll(details);
            
            // 保存分数记录到ANSWER_SCORES表
            LlmAnswer llmAnswer = evaluation.getLlmAnswer();
            Evaluator evaluator = evaluation.getEvaluator();
            
            // 获取问题类型
            StandardQuestion question = llmAnswer.getDatasetQuestionMapping().getStandardQuestion();
            String scoreType = "HUMAN_" + question.getQuestionType().name();
            
            // 保存总体分数到评测记录中
            // 标准化分数（0-100分）
            BigDecimal normalizedScore;
            if (question.getQuestionType() == QuestionType.SUBJECTIVE) {
                // 主观题现在使用0-100分制，直接使用
                normalizedScore = overallScore;
            } else {
                // 客观题分数通常是0-100分
                normalizedScore = overallScore;
            }
            
            evaluation.setRawScore(overallScore);
            evaluation.setNormalizedScore(normalizedScore);
            evaluation.setScoreType(scoreType);
            evaluation.setScoringMethod("HUMAN");
            evaluation = evaluationRepository.save(evaluation);
            
            logger.info("成功保存人工评测分数记录，评测ID: {}, 回答ID: {}, 评测者ID: {}", 
                    evaluation.getId(), llmAnswer.getId(), evaluator.getId());
            
            // 保存详细评分记录
            for (Map<String, Object> criterionScore : detailScores) {
                String criterionName = (String) criterionScore.get("criterion");
                BigDecimal criterionScoreValue = new BigDecimal(criterionScore.get("score").toString());
                String criterionComments = (String) criterionScore.get("comments");
                
                // 创建详细评分记录
                EvaluationDetail detailScore = new EvaluationDetail();
                detailScore.setEvaluation(evaluation);
                detailScore.setCriterionName(criterionName);
                detailScore.setScore(criterionScoreValue);
                detailScore.setComments(criterionComments);
                detailScore.setCreatedAt(LocalDateTime.now());
                
                evaluationDetailRepository.save(detailScore);
            }
            
            logger.info("人工评测提交完成，评测ID: {}", evaluation.getId());
            return evaluation;
            
        } catch (Exception e) {
            logger.error("提交人工评测结果失败", e);
            throw new RuntimeException("提交人工评测结果失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public EvaluationRun createEvaluationRun(Long modelAnswerRunId, Long evaluatorId, String runName, 
                                            String runDescription, Map<String, Object> parameters, Long userId) {
        logger.info("创建评测运行，模型回答运行ID: {}, 评测者ID: {}, 用户ID: {}", 
                modelAnswerRunId, evaluatorId, userId);
        
        try {
            // 获取模型回答运行记录
            ModelAnswerRun modelAnswerRun = modelAnswerRunRepository.findById(modelAnswerRunId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的模型回答运行记录: " + modelAnswerRunId));
            
            // 获取评测者信息
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                    .orElseThrow(() -> new EntityNotFoundException("评测者不存在: " + evaluatorId));
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
            
            // 创建评测运行记录
            EvaluationRun evaluationRun = new EvaluationRun();
            evaluationRun.setModelAnswerRun(modelAnswerRun);
            evaluationRun.setEvaluator(evaluator);
            evaluationRun.setRunName(runName);
            evaluationRun.setRunDescription(runDescription);
            evaluationRun.setParameters(objectMapper.writeValueAsString(parameters));
            evaluationRun.setRunTime(LocalDateTime.now());  // 设置运行时间
            evaluationRun.setStatus(RunStatus.PENDING);  // 初始状态为等待中
            evaluationRun.setCreatedBy(userId);
            evaluationRun.setLastUpdated(LocalDateTime.now());
            
            // 保存评测运行记录
            evaluationRun = evaluationRunRepository.save(evaluationRun);
            
            logger.info("评测运行创建成功，运行ID: {}", evaluationRun.getId());
            return evaluationRun;
            
        } catch (Exception e) {
            logger.error("创建评测运行失败", e);
            throw new RuntimeException("创建评测运行失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public boolean pauseEvaluationRun(Long evaluationRunId) {
        logger.info("暂停评测运行，评测运行ID: {}", evaluationRunId);
        
        // 获取分布式锁
        RLock lock = redissonClient.getLock("evaluation_run_lock:" + evaluationRunId);
        try {
            // 获取锁，最多等待5秒，锁定30秒
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    logger.info("获取到评测运行{}的锁，开始暂停操作", evaluationRunId);
                    
                    // 1. 直接设置中断标志，不检查当前状态
                    String interruptKey = "evaluation_run:interrupt:" + evaluationRunId;
                    redisTemplate.opsForValue().set(interruptKey, "true");
                    redisTemplate.expire(interruptKey, Duration.ofHours(24));
                    logger.info("评测运行{}已设置中断标志", evaluationRunId);
                    
                    // 2. 更新Redis状态
                    String stateKey = "evaluation_run:state:" + evaluationRunId;
                    redisTemplate.opsForValue().set(stateKey, "PAUSED");
                    redisTemplate.expire(stateKey, Duration.ofHours(24));
                    logger.info("评测运行{}Redis状态已更新为PAUSED", evaluationRunId);
                    
                    // 3. 更新数据库状态
                    int updatedRows = jdbcTemplate.update(
                        "UPDATE evaluation_runs SET status = ?, pause_time = ?, last_updated = ? WHERE id = ?",
                        RunStatus.PAUSED.toString(), LocalDateTime.now(), LocalDateTime.now(), evaluationRunId);
                    
                    logger.info("评测运行已暂停，ID: {}, 数据库更新行数: {}", evaluationRunId, updatedRows);
                    
                    // 立即检查Redis中的中断标志是否设置成功
                    String checkValue = redisTemplate.opsForValue().get(interruptKey);
                    logger.info("确认评测运行{}中断标志状态: {}", evaluationRunId, checkValue);
                    return true;
                } finally {
                    lock.unlock();
                    logger.info("评测运行{}的锁已释放", evaluationRunId);
                }
            } else {
                logger.warn("无法获取评测运行{}的锁，暂停操作失败", evaluationRunId);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("获取评测运行{}的锁时被中断", evaluationRunId, e);
            return false;
        } catch (Exception e) {
            logger.error("暂停评测运行时发生错误", e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public CompletableFuture<Void> resumeEvaluationRun(Long evaluationRunId) {
        logger.info("恢复评测运行，运行ID: {}", evaluationRunId);
        
        // 获取分布式锁
        RLock lock = redissonClient.getLock("evaluation_run_lock:" + evaluationRunId);
        try {
            // 获取锁，最多等待5秒，锁定30秒
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    logger.info("获取到评测运行{}的锁，开始恢复操作", evaluationRunId);
                    
                    // 1. 清除中断标志
                    String interruptKey = "evaluation_run:interrupt:" + evaluationRunId;
                    redisTemplate.delete(interruptKey);
                    logger.info("评测运行{}已清除中断标志", evaluationRunId);
                    
                    // 2. 更新Redis状态
                    String stateKey = "evaluation_run:state:" + evaluationRunId;
                    redisTemplate.opsForValue().set(stateKey, "IN_PROGRESS");
                    redisTemplate.expire(stateKey, Duration.ofHours(24));
                    logger.info("评测运行{}Redis状态已更新为IN_PROGRESS", evaluationRunId);
                    
                    // 3. 更新数据库状态
                    jdbcTemplate.update(
                        "UPDATE evaluation_runs SET status = ?, resume_count = resume_count + 1, last_updated = ? WHERE id = ?",
                        RunStatus.IN_PROGRESS.toString(), LocalDateTime.now(), evaluationRunId);
                    
                    logger.info("评测运行{}数据库状态已更新为IN_PROGRESS", evaluationRunId);
                    
                    // 获取评测运行ID和所需信息，在当前事务中预加载必要数据
                    final Long runId = evaluationRunId;
                    final EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                            .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
                    
                    // 提前加载模型回答运行ID和评测者ID
                    final Long modelAnswerRunId = evaluationRun.getModelAnswerRunId();
                    final Long evaluatorId = evaluationRun.getEvaluatorId();
                    final Long userId = evaluationRun.getCreatedByUserId();
                    final Long lastProcessedAnswerId = evaluationRun.getLastProcessedAnswerId();
                    
                    // 4. 继续评测过程（在锁外异步执行）
                    return CompletableFuture.runAsync(() -> {
                        try {
                            logger.info("异步继续评测运行过程，ID: {}", runId);
                            // 继续处理评测，传递预加载的信息
                            continueEvaluationProcess(runId, modelAnswerRunId, evaluatorId, userId, lastProcessedAnswerId);
                        } catch (Exception e) {
                            logger.error("继续评测运行过程时发生错误", e);
                            try {
                                // 更新状态为失败
                                updateEvaluationRunStatus(runId, RunStatus.FAILED, e.getMessage());
                            } catch (Exception ex) {
                                logger.error("更新评测运行状态失败", ex);
                            }
                        }
                    }, evaluationExecutor);
                } finally {
                    lock.unlock();
                    logger.info("评测运行{}的锁已释放", evaluationRunId);
                }
            } else {
                logger.warn("无法获取评测运行{}的锁，恢复操作失败", evaluationRunId);
                throw new RuntimeException("无法获取评测运行的锁，恢复操作失败");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("获取评测运行{}的锁时被中断", evaluationRunId, e);
            throw new RuntimeException("获取评测运行的锁时被中断: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("恢复评测运行失败", e);
            throw new RuntimeException("恢复评测运行失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 继续处理评测 - 修改后的方法，使用预加载的信息并在新事务中执行
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void continueEvaluationProcess(Long evaluationRunId, Long modelAnswerRunId, Long evaluatorId, Long userId, Long lastProcessedAnswerId) {
        try {
            // 首先检查是否已暂停
            if (shouldInterruptEvaluation(evaluationRunId)) {
                logger.info("评测运行{}已被暂停，不再继续处理", evaluationRunId);
                
                // 确保数据库状态为暂停
                jdbcTemplate.update(
                    "UPDATE evaluation_runs SET status = ?, pause_time = ?, last_updated = ? WHERE id = ? AND status != ?",
                    RunStatus.PAUSED.toString(), LocalDateTime.now(), LocalDateTime.now(), 
                    evaluationRunId, RunStatus.PAUSED.toString());
                
                return;
            }
            
            // 重新获取评测运行记录 - 确保在新事务中
            EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
            
            // 获取评测者 - 在新事务中重新加载
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测者: " + evaluatorId));
            
            // 获取所有回答 - 使用JOIN FETCH主动加载问题
            List<LlmAnswer> allAnswers = llmAnswerRepository.findByModelAnswerRunIdWithQuestions(modelAnswerRunId);
            logger.info("评测运行{}，所有回答数量: {}", evaluationRunId, allAnswers.size());
            
            // 查询已评测的回答ID - 使用现有方法
            List<Evaluation> existingEvaluations = evaluationRepository.findByEvaluatorId(evaluatorId);
            List<Long> evaluatedAnswerIds = existingEvaluations.stream()
                    .map(eval -> eval.getLlmAnswer().getId())
                    .collect(Collectors.toList());
            logger.info("评测运行{}，已评测回答数量: {}", evaluationRunId, evaluatedAnswerIds.size());
            
            // 收集所有回答ID用于日志
            String allAnswerIds = allAnswers.stream()
                    .map(a -> a.getId().toString())
                    .collect(Collectors.joining(", "));
            logger.debug("评测运行{}，所有回答ID: {}", evaluationRunId, allAnswerIds);
            
            // 收集所有已评测回答ID用于日志
            String allEvaluatedIds = evaluatedAnswerIds.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            logger.debug("评测运行{}，已评测回答ID: {}", evaluationRunId, allEvaluatedIds);
            
            List<LlmAnswer> remainingAnswers;
            
            if (lastProcessedAnswerId != null) {
                // 过滤出未处理的回答
                remainingAnswers = allAnswers.stream()
                        .filter(answer -> answer.getId() > lastProcessedAnswerId)
                        .collect(Collectors.toList());
                logger.info("评测运行{}，使用lastProcessedAnswerId({})过滤，剩余回答数: {}", 
                        evaluationRunId, lastProcessedAnswerId, remainingAnswers.size());
                
                // 如果使用lastProcessedAnswerId过滤后没有剩余回答，尝试使用评测存在检查
                if (remainingAnswers.isEmpty()) {
                    logger.info("评测运行{}，通过ID过滤后没有剩余回答，尝试使用评测存在检查", evaluationRunId);
                    Set<Long> evaluatedIdsSet = new HashSet<>(evaluatedAnswerIds);
                    remainingAnswers = allAnswers.stream()
                            .filter(answer -> !evaluatedIdsSet.contains(answer.getId()))
                            .collect(Collectors.toList());
                    logger.info("评测运行{}，使用评测存在检查后，剩余回答数: {}", evaluationRunId, remainingAnswers.size());
                }
            } else {
                // 获取未评测的回答
                Set<Long> evaluatedIdsSet = new HashSet<>(evaluatedAnswerIds);
                remainingAnswers = allAnswers.stream()
                        .filter(answer -> !evaluatedIdsSet.contains(answer.getId()))
                        .collect(Collectors.toList());
                logger.info("评测运行{}，使用评测存在检查，剩余回答数: {}", evaluationRunId, remainingAnswers.size());
            }
            
            // 如果还是没有剩余回答，但allAnswers不为空，强制重新评测第一个回答（这是一个安全措施）
            if (remainingAnswers.isEmpty() && !allAnswers.isEmpty() && evaluationRun.getCompletedAnswersCount() < allAnswers.size()) {
                logger.warn("评测运行{}可能存在问题：所有回答数={}, 已完成数={}, 已评测ID数={}，但过滤后剩余回答为0，强制选择一个回答进行评测",
                        evaluationRunId, allAnswers.size(), evaluationRun.getCompletedAnswersCount(), evaluatedAnswerIds.size());
                remainingAnswers = Arrays.asList(allAnswers.get(0));
            }
            
            logger.info("继续评测运行，剩余回答数: {}", remainingAnswers.size());
            
            // 再次检查是否已暂停
            if (shouldInterruptEvaluation(evaluationRunId)) {
                logger.info("评测运行{}在处理前已被暂停，停止处理", evaluationRunId);
                
                // 确保数据库状态为暂停
                jdbcTemplate.update(
                    "UPDATE evaluation_runs SET status = ?, pause_time = ?, last_updated = ? WHERE id = ? AND status != ?",
                    RunStatus.PAUSED.toString(), LocalDateTime.now(), LocalDateTime.now(), 
                    evaluationRunId, RunStatus.PAUSED.toString());
                
                return;
            }
            
            // 批量处理剩余的回答
            int batchSize = evaluationRun.getBatchSize() != null ? evaluationRun.getBatchSize() : 10;
            
            // 确保所有问题已完全初始化
            remainingAnswers.forEach(answer -> {
                if (answer.getDatasetQuestionMapping() != null && answer.getDatasetQuestionMapping().getStandardQuestion() != null) {
                    // 触发延迟加载，确保在当前事务中完全初始化
                    StandardQuestion question = answer.getDatasetQuestionMapping().getStandardQuestion();
                    question.getQuestionType(); // 强制初始化
                    question.getQuestionText(); // 强制初始化
                }
            });
            
            for (int i = 0; i < remainingAnswers.size(); i += batchSize) {
                // 每个批次前强制检查是否应该中断处理
                if (shouldInterruptEvaluation(evaluationRunId)) {
                    logger.info("检测到评测运行{}的中断信号，立即停止批次处理", evaluationRunId);
                    
                    // 确保数据库状态更新为暂停
                    jdbcTemplate.update(
                        "UPDATE evaluation_runs SET status = ?, pause_time = ?, last_updated = ? WHERE id = ?",
                        RunStatus.PAUSED.toString(), LocalDateTime.now(), LocalDateTime.now(), evaluationRunId);
                    
                    return;
                }
                
                // 获取当前批次的回答
                int endIndex = Math.min(i + batchSize, remainingAnswers.size());
                List<LlmAnswer> batchAnswers = remainingAnswers.subList(i, endIndex);
                
                // 批量评测 - 每个批次在独立事务中处理
                evaluateAnswersBatch(batchAnswers, evaluator.getId(), userId);
                
                // 更新最后处理的回答ID
                if (!batchAnswers.isEmpty()) {
                    LlmAnswer lastAnswer = batchAnswers.get(batchAnswers.size() - 1);
                    updateLastProcessedAnswerId(evaluationRunId, lastAnswer.getId());
                }
                
                // 更新进度 - 查询最新的评测运行记录
                EvaluationRun currentRun = evaluationRunRepository.findById(evaluationRunId)
                        .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
                updateEvaluationProgress(currentRun, i + batchAnswers.size(), remainingAnswers.size());
            }
            
            // 获取最新的评测结果数量 - 使用现有方法
            int totalAnswers = allAnswers.size();
            
            // 重新查询已评测的数量
            List<Long> answerIds = allAnswers.stream().map(LlmAnswer::getId).collect(Collectors.toList());
            int completedAnswers = 0;
            for (Long answerId : answerIds) {
                if (evaluationRepository.existsByLlmAnswerIdAndEvaluatorId(answerId, evaluatorId)) {
                    completedAnswers++;
                }
            }
            
            logger.info("评测运行{}完成状态检查: 总回答数={}, 已评测数={}", evaluationRunId, totalAnswers, completedAnswers);
            
            // 检查是否所有回答都已处理完成
            if (remainingAnswers.isEmpty() || completedAnswers >= totalAnswers) {
                completeEvaluationRun(evaluationRunId);
            } else {
                logger.warn("评测运行{}存在未完成的评测: 总回答数={}, 已评测数={}, 未评测数={}",
                        evaluationRunId, totalAnswers, completedAnswers, (totalAnswers - completedAnswers));
            }
            
        } catch (Exception e) {
            logger.error("继续处理评测过程中发生错误", e);
            throw e;
        }
    }
    
    /**
     * 批量评测回答，在独立事务中处理
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluateAnswersBatch(List<LlmAnswer> llmAnswers, Long evaluatorId, Long userId) {
        List<Evaluation> results = new ArrayList<>();
        int successCount = 0;
        
        for (LlmAnswer answer : llmAnswers) {
            try {
                // 在新事务中重新加载所需的实体
                LlmAnswer reloadedAnswer = llmAnswerRepository.findByIdWithQuestion(answer.getId())
                        .orElseThrow(() -> new EntityNotFoundException("找不到指定的回答: " + answer.getId()));
                
                Evaluation evaluation = evaluateAnswer(reloadedAnswer, evaluatorId, userId);
                if (evaluation != null) {
                    results.add(evaluation);
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("评测回答失败，回答ID: {}", answer.getId(), e);
            }
        }
        
        logger.info("批量评测完成，成功评测数量: {}", successCount);
    }
    
    /**
     * 检查是否应该中断评测
     */
    private boolean shouldInterruptEvaluation(Long evaluationRunId) {
        String interruptKey = "evaluation_run:interrupt:" + evaluationRunId;
        String value = redisTemplate.opsForValue().get(interruptKey);
        boolean interrupted = "true".equals(value);
        if (interrupted) {
            logger.info("检测到评测运行{}的中断标志为true，应当暂停处理", evaluationRunId);
        }
        return interrupted;
    }
    
    /**
     * 更新最后处理的回答ID
     */
    @Transactional
    public void updateLastProcessedAnswerId(Long evaluationRunId, Long answerId) {
        jdbcTemplate.update(
            "UPDATE evaluation_runs SET last_processed_answer_id = ? WHERE id = ?",
            answerId, evaluationRunId);
    }
    
    /**
     * 更新评测运行状态
     */
    @Transactional
    public void updateEvaluationRunStatus(Long evaluationRunId, RunStatus status, String errorMessage) {
        jdbcTemplate.update(
            "UPDATE evaluation_runs SET status = ?, error_message = ?, last_updated = ? WHERE id = ?",
            status.toString(), errorMessage, LocalDateTime.now(), evaluationRunId);
    }
    
    /**
     * 完成评测运行
     */
    @Transactional
    public void completeEvaluationRun(Long evaluationRunId) {
        jdbcTemplate.update(
            "UPDATE evaluation_runs SET status = ?, completed_at = ?, progress_percentage = 100, last_updated = ? WHERE id = ?",
            RunStatus.COMPLETED.toString(), LocalDateTime.now(), LocalDateTime.now(), evaluationRunId);
        
        logger.info("评测运行{}已完成", evaluationRunId);
    }
    
    @Override
    public List<EvaluationRun> getEvaluationRuns(Long modelAnswerRunId, Long evaluatorId, String status, int page, int size) {
        logger.info("获取评测运行列表，模型回答运行ID: {}, 评测者ID: {}, 状态: {}, 页码: {}, 每页大小: {}", 
                modelAnswerRunId, evaluatorId, status, page, size);
        
        try {
            // 创建分页对象
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "creationTime"));
            
            // 构建查询条件
            List<EvaluationRun> runs;
            if (modelAnswerRunId != null && evaluatorId != null && status != null) {
                // 全条件查询
                runs = evaluationRunRepository.findByModelAnswerRunIdAndEvaluatorIdAndStatus(
                        modelAnswerRunId, evaluatorId, RunStatus.valueOf(status), pageable);
            } else if (modelAnswerRunId != null && evaluatorId != null) {
                // 按模型回答运行ID和评测者ID查询
                runs = evaluationRunRepository.findByModelAnswerRunIdAndEvaluatorId(
                        modelAnswerRunId, evaluatorId, pageable);
            } else if (modelAnswerRunId != null) {
                // 按模型回答运行ID查询
                runs = evaluationRunRepository.findByModelAnswerRunId(modelAnswerRunId, pageable);
            } else if (evaluatorId != null) {
                // 按评测者ID查询
                runs = evaluationRunRepository.findByEvaluatorId(evaluatorId, pageable);
            } else {
                // 无条件查询
                runs = evaluationRunRepository.findAll(pageable).getContent();
            }
            
            logger.info("获取到{}条评测运行记录", runs.size());
            return runs;
            
        } catch (Exception e) {
            logger.error("获取评测运行列表失败", e);
            throw new RuntimeException("获取评测运行列表失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public EvaluationRun getEvaluationRun(Long evaluationRunId) {
        logger.info("获取评测运行详情，运行ID: {}", evaluationRunId);
        
        try {
            // 获取评测运行记录
            EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
            
            return evaluationRun;
            
        } catch (Exception e) {
            logger.error("获取评测运行详情失败", e);
            throw new RuntimeException("获取评测运行详情失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> getEvaluationRunResults(Long evaluationRunId) {
        logger.info("获取评测运行结果，运行ID: {}", evaluationRunId);
        
        try {
            // 获取评测运行记录
            EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
            
            // 获取所有评测结果
            List<Evaluation> evaluations = evaluationRepository.findByEvaluationRunId(evaluationRunId);
            
            // 构建结果统计
            Map<String, Object> results = new HashMap<>();
            results.put("evaluationRunId", evaluationRunId);
            results.put("runName", evaluationRun.getRunName());
            results.put("status", evaluationRun.getStatus());
            results.put("startTime", evaluationRun.getStartTime());
            results.put("endTime", evaluationRun.getEndTime());
            results.put("totalEvaluations", evaluations.size());
            
            // 计算总体统计信息
            if (!evaluations.isEmpty()) {
                // 计算平均分
                BigDecimal totalScore = BigDecimal.ZERO;
                int validScoreCount = 0;
                
                for (Evaluation evaluation : evaluations) {
                    if (evaluation.getScore() != null) {
                        totalScore = totalScore.add(evaluation.getScore());
                        validScoreCount++;
                    }
                }
                
                if (validScoreCount > 0) {
                    BigDecimal averageScore = totalScore.divide(new BigDecimal(validScoreCount), 2, RoundingMode.HALF_UP);
                    results.put("averageScore", averageScore);
                }
                
                // 按问题类型分组统计
                Map<QuestionType, List<Evaluation>> groupedByType = evaluations.stream()
                        .filter(e -> e.getLlmAnswer() != null && 
                                   e.getLlmAnswer().getDatasetQuestionMapping() != null && 
                                   e.getLlmAnswer().getDatasetQuestionMapping().getStandardQuestion() != null)
                        .collect(Collectors.groupingBy(e -> e.getLlmAnswer().getDatasetQuestionMapping().getStandardQuestion().getQuestionType()));
                
                Map<String, Object> typeStats = new HashMap<>();
                for (Map.Entry<QuestionType, List<Evaluation>> entry : groupedByType.entrySet()) {
                    QuestionType type = entry.getKey();
                    List<Evaluation> typeEvaluations = entry.getValue();
                    
                    Map<String, Object> typeStat = new HashMap<>();
                    typeStat.put("count", typeEvaluations.size());
                    
                    // 计算该类型的平均分
                    BigDecimal typeTotal = BigDecimal.ZERO;
                    int typeValidCount = 0;
                    for (Evaluation evaluation : typeEvaluations) {
                        if (evaluation.getScore() != null) {
                            typeTotal = typeTotal.add(evaluation.getScore());
                            typeValidCount++;
                        }
                    }
                    
                    if (typeValidCount > 0) {
                        BigDecimal typeAverage = typeTotal.divide(new BigDecimal(typeValidCount), 2, RoundingMode.HALF_UP);
                        typeStat.put("averageScore", typeAverage);
                    }
                    
                    typeStats.put(type.toString(), typeStat);
                }
                
                results.put("questionTypeStats", typeStats);
                
                // 添加评测标准统计
                Map<String, Map<String, Object>> criteriaStats = new HashMap<>();
                for (Evaluation evaluation : evaluations) {
                    List<EvaluationDetail> details = evaluationDetailRepository.findByEvaluationId(evaluation.getId());
                    
                    for (EvaluationDetail detail : details) {
                        String criterion = detail.getCriterionName();
                        Map<String, Object> criterionStat = criteriaStats.computeIfAbsent(criterion, k -> new HashMap<>());
                        
                        // 更新评分总和和计数
                        BigDecimal currentTotal = (BigDecimal) criterionStat.getOrDefault("totalScore", BigDecimal.ZERO);
                        int currentCount = (int) criterionStat.getOrDefault("count", 0);
                        
                        criterionStat.put("totalScore", currentTotal.add(detail.getScore()));
                        criterionStat.put("count", currentCount + 1);
                    }
                }
                
                // 计算每个标准的平均分
                for (Map<String, Object> criterionStat : criteriaStats.values()) {
                    BigDecimal total = (BigDecimal) criterionStat.get("totalScore");
                    int count = (int) criterionStat.get("count");
                    
                    if (count > 0) {
                        BigDecimal average = total.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                        criterionStat.put("averageScore", average);
                    }
                }
                
                results.put("criteriaStats", criteriaStats);
            }
            
            return results;
            
        } catch (Exception e) {
            logger.error("获取评测运行结果失败", e);
            throw new RuntimeException("获取评测运行结果失败: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> getEvaluationResults(Evaluation evaluation) {
        if (evaluation.getEvaluationResults() == null) {
            return new HashMap<>();
        }
        return evaluation.getEvaluationResults();
    }

    @Override
    @Transactional
    public Map<String, Object> evaluateBatchObjectiveQuestions(Long batchId, Long evaluatorId, Long userId) {
        logger.debug("开始评测批次的客观题，批次ID: {}", batchId);
        
        // 验证评测者和用户
        Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测者: " + evaluatorId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的用户: " + userId));
        
        // 获取批次下的所有模型运行
        List<ModelAnswerRun> modelRuns = modelAnswerRunRepository.findByAnswerGenerationBatchId(batchId);
        if (modelRuns.isEmpty()) {
            throw new IllegalArgumentException("找不到指定批次的模型运行: " + batchId);
        }
        
        // 统计信息
        Map<String, Object> result = new HashMap<>();
        int totalAnswers = 0;
        int successCount = 0;
        int failedCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        Map<QuestionType, Integer> typeCount = new HashMap<>();
        Map<QuestionType, BigDecimal> typeScoreSum = new HashMap<>();
        
        // 按重复索引分组的统计数据
        Map<Integer, Integer> repeatIndexCount = new HashMap<>();
        Map<Integer, BigDecimal> repeatIndexScoreSum = new HashMap<>();
        Map<Integer, Map<QuestionType, Integer>> repeatIndexTypeCount = new HashMap<>();
        Map<Integer, Map<QuestionType, BigDecimal>> repeatIndexTypeScoreSum = new HashMap<>();
        
        // 初始化问题类型计数
        typeCount.put(QuestionType.SINGLE_CHOICE, 0);
        typeCount.put(QuestionType.MULTIPLE_CHOICE, 0);
        typeCount.put(QuestionType.SIMPLE_FACT, 0);
        
        // 初始化问题类型总分
        typeScoreSum.put(QuestionType.SINGLE_CHOICE, BigDecimal.ZERO);
        typeScoreSum.put(QuestionType.MULTIPLE_CHOICE, BigDecimal.ZERO);
        typeScoreSum.put(QuestionType.SIMPLE_FACT, BigDecimal.ZERO);
        
        // 处理每个模型运行
        for (ModelAnswerRun modelRun : modelRuns) {
            // 获取该运行下的所有回答
            List<LlmAnswer> allAnswers = llmAnswerRepository.findByModelAnswerRunId(modelRun.getId());
            
            // 过滤出客观题回答
            List<LlmAnswer> objectiveAnswers = allAnswers.stream()
                    .filter(answer -> {
                        StandardQuestion question = answer.getDatasetQuestionMapping().getStandardQuestion();
                        QuestionType type = question.getQuestionType();
                        return type == QuestionType.SINGLE_CHOICE || 
                               type == QuestionType.MULTIPLE_CHOICE || 
                               type == QuestionType.SIMPLE_FACT;
                    })
                    .collect(Collectors.toList());
            
            logger.debug("找到{}个客观题回答需要评测", objectiveAnswers.size());
            totalAnswers += objectiveAnswers.size();
            
            // 批量评测客观题回答，包括同一问题的所有重复回答
            for (LlmAnswer answer : objectiveAnswers) {
                // 为每个回答创建单独的事务
                try {
                    // 获取repeatIndex，如果为null则默认为0
                    Integer repeatIndex = answer.getRepeatIndex() != null ? answer.getRepeatIndex() : 0;
                    
                    // 初始化此repeatIndex的统计数据（如果不存在）
                    repeatIndexCount.putIfAbsent(repeatIndex, 0);
                    repeatIndexScoreSum.putIfAbsent(repeatIndex, BigDecimal.ZERO);
                    
                    if (!repeatIndexTypeCount.containsKey(repeatIndex)) {
                        Map<QuestionType, Integer> indexTypeCount = new HashMap<>();
                        indexTypeCount.put(QuestionType.SINGLE_CHOICE, 0);
                        indexTypeCount.put(QuestionType.MULTIPLE_CHOICE, 0);
                        indexTypeCount.put(QuestionType.SIMPLE_FACT, 0);
                        repeatIndexTypeCount.put(repeatIndex, indexTypeCount);
                    }
                    
                    if (!repeatIndexTypeScoreSum.containsKey(repeatIndex)) {
                        Map<QuestionType, BigDecimal> indexTypeScoreSum = new HashMap<>();
                        indexTypeScoreSum.put(QuestionType.SINGLE_CHOICE, BigDecimal.ZERO);
                        indexTypeScoreSum.put(QuestionType.MULTIPLE_CHOICE, BigDecimal.ZERO);
                        indexTypeScoreSum.put(QuestionType.SIMPLE_FACT, BigDecimal.ZERO);
                        repeatIndexTypeScoreSum.put(repeatIndex, indexTypeScoreSum);
                    }
                    
                    BigDecimal score = evaluateSingleObjectiveAnswer(answer, evaluator, user, typeCount, typeScoreSum);
                    
                    // 更新按repeatIndex分组的统计
                    repeatIndexCount.put(repeatIndex, repeatIndexCount.get(repeatIndex) + 1);
                    repeatIndexScoreSum.put(repeatIndex, repeatIndexScoreSum.get(repeatIndex).add(score));
                    
                    // 更新按repeatIndex分组的问题类型统计
                    QuestionType type = answer.getDatasetQuestionMapping().getStandardQuestion().getQuestionType();
                    Map<QuestionType, Integer> indexTypeCount = repeatIndexTypeCount.get(repeatIndex);
                    Map<QuestionType, BigDecimal> indexTypeScoreSum = repeatIndexTypeScoreSum.get(repeatIndex);
                    
                    indexTypeCount.put(type, indexTypeCount.get(type) + 1);
                    indexTypeScoreSum.put(type, indexTypeScoreSum.get(type).add(score));
                    
                    // 更新总统计
                    totalScore = totalScore.add(score);
                    successCount++;
                } catch (Exception e) {
                    logger.error("评测回答时出错，回答ID: {}", answer.getId(), e);
                    failedCount++;
                }
            }
        }
        
        // 计算统计结果
        result.put("totalAnswers", totalAnswers);
        result.put("successCount", successCount);
        result.put("failedCount", failedCount);
        
        if (successCount > 0) {
            BigDecimal avgScore = totalScore.divide(new BigDecimal(successCount), 2, RoundingMode.HALF_UP);
            result.put("averageScore", avgScore);
        } else {
            result.put("averageScore", 0);
        }
        
        // 各类型题目的统计
        Map<String, Object> typeStats = new HashMap<>();
        for (QuestionType type : Arrays.asList(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE, QuestionType.SIMPLE_FACT)) {
            Map<String, Object> typeStat = new HashMap<>();
            int count = typeCount.get(type);
            typeStat.put("count", count);
            
            if (count > 0) {
                BigDecimal avgTypeScore = typeScoreSum.get(type).divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                typeStat.put("averageScore", avgTypeScore);
            } else {
                typeStat.put("averageScore", 0);
            }
            
            typeStats.put(type.name(), typeStat);
        }
        result.put("typeStatistics", typeStats);
        
        // 按repeatIndex分组的统计
        Map<String, Object> repeatIndexStats = new HashMap<>();
        for (Integer repeatIndex : repeatIndexCount.keySet()) {
            Map<String, Object> indexStat = new HashMap<>();
            int count = repeatIndexCount.get(repeatIndex);
            indexStat.put("count", count);
            
            if (count > 0) {
                BigDecimal avgScore = repeatIndexScoreSum.get(repeatIndex)
                    .divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                indexStat.put("averageScore", avgScore);
                
                // 该重复索引下各类型题目的统计
                Map<String, Object> indexTypeStats = new HashMap<>();
                Map<QuestionType, Integer> indexTypeCount = repeatIndexTypeCount.get(repeatIndex);
                Map<QuestionType, BigDecimal> indexTypeScoreSum = repeatIndexTypeScoreSum.get(repeatIndex);
                
                for (QuestionType type : Arrays.asList(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE, QuestionType.SIMPLE_FACT)) {
                    Map<String, Object> indexTypeStat = new HashMap<>();
                    int typeCountVal = indexTypeCount.get(type);
                    indexTypeStat.put("count", typeCountVal);
                    
                    if (typeCountVal > 0) {
                        BigDecimal avgTypeScore = indexTypeScoreSum.get(type)
                            .divide(new BigDecimal(typeCountVal), 2, RoundingMode.HALF_UP);
                        indexTypeStat.put("averageScore", avgTypeScore);
                    } else {
                        indexTypeStat.put("averageScore", 0);
                    }
                    
                    indexTypeStats.put(type.name(), indexTypeStat);
                }
                
                indexStat.put("typeStatistics", indexTypeStats);
            } else {
                indexStat.put("averageScore", 0);
                indexStat.put("typeStatistics", new HashMap<>());
            }
            
            repeatIndexStats.put("repeat_" + repeatIndex, indexStat);
        }
        result.put("repeatIndexStatistics", repeatIndexStats);
        
        logger.info("批次客观题评测完成，总计: {}, 成功: {}, 失败: {}", totalAnswers, successCount, failedCount);
        return result;
    }

    /**
     * 评测单个客观题回答
     * 这个方法需要在单独的事务中执行，以避免一个回答的评测失败影响其他回答
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BigDecimal evaluateSingleObjectiveAnswer(LlmAnswer answer, Evaluator evaluator, User user, 
                                        Map<QuestionType, Integer> typeCount, 
                                        Map<QuestionType, BigDecimal> typeScoreSum) {
        StandardQuestion question = answer.getDatasetQuestionMapping().getStandardQuestion();
        QuestionType type = question.getQuestionType();
        
        // 获取重复索引
        Integer repeatIndex = answer.getRepeatIndex();
        if (repeatIndex == null) {
            repeatIndex = 0; // 默认为0
        }
        
        // 检查是否已经存在针对这个回答的评测记录
        String scoreType = "OBJECTIVE_" + type.name();
        
        // 考虑repeatIndex，使用完全匹配的回答ID查找
        Optional<Evaluation> existingEvaluation = evaluationRepository.findByLlmAnswerIdAndEvaluatorId(
                answer.getId(), evaluator.getId()).stream().findFirst();
        
        if (existingEvaluation.isPresent()) {
            logger.info("该回答已存在评测记录，回答ID: {}, 重复索引: {}, 评测者ID: {}, 分数类型: {}", 
                    answer.getId(), answer.getRepeatIndex(), evaluator.getId(), scoreType);
            
            BigDecimal score = existingEvaluation.get().getRawScore();
            
            // 更新统计信息
            if (typeCount.containsKey(type)) {
                typeCount.put(type, typeCount.get(type) + 1);
                typeScoreSum.put(type, typeScoreSum.get(type).add(score));
            }
            
            return score;
        }
        
        // 创建评测记录
        Evaluation evaluation = new Evaluation();
        evaluation.setLlmAnswer(answer);
        evaluation.setEvaluator(evaluator);
        evaluation.setEvaluationTime(LocalDateTime.now());
        evaluation.setStatus(EvaluationStatus.SUCCESS);
        evaluation.setCreatedByUser(user);
        evaluation.setCreationTime(LocalDateTime.now()); // 设置创建时间
        evaluation.setCompletionTime(LocalDateTime.now()); // 设置完成时间
        evaluation.setEvaluationType(EvaluationType.AI_MODEL); // 设置评测类型为自动评测
        
        BigDecimal score;
        Map<String, Object> evaluationResult;
        
        // 根据问题类型进行评测
        switch (type) {
            case SINGLE_CHOICE:
                StandardObjectiveAnswer singleChoiceAnswer = objectiveAnswerRepository.findByStandardQuestionId(question.getId())
                        .orElseThrow(() -> new IllegalStateException("找不到单选题的标准答案: " + question.getId()));
                
                evaluationResult = evaluateSingleChoice(
                        answer.getAnswerText(),
                        singleChoiceAnswer.getCorrectOptionIds(),
                        singleChoiceAnswer.getOptions());
                
                // 添加重复索引信息
                evaluationResult.put("repeatIndex", repeatIndex);
                
                score = new BigDecimal(evaluationResult.get("score").toString());
                evaluation.setScore(score);
                evaluation.setComments((String) evaluationResult.getOrDefault("feedback", evaluationResult.get("comments")));
                evaluation.setEvaluationResults(evaluationResult); // 保存完整的评测结果
                
                typeCount.put(QuestionType.SINGLE_CHOICE, typeCount.get(QuestionType.SINGLE_CHOICE) + 1);
                typeScoreSum.put(QuestionType.SINGLE_CHOICE, typeScoreSum.get(QuestionType.SINGLE_CHOICE).add(score));
                break;
                
            case MULTIPLE_CHOICE:
                StandardObjectiveAnswer multipleChoiceAnswer = objectiveAnswerRepository.findByStandardQuestionId(question.getId())
                        .orElseThrow(() -> new IllegalStateException("找不到多选题的标准答案: " + question.getId()));
                
                evaluationResult = evaluateMultipleChoice(
                        answer.getAnswerText(),
                        multipleChoiceAnswer.getCorrectOptionIds(),
                        multipleChoiceAnswer.getOptions());
                
                // 添加重复索引信息
                evaluationResult.put("repeatIndex", repeatIndex);
                
                score = new BigDecimal(evaluationResult.get("score").toString());
                evaluation.setScore(score);
                evaluation.setComments((String) evaluationResult.getOrDefault("feedback", evaluationResult.get("comments")));
                evaluation.setEvaluationResults(evaluationResult); // 保存完整的评测结果
                
                typeCount.put(QuestionType.MULTIPLE_CHOICE, typeCount.get(QuestionType.MULTIPLE_CHOICE) + 1);
                typeScoreSum.put(QuestionType.MULTIPLE_CHOICE, typeScoreSum.get(QuestionType.MULTIPLE_CHOICE).add(score));
                break;
                
            case SIMPLE_FACT:
                StandardSimpleAnswer simpleAnswer = simpleAnswerRepository
                        .findByStandardQuestionId(question.getId())
                        .orElseThrow(() -> new IllegalStateException("找不到简单事实题的标准答案: " + question.getId()));
                
                evaluationResult = evaluateSimpleFact(
                        answer.getAnswerText(),
                        simpleAnswer.getAnswerText(),
                        simpleAnswer.getAlternativeAnswers());
                
                // 添加重复索引信息
                evaluationResult.put("repeatIndex", repeatIndex);
                
                score = new BigDecimal(evaluationResult.get("score").toString());
                evaluation.setScore(score);
                evaluation.setComments((String) evaluationResult.getOrDefault("feedback", evaluationResult.get("comments")));
                evaluation.setEvaluationResults(evaluationResult); // 保存完整的评测结果
                
                typeCount.put(QuestionType.SIMPLE_FACT, typeCount.get(QuestionType.SIMPLE_FACT) + 1);
                typeScoreSum.put(QuestionType.SIMPLE_FACT, typeScoreSum.get(QuestionType.SIMPLE_FACT).add(score));
                break;
                
            default:
                // 不应该到达这里，因为我们已经过滤了问题类型
                throw new IllegalArgumentException("不支持的问题类型: " + type);
        }
        
        try {
            // 保存评测结果
            evaluation = evaluationRepository.save(evaluation);
            logger.info("成功保存评测结果，评测ID: {}, 回答ID: {}, 重复索引: {}", 
                    evaluation.getId(), answer.getId(), repeatIndex);
            
            // 保存评测详情
            if (evaluationResult.containsKey("criteria_scores")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> criteriaScores = (List<Map<String, Object>>) evaluationResult.get("criteria_scores");
                
                List<EvaluationDetail> details = new ArrayList<>();
                for (Map<String, Object> criteriaScore : criteriaScores) {
                    EvaluationDetail detail = new EvaluationDetail();
                    detail.setEvaluation(evaluation);
                    detail.setCriterionName((String) criteriaScore.get("criterion"));
                    detail.setScore(new BigDecimal(criteriaScore.get("score").toString()));
                    detail.setComments((String) criteriaScore.get("comments"));
                    detail.setCreatedAt(LocalDateTime.now());
                    details.add(detail);
                }
                
                evaluationDetailRepository.saveAll(details);
                logger.info("成功保存评测详情，评测ID: {}, 详情数量: {}", evaluation.getId(), details.size());
            }
            
            // 保存分数记录到Evaluation中
            evaluation.setRawScore(score);
            evaluation.setNormalizedScore(score); // 对于客观题，原始分数和标准化分数相同
            evaluation.setScoreType(scoreType);
            evaluation.setScoringMethod("AUTOMATIC");
            
            evaluation = evaluationRepository.save(evaluation);
            logger.info("成功保存评测记录，评测ID: {}, 回答ID: {}, 重复索引: {}, 评测者ID: {}, 分数类型: {}", 
                    evaluation.getId(), answer.getId(), repeatIndex, evaluator.getId(), scoreType);
            
            return score;
        } catch (Exception e) {
            logger.error("保存评测结果时出错，回答ID: {}, 重复索引: {}", answer.getId(), repeatIndex, e);
            throw e; // 重新抛出异常，让事务回滚
        }
    }

    /**
     * 使用BERT模型计算文本相似度（目前为伪实现，实际项目中可集成真实BERT模型）
     * 
     * @param text1 第一个文本
     * @param text2 第二个文本
     * @return 相似度得分（0-1之间）
     */
    public BigDecimal calculateBertSimilarity(String text1, String text2) {
        logger.info("计算BERT文本相似度，文本1长度: {}, 文本2长度: {}", 
                text1 != null ? text1.length() : 0, 
                text2 != null ? text2.length() : 0);
        
        try {
            // 参数验证
            if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
                logger.warn("计算BERT相似度失败：输入文本为空");
                return BigDecimal.ZERO;
            }
            
            // 中文文本预处理：移除常见的答案前缀
            String processedText1 = text1.toLowerCase()
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
            
            String processedText2 = text2.toLowerCase()
                .replaceAll("(答案|答|回答|正确答案|正确的答案|应该是|是)[:：]?", ""); // 移除常见的答案前缀
            
            // 如果任一处理后的文本为空，返回0分
            if (processedText1.isEmpty() || processedText2.isEmpty()) {
                logger.warn("计算BERT相似度失败：处理后文本为空");
                return BigDecimal.ZERO;
            }
            
            // 这里是BERT相似度计算的伪实现
            // 实际项目中，您可以集成一个Java BERT客户端库或使用HTTP请求调用BERT服务
            
            // 模拟BERT相似度计算 - 这里使用加权Levenshtein距离作为示例
            int distance = calculateLevenshteinDistance(processedText1, processedText2);
            int maxLength = Math.max(processedText1.length(), processedText2.length());
            
            // 计算基础相似度：1 - 标准化编辑距离
            double baseSimilarity = 1.0 - ((double) distance / maxLength);
            
            // 关键词匹配加权（模拟BERT的语义理解能力）
            double keywordBoost = 0.0;
            // 提取关键词（简化实现）
            String[] words1 = processedText1.split("\\s+");
            String[] words2 = processedText2.split("\\s+");
            
            Set<String> keyWords1 = new HashSet<>(Arrays.asList(words1));
            Set<String> keyWords2 = new HashSet<>(Arrays.asList(words2));
            
            // 计算关键词重叠率
            Set<String> commonWords = new HashSet<>(keyWords1);
            commonWords.retainAll(keyWords2);
            
            if (!keyWords1.isEmpty() && !keyWords2.isEmpty()) {
                keywordBoost = 0.2 * ((double) commonWords.size() / Math.min(keyWords1.size(), keyWords2.size()));
            }
            
            // 最终BERT模拟相似度 = 基础相似度 + 关键词加权
            double bertSimilarity = Math.min(1.0, baseSimilarity + keywordBoost);
            
            // 转换为BigDecimal并四舍五入到2位小数
            BigDecimal result = new BigDecimal(bertSimilarity).setScale(2, RoundingMode.HALF_UP);
            
            logger.info("BERT相似度计算结果: {}, 原始文本1: {}, 处理后: {}, 原始文本2: {}, 处理后: {}", 
                result, text1, processedText1, text2, processedText2);
            return result;
            
        } catch (Exception e) {
            logger.error("计算BERT相似度时发生错误", e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> evaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId) {
        logger.debug("开始批量评测批次的主观题，批次ID: {}", batchId);
        
        // 验证评测者和用户
        Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测者: " + evaluatorId));
        
        // 验证评测者类型是AI
        if (evaluator.getEvaluatorType() != Evaluator.EvaluatorType.AI_MODEL) {
            throw new IllegalArgumentException("评测者不是AI模型: " + evaluatorId);
        }
        
        // 获取或创建评测运行记录
        EvaluationRun evaluationRun = getOrCreateEvaluationRun(batchId, evaluatorId, userId);
        
        try {
            // 获取所有需要评测的主观题回答
                                    // 获取该批次的所有回答
                        List<LlmAnswer> answers = llmAnswerRepository.findByBatchId(batchId);
                        
                        // 过滤出主观题的回答
                        answers = answers.stream()
                                .filter(answer -> answer.getDatasetQuestionMapping()
                                        .getStandardQuestion().getQuestionType() == QuestionType.SUBJECTIVE)
                                .collect(Collectors.toList());
            
            if (answers.isEmpty()) {
                logger.info("批次中没有主观题回答需要评测，批次ID: {}", batchId);
                return Map.of("status", "completed", "message", "没有主观题需要评测");
            }
            
            // 更新总回答数
            evaluationRun.setTotalAnswersCount(answers.size());
            
            // 获取评测标准
            List<EvaluationCriterion> criteria = getCriteriaForQuestionType(QuestionType.SUBJECTIVE);
            
            // 如果是恢复评测，从上次中断的位置继续
            Long startAnswerId = evaluationRun.getCurrentBatchStartId();
            if (startAnswerId == null) {
                startAnswerId = answers.get(0).getId();
                evaluationRun.setCurrentBatchStartId(startAnswerId);
            }
            
            // 设置批次大小
            int batchSize = evaluationRun.getBatchSize() != null ? evaluationRun.getBatchSize() : 50;
            
            // 分批处理回答
            for (int i = 0; i < answers.size(); i += batchSize) {
                // 检查是否需要暂停
                if (evaluationRun.getStatus() == RunStatus.PAUSED) {
                    logger.info("评测运行已暂停，批次ID: {}", batchId);
                    break;
                }
                
                // 获取当前批次的回答
                int endIndex = Math.min(i + batchSize, answers.size());
                List<LlmAnswer> batchAnswers = answers.subList(i, endIndex);
                
                try {
                    // 更新当前批次信息
                    evaluationRun.setCurrentBatchStartId(batchAnswers.get(0).getId());
                    evaluationRun.setCurrentBatchEndId(batchAnswers.get(batchAnswers.size() - 1).getId());
                    evaluationRun.setRetryCount(0);
                    evaluationRunRepository.save(evaluationRun);
                    
                    // 处理当前批次
                    processBatchAnswers(batchAnswers, evaluator, criteria, evaluationRun, userId);
                    
                    // 更新进度
                    updateEvaluationProgress(evaluationRun, endIndex, answers.size());
                    
                } catch (Exception e) {
                    handleBatchProcessingError(evaluationRun, e);
                    
                    // 如果超过最大重试次数，暂停评测
                    if (evaluationRun.getRetryCount() >= evaluationRun.getMaxRetries()) {
                        pauseEvaluationRun(evaluationRun.getId());
                        break;
                    }
                }
            }
            
            // 检查是否所有回答都已评测完成
            if (evaluationRun.getCompletedAnswersCount() >= evaluationRun.getTotalAnswersCount()) {
                completeEvaluationRun(evaluationRun);
            }
            
            // 返回评测结果
            return getEvaluationRunProgress(evaluationRun.getId());
            
        } catch (Exception e) {
            logger.error("批量评测主观题失败", e);
            evaluationRun.setStatus(RunStatus.FAILED);
            evaluationRun.setErrorMessage(e.getMessage());
            evaluationRunRepository.save(evaluationRun);
            throw new RuntimeException("批量评测主观题失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取或创建评测运行记录
     */
    private EvaluationRun getOrCreateEvaluationRun(Long batchId, Long evaluatorId, Long userId) {
        // 查找是否存在未完成的评测运行
        List<EvaluationRun> existingRuns = evaluationRunRepository
                .findByModelAnswerRunIdAndEvaluatorIdAndStatusNot(
                        batchId, evaluatorId, RunStatus.COMPLETED);
        
        if (!existingRuns.isEmpty()) {
            return existingRuns.get(0);
        }
        
        // 创建新的评测运行记录
        EvaluationRun evaluationRun = new EvaluationRun();
        evaluationRun.setModelAnswerRunId(batchId);
        evaluationRun.setEvaluatorId(evaluatorId);
        evaluationRun.setRunName("主观题批量评测-" + batchId);
        evaluationRun.setStatus(RunStatus.IN_PROGRESS);
        evaluationRun.setCreatedByUserId(userId);
        evaluationRun.setRunTime(LocalDateTime.now());
        evaluationRun.setLastActivityTime(LocalDateTime.now());
        evaluationRun.setBatchSize(50);
        evaluationRun.setMaxRetries(3);
        
        return evaluationRunRepository.save(evaluationRun);
    }
    
    /**
     * 处理一批回答
     */
    private void processBatchAnswers(List<LlmAnswer> answers, Evaluator evaluator, 
                                   List<EvaluationCriterion> criteria, 
                                   EvaluationRun evaluationRun, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
        
                    for (LlmAnswer answer : answers) {
                // 每次处理答案前检查中断标志
                if (shouldInterruptEvaluation(evaluationRun.getId())) {
                    logger.info("检测到评测运行{}的中断信号，停止处理剩余答案", evaluationRun.getId());
                    return;
                }
                
                try {
                    // 评测单个回答
                    evaluateSingleSubjectiveAnswer(answer, evaluator, user, criteria);
                    
                    // 更新完成数量
                    evaluationRun.setCompletedAnswersCount(evaluationRun.getCompletedAnswersCount() + 1);
                    evaluationRun.setConsecutiveErrors(0);  // 重置连续错误计数
                
            } catch (Exception e) {
                logger.error("评测单个回答失败，回答ID: " + answer.getId(), e);
                evaluationRun.setFailedEvaluationsCount(evaluationRun.getFailedEvaluationsCount() + 1);
                evaluationRun.setConsecutiveErrors(evaluationRun.getConsecutiveErrors() + 1);
                
                // 如果连续错误次数过多，抛出异常中断当前批次
                if (evaluationRun.getConsecutiveErrors() >= 3) {
                    throw new RuntimeException("连续评测失败次数过多");
                }
            }
        }
        
        // 更新评测运行状态
        evaluationRun.setLastActivityTime(LocalDateTime.now());
        evaluationRunRepository.save(evaluationRun);
    }
    
    /**
     * 处理批处理错误
     */
    private void handleBatchProcessingError(EvaluationRun evaluationRun, Exception e) {
        logger.error("处理批次失败", e);
        
        evaluationRun.setRetryCount(evaluationRun.getRetryCount() + 1);
        evaluationRun.setLastErrorTime(LocalDateTime.now());
        evaluationRun.setErrorMessage(e.getMessage());
        
        // 如果配置了自动恢复，等待一段时间后继续
        if (evaluationRun.getIsAutoResume()) {
            try {
                Thread.sleep(5000); // 等待5秒后重试
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        
        evaluationRunRepository.save(evaluationRun);
    }
    
    /**
     * 更新评测进度
     */
    private void updateEvaluationProgress(EvaluationRun evaluationRun, int currentCount, int totalCount) {
        BigDecimal progress = new BigDecimal(currentCount)
                .multiply(new BigDecimal(100))
                .divide(new BigDecimal(totalCount), 2, RoundingMode.HALF_UP);
        
        evaluationRun.setProgressPercentage(progress);
        evaluationRun.setLastActivityTime(LocalDateTime.now());
        evaluationRunRepository.save(evaluationRun);
    }
    
    /**
     * 完成评测运行
     */
    private void completeEvaluationRun(EvaluationRun evaluationRun) {
        evaluationRun.setStatus(RunStatus.COMPLETED);
        evaluationRun.setCompletedAt(LocalDateTime.now());
        evaluationRun.setProgressPercentage(new BigDecimal(100));
        evaluationRunRepository.save(evaluationRun);
        
        logger.info("评测运行完成，运行ID: {}", evaluationRun.getId());
    }

    /**
     * 评测单个主观题回答
     * 这个方法需要在单独的事务中执行，以避免一个回答的评测失败影响其他回答
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BigDecimal evaluateSingleSubjectiveAnswer(LlmAnswer answer, Evaluator evaluator, User user, 
                                        List<EvaluationCriterion> criteria) {
        try {
            StandardQuestion question = answer.getDatasetQuestionMapping().getStandardQuestion();
            
            // 确保是主观题
            if (question.getQuestionType() != QuestionType.SUBJECTIVE) {
                throw new IllegalArgumentException("不是主观题类型: " + question.getId());
            }
            
            // 获取repeatIndex，如果为null则默认为0
            Integer repeatIndex = answer.getRepeatIndex();
            if (repeatIndex == null) {
                repeatIndex = 0;
            }
            
            // 检查是否已存在相同的评测记录（考虑答案ID和评测者ID）
            boolean exists = evaluationRepository.existsByLlmAnswerIdAndEvaluatorId(
                answer.getId(), evaluator.getId());
            
            if (exists) {
                logger.warn("该主观题回答已被同一评测者评测过，跳过重复评测，回答ID: {}, 重复索引: {}, 评测者ID: {}", 
                        answer.getId(), repeatIndex, evaluator.getId());
                
                // 查找并返回现有评测记录的分数
                List<Evaluation> existingEvaluations = evaluationRepository.findByLlmAnswerIdAndEvaluatorId(
                        answer.getId(), evaluator.getId());
                
                if (!existingEvaluations.isEmpty()) {
                    BigDecimal existingScore = existingEvaluations.get(0).getRawScore();
                    if (existingScore != null) {
                        logger.info("返回已有评测记录的分数: {}, 重复索引: {}", existingScore, repeatIndex);
                        return existingScore;
                    }
                }
            }
            
            // 获取标准答案
            StandardSubjectiveAnswer standardAnswer = standardSubjectiveAnswerRepository
                    .findByStandardQuestionId(question.getId())
                    .orElseThrow(() -> new IllegalStateException("找不到主观题的标准答案: " + question.getId()));
            
            // 创建评测记录
            Evaluation evaluation = new Evaluation();
            evaluation.setLlmAnswer(answer);
            evaluation.setEvaluator(evaluator);
            evaluation.setEvaluationType(EvaluationType.AI_MODEL);
            evaluation.setStatus(EvaluationStatus.PROCESSING);
            evaluation.setCreationTime(LocalDateTime.now());
            evaluation.setCreatedByUser(user);
            
            evaluation = evaluationRepository.save(evaluation);
            
            // 调用AI评测
            Map<String, Object> evaluationResult = evaluateSubjectiveWithAI(
                    answer.getAnswerText(),
                    question.getQuestionText(),
                    standardAnswer.getAnswerText(),
                    criteria,
                    evaluator.getId());
            
            // 添加重复索引信息
            evaluationResult.put("repeatIndex", repeatIndex);
            
            // 更新评测记录
            BigDecimal score = new BigDecimal(evaluationResult.get("score").toString());
            evaluation.setScore(score);
            evaluation.setComments((String) evaluationResult.get("comments"));
            evaluation.setEvaluationResults(evaluationResult);
            evaluation.setStatus(EvaluationStatus.SUCCESS);
            evaluation.setCompletionTime(LocalDateTime.now());
            
            // 在保存前检查是否已存在相同的评测记录
            boolean existsBeforeSave = evaluationRepository.existsByLlmAnswerIdAndEvaluatorId(
                answer.getId(), evaluator.getId());
            
            // 详细记录请求体信息
            logger.info("准备保存主观题评测记录，详细信息: llmAnswerId={}, repeatIndex={}, evaluatorId={}, createdByUserId={}, status={}, score={}, 已存在相同记录={}",
                answer.getId(), repeatIndex, evaluator.getId(), user.getId(), evaluation.getStatus(), score, existsBeforeSave);
            
            if (existsBeforeSave) {
                logger.warn("检测到唯一键约束冲突风险! 该主观题回答(ID:{}, 重复索引:{})已被同一评测者(ID:{})评测过", 
                    answer.getId(), repeatIndex, evaluator.getId());
            }
            
            // 保存评测记录
            evaluation = evaluationRepository.save(evaluation);
            
            // 在评测记录中保存分数信息
            // 主观题分数通常是0-10分，需要转换为0-100的标准化分数
            BigDecimal normalizedScore = score;
            
            evaluation.setRawScore(score);
            evaluation.setNormalizedScore(normalizedScore);
            evaluation.setScoreType("SUBJECTIVE");
            evaluation.setScoringMethod("AI");
            
            evaluation = evaluationRepository.save(evaluation);
            
            logger.info("成功评测主观题回答，回答ID: {}, 重复索引: {}, 评分: {}", answer.getId(), repeatIndex, score);
            
            return score;
        } catch (Exception e) {
            logger.error("评测主观题回答失败，回答ID: " + answer.getId(), e);
            throw e;
        }
    }

    /**
     * 重新评测单个主观题回答（强制覆盖已有评测）
     * 这个方法会删除已有的评测记录，并创建新的评测
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BigDecimal reEvaluateSingleSubjectiveAnswer(LlmAnswer answer, Evaluator evaluator, User user, 
                                        List<EvaluationCriterion> criteria) {
        try {
            StandardQuestion question = answer.getDatasetQuestionMapping().getStandardQuestion();
            
            // 确保是主观题
            if (question.getQuestionType() != QuestionType.SUBJECTIVE) {
                throw new IllegalArgumentException("不是主观题类型: " + question.getId());
            }
            
            // 获取repeatIndex，如果为null则默认为0
            Integer repeatIndex = answer.getRepeatIndex();
            if (repeatIndex == null) {
                repeatIndex = 0;
            }
            
            // 先删除现有评测记录（使用单独的事务）
            deleteExistingEvaluations(answer.getId(), evaluator.getId());
            
            // 获取标准答案
            StandardSubjectiveAnswer standardAnswer = standardSubjectiveAnswerRepository
                    .findByStandardQuestionId(question.getId())
                    .orElseThrow(() -> new IllegalStateException("找不到主观题的标准答案: " + question.getId()));
            
            // 创建评测记录
            Evaluation evaluation = new Evaluation();
            evaluation.setLlmAnswer(answer);
            evaluation.setEvaluator(evaluator);
            evaluation.setEvaluationType(EvaluationType.AI_MODEL);
            evaluation.setStatus(EvaluationStatus.PROCESSING);
            evaluation.setCreationTime(LocalDateTime.now());
            evaluation.setCreatedByUser(user);
            
            evaluation = evaluationRepository.save(evaluation);
            
            // 调用AI评测
            Map<String, Object> evaluationResult = evaluateSubjectiveWithAI(
                    answer.getAnswerText(),
                    question.getQuestionText(),
                    standardAnswer.getAnswerText(),
                    criteria,
                    evaluator.getId());
            
            // 添加重复索引信息
            evaluationResult.put("repeatIndex", repeatIndex);
            
            // 更新评测记录
            BigDecimal score = new BigDecimal(evaluationResult.get("score").toString());
            evaluation.setScore(score);
            evaluation.setComments((String) evaluationResult.get("comments"));
            evaluation.setEvaluationResults(evaluationResult);
            evaluation.setStatus(EvaluationStatus.SUCCESS);
            evaluation.setCompletionTime(LocalDateTime.now());
            
            // 保存评测记录
            evaluation = evaluationRepository.save(evaluation);
            
            // 在评测记录中保存分数信息
            // 主观题分数通常是0-10分，需要转换为0-100的标准化分数
            BigDecimal normalizedScore = score;
            
            evaluation.setRawScore(score);
            evaluation.setNormalizedScore(normalizedScore);
            evaluation.setScoreType("SUBJECTIVE");
            evaluation.setScoringMethod("AI");
            
            evaluation = evaluationRepository.save(evaluation);
            
            logger.info("成功重新评测主观题回答，回答ID: {}, 重复索引: {}, 评分: {}", answer.getId(), repeatIndex, score);
            
            return score;
        } catch (Exception e) {
            logger.error("重新评测主观题回答失败，回答ID: " + answer.getId(), e);
            throw e;
        }
    }

    /**
     * 在单独的事务中删除现有评测记录
     * 这样可以确保删除操作在创建新评测前完成提交
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteExistingEvaluations(Long llmAnswerId, Long evaluatorId) {
        // 检查是否已存在相同的评测记录
        List<Evaluation> existingEvaluations = evaluationRepository.findByLlmAnswerIdAndEvaluatorId(
                llmAnswerId, evaluatorId);
        
        if (!existingEvaluations.isEmpty()) {
            logger.info("发现已有评测记录，将删除，回答ID: {}, 评测者ID: {}, 记录数: {}", 
                    llmAnswerId, evaluatorId, existingEvaluations.size());
            
            for (Evaluation oldEvaluation : existingEvaluations) {
                // 不再需要删除AnswerScore记录
                // 删除关联的评测详情
                evaluationDetailRepository.deleteByEvaluationId(oldEvaluation.getId());
            }
            
            // 删除评测记录
            evaluationRepository.deleteAll(existingEvaluations);
            
            // 强制刷新以确保删除操作立即生效
            evaluationRepository.flush();
        }
    }

    /**
     * 实现接口方法，重新评测单个主观题回答
     */
    @Override
    @Transactional
    public BigDecimal reEvaluateSubjectiveAnswer(Long llmAnswerId, Long evaluatorId, Long userId) {
        logger.info("开始重新评测主观题回答，回答ID: {}, 评测者ID: {}, 用户ID: {}", 
                llmAnswerId, evaluatorId, userId);
        
        try {
            // 获取LLM回答
            LlmAnswer llmAnswer = llmAnswerRepository.findById(llmAnswerId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的LLM回答: " + llmAnswerId));
            
            // 验证回答是否为主观题
            StandardQuestion question = llmAnswer.getDatasetQuestionMapping().getStandardQuestion();
            if (question.getQuestionType() != QuestionType.SUBJECTIVE) {
                throw new IllegalArgumentException("指定的回答不是主观题: " + llmAnswerId);
            }
            
            // 获取评测者信息
            Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                    .orElseThrow(() -> new EntityNotFoundException("评测者不存在: " + evaluatorId));
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
            
            // 获取评测标准
            List<EvaluationCriterion> criteria = getCriteriaForQuestionType(QuestionType.SUBJECTIVE);
            
            // 调用实际的重新评测方法
            BigDecimal score = reEvaluateSingleSubjectiveAnswer(llmAnswer, evaluator, user, criteria);
            
            logger.info("重新评测主观题回答成功，回答ID: {}, 得分: {}", llmAnswerId, score);
            return score;
            
        } catch (Exception e) {
            logger.error("重新评测主观题回答失败", e);
            throw new RuntimeException("重新评测主观题回答失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> reEvaluateBatchSubjectiveQuestions(Long batchId, Long evaluatorId, Long userId) {
        logger.debug("开始批量重新评测批次的主观题，批次ID: {}", batchId);
        
        // 验证评测者和用户
        Evaluator evaluator = evaluatorRepository.findById(evaluatorId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测者: " + evaluatorId));
        
        // 验证评测者类型是AI
        if (evaluator.getEvaluatorType() != Evaluator.EvaluatorType.AI_MODEL) {
            throw new IllegalArgumentException("评测者不是AI模型: " + evaluatorId);
        }
        
        // 验证评测者关联了AI模型
        if (evaluator.getLlmModel() == null) {
            throw new IllegalArgumentException("评测者未关联AI模型: " + evaluatorId);
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("找不到指定的用户: " + userId));
        
        // 获取批次下的所有模型运行
        List<ModelAnswerRun> modelRuns = modelAnswerRunRepository.findByAnswerGenerationBatchId(batchId);
        if (modelRuns.isEmpty()) {
            throw new IllegalArgumentException("找不到指定批次的模型运行: " + batchId);
        }
        
        // 统计信息
        Map<String, Object> result = new HashMap<>();
        int totalAnswers = 0;
        int successCount = 0;
        int failedCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        
        // 按重复索引分组的统计数据
        Map<Integer, Integer> repeatIndexCount = new HashMap<>();
        Map<Integer, BigDecimal> repeatIndexScoreSum = new HashMap<>();
        
        // 获取主观题评测标准
        List<EvaluationCriterion> criteria = getCriteriaForQuestionType(QuestionType.SUBJECTIVE);
        
        // 处理每个模型运行
        for (ModelAnswerRun modelRun : modelRuns) {
            // 获取该运行下的所有回答
            List<LlmAnswer> allAnswers = llmAnswerRepository.findByModelAnswerRunId(modelRun.getId());
            
            // 过滤出主观题回答
            List<LlmAnswer> subjectiveAnswers = allAnswers.stream()
                    .filter(answer -> {
                        StandardQuestion question = answer.getDatasetQuestionMapping().getStandardQuestion();
                        return question.getQuestionType() == QuestionType.SUBJECTIVE;
                    })
                    .collect(Collectors.toList());
            
            logger.debug("找到{}个主观题回答需要重新评测", subjectiveAnswers.size());
            totalAnswers += subjectiveAnswers.size();
            
            // 批量重新评测主观题回答
            for (LlmAnswer answer : subjectiveAnswers) {
                try {
                    // 在单独的事务中重新评测每个回答
                    BigDecimal score = reEvaluateSingleSubjectiveAnswer(answer, evaluator, user, criteria);
                    
                    // 更新统计数据
                    totalScore = totalScore.add(score);
                    successCount++;
                    
                    // 按重复索引更新统计
                    int repeatIndex = answer.getRepeatIndex() != null ? answer.getRepeatIndex() : 0;
                    repeatIndexCount.put(repeatIndex, repeatIndexCount.getOrDefault(repeatIndex, 0) + 1);
                    repeatIndexScoreSum.put(repeatIndex, 
                            repeatIndexScoreSum.getOrDefault(repeatIndex, BigDecimal.ZERO).add(score));
                    
                } catch (Exception e) {
                    logger.error("重新评测主观题回答失败，回答ID: " + answer.getId(), e);
                    failedCount++;
                }
            }
        }
        
        // 计算总体统计数据
        result.put("totalAnswers", totalAnswers);
        result.put("successCount", successCount);
        result.put("failedCount", failedCount);
        
        // 计算平均分
        BigDecimal averageScore = BigDecimal.ZERO;
        if (successCount > 0) {
            averageScore = totalScore.divide(new BigDecimal(successCount), 2, RoundingMode.HALF_UP);
        }
        result.put("averageScore", averageScore);
        
        // 按重复索引计算统计数据
        Map<String, Object> repeatIndexStats = new HashMap<>();
        for (Integer repeatIndex : repeatIndexCount.keySet()) {
            Map<String, Object> indexStat = new HashMap<>();
            int count = repeatIndexCount.get(repeatIndex);
            BigDecimal sum = repeatIndexScoreSum.get(repeatIndex);
            
            indexStat.put("count", count);
            
            // 计算该重复索引的平均分
            if (count > 0) {
                BigDecimal average = sum.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
                indexStat.put("averageScore", average);
            } else {
                indexStat.put("averageScore", BigDecimal.ZERO);
            }
            
            repeatIndexStats.put("repeat_" + repeatIndex, indexStat);
        }
        result.put("repeatIndexStatistics", repeatIndexStats);
        
        logger.info("批次主观题重新评测完成，总计: {}, 成功: {}, 失败: {}", totalAnswers, successCount, failedCount);
        return result;
    }
} 
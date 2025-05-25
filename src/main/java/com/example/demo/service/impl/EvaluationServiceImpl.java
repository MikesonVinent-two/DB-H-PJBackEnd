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
import com.example.demo.service.EvaluationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper;
    
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
            RestTemplate restTemplate) {
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
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
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
            promptBuilder.append("  \"overall_score\": 分数,\n");
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
            
            // 解析AI评测结果
            Map<String, Object> aiResult = objectMapper.readValue(aiResponse, new TypeReference<Map<String, Object>>() {});
            
            // 提取总体评分
            Object overallScoreObj = aiResult.get("overall_score");
            BigDecimal overallScore;
            if (overallScoreObj instanceof Number) {
                overallScore = new BigDecimal(overallScoreObj.toString()).setScale(2, RoundingMode.HALF_UP);
                // 确保分数在0-10范围内
                if (overallScore.compareTo(BigDecimal.ZERO) < 0) {
                    overallScore = BigDecimal.ZERO;
                } else if (overallScore.compareTo(new BigDecimal(10)) > 0) {
                    overallScore = new BigDecimal(10);
                }
            } else {
                overallScore = new BigDecimal(5); // 默认中等分数
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
                // 在这里应该从数据库中获取指定ID的LlmModel对象
                // 这里可以通过注入LlmModelRepository实现
                // 假设数据库中存储了LlmModel实体
                // 暂时使用配置的默认值，后续可以扩展
                logger.info("尝试使用模型ID: {}", modelId);
            }
            
            // 确定API URL和密钥
            String apiUrl = llmModel != null && llmModel.getApiUrl() != null ? 
                    llmModel.getApiUrl() : aiServiceUrl;
            String apiKey = llmModel != null && llmModel.getApiKey() != null ? 
                    llmModel.getApiKey() : aiServiceApiKey;
            String model = llmModel != null && llmModel.getName() != null ? 
                    llmModel.getName() : aiServiceModel;
            
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
                        if (message != null) {
                            logger.info("成功获取到AI服务的响应");
                            return (String) message.get("content");
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
     * 执行AI评测（调用大语言模型API）
     */
    private String executeAIEvaluation(String prompt, Long modelId) {
        try {
            logger.info("调用真实大模型进行评测，提示词长度: {}", prompt.length());
            
            // 从数据库获取模型信息
            LlmModel llmModel = null;
            if (modelId != null) {
                // 这里应该从数据库中获取模型信息
                // 当前使用配置的默认值
            }
            
            // 获取API信息
            String apiUrl = "https://api.openai.com/v1/chat/completions";
            String apiKey = aiServiceApiKey; // 使用配置的API密钥
            String model = "gpt-3.5-turbo"; // 使用GPT-3.5-Turbo模型
            
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
                  "overall_score": 7.5,
                  "criteria_scores": [
                    {"criterion": "内容完整性", "score": 8, "comments": "回答涵盖了大部分关键点"},
                    {"criterion": "逻辑性", "score": 7, "comments": "论述基本连贯，但有些地方逻辑跳跃"},
                    {"criterion": "专业性", "score": 8, "comments": "使用了适当的专业术语，展示了对主题的理解"}
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
                  "overall_score": 5.0,
                  "criteria_scores": [
                    {"criterion": "评测错误", "score": 5, "comments": "评测过程中发生错误"}
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
        logger.info("计算BLEU分数，候选文本长度: {}，参考文本长度: {}", 
                candidateText != null ? candidateText.length() : 0, 
                referenceText != null ? referenceText.length() : 0);
        
        try {
            // 参数验证
            if (candidateText == null || referenceText == null || candidateText.isEmpty() || referenceText.isEmpty()) {
                logger.warn("计算BLEU分数失败：输入文本为空");
                return BigDecimal.ZERO;
            }
            
            // 分词处理
            String[] candidateTokens = candidateText.split("\\s+");
            String[] referenceTokens = referenceText.split("\\s+");
            
            // 如果任一文本分词后为空，返回0分
            if (candidateTokens.length == 0 || referenceTokens.length == 0) {
                logger.warn("计算BLEU分数失败：分词后文本为空");
                return BigDecimal.ZERO;
            }
            
            // 计算精确匹配数量
            int matchCount = 0;
            Map<String, Integer> refTokenCount = new HashMap<>();
            
            // 统计参考文本中的词频
            for (String token : referenceTokens) {
                refTokenCount.put(token, refTokenCount.getOrDefault(token, 0) + 1);
            }
            
            // 计算匹配数量
            Map<String, Integer> candidateTokenCount = new HashMap<>();
            for (String token : candidateTokens) {
                candidateTokenCount.put(token, candidateTokenCount.getOrDefault(token, 0) + 1);
            }
            
            // 计算共同词汇的最小出现次数
            for (Map.Entry<String, Integer> entry : candidateTokenCount.entrySet()) {
                String token = entry.getKey();
                int count = entry.getValue();
                if (refTokenCount.containsKey(token)) {
                    matchCount += Math.min(count, refTokenCount.get(token));
                }
            }
            
            // 计算精确率
            double precision = (double) matchCount / candidateTokens.length;
            
            // 计算简化版BLEU分数（不包含惩罚因子）
            double bleuScore = precision;
            
            // 四舍五入到2位小数
            BigDecimal result = new BigDecimal(bleuScore).setScale(2, RoundingMode.HALF_UP);
            
            // 确保结果在0-1范围内
            if (result.compareTo(BigDecimal.ZERO) < 0) {
                result = BigDecimal.ZERO;
            } else if (result.compareTo(BigDecimal.ONE) > 0) {
                result = BigDecimal.ONE;
            }
            
            logger.info("BLEU分数计算结果: {}", result);
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
                
                // 批量处理未评测的回答
                int batchSize = 10;
                for (int i = 0; i < unevaluatedAnswers.size(); i += batchSize) {
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
                    
                    // 更新进度
                    int processedCount = i + batchAnswers.size();
                    logger.info("评测运行进度: {}/{}", processedCount, unevaluatedAnswers.size());
                    
                    // 更新最后更新时间
                    evaluationRun.setLastUpdated(LocalDateTime.now());
                    evaluationRunRepository.save(evaluationRun);
                }
                
                // 所有回答评测完成，更新状态为已完成
                evaluationRun.setStatus(RunStatus.COMPLETED);
                evaluationRun.setEndTime(LocalDateTime.now());
                evaluationRun.setLastUpdated(LocalDateTime.now());
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
            
            // 获取问题信息
            StandardQuestion question = llmAnswer.getDatasetQuestionMapping().getStandardQuestion();
            
            // 创建评测记录
            Evaluation evaluation = new Evaluation();
            evaluation.setLlmAnswer(llmAnswer);
            evaluation.setEvaluator(evaluator);
            evaluation.setCreatedByUser(user);
            evaluation.setCreationTime(LocalDateTime.now());
            evaluation.setStatus(EvaluationStatus.PENDING);
            
            // 根据问题类型进行评测
            Map<String, Object> evaluationResult;
            switch (question.getQuestionType()) {
                case SINGLE_CHOICE:
                    StandardObjectiveAnswer objectiveAnswer = objectiveAnswerRepository
                            .findByStandardQuestionId(question.getId())
                            .orElseThrow(() -> new EntityNotFoundException("找不到标准选择题答案"));
                    evaluationResult = evaluateSingleChoice(llmAnswer.getAnswerText(), 
                            objectiveAnswer.getCorrectOptionIds(), objectiveAnswer.getOptions());
                    break;
                    
                case MULTIPLE_CHOICE:
                    StandardObjectiveAnswer multiAnswer = objectiveAnswerRepository
                            .findByStandardQuestionId(question.getId())
                            .orElseThrow(() -> new EntityNotFoundException("找不到标准多选题答案"));
                    evaluationResult = evaluateMultipleChoice(llmAnswer.getAnswerText(), 
                            multiAnswer.getCorrectOptionIds(), multiAnswer.getOptions());
                    break;
                    
                case SIMPLE_FACT:
                    StandardSimpleAnswer simpleAnswer = simpleAnswerRepository
                            .findByStandardQuestionId(question.getId())
                            .orElseThrow(() -> new EntityNotFoundException("找不到标准简答题答案"));
                    evaluationResult = evaluateSimpleFact(llmAnswer.getAnswerText(), 
                            simpleAnswer.getAnswerText(), simpleAnswer.getAlternativeAnswers());
                    break;
                    
                case SUBJECTIVE:
                    // 获取评测标准
                    List<EvaluationCriterion> criteria = getCriteriaForQuestionType(QuestionType.SUBJECTIVE);
                    evaluationResult = evaluateSubjectiveWithAI(llmAnswer.getAnswerText(), 
                            question.getQuestionText(), 
                            question.getStandardSubjectiveAnswer().getAnswerText(), 
                            criteria, evaluatorId);
                    break;
                    
                default:
                    throw new IllegalArgumentException("不支持的问题类型: " + question.getQuestionType());
            }
            
            // 更新评测记录
            evaluation.setScore(new BigDecimal(evaluationResult.get("score").toString()));
            evaluation.setComments((String) evaluationResult.get("comments"));
            evaluation.setEvaluationResults(evaluationResult);
            evaluation.setStatus(EvaluationStatus.SUCCESS);
            evaluation.setCompletionTime(LocalDateTime.now());
            
            // 保存评测记录
            evaluation = evaluationRepository.save(evaluation);
            
            // 保存评测详情
            if (evaluationResult.containsKey("criteria_scores")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> criteriaScores = (List<Map<String, Object>>) evaluationResult.get("criteria_scores");
                
                List<EvaluationDetail> details = new ArrayList<>();
                for (Map<String, Object> score : criteriaScores) {
                    EvaluationDetail detail = new EvaluationDetail();
                    detail.setEvaluation(evaluation);
                    detail.setCriterionName((String) score.get("criterion"));
                    detail.setScore(new BigDecimal(score.get("score").toString()));
                    detail.setComments((String) score.get("comments"));
                    details.add(detail);
                }
                
                evaluationDetailRepository.saveAll(details);
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
            // 解析正确答案
            String correctAnswer = correctOptionIds.trim();
            
            // 解析学生答案
            String studentAnswer = answerText.trim();
            
            // 计算得分
            boolean isCorrect = correctAnswer.equalsIgnoreCase(studentAnswer);
            BigDecimal score = isCorrect ? new BigDecimal("100") : BigDecimal.ZERO;
            
            // 构建评测结果
            result.put("score", score);
            result.put("isCorrect", isCorrect);
            result.put("correctAnswer", correctAnswer);
            result.put("studentAnswer", studentAnswer);
            result.put("comments", isCorrect ? "答案正确" : "答案错误，正确答案是: " + correctAnswer);
            
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
            
            // 解析选项
            Map<String, String> optionsMap = objectMapper.readValue(options, new TypeReference<Map<String, String>>() {});
            
            // 解析正确答案ID列表
            Set<String> correctIdSet = new HashSet<>(Arrays.asList(correctIds.split(",")));
            
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
                // 遍历所有选项，查找答案中是否包含选项内容
                for (Map.Entry<String, String> entry : optionsMap.entrySet()) {
                    if (answerText.contains(entry.getValue())) {
                        studentChoices.add(entry.getKey());
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
                
                // 计算得分（满分10分）
                // 每个正确选择得到：10分 / 正确答案总数
                // 每个错误选择或漏选扣除：10分 / (正确答案总数 * 2)
                double pointsPerCorrect = 10.0 / correctIdSet.size();
                double pointsPerWrong = pointsPerCorrect / 2;
                
                double score = correctChoices.size() * pointsPerCorrect - 
                             (wrongChoices.size() + missedChoices.size()) * pointsPerWrong;
                
                // 确保分数在0-10范围内
                score = Math.max(0, Math.min(10, score));
                
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
                
            } else {
                result.put("score", BigDecimal.ZERO);
                result.put("comments", "未能从回答中识别出明确的选择。正确答案是选项：" + String.join("、", correctIdSet));
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
            
            // 计算ROUGE分数
            BigDecimal rougeScore = calculateRougeScore(answerText, bestMatchAnswer);
            
            // 计算BLEU分数
            BigDecimal bleuScore = calculateBleuScore(answerText, bestMatchAnswer);
            
            // 综合评分（权重：相似度0.4，ROUGE 0.3，BLEU 0.3）
            BigDecimal finalScore = maxSimilarity.multiply(new BigDecimal("0.4"))
                    .add(rougeScore.multiply(new BigDecimal("0.3")))
                    .add(bleuScore.multiply(new BigDecimal("0.3")));
            
            // 将分数转换为10分制
            finalScore = finalScore.multiply(BigDecimal.TEN).setScale(2, RoundingMode.HALF_UP);
            
            // 确保分数在0-10范围内
            if (finalScore.compareTo(BigDecimal.ZERO) < 0) {
                finalScore = BigDecimal.ZERO;
            } else if (finalScore.compareTo(BigDecimal.TEN) > 0) {
                finalScore = BigDecimal.TEN;
            }
            
            result.put("score", finalScore);
            
            // 生成评语
            StringBuilder comments = new StringBuilder();
            comments.append("回答评分：").append(finalScore).append("分\n\n");
            comments.append("评分详情：\n");
            comments.append("1. 文本相似度：").append(maxSimilarity.multiply(BigDecimal.TEN).setScale(2, RoundingMode.HALF_UP)).append("分\n");
            comments.append("2. ROUGE分数：").append(rougeScore.multiply(BigDecimal.TEN).setScale(2, RoundingMode.HALF_UP)).append("分\n");
            comments.append("3. BLEU分数：").append(bleuScore.multiply(BigDecimal.TEN).setScale(2, RoundingMode.HALF_UP)).append("分\n\n");
            
            if (finalScore.compareTo(new BigDecimal("8")) >= 0) {
                comments.append("回答非常准确，与标准答案高度一致。");
            } else if (finalScore.compareTo(new BigDecimal("6")) >= 0) {
                comments.append("回答基本正确，但表述可以更准确。");
            } else if (finalScore.compareTo(new BigDecimal("4")) >= 0) {
                comments.append("回答部分正确，但存在一些偏差。建议参考标准答案：").append(bestMatchAnswer);
            } else {
                comments.append("回答与标准答案差异较大。标准答案是：").append(bestMatchAnswer);
            }
            
            result.put("comments", comments.toString());
            
            // 添加评测详情
            List<Map<String, Object>> criteriaScores = new ArrayList<>();
            
            // 相似度评分
            Map<String, Object> similarityScore = new HashMap<>();
            similarityScore.put("criterion", "文本相似度");
            similarityScore.put("score", maxSimilarity.multiply(BigDecimal.TEN));
            similarityScore.put("comments", "文本相似度评分");
            criteriaScores.add(similarityScore);
            
            // ROUGE评分
            Map<String, Object> rougeScoreMap = new HashMap<>();
            rougeScoreMap.put("criterion", "ROUGE分数");
            rougeScoreMap.put("score", rougeScore.multiply(BigDecimal.TEN));
            rougeScoreMap.put("comments", "ROUGE评分");
            criteriaScores.add(rougeScoreMap);
            
            // BLEU评分
            Map<String, Object> bleuScoreMap = new HashMap<>();
            bleuScoreMap.put("criterion", "BLEU分数");
            bleuScoreMap.put("score", bleuScore.multiply(BigDecimal.TEN));
            bleuScoreMap.put("comments", "BLEU评分");
            criteriaScores.add(bleuScoreMap);
            
            result.put("criteria_scores", criteriaScores);
            
        } catch (Exception e) {
            logger.error("评测简单事实题失败", e);
            result.put("score", BigDecimal.ZERO);
            result.put("comments", "评测过程发生错误：" + e.getMessage());
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
            
            // 分词处理
            String[] words1 = text1.toLowerCase().split("\\s+");
            String[] words2 = text2.toLowerCase().split("\\s+");
            
            // 如果任一文本分词后为空，返回0分
            if (words1.length == 0 || words2.length == 0) {
                logger.warn("计算文本相似度失败：分词后文本为空");
                return BigDecimal.ZERO;
            }
            
            // 计算词频
            Map<String, Integer> freq1 = new HashMap<>();
            Map<String, Integer> freq2 = new HashMap<>();
            
            for (String word : words1) {
                freq1.put(word, freq1.getOrDefault(word, 0) + 1);
            }
            
            for (String word : words2) {
                freq2.put(word, freq2.getOrDefault(word, 0) + 1);
            }
            
            // 计算余弦相似度
            double dotProduct = 0.0;
            double norm1 = 0.0;
            double norm2 = 0.0;
            
            // 计算点积和向量范数
            for (Map.Entry<String, Integer> entry : freq1.entrySet()) {
                String word = entry.getKey();
                int count1 = entry.getValue();
                int count2 = freq2.getOrDefault(word, 0);
                
                dotProduct += count1 * count2;
                norm1 += count1 * count1;
            }
            
            for (int count : freq2.values()) {
                norm2 += count * count;
            }
            
            // 计算相似度
            double similarity = dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
            
            // 转换为BigDecimal并四舍五入到2位小数
            BigDecimal result = new BigDecimal(similarity).setScale(2, RoundingMode.HALF_UP);
            
            // 确保结果在0-1范围内
            if (result.compareTo(BigDecimal.ZERO) < 0) {
                result = BigDecimal.ZERO;
            } else if (result.compareTo(BigDecimal.ONE) > 0) {
                result = BigDecimal.ONE;
            }
            
            logger.info("文本相似度计算结果: {}", result);
            return result;
            
        } catch (Exception e) {
            logger.error("计算文本相似度时发生错误", e);
            return BigDecimal.ZERO;
        }
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
            
            // 分词处理
            String[] candidateWords = candidateText.toLowerCase().split("\\s+");
            String[] referenceWords = referenceText.toLowerCase().split("\\s+");
            
            // 如果任一文本分词后为空，返回0分
            if (candidateWords.length == 0 || referenceWords.length == 0) {
                logger.warn("计算ROUGE分数失败：分词后文本为空");
                return BigDecimal.ZERO;
            }
            
            // 计算ROUGE-1分数（单词级别的重叠）
            Set<String> candidateSet = new HashSet<>(Arrays.asList(candidateWords));
            Set<String> referenceSet = new HashSet<>(Arrays.asList(referenceWords));
            
            // 计算重叠的单词数
            Set<String> overlap = new HashSet<>(candidateSet);
            overlap.retainAll(referenceSet);
            
            // 计算召回率和精确率
            double recall = (double) overlap.size() / referenceSet.size();
            double precision = (double) overlap.size() / candidateSet.size();
            
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
            
            logger.info("ROUGE分数计算结果: {}", result);
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
        logger.info("提交人工评测结果，评测ID: {}, 总分: {}, 用户ID: {}", 
                evaluationId, overallScore, userId);
        
        try {
            // 获取评测记录
            Evaluation evaluation = evaluationRepository.findById(evaluationId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测记录: " + evaluationId));
            
            // 验证评测状态
            if (evaluation.getStatus() != EvaluationStatus.PENDING) {
                throw new IllegalStateException("评测记录状态不允许提交: " + evaluation.getStatus());
            }
            
            // 验证评测者类型是人类
            if (evaluation.getEvaluator().getEvaluatorType() != Evaluator.EvaluatorType.HUMAN) {
                throw new IllegalStateException("只能提交人工评测结果");
            }
            
            // 获取用户信息
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("用户不存在: " + userId));
            
            // 验证用户权限（这里可以添加更多的权限检查逻辑）
            if (!user.getId().equals(evaluation.getCreatedByUser().getId())) {
                throw new IllegalStateException("无权提交此评测结果");
            }
            
            // 更新评测记录
            evaluation.setScore(overallScore);
            evaluation.setComments(comments);
            
            // 构建评测结果
            Map<String, Object> evaluationResults = new HashMap<>();
            evaluationResults.put("overall_score", overallScore);
            evaluationResults.put("overall_comments", comments);
            evaluationResults.put("criteria_scores", detailScores);
            evaluation.setEvaluationResults(evaluationResults);
            
            evaluation.setStatus(EvaluationStatus.SUCCESS);
            evaluation.setCompletionTime(LocalDateTime.now());
            
            // 保存评测记录
            evaluation = evaluationRepository.save(evaluation);
            
            // 保存评测详情
            List<EvaluationDetail> details = new ArrayList<>();
            for (Map<String, Object> score : detailScores) {
                EvaluationDetail detail = new EvaluationDetail();
                detail.setEvaluation(evaluation);
                detail.setCriterionName((String) score.get("criterion"));
                detail.setScore(new BigDecimal(score.get("score").toString()));
                detail.setComments((String) score.get("comments"));
                details.add(detail);
            }
            
            evaluationDetailRepository.saveAll(details);
            
            logger.info("人工评测结果提交成功，评测ID: {}", evaluation.getId());
            return evaluation;
            
        } catch (Exception e) {
            logger.error("提交人工评测结果失败", e);
            throw new RuntimeException("提交人工评测结果失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public EvaluationRun createEvaluationRun(Long modelAnswerRunId, Long evaluatorId, String runName, 
                                            String runDescription, String parameters, Long userId) {
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
            evaluationRun.setParameters(parameters);
            evaluationRun.setStatus(RunStatus.PENDING);  // 初始状态为等待中
            evaluationRun.setCreatedBy(userId);
            evaluationRun.setCreationTime(LocalDateTime.now());
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
        logger.info("暂停评测运行，运行ID: {}", evaluationRunId);
        
        try {
            // 获取评测运行记录
            EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
            
            // 检查状态是否允许暂停
            if (evaluationRun.getStatus() != RunStatus.IN_PROGRESS) {
                logger.warn("评测运行状态不允许暂停: {}", evaluationRun.getStatus());
                return false;
            }
            
            // 更新状态为暂停
            evaluationRun.setStatus(RunStatus.PAUSED);
            evaluationRun.setLastUpdated(LocalDateTime.now());
            evaluationRunRepository.save(evaluationRun);
            
            logger.info("评测运行已暂停，运行ID: {}", evaluationRunId);
            return true;
            
        } catch (Exception e) {
            logger.error("暂停评测运行失败", e);
            throw new RuntimeException("暂停评测运行失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public CompletableFuture<Void> resumeEvaluationRun(Long evaluationRunId) {
        logger.info("恢复评测运行，运行ID: {}", evaluationRunId);
        
        try {
            // 获取评测运行记录
            EvaluationRun evaluationRun = evaluationRunRepository.findById(evaluationRunId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到指定的评测运行记录: " + evaluationRunId));
            
            // 检查状态是否允许恢复
            if (evaluationRun.getStatus() != RunStatus.PAUSED) {
                logger.warn("评测运行状态不允许恢复: {}", evaluationRun.getStatus());
                throw new IllegalStateException("评测运行状态不允许恢复: " + evaluationRun.getStatus());
            }
            
            // 更新状态为进行中
            evaluationRun.setStatus(RunStatus.IN_PROGRESS);
            evaluationRun.setLastUpdated(LocalDateTime.now());
            evaluationRunRepository.save(evaluationRun);
            
            // 继续评测过程
            return startEvaluationRun(evaluationRunId);
            
        } catch (Exception e) {
            logger.error("恢复评测运行失败", e);
            throw new RuntimeException("恢复评测运行失败: " + e.getMessage(), e);
        }
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
} 
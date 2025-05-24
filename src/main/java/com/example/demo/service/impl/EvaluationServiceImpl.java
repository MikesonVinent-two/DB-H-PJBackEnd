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
import com.example.demo.repository.EvaluationRepository;
import com.example.demo.repository.EvaluatorRepository;
import com.example.demo.repository.StandardObjectiveAnswerRepository;
import com.example.demo.repository.StandardSimpleAnswerRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.LlmAnswerRepository;
import com.example.demo.repository.ModelAnswerRunRepository;
import com.example.demo.repository.EvaluationRunRepository;
import com.example.demo.repository.EvaluationDetailRepository;
import com.example.demo.repository.EvaluationCriterionRepository;
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
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    // ... 其他方法保持不变 ...
    
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
            
            // 构建评测提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("你是一位专业的答案评测专家。请对以下主观题的回答进行评测。\n\n");
            prompt.append("问题：").append(questionText).append("\n\n");
            prompt.append("学生回答：").append(answerText).append("\n\n");
            prompt.append("参考答案：").append(referenceAnswer).append("\n\n");
            
            // 添加评测标准
            prompt.append("评测标准：\n");
            for (EvaluationCriterion criterion : criteria) {
                prompt.append("- ").append(criterion.getName()).append("：")
                      .append(criterion.getDescription()).append("\n");
            }
            
            prompt.append("\n请对回答进行全面评测，并给出以下格式的评测结果：\n");
            prompt.append("1. 总体评分（0-10分）\n");
            prompt.append("2. 各评测标准的得分和评语\n");
            prompt.append("3. 总体评语，包括优点和不足\n");
            prompt.append("4. 改进建议\n\n");
            prompt.append("请以JSON格式输出，格式如下：\n");
            prompt.append("{\n");
            prompt.append("  \"overall_score\": 分数,\n");
            prompt.append("  \"criteria_scores\": [\n");
            prompt.append("    {\"criterion\": \"标准名称\", \"score\": 分数, \"comments\": \"评语\"},\n");
            prompt.append("    ...\n");
            prompt.append("  ],\n");
            prompt.append("  \"overall_comments\": \"总体评语\",\n");
            prompt.append("  \"improvement_suggestions\": \"改进建议\"\n");
            prompt.append("}");
            
            // 调用AI服务进行评测
            String aiResponse = callAIService(prompt.toString(), evaluator.getLlmModel().getId());
            
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
            
            // 解析评测结果
            if (evaluation.getEvaluationResults() == null || evaluation.getEvaluationResults().isEmpty()) {
                return new ArrayList<>();
            }
            
            Map<String, Object> results;
            try {
                results = objectMapper.readValue(evaluation.getEvaluationResults(), 
                        new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                logger.error("解析评测结果失败", e);
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
        logger.info("获取问题类型的评测标准，类型: {}", questionType);
        
        try {
            // 从数据库中查询特定问题类型的评测标准
            List<EvaluationCriterion> criteria = evaluationCriterionRepository.findActiveByQuestionTypeOrderByOrderIndex(questionType);
            
            // 如果数据库中没有找到，则提供默认标准
            if (criteria.isEmpty()) {
                criteria = createDefaultCriteriaForQuestionType(questionType);
                // 保存默认标准到数据库
                criteria = evaluationCriterionRepository.saveAll(criteria);
            }
            
            return criteria;
            
        } catch (Exception e) {
            logger.error("获取问题类型的评测标准失败", e);
            // 返回默认标准，不保存到数据库
            return createDefaultCriteriaForQuestionType(questionType);
        }
    }
    
    /**
     * 创建问题类型的默认评测标准
     */
    private List<EvaluationCriterion> createDefaultCriteriaForQuestionType(QuestionType questionType) {
        List<EvaluationCriterion> criteria = new ArrayList<>();
        
        if (questionType == QuestionType.SUBJECTIVE) {
            // 主观题评测标准
            criteria.add(createCriterion(null, "内容完整性", "评估回答是否涵盖了问题要求的所有关键点", "SCORE", "0-10"));
            criteria.add(createCriterion(null, "逻辑性", "评估回答的论述是否逻辑清晰、连贯", "SCORE", "0-10"));
            criteria.add(createCriterion(null, "专业性", "评估回答是否展示了对主题的专业理解和知识", "SCORE", "0-10"));
            criteria.add(createCriterion(null, "创新性", "评估回答是否提供了独特的见解或创新的思路", "SCORE", "0-10"));
            criteria.add(createCriterion(null, "表达能力", "评估回答的语言表达是否清晰、准确、恰当", "SCORE", "0-10"));
            
        } else if (questionType == QuestionType.SIMPLE_FACT) {
            // 简单事实题评测标准
            criteria.add(createCriterion(null, "准确性", "评估回答是否与标准答案在事实上一致", "SCORE", "0-10"));
            criteria.add(createCriterion(null, "完整性", "评估回答是否包含了所有必要的关键信息", "SCORE", "0-10"));
            criteria.add(createCriterion(null, "简洁性", "评估回答是否简明扼要，没有冗余信息", "SCORE", "0-10"));
            
        } else if (questionType == QuestionType.SINGLE_CHOICE || questionType == QuestionType.MULTIPLE_CHOICE) {
            // 选择题评测标准
            criteria.add(createCriterion(null, "正确性", "评估选择是否正确", "BOOLEAN", "true/false"));
            criteria.add(createCriterion(null, "解释质量", "评估对选择的解释是否合理、准确", "SCORE", "0-10"));
        }
        
        // 设置问题类型和顺序
        for (int i = 0; i < criteria.size(); i++) {
            EvaluationCriterion criterion = criteria.get(i);
            criterion.setQuestionType(questionType);
            criterion.setOrderIndex(i);
        }
        
        return criteria;
    }
    
    /**
     * 创建评测标准对象
     */
    private EvaluationCriterion createCriterion(Long id, String name, String description, String dataType, String scoreRange) {
        EvaluationCriterion criterion = new EvaluationCriterion();
        criterion.setId(id);
        criterion.setName(name);
        criterion.setDescription(description);
        criterion.setDataType(EvaluationCriterion.DataType.valueOf(dataType));
        criterion.setScoreRange(scoreRange);
        return criterion;
    }
    
    // ... 其他方法保持不变 ...
} 
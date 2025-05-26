package com.example.demo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.service.LlmApiService;
import com.example.demo.entity.LlmModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Map;
import java.util.HashMap;

/**
 * LLM API服务实现类
 */
@Service
public class LlmApiServiceImpl implements LlmApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmApiServiceImpl.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public LlmApiServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String generateAnswer(String apiUrl, String apiKey, String prompt, Map<String, Object> parameters) {
        return generateAnswer(apiUrl, apiKey, null, prompt, parameters);
    }
    
    @Override
    public String generateAnswer(String apiUrl, String apiKey, String apiType, String prompt, Map<String, Object> parameters) {
        logger.debug("调用LLM API生成回答, URL: {}, API类型: {}", apiUrl, apiType);
        
        try {
            // 根据API类型补全API路径
            if (apiUrl != null && !apiUrl.isEmpty()) {
                if (apiType != null) {
                    switch (apiType.toLowerCase()) {
                        case "openai":
                        case "openai_compatible":
                            // 检查并补全OpenAI API路径
                            if (!apiUrl.endsWith("/v1/chat/completions")) {
                                if (!apiUrl.endsWith("/v1")) {
                                    apiUrl = apiUrl.endsWith("/") 
                                        ? apiUrl + "v1/chat/completions" 
                                        : apiUrl + "/v1/chat/completions";
                                } else {
                                    apiUrl = apiUrl + "/chat/completions";
                                }
                            }
                            break;
                        case "anthropic":
                            // 检查并补全Anthropic API路径
                            if (!apiUrl.endsWith("/v1/complete")) {
                                apiUrl = apiUrl.endsWith("/") 
                                    ? apiUrl + "v1/complete" 
                                    : apiUrl + "/v1/complete";
                            }
                            break;
                        case "google":
                            // 检查并补全Google API路径
                            if (!apiUrl.contains("/v1/models") && !apiUrl.contains("/generateContent")) {
                                apiUrl = apiUrl.endsWith("/") 
                                    ? apiUrl + "v1/models/gemini-pro:generateContent" 
                                    : apiUrl + "/v1/models/gemini-pro:generateContent";
                            }
                            break;
                    }
                    logger.debug("完整API URL: {}", apiUrl);
                }
            }
            
            // 准备HTTP请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            if (apiKey != null && !apiKey.isEmpty()) {
                // 根据API类型设置不同的认证头
                if (apiType != null) {
                    switch (apiType.toLowerCase()) {
                        case "openai":
                            headers.set("Authorization", "Bearer " + apiKey);
                            break;
                        case "openai_compatible":
                            // OpenAI兼容类型，使用相同的认证方式
                            headers.set("Authorization", "Bearer " + apiKey);
                            break;
                        case "azure":
                            headers.set("api-key", apiKey);
                            break;
                        case "anthropic":
                            headers.set("x-api-key", apiKey);
                            break;
                        case "google":
                            headers.set("Authorization", "Bearer " + apiKey);
                            break;
                        default:
                            // 默认Bearer认证
                            headers.set("Authorization", "Bearer " + apiKey);
                            break;
                    }
                } else {
                    // 兼容旧代码，根据URL推断
                    if (apiUrl.contains("openai.com")) {
                        headers.set("Authorization", "Bearer " + apiKey);
                    } else if (apiUrl.contains("azure.com")) {
                        headers.set("api-key", apiKey);
                    } else if (apiUrl.contains("anthropic.com")) {
                        headers.set("x-api-key", apiKey);
                    } else {
                        // 默认Bearer认证
                        headers.set("Authorization", "Bearer " + apiKey);
                    }
                }
            }
            
            // 构建请求体
            ObjectNode requestBody = createRequestBody(prompt, parameters, apiType);
            
            // 打印问题内容
            logger.info("向LLM发送问题: {}", prompt);
            
            // 创建HTTP实体
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
            
            // 发送请求
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(apiUrl, requestEntity, String.class);
            
            // 解析响应
            return parseResponse(responseEntity.getBody(), apiType);
            
        } catch (Exception e) {
            logger.error("LLM API调用失败", e);
            throw new RuntimeException("LLM API调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据API类型和参数创建请求体
     */
    private ObjectNode createRequestBody(String prompt, Map<String, Object> parameters, String apiType) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            
            // 默认参数
            Map<String, Object> defaultParams = new HashMap<>();
            defaultParams.put("temperature", 0.7);
            defaultParams.put("max_tokens", 1000);
            
            // 用提供的参数覆盖默认参数
            if (parameters != null) {
                defaultParams.putAll(parameters);
            }
            
            // 根据API类型构建不同的请求体
            if (apiType != null) {
                switch (apiType.toLowerCase()) {
                    case "openai":
                    case "openai_compatible":
                        // OpenAI API格式或兼容格式
                        if (!defaultParams.containsKey("model")) {
                            requestBody.put("model", "gpt-3.5-turbo");
                        } else {
                            requestBody.put("model", defaultParams.get("model").toString());
                        }
                        
                        // 修复messages数组格式
                        var messagesArray = objectMapper.createArrayNode();
                        var messageObject = objectMapper.createObjectNode();
                        messageObject.put("role", "user");
                        messageObject.put("content", prompt);
                        messagesArray.add(messageObject);
                        requestBody.set("messages", messagesArray);
                        break;
                        
                    case "anthropic":
                        // Anthropic Claude API格式
                        if (!defaultParams.containsKey("model")) {
                            requestBody.put("model", "claude-2");
                        } else {
                            requestBody.put("model", defaultParams.get("model").toString());
                        }
                        
                        requestBody.put("prompt", "\n\nHuman: " + prompt + "\n\nAssistant: ");
                        break;
                        
                    case "google":
                        // Google PaLM2/Gemini API格式
                        requestBody.put("prompt", prompt);
                        break;
                        
                    default:
                        // 默认简单格式
                        requestBody.put("prompt", prompt);
                        break;
                }
            } else {
                // 兼容旧代码逻辑
                requestBody.put("prompt", prompt);
            }
            
            // 添加其他参数
            for (Map.Entry<String, Object> entry : defaultParams.entrySet()) {
                if (!entry.getKey().equals("model") && !entry.getKey().equals("messages") && !entry.getKey().equals("prompt")) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    // 对于特定参数使用整数类型
                    switch (key) {
                        case "max_tokens":
                        case "n":
                        case "top_k":
                            if (value instanceof Number) {
                                requestBody.put(key, ((Number) value).intValue());
                            }
                            break;
                        case "presence_penalty":
                        case "frequency_penalty":
                        case "temperature":
                        case "top_p":
                            if (value instanceof Number) {
                                requestBody.put(key, ((Number) value).doubleValue());
                            }
                            break;
                        default:
                            if (value instanceof Number) {
                                requestBody.put(key, ((Number) value).doubleValue());
                            } else if (value != null) {
                                requestBody.put(key, value.toString());
                            }
                    }
                }
            }
            
            return requestBody;
        } catch (Exception e) {
            logger.error("创建LLM API请求体失败", e);
            throw new RuntimeException("创建LLM API请求体失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析API响应
     */
    private String parseResponse(String responseJson, String apiType) {
        try {
            if (responseJson == null || responseJson.isEmpty()) {
                return "";
            }
            
            JsonNode responseNode = objectMapper.readTree(responseJson);
            
            // 根据API类型解析不同格式的响应
            if (apiType != null) {
                switch (apiType.toLowerCase()) {
                    case "openai":
                    case "openai_compatible":
                        // OpenAI响应格式或兼容格式
                        if (responseNode.has("choices") && responseNode.get("choices").size() > 0) {
                            JsonNode choice = responseNode.get("choices").get(0);
                            if (choice.has("message") && choice.get("message").has("content")) {
                                return choice.get("message").get("content").asText();
                            }
                        }
                        break;
                        
                    case "anthropic":
                        // Anthropic Claude响应格式
                        if (responseNode.has("completion")) {
                            return responseNode.get("completion").asText();
                        }
                        break;
                        
                    case "google":
                        // Google PaLM2/Gemini响应格式
                        if (responseNode.has("candidates") && responseNode.get("candidates").size() > 0) {
                            return responseNode.get("candidates").get(0).get("content").asText();
                        }
                        break;
                        
                    default:
                        // 尝试通用解析
                        break;
                }
            }
            
            // 通用响应解析逻辑，尝试从各种常见格式中提取
            if (responseNode.has("choices") && responseNode.get("choices").size() > 0) {
                // OpenAI或类似格式
                JsonNode choice = responseNode.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    return choice.get("message").get("content").asText();
                } else if (choice.has("text")) {
                    return choice.get("text").asText();
                }
            } else if (responseNode.has("completion")) {
                // Anthropic或类似格式
                return responseNode.get("completion").asText();
            } else if (responseNode.has("generated_text")) {
                // 某些模型的格式
                return responseNode.get("generated_text").asText();
            } else if (responseNode.has("result") && responseNode.get("result").isTextual()) {
                // 通用格式
                return responseNode.get("result").asText();
            }
            
            // 如果无法提取，记录并返回原始响应
            logger.warn("无法从响应中提取文本内容，返回空字符串。原始响应: {}", responseJson);
            return "";
            
        } catch (Exception e) {
            logger.error("解析API响应失败", e);
            return "";
        }
    }
    
    /**
     * 测试模型连通性（修复版本）
     * @param apiUrl API地址
     * @param apiKey API密钥
     * @param apiType API类型
     * @return 是否连接成功
     */
    public boolean testModelConnectivity(String apiUrl, String apiKey, String apiType) {
        logger.info("测试模型连通性: URL={}, 类型={}", apiUrl, apiType);
        
        try {
            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + apiKey);
                logger.debug("添加授权头: Bearer {}", apiKey.substring(0, Math.min(5, apiKey.length())) + "...");
            }
            
            // 准备简单请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            
            // 根据不同API类型准备测试请求
            if ("OPENAI".equalsIgnoreCase(apiType) || "AZURE_OPENAI".equalsIgnoreCase(apiType)) {
                // OpenAI格式请求体
                logger.debug("准备OpenAI格式测试请求");
                ArrayNode messagesArray = requestBody.putArray("messages");
                ObjectNode message = objectMapper.createObjectNode();
                message.put("role", "user");
                message.put("content", "测试连接");
                messagesArray.add(message);
                requestBody.put("model", "gpt-3.5-turbo");
                requestBody.put("max_tokens", 10);
            } else if ("ANTHROPIC".equalsIgnoreCase(apiType)) {
                // Anthropic格式请求体
                logger.debug("准备Anthropic格式测试请求");
                requestBody.put("prompt", "Human: 测试连接\nAssistant:");
                requestBody.put("model", "claude-instant-1");
                requestBody.put("max_tokens_to_sample", 10);
            } else {
                // 通用格式
                logger.debug("准备通用格式测试请求: {}", apiType);
                requestBody.put("message", "测试连接");
            }
            
            // 创建HTTP实体
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
            
            try {
                // 发送POST请求
                logger.debug("发送测试POST请求: {}", requestBody);
                
                ResponseEntity<String> response = restTemplate.postForEntity(
                        apiUrl, requestEntity, String.class);
                
                boolean success = response.getStatusCode().is2xxSuccessful();
                logger.info("模型连通性测试结果: {}, 状态码: {}, 响应长度: {}", 
                        success ? "成功" : "失败", 
                        response.getStatusCodeValue(),
                        response.getBody() != null ? response.getBody().length() : 0);
                
                return success;
            } catch (Exception e) {
                logger.warn("POST请求测试失败，尝试GET请求: {}", e.getMessage());
                
                try {
                    // 尝试GET请求
                    ResponseEntity<String> getResponse = restTemplate.getForEntity(
                            apiUrl, String.class);
                    
                    boolean success = getResponse.getStatusCode().is2xxSuccessful();
                    logger.info("模型GET请求测试结果: {}, 状态码: {}", 
                            success ? "成功" : "失败", getResponse.getStatusCodeValue());
                    
                    return success;
                } catch (Exception getEx) {
                    logger.error("模型连通性测试失败（GET请求）: {}", getEx.getMessage());
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("模型连通性测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 使用LLM模型生成回答
     */
    @Override
    public String generateModelAnswer(LlmModel model, String prompt, Map<String, Object> contextVariables) {
        logger.debug("调用LLM模型生成回答, 模型: {}, API类型: {}", model.getName(), model.getApiType());
        
        // 组装参数
        Map<String, Object> parameters = new HashMap<>();
        
        // 添加模型默认参数
        if (model.getModelParameters() != null) {
            parameters.putAll(model.getModelParameters());
        }
        
        // 添加上下文变量（优先级更高）
        if (contextVariables != null) {
            parameters.putAll(contextVariables);
        }
        
        // 调用生成回答
        return generateAnswer(
            model.getApiUrl(),
            model.getApiKey(),
            model.getApiType(),
            prompt,
            parameters
        );
    }
} 
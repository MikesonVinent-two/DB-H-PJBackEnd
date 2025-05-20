package com.example.demo.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.demo.config.LlmConfig;
import com.example.demo.config.RestTemplateConfig;
import com.example.demo.dto.LlmRequestDTO;
import com.example.demo.dto.LlmResponseDTO;
import com.example.demo.dto.ModelInfoDTO;
import com.example.demo.service.LlmService;

@Service
public class LlmServiceImpl implements LlmService {

    private static final Logger logger = LoggerFactory.getLogger(LlmServiceImpl.class);
    
    private final RestTemplate restTemplate;
    private final RestTemplateBuilder restTemplateBuilder;
    private final RestTemplateConfig restTemplateConfig;
    private final LlmConfig llmConfig;

    @Autowired
    public LlmServiceImpl(RestTemplate restTemplate, RestTemplateBuilder restTemplateBuilder, 
                         RestTemplateConfig restTemplateConfig, LlmConfig llmConfig) {
        this.restTemplate = restTemplate;
        this.restTemplateBuilder = restTemplateBuilder;
        this.restTemplateConfig = restTemplateConfig;
        this.llmConfig = llmConfig;
    }

    /**
     * 根据基础URL构建完整的API端点URL
     */
    private String buildApiUrl(String baseUrl) {
        // 移除URL末尾的斜杠
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        
        // 根据不同的API类型添加对应的端点路径        
        return baseUrl + "/v1/chat/completions";
    }

    @Override
    @Retryable(
        value = {RestClientException.class},
        maxAttemptsExpression = "#{@llmConfig.retry.maxAttempts}",
        backoff = @Backoff(delayExpression = "#{@llmConfig.retry.backoffDelay}")
    )
    public LlmResponseDTO sendRequest(LlmRequestDTO request) {
        long startTime = System.currentTimeMillis();
        
        // 如果API URL为空，使用默认值
        if (request.getApi() == null || request.getApi().isEmpty()) {
            request.setApi(llmConfig.getDefaultApiUrl());
            logger.info("使用默认API URL: {}", llmConfig.getDefaultApiUrl());
        }
        
        // 构建完整的API URL
        String apiUrl = buildApiUrl(request.getApi());
        logger.info("构建完整API URL: {}", apiUrl);
        
        // 如果模型为空，使用默认值
        if (request.getModel() == null || request.getModel().isEmpty()) {
            request.setModel(llmConfig.getDefaultModel());
            logger.info("使用默认模型: {}", llmConfig.getDefaultModel());
        }
        
        try {
            logger.info("发送请求到LLM API: {}, 模型: {}", apiUrl, request.getModel());
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + request.getApiKey());
            
            // 根据不同的API构建不同的请求体
            Map<String, Object> requestBody = buildRequestBody(request);
            
            // 创建HTTP实体
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // 根据模型类型获取特定的RestTemplate
            RestTemplate modelSpecificRestTemplate = 
                restTemplateConfig.getModelSpecificRestTemplate(restTemplateBuilder, request.getModel());
            
            // 记录实际使用的超时设置
            logger.info("模型 {} 使用的超时设置: {}", request.getModel(), 
                       modelSpecificRestTemplate.getRequestFactory().toString());
            
            // 发送请求
            ResponseEntity<Map> response = modelSpecificRestTemplate.postForEntity(
                    apiUrl,
                    entity,
                    Map.class
            );
            
            // 处理响应
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            
            logger.info("LLM API响应成功，耗时: {}ms", responseTime);
            
            return parseResponse(response.getBody(), request.getModel(), responseTime);
            
        } catch (RestClientException e) {
            logger.error("API请求失败: {}", e.getMessage(), e);
            return new LlmResponseDTO(false, "API请求失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("处理请求时发生错误: {}", e.getMessage(), e);
            return new LlmResponseDTO(false, "处理请求时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 根据不同的API构建请求体
     */
    private Map<String, Object> buildRequestBody(LlmRequestDTO request) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // 判断API类型并构建相应的请求体
        if (request.getApi().contains("openai.com")) {
            // OpenAI API格式
            requestBody.put("model", request.getModel());
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统提示
            if (request.getSystemPrompts() != null && !request.getSystemPrompts().isEmpty()) {
                messages.addAll(request.getSystemPrompts());
            } else {
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", "你是一个有用的AI助手。");
                messages.add(systemMessage);
            }
            
            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getMessage());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            
            // 添加其他参数
            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            }
            
            if (request.getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getMaxTokens());
            }
        } else if (request.getApi().contains("anthropic.com")) {
            // Anthropic API格式
            requestBody.put("model", request.getModel());
            
            // 添加系统提示
            if (request.getSystemPrompts() != null && !request.getSystemPrompts().isEmpty()) {
                Map<String, String> systemPrompt = request.getSystemPrompts().get(0);
                if (systemPrompt.containsKey("content")) {
                    requestBody.put("system", systemPrompt.get("content"));
                }
            }
            
            // 添加用户消息
            requestBody.put("prompt", request.getMessage());
            
            // 添加其他参数
            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            }
            
            if (request.getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getMaxTokens());
            }
        } else {
            // 默认格式，使用通用的chat completions格式
            requestBody.put("model", request.getModel());
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统提示
            if (request.getSystemPrompts() != null && !request.getSystemPrompts().isEmpty()) {
                messages.addAll(request.getSystemPrompts());
            } else {
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", "你是一个有用的AI助手。");
                messages.add(systemMessage);
            }
            
            // 添加用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getMessage());
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            
            // 添加其他参数
            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            }
            
            if (request.getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getMaxTokens());
            }
            
            // 添加其他自定义参数
            if (request.getAdditionalParams() != null) {
                requestBody.putAll(request.getAdditionalParams());
            }
        }
        
        return requestBody;
    }
    
    /**
     * 解析不同API的响应
     */
    private LlmResponseDTO parseResponse(Map<String, Object> responseBody, String model, long responseTime) {
        if (responseBody == null) {
            logger.warn("API返回空响应");
            return new LlmResponseDTO(false, "API返回空响应");
        }
        
        try {
            String content = "";
            Integer tokenCount = null;
            
            // 解析不同API的响应格式
            if (responseBody.containsKey("choices")) {
                // OpenAI格式
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    
                    if (choice.containsKey("message")) {
                        Map<String, String> message = (Map<String, String>) choice.get("message");
                        content = message.get("content");
                    } else if (choice.containsKey("text")) {
                        content = (String) choice.get("text");
                    }
                }
                
                // 获取token计数
                if (responseBody.containsKey("usage")) {
                    Map<String, Integer> usage = (Map<String, Integer>) responseBody.get("usage");
                    tokenCount = usage.get("total_tokens");
                }
            } else if (responseBody.containsKey("content")) {
                // 简单格式
                content = (String) responseBody.get("content");
            } else if (responseBody.containsKey("completion")) {
                // 某些API使用completion字段
                content = (String) responseBody.get("completion");
            }
            
            LlmResponseDTO response = new LlmResponseDTO(content, model, tokenCount, responseTime, true);
            response.setMetadata(responseBody);
            
            return response;
        } catch (Exception e) {
            logger.error("解析API响应时发生错误: {}", e.getMessage(), e);
            return new LlmResponseDTO(false, "解析API响应时发生错误: " + e.getMessage());
        }
    }

    @Override
    @Retryable(
        value = {RestClientException.class},
        maxAttemptsExpression = "#{@llmConfig.retry.maxAttempts}",
        backoff = @Backoff(delayExpression = "#{@llmConfig.retry.backoffDelay}")
    )
    public List<ModelInfoDTO> getAvailableModels(String apiUrl, String apiKey) {
        logger.info("正在获取可用模型列表，API URL: {}", apiUrl);
        
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 构建请求实体
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 构建模型列表API URL（保持与OpenAI兼容的路径）
            String modelsUrl = apiUrl.endsWith("/") ? apiUrl + "v1/models" : apiUrl + "/v1/models";
            
            // 使用默认RestTemplate发送请求 - 这里不需要模型特定的超时设置
            ResponseEntity<Map> response = restTemplate.exchange(
                modelsUrl,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            // 解析响应（假设响应格式与OpenAI兼容）
            List<ModelInfoDTO> models = new ArrayList<>();
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                List<Map<String, Object>> modelData = (List<Map<String, Object>>) response.getBody().get("data");
                
                for (Map<String, Object> model : modelData) {
                    ModelInfoDTO modelInfo = new ModelInfoDTO(
                        (String) model.get("id"),
                        (String) model.get("id"), // 使用id作为名称
                        getProviderFromUrl(apiUrl)
                    );
                    
                    // 设置其他属性
                    if (model.containsKey("description")) {
                        modelInfo.setDescription((String) model.get("description"));
                    }
                    
                    // 设置最大token数（如果有）
                    if (model.containsKey("max_tokens")) {
                        modelInfo.setMaxTokens((Integer) model.get("max_tokens"));
                    }
                    
                    // 设置可用性
                    modelInfo.setAvailable(true);
                    
                    models.add(modelInfo);
                }
            }
            
            logger.info("成功获取到{}个可用模型", models.size());
            return models;
            
        } catch (Exception e) {
            logger.error("获取模型列表时发生错误: {}", e.getMessage(), e);
            throw new RestClientException("获取模型列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 从API URL中获取提供商名称
     */
    private String getProviderFromUrl(String apiUrl) {
        if (apiUrl.contains("openai.com")) {
            return "OpenAI";
        } else if (apiUrl.contains("anthropic.com")) {
            return "Anthropic";
        } else {
            // 尝试从URL中提取提供商名称
            try {
                String host = new java.net.URL(apiUrl).getHost();
                // 移除常见的域名后缀
                host = host.replaceAll("(?i)\\.com|\\.cn|\\.org|\\.net|\\.ai", "");
                // 获取最后一个部分作为提供商名称
                String[] parts = host.split("\\.");
                String provider = parts[parts.length - 1];
                // 首字母大写
                return provider.substring(0, 1).toUpperCase() + provider.substring(1);
            } catch (Exception e) {
                return "OpenAI Compatible";
            }
        }
    }
} 
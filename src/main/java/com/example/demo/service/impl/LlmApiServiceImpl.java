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
     * 测试模型连通性（改进版本）
     * @param apiUrl API地址
     * @param apiKey API密钥
     * @param apiType API类型
     * @return 是否连接成功
     */
    public boolean testModelConnectivity(String apiUrl, String apiKey, String apiType) {
        logger.info("测试模型连通性: URL={}, 类型={}", apiUrl, apiType);
        
        // 确保API URL不为空
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            logger.error("API URL为空，无法测试连通性");
            return false;
        }
        
        // 规范化apiType，确保后续处理一致性
        String normalizedApiType = apiType;
        if (normalizedApiType == null) {
            normalizedApiType = "GENERIC";
        } else {
            normalizedApiType = normalizedApiType.toUpperCase();
        }
        
        try {
            // 尝试简单的连接测试 - 首先验证服务器是否可达
            boolean simpleConnectivityTest = testSimpleConnectivity(apiUrl);
            if (!simpleConnectivityTest) {
                logger.error("无法连接到服务器: {}", apiUrl);
                return false;
            }
            
            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 根据API类型设置不同的认证头
            if (apiKey != null && !apiKey.isEmpty()) {
                switch (normalizedApiType) {
                    case "AZURE_OPENAI":
                        logger.debug("使用Azure OpenAI认证方式");
                        headers.set("api-key", apiKey);
                        break;
                    case "ANTHROPIC":
                        logger.debug("使用Anthropic认证方式");
                        headers.set("x-api-key", apiKey);
                        break;
                    case "ZHIPU":
                    case "GLM":
                        logger.debug("使用智谱/GLM认证方式");
                        headers.set("Authorization", apiKey); // 智谱API可能直接使用token
                        break;
                    default:
                        logger.debug("使用标准Bearer认证方式");
                        headers.set("Authorization", "Bearer " + apiKey);
                        break;
                }
                
                if (!normalizedApiType.equals("ZHIPU") && !normalizedApiType.equals("GLM")) {
                    logger.debug("添加认证头: {} [部分隐藏]", 
                        apiKey.substring(0, Math.min(5, apiKey.length())) + "...");
                }
            }
            
            // 准备最小化请求体，适应不同API类型
            ObjectNode requestBody = objectMapper.createObjectNode();
            String endpointUrl = apiUrl;
            
            // 根据不同API类型准备测试请求
            switch (normalizedApiType) {
                case "OPENAI":
                    // OpenAI格式请求体
                    logger.debug("准备OpenAI格式测试请求");
                    ArrayNode messagesArray = requestBody.putArray("messages");
                    ObjectNode message = objectMapper.createObjectNode();
                    message.put("role", "user");
                    message.put("content", "Hello");
                    messagesArray.add(message);
                    requestBody.put("model", "gpt-3.5-turbo");
                    requestBody.put("max_tokens", 5);
                    
                    // 检查并修复API端点
                    if (!apiUrl.contains("/chat/completions")) {
                        if (apiUrl.contains("/v1")) {
                            endpointUrl = apiUrl.endsWith("/v1") ? 
                                apiUrl + "/chat/completions" : 
                                apiUrl + (apiUrl.endsWith("/") ? "v1/chat/completions" : "/v1/chat/completions");
                        } else {
                            endpointUrl = apiUrl.endsWith("/") ? 
                                apiUrl + "v1/chat/completions" : 
                                apiUrl + "/v1/chat/completions";
                        }
                    }
                    break;
                    
                case "OPENAI_COMPATIBLE":
                    // OpenAI兼容格式请求体
                    logger.debug("准备OpenAI兼容格式测试请求");
                    
                    // 针对特定域名设置特殊处理
                    if (apiUrl.contains("littlewheat.com")) {
                        logger.info("检测到littlewheat.com API，使用特定格式");
                        
                        // 使用API探测功能自动发现正确的端点
                        endpointUrl = probeApiEndpoint(apiUrl, apiKey);
                        logger.info("探测结果端点URL: {}", endpointUrl);
                        
                        // 使用标准OpenAI格式请求体
                        messagesArray = requestBody.putArray("messages");
                        message = objectMapper.createObjectNode();
                        message.put("role", "user");
                        message.put("content", "Hello");
                        messagesArray.add(message);
                        requestBody.put("model", "gpt-3.5-turbo");
                        requestBody.put("max_tokens", 5);
                    } else {
                        // 通用OpenAI兼容服务处理
                        messagesArray = requestBody.putArray("messages");
                        message = objectMapper.createObjectNode();
                        message.put("role", "user");
                        message.put("content", "Hello");
                        messagesArray.add(message);
                        requestBody.put("model", "gpt-3.5-turbo");
                        requestBody.put("max_tokens", 5);
                        
                        // 检查并修复API端点 - 兼容服务通常直接使用/v1/chat/completions
                        if (!apiUrl.contains("/chat/completions")) {
                            if (apiUrl.contains("/v1")) {
                                endpointUrl = apiUrl.endsWith("/v1") ? 
                                    apiUrl + "/chat/completions" : 
                                    apiUrl + (apiUrl.endsWith("/") ? "v1/chat/completions" : "/v1/chat/completions");
                            } else {
                                endpointUrl = apiUrl.endsWith("/") ? 
                                    apiUrl + "v1/chat/completions" : 
                                    apiUrl + "/v1/chat/completions";
                            }
                        }
                    }
                    break;
                    
                case "AZURE_OPENAI":
                    // Azure OpenAI格式请求体
                    logger.debug("准备Azure OpenAI格式测试请求");
                    messagesArray = requestBody.putArray("messages");
                    message = objectMapper.createObjectNode();
                    message.put("role", "user");
                    message.put("content", "Hello");
                    messagesArray.add(message);
                    requestBody.put("max_tokens", 5);
                    
                    // Azure OpenAI通常需要特定的部署名称在URL中
                    if (!apiUrl.contains("/deployments/")) {
                        logger.warn("Azure OpenAI URL可能不完整，可能需要包含部署名称");
                    }
                    break;
                    
                case "ANTHROPIC":
                    // Anthropic Claude格式请求体
                    logger.debug("准备Anthropic格式测试请求");
                    requestBody.put("prompt", "Human: Hello\nAssistant:");
                    requestBody.put("model", "claude-instant-1");
                    requestBody.put("max_tokens_to_sample", 5);
                    
                    // 检查并修复API端点
                    if (!apiUrl.contains("/complete")) {
                        if (apiUrl.contains("/v1")) {
                            endpointUrl = apiUrl.endsWith("/v1") ? 
                                apiUrl + "/complete" : 
                                apiUrl + (apiUrl.endsWith("/") ? "v1/complete" : "/v1/complete");
                        } else {
                            endpointUrl = apiUrl.endsWith("/") ? 
                                apiUrl + "v1/complete" : 
                                apiUrl + "/v1/complete";
                        }
                    }
                    break;
                    
                case "GOOGLE":
                case "GEMINI":
                    // Google/Gemini格式请求体
                    logger.debug("准备Google/Gemini格式测试请求");
                    ArrayNode partsArray = objectMapper.createArrayNode();
                    ObjectNode part = objectMapper.createObjectNode();
                    part.put("text", "Hello");
                    partsArray.add(part);
                    
                    ObjectNode contentObject = objectMapper.createObjectNode();
                    contentObject.set("parts", partsArray);
                    
                    ArrayNode contentsArray = objectMapper.createArrayNode();
                    contentsArray.add(contentObject);
                    requestBody.set("contents", contentsArray);
                    
                    // 检查并修复API端点
                    if (!apiUrl.contains("/generateContent")) {
                        if (apiUrl.contains("/v1")) {
                            if (apiUrl.contains("/models")) {
                                // 已经包含模型信息的URL
                                if (!apiUrl.endsWith(":generateContent")) {
                                    endpointUrl = apiUrl + ":generateContent";
                                }
                            } else {
                                // 没有包含模型信息的URL
                                endpointUrl = apiUrl.endsWith("/v1") || apiUrl.endsWith("/v1/") ? 
                                    apiUrl + "models/gemini-pro:generateContent" : 
                                    apiUrl + "/models/gemini-pro:generateContent";
                            }
                        } else {
                            endpointUrl = apiUrl.endsWith("/") ? 
                                apiUrl + "v1/models/gemini-pro:generateContent" : 
                                apiUrl + "/v1/models/gemini-pro:generateContent";
                        }
                    }
                    break;
                    
                case "ZHIPU":
                case "GLM":
                    // 智谱格式请求体
                    logger.debug("准备智谱/GLM格式测试请求");
                    // ChatGLM支持多种接口，尝试使用通用的聊天接口
                    requestBody.put("prompt", "Hello");
                    requestBody.put("temperature", 0.7);
                    requestBody.put("top_p", 0.7);
                    requestBody.put("max_tokens", 5);
                    
                    // 智谱API接口检查
                    if (!apiUrl.contains("/chat") && !apiUrl.contains("/generate")) {
                        endpointUrl = apiUrl.endsWith("/") ? 
                            apiUrl + "chat" : 
                            apiUrl + "/chat";
                    }
                    break;
                    
                default:
                    // 通用格式，发送最少的请求体
                    logger.debug("准备通用格式测试请求: {}", normalizedApiType);
                    requestBody.put("prompt", "Hello");
                    requestBody.put("max_tokens", 5);
                    break;
            }
            
            // 创建HTTP实体
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
            
            // 记录详细的请求信息，帮助调试
            logger.info("发送测试POST请求到 {}", endpointUrl);
            logger.debug("请求头: {}", headers);
            logger.debug("请求体: {}", requestBody.toString());
                
            try {
                // 发送POST请求
                ResponseEntity<String> response = restTemplate.postForEntity(
                        endpointUrl, requestEntity, String.class);
                
                int statusCode = response.getStatusCodeValue();
                String responseBody = response.getBody();
                
                // 放宽成功条件: 2xx成功，401/403表示认证问题但API可达
                boolean apiReachable = response.getStatusCode().is2xxSuccessful() || 
                                      statusCode == 401 || statusCode == 403;
                boolean apiUsable = response.getStatusCode().is2xxSuccessful();
                
                logger.info("模型API连通性测试结果 - 端点可达: {}, API可用: {}, 状态码: {}", 
                        apiReachable, apiUsable, statusCode);
                
                if (apiReachable) {
                    if (!apiUsable) {
                        logger.warn("API端点可达但认证失败，状态码: {}，这通常表示API密钥有问题", statusCode);
                    }
                    // 考虑端点可达就算成功，认证问题由用户检查API密钥解决
                    return true;
                } else {
                    logger.error("API端点请求失败，状态码: {}", statusCode);
                    return false;
                }
            } catch (Exception e) {
                logger.error("发送API请求失败: {}", e.getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.error("模型连通性测试过程中出现异常: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 简单连接测试 - 检查服务器是否可达
     * @param apiUrl API地址
     * @return 是否连接成功
     */
    private boolean testSimpleConnectivity(String apiUrl) {
        try {
            // 提取主机部分
            String baseUrl = apiUrl;
            if (baseUrl.contains("://")) {
                baseUrl = baseUrl.substring(baseUrl.indexOf("://") + 3);
            }
            if (baseUrl.contains("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.indexOf("/"));
            }
            
            // 如果URL包含端口，则提取域名部分
            String host = baseUrl;
            int port = 443; // 默认HTTPS端口
            if (baseUrl.contains(":")) {
                host = baseUrl.substring(0, baseUrl.indexOf(":"));
                try {
                    port = Integer.parseInt(baseUrl.substring(baseUrl.indexOf(":") + 1));
                } catch (NumberFormatException e) {
                    // 忽略解析错误，使用默认端口
                }
            }
            
            logger.debug("尝试连接到主机: {}:{}", host, port);
            
            // 尝试简单的Socket连接
            try (java.net.Socket socket = new java.net.Socket()) {
                // 设置3秒连接超时
                socket.connect(new java.net.InetSocketAddress(host, port), 3000);
                logger.debug("成功连接到主机 {}:{}", host, port);
                return true;
            }
        } catch (Exception e) {
            logger.error("连接服务器失败: {}", e.getMessage());
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

    /**
     * 探测正确的API端点路径
     * @param baseUrl 基础API URL
     * @param apiKey API密钥
     * @return 找到的有效端点路径，如果没找到返回原始URL
     */
    private String probeApiEndpoint(String baseUrl, String apiKey) {
        logger.info("开始探测API端点: {}", baseUrl);
        
        // 常见的API路径组合
        String[] commonPaths = {
            "/v1/chat/completions",
            "/chat/completions", 
            "/v1/completions",
            "/completions",
            "/v1/generate",
            "/generate",
            "/api/chat",
            "/api/generate"
        };
        
        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("Authorization", "Bearer " + apiKey);
        }
        
        // 尝试所有常见路径
        for (String path : commonPaths) {
            String testUrl = baseUrl;
            
            // 确保URL拼接正确
            if (baseUrl.endsWith("/")) {
                testUrl = baseUrl + path.substring(1);
            } else {
                testUrl = baseUrl + path;
            }
            
            try {
                logger.debug("探测API端点: {}", testUrl);
                
                // 创建最小请求体
                ObjectNode requestBody = objectMapper.createObjectNode();
                ArrayNode messagesNode = requestBody.putArray("messages");
                ObjectNode messageObject = objectMapper.createObjectNode();
                messageObject.put("role", "user");
                messageObject.put("content", "test");
                messagesNode.add(messageObject);
                requestBody.put("max_tokens", 1);
                requestBody.put("model", "gpt-3.5-turbo");
                
                // 创建HTTP实体
                HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
                
                // 发送请求
                ResponseEntity<String> response = restTemplate.postForEntity(testUrl, requestEntity, String.class);
                
                // 检查响应
                if (response.getStatusCode().is2xxSuccessful() || response.getStatusCodeValue() == 401) {
                    logger.info("找到可能的API端点: {}, 状态码: {}", testUrl, response.getStatusCodeValue());
                    return testUrl;
                }
            } catch (Exception e) {
                // 忽略错误，继续尝试下一个路径
                logger.debug("探测路径 {} 失败: {}", testUrl, e.getMessage());
            }
        }
        
        // 如果没有找到有效端点，返回原始URL
        logger.warn("未找到有效的API端点，使用原始URL: {}", baseUrl);
        return baseUrl;
    }
} 
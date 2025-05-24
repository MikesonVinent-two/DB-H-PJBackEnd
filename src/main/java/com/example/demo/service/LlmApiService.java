package com.example.demo.service;

import java.util.Map;

/**
 * LLM API服务接口
 */
public interface LlmApiService {
    
    /**
     * 调用LLM API生成回答
     * 
     * @param apiUrl API URL
     * @param apiKey API密钥
     * @param prompt 提示词
     * @param parameters 参数
     * @return 生成的回答文本
     */
    String generateAnswer(String apiUrl, String apiKey, String prompt, Map<String, Object> parameters);
    
    /**
     * 调用LLM API生成回答（带API类型）
     * 
     * @param apiUrl API URL
     * @param apiKey API密钥
     * @param apiType API类型(如"openai", "azure", "anthropic", "google"等)
     * @param prompt 提示词
     * @param parameters 参数
     * @return 生成的回答文本
     */
    String generateAnswer(String apiUrl, String apiKey, String apiType, String prompt, Map<String, Object> parameters);
} 
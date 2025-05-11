package com.example.demo.service;

import com.example.demo.dto.LlmRequestDTO;
import com.example.demo.dto.LlmResponseDTO;

public interface LlmService {
    
    /**
     * 发送请求到LLM API并获取回答
     * 
     * @param request LLM请求参数
     * @return LLM响应结果
     */
    LlmResponseDTO sendRequest(LlmRequestDTO request);
} 
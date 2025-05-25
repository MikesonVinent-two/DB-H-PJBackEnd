package com.example.demo.service;

import com.example.demo.dto.LLMModelDTO;
import com.example.demo.dto.LLMModelRegistrationRequest;
import com.example.demo.dto.LLMModelRegistrationResponse;

import java.util.List;

public interface LLMModelService {
    /**
     * 注册LLM模型
     * 1. 验证用户提供的API认证信息
     * 2. 调用API获取可用模型列表
     * 3. 将模型信息保存到数据库
     */
    LLMModelRegistrationResponse registerModels(LLMModelRegistrationRequest request);
    
    /**
     * 获取所有已注册的LLM模型
     * @return 已注册模型列表
     */
    List<LLMModelDTO> getAllModels();
} 
package com.example.demo.service.impl;

import com.example.demo.dto.LLMModelDTO;
import com.example.demo.dto.LLMModelRegistrationRequest;
import com.example.demo.dto.LLMModelRegistrationResponse;
import com.example.demo.entity.LLMModel;
import com.example.demo.repository.LLMModelRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.LLMModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LLMModelServiceImpl implements LLMModelService {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMModelServiceImpl.class);
    
    @Autowired
    private LLMModelRepository llmModelRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RestTemplate restTemplate;

    @Override
    @Transactional
    public LLMModelRegistrationResponse registerModels(LLMModelRegistrationRequest request) {
        try {
            // 1. 验证用户是否存在
            if (!userRepository.existsById(request.getUserId())) {
                return LLMModelRegistrationResponse.error("用户不存在", null);
            }

            // 2. 调用API获取可用模型列表
            List<Map<String, Object>> availableModels = fetchAvailableModels(request.getApiUrl(), request.getApiKey());
            if (availableModels == null || availableModels.isEmpty()) {
                return LLMModelRegistrationResponse.error("无法获取可用模型列表", "API调用未返回有效数据");
            }

            // 3. 保存模型信息到数据库
            List<LLMModelDTO> registeredModels = new ArrayList<>();
            for (Map<String, Object> modelInfo : availableModels) {
                LLMModel model = new LLMModel();
                model.setName((String) modelInfo.get("id")); // 假设API返回的模型ID作为名称
                model.setProvider(extractProviderFromModelId((String) modelInfo.get("id")));
                model.setApiUrl(request.getApiUrl());
                model.setApiKey(request.getApiKey());
                model.setCreatedByUserId(request.getUserId());
                
                // 如果API返回了其他信息，也可以设置
                if (modelInfo.get("description") != null) {
                    model.setDescription((String) modelInfo.get("description"));
                }
                if (modelInfo.get("version") != null) {
                    model.setVersion((String) modelInfo.get("version"));
                }

                // 保存到数据库
                model = llmModelRepository.save(model);
                
                // 转换为DTO并添加到结果列表
                registeredModels.add(convertToDTO(model));
            }

            return LLMModelRegistrationResponse.success(registeredModels);

        } catch (Exception e) {
            logger.error("注册模型时发生错误", e);
            return LLMModelRegistrationResponse.error("注册模型失败", e.getMessage());
        }
    }

    private List<Map<String, Object>> fetchAvailableModels(String apiUrl, String apiKey) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 创建请求实体
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/v1/models",  // 假设这是获取模型列表的端点
                HttpMethod.GET,
                entity,
                Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("data")) {
                return (List<Map<String, Object>>) response.getBody().get("data");
            }

            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("调用API获取模型列表时发生错误", e);
            throw e;
        }
    }

    private String extractProviderFromModelId(String modelId) {
        // 根据模型ID推断提供商
        if (modelId.startsWith("gpt-")) {
            return "OpenAI";
        } else if (modelId.startsWith("claude-")) {
            return "Anthropic";
        } else {
            return "Unknown";
        }
    }

    private LLMModelDTO convertToDTO(LLMModel model) {
        LLMModelDTO dto = new LLMModelDTO();
        dto.setId(model.getId());
        dto.setName(model.getName());
        dto.setProvider(model.getProvider());
        dto.setVersion(model.getVersion());
        dto.setDescription(model.getDescription());
        return dto;
    }
} 
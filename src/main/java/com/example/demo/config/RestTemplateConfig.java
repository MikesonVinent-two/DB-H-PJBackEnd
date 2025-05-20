package com.example.demo.config;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }
    
    /**
     * 根据模型类型获取具有适当超时设置的RestTemplate
     * 
     * @param builder RestTemplateBuilder实例
     * @param model 模型名称
     * @return 配置了适当超时的RestTemplate实例
     */
    public RestTemplate getModelSpecificRestTemplate(RestTemplateBuilder builder, String model) {
        // 默认超时设置
        int readTimeoutSeconds = 60;
        
        // 根据模型类型设置不同的超时时间
        if (model != null) {
            String modelLower = model.toLowerCase();
            
            // deepsearch和deepresearch类型的模型超时设置为5分钟
            if (modelLower.contains("deepsearch") || modelLower.contains("deepresearch")) {
                readTimeoutSeconds = 300; // 5分钟
            } 
            // 思考类型的模型超时设置为3分钟
            else if (modelLower.contains("think") || modelLower.contains("thought") || 
                     modelLower.contains("reasoning") || modelLower.contains("思考")) {
                readTimeoutSeconds = 180; // 3分钟
            }
        }
        
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .build();
    }
} 
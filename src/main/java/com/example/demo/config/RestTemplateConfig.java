package com.example.demo.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate配置类
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 设置连接超时时间（30秒）
        factory.setConnectTimeout(30000);
        // 设置读取超时时间（120秒，大型模型可能需要较长响应时间）
        factory.setReadTimeout(120000);
            
        // 使用BufferingClientHttpRequestFactory以允许多次读取响应
        BufferingClientHttpRequestFactory bufferingFactory = new BufferingClientHttpRequestFactory(factory);
        
        return new RestTemplate(bufferingFactory);
    }
    
    /**
     * 根据模型类型获取具有特定超时设置的RestTemplate实例
     * 
     * @param builder RestTemplateBuilder
     * @param modelName 模型名称
     * @return 自定义的RestTemplate实例
     */
    public RestTemplate getModelSpecificRestTemplate(RestTemplateBuilder builder, String modelName) {
        // 根据模型名称设置不同的超时时间
        int readTimeoutSeconds = 120; // 默认2分钟
        
        // 大型模型需要更长的超时时间
        if (modelName != null) {
            if (modelName.contains("gpt-4")) {
                readTimeoutSeconds = 300; // GPT-4模型设置5分钟超时
            } else if (modelName.contains("claude")) {
                readTimeoutSeconds = 240; // Claude模型设置4分钟超时
            } else if (modelName.contains("gpt-3.5")) {
                readTimeoutSeconds = 180; // GPT-3.5模型设置3分钟超时
            }
        }
        
        // 创建并配置RestTemplate
        return builder
                .setConnectTimeout(Duration.ofSeconds(30)) // 连接超时30秒
                .setReadTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .build();
    }
} 
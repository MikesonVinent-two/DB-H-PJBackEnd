package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.example.demo.entity.QuestionType;
import com.example.demo.entity.DifficultyLevel;
import com.example.demo.entity.EntityType;
import com.example.demo.entity.ChangeType;
import com.example.demo.exception.EnumDeserializationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

import java.io.IOException;

/**
 * Jackson配置类，用于处理枚举的大小写敏感问题及Hibernate延迟加载
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        
        // 注册Hibernate6Module，处理延迟加载的实体
        Hibernate6Module hibernate6Module = new Hibernate6Module();
        // 配置不要序列化延迟加载的关联
        hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
        hibernate6Module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
        objectMapper.registerModule(hibernate6Module);
        
        // 创建自定义模块处理特殊枚举
        SimpleModule module = new SimpleModule();
        
        // 为QuestionType添加自定义反序列化器
        module.addDeserializer(QuestionType.class, new StdScalarDeserializer<QuestionType>(QuestionType.class) {
            @Override
            public QuestionType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                try {
                    return QuestionType.fromString(value);
                } catch (EnumDeserializationException e) {
                    // 直接抛出异常，由全局异常处理器处理
                    throw e;
                }
            }
        });
        
        // 为DifficultyLevel添加自定义反序列化器
        module.addDeserializer(DifficultyLevel.class, new StdScalarDeserializer<DifficultyLevel>(DifficultyLevel.class) {
            @Override
            public DifficultyLevel deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                try {
                    return DifficultyLevel.fromString(value);
                } catch (EnumDeserializationException e) {
                    // 直接抛出异常，由全局异常处理器处理
                    throw e;
                }
            }
        });
        
        // 为ChangeType添加自定义反序列化器
        module.addDeserializer(ChangeType.class, new StdScalarDeserializer<ChangeType>(ChangeType.class) {
            @Override
            public ChangeType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                try {
                    return ChangeType.fromString(value);
                } catch (EnumDeserializationException e) {
                    // 直接抛出异常，由全局异常处理器处理
                    throw e;
                }
            }
        });
        
        objectMapper.registerModule(module);
        return objectMapper;
    }
} 
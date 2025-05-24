package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.AnswerPromptAssemblyConfig;

import java.util.List;

/**
 * 回答场景的prompt组装配置仓库接口
 */
@Repository
public interface AnswerPromptAssemblyConfigRepository extends JpaRepository<AnswerPromptAssemblyConfig, Long> {
    
    /**
     * 查找所有激活状态的配置
     * 
     * @return 激活状态的配置列表
     */
    List<AnswerPromptAssemblyConfig> findByIsActiveTrue();
    
    /**
     * 根据名称查找配置
     * 
     * @param name 配置名称
     * @return 匹配的配置列表
     */
    List<AnswerPromptAssemblyConfig> findByName(String name);
    
    /**
     * 根据创建者ID查找配置
     * 
     * @param userId 用户ID
     * @return 该用户创建的配置列表
     */
    List<AnswerPromptAssemblyConfig> findByCreatedByUserId(Long userId);
} 
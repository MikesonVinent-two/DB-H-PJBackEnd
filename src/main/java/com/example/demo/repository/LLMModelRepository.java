package com.example.demo.repository;

import com.example.demo.entity.LlmModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * LLM模型仓库接口
 */
@Repository
public interface LlmModelRepository extends JpaRepository<LlmModel, Long> {
    
    /**
     * 根据模型名称查找
     * 
     * @param name 模型名称
     * @return 匹配的模型列表
     */
    List<LlmModel> findByName(String name);
    
    /**
     * 根据提供商查找
     * 
     * @param provider 提供商名称
     * @return 匹配的模型列表
     */
    List<LlmModel> findByProvider(String provider);
    
    /**
     * 查找所有未删除的模型
     * 
     * @return 未删除的模型列表
     */
    List<LlmModel> findByDeletedAtIsNull();
    
    /**
     * 根据提供商和版本查找
     * 
     * @param provider 提供商名称
     * @param version 版本号
     * @return 匹配的模型列表
     */
    List<LlmModel> findByProviderAndVersion(String provider, String version);
    
    /**
     * 检查指定名称和API URL的模型是否存在
     * 
     * @param name 模型名称
     * @param apiUrl API URL
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByNameAndApiUrl(String name, String apiUrl);
} 
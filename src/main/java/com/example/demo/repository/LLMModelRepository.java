package com.example.demo.repository;

import com.example.demo.entity.LlmModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface LLMModelRepository extends JpaRepository<LlmModel, Long> {
    // 基础的CRUD操作由JpaRepository提供
} 
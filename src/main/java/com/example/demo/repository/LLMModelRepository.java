package com.example.demo.repository;

import com.example.demo.entity.LLMModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface LLMModelRepository extends JpaRepository<LLMModel, Long> {
    // 基础的CRUD操作由JpaRepository提供
} 
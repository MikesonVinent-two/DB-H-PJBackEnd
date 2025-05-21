package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.ChangeLogDetail;

@Repository
public interface ChangeLogDetailRepository extends JpaRepository<ChangeLogDetail, Long> {
    // 可以根据需要添加自定义查询方法
} 
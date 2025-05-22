package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.RawAnswer;

@Repository
public interface RawAnswerRepository extends JpaRepository<RawAnswer, Long> {
    // 根据原始问题ID查询所有回答
    List<RawAnswer> findByRawQuestionId(Long rawQuestionId);
} 
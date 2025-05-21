package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.RawQuestion;

@Repository
public interface RawQuestionRepository extends JpaRepository<RawQuestion, Long> {
    boolean existsBySourceUrl(String sourceUrl);
} 
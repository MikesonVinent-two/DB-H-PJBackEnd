package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.example.demo.entity.StandardSubjectiveAnswer;

@Repository
public interface StandardSubjectiveAnswerRepository extends JpaRepository<StandardSubjectiveAnswer, Long> {
    StandardSubjectiveAnswer findByStandardQuestionIdAndDeletedAtIsNull(Long standardQuestionId);
    
    /**
     * 根据标准问题ID查找主观题答案
     * 
     * @param standardQuestionId 标准问题ID
     * @return 主观题答案
     */
    Optional<StandardSubjectiveAnswer> findByStandardQuestionId(Long standardQuestionId);
} 
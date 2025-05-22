package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.example.demo.entity.StandardQuestion;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface StandardQuestionRepository extends JpaRepository<StandardQuestion, Long> {
    // 获取所有已标准化的原始问题ID
    @Query("SELECT DISTINCT sq.originalRawQuestion.id FROM StandardQuestion sq WHERE sq.originalRawQuestion IS NOT NULL")
    List<Long> findDistinctOriginalRawQuestionIds();
    
    // 根据原始问题ID查找最新的标准问题
    Optional<StandardQuestion> findFirstByOriginalRawQuestionIdOrderByCreationTimeDesc(Long rawQuestionId);

    List<StandardQuestion> findByParentStandardQuestionId(Long parentId);
    
    // 根据原始问题ID查找所有关联的标准问题
    List<StandardQuestion> findByOriginalRawQuestionId(Long rawQuestionId);
} 
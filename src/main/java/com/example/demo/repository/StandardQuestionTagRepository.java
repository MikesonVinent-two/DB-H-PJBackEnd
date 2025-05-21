package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.StandardQuestion;
import com.example.demo.entity.StandardQuestionTag;
import com.example.demo.entity.Tag;

@Repository
public interface StandardQuestionTagRepository extends JpaRepository<StandardQuestionTag, Long> {
    List<StandardQuestionTag> findByStandardQuestion(StandardQuestion standardQuestion);
    List<StandardQuestionTag> findByTag(Tag tag);
    void deleteByStandardQuestionAndTag(StandardQuestion standardQuestion, Tag tag);
    boolean existsByStandardQuestionAndTag(StandardQuestion standardQuestion, Tag tag);
} 
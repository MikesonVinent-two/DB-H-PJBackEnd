package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.RawQuestion;
import com.example.demo.entity.RawQuestionTag;
import com.example.demo.entity.Tag;

@Repository
public interface RawQuestionTagRepository extends JpaRepository<RawQuestionTag, Long> {
    List<RawQuestionTag> findByRawQuestion(RawQuestion rawQuestion);
    List<RawQuestionTag> findByTag(Tag tag);
    void deleteByRawQuestionAndTag(RawQuestion rawQuestion, Tag tag);
    boolean existsByRawQuestionAndTag(RawQuestion rawQuestion, Tag tag);
    List<RawQuestionTag> findByRawQuestionId(Long rawQuestionId);
} 
package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.RawQuestion;
import java.util.List;

@Repository
public interface RawQuestionRepository extends JpaRepository<RawQuestion, Long> {
    boolean existsBySourceUrl(String sourceUrl);
    
    // 根据ID列表查询并按ID降序排序
    Page<RawQuestion> findByIdInOrderByIdDesc(List<Long> ids, Pageable pageable);
    
    // 查询不在ID列表中的记录并按ID降序排序
    Page<RawQuestion> findByIdNotInOrderByIdDesc(List<Long> ids, Pageable pageable);
    
    // 根据来源网站模糊查询
    Page<RawQuestion> findBySourceSiteContainingIgnoreCase(String sourceSite, Pageable pageable);
    
    // 根据标题或内容模糊查询
    Page<RawQuestion> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            String titleKeyword, String contentKeyword, Pageable pageable);
            
    // 根据多个标签查询问题
    @Query("SELECT DISTINCT rq FROM RawQuestion rq " +
           "JOIN rq.questionTags qt " +
           "JOIN qt.tag t " +
           "WHERE t.tagName IN :tagNames " +
           "GROUP BY rq " +
           "HAVING COUNT(DISTINCT t.tagName) = :tagCount")
    Page<RawQuestion> findByTagNames(@Param("tagNames") List<String> tagNames, 
                                    @Param("tagCount") Long tagCount,
                                    Pageable pageable);
} 
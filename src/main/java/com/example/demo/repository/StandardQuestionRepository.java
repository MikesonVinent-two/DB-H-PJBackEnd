package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.demo.entity.StandardQuestion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    // 根据数据集版本ID查找标准问题
    @Query("SELECT sq FROM StandardQuestion sq JOIN DatasetQuestionMapping dqm ON sq.id = dqm.standardQuestion.id WHERE dqm.datasetVersion.id = :datasetVersionId")
    List<StandardQuestion> findByDatasetVersionId(@Param("datasetVersionId") Long datasetVersionId);
    
    // 预加载标签的数据集版本问题查询
    @Query("SELECT DISTINCT sq FROM StandardQuestion sq JOIN FETCH sq.questionTags qt JOIN FETCH qt.tag JOIN DatasetQuestionMapping dqm ON sq.id = dqm.standardQuestion.id WHERE dqm.datasetVersion.id = :datasetVersionId")
    List<StandardQuestion> findByDatasetVersionIdWithTags(@Param("datasetVersionId") Long datasetVersionId);
    
    // 预加载数据集映射的查询
    @Query("SELECT DISTINCT sq FROM StandardQuestion sq JOIN FETCH sq.datasetMappings dm WHERE sq.id IN :questionIds")
    List<StandardQuestion> findByIdsWithDatasetMappings(@Param("questionIds") List<Long> questionIds);
    
    // 根据问题文本内容查找标准问题
    List<StandardQuestion> findByQuestionTextContaining(String questionText);
    
    // 查找所有最新版本的标准问题（没有子问题的问题，即版本树的叶子节点）
    @Query("SELECT sq FROM StandardQuestion sq WHERE NOT EXISTS (SELECT 1 FROM StandardQuestion child WHERE child.parentStandardQuestion.id = sq.id)")
    Page<StandardQuestion> findLatestVersions(Pageable pageable);
} 
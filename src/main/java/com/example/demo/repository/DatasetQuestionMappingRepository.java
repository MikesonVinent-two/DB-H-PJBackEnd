package com.example.demo.repository;

import com.example.demo.entity.DatasetQuestionMapping;
import com.example.demo.entity.DatasetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetQuestionMappingRepository extends JpaRepository<DatasetQuestionMapping, Long> {
    
    // 根据数据集版本查找所有问题映射
    List<DatasetQuestionMapping> findByDatasetVersionOrderByOrderInDataset(DatasetVersion datasetVersion);
    
    // 根据数据集版本ID查找所有问题映射
    @Query("SELECT dqm FROM DatasetQuestionMapping dqm WHERE dqm.datasetVersion.id = :datasetVersionId ORDER BY dqm.orderInDataset")
    List<DatasetQuestionMapping> findByDatasetVersionId(Long datasetVersionId);
    
    // 检查某个标准问题是否已经在指定数据集版本中
    boolean existsByDatasetVersionIdAndStandardQuestionId(Long datasetVersionId, Long standardQuestionId);
    
    // 获取数据集版本中的问题数量
    long countByDatasetVersionId(Long datasetVersionId);
    
    // 获取数据集版本中的最大顺序号
    @Query("SELECT MAX(dqm.orderInDataset) FROM DatasetQuestionMapping dqm WHERE dqm.datasetVersion.id = :datasetVersionId")
    Integer findMaxOrderInDataset(Long datasetVersionId);
} 
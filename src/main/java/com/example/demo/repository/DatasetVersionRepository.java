package com.example.demo.repository;

import com.example.demo.entity.DatasetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetVersionRepository extends JpaRepository<DatasetVersion, Long> {
    
    // 根据版本号查找数据集版本
    Optional<DatasetVersion> findByVersionNumber(String versionNumber);
    
    // 查找未被删除的数据集版本，并按创建时间降序排序
    @Query("SELECT dv FROM DatasetVersion dv WHERE dv.deletedAt IS NULL ORDER BY dv.creationTime DESC")
    List<DatasetVersion> findAllActiveVersions();
    
    // 检查版本号是否已存在
    boolean existsByVersionNumber(String versionNumber);
    
    // 根据名称模糊查询数据集版本
    @Query("SELECT dv FROM DatasetVersion dv WHERE dv.deletedAt IS NULL AND dv.name LIKE %:name% ORDER BY dv.creationTime DESC")
    List<DatasetVersion> findByNameContaining(String name);
} 
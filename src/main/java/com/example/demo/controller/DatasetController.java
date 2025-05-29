package com.example.demo.controller;

import com.example.demo.dto.CreateDatasetVersionRequest;
import com.example.demo.dto.DatasetQuestionMappingDTO;
import com.example.demo.dto.DatasetVersionDTO;
import com.example.demo.dto.UpdateDatasetVersionRequest;
import com.example.demo.dto.DeleteDatasetVersionRequest;
import com.example.demo.dto.CloneDatasetVersionRequest;
import com.example.demo.entity.jdbc.DatasetQuestionMapping;
import com.example.demo.repository.jdbc.DatasetQuestionMappingRepository;
import com.example.demo.service.DatasetVersionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/datasets")
public class DatasetController {

    @Autowired
    private DatasetVersionService datasetVersionService;
    
    @Autowired
    private DatasetQuestionMappingRepository mappingRepository;
    
    /**
     * 创建新的数据集版本
     */
    @PostMapping("/versions")
    public ResponseEntity<DatasetVersionDTO> createDatasetVersion(
            @Valid @RequestBody CreateDatasetVersionRequest request) {
        
        DatasetVersionDTO createdVersion = datasetVersionService.createDatasetVersion(request, request.getUserId());
        return new ResponseEntity<>(createdVersion, HttpStatus.CREATED);
    }
    
    /**
     * 获取数据集版本列表
     */
    @GetMapping("/versions")
    public ResponseEntity<List<DatasetVersionDTO>> getAllDatasetVersions(
            @RequestParam(required = false) String name) {
        
        List<DatasetVersionDTO> versions = datasetVersionService.getAllDatasetVersions(name);
        return ResponseEntity.ok(versions);
    }
    
    /**
     * 获取指定数据集版本详情
     */
    @GetMapping("/versions/{id}")
    public ResponseEntity<DatasetVersionDTO> getDatasetVersionById(@PathVariable Long id) {
        DatasetVersionDTO version = datasetVersionService.getDatasetVersionById(id);
        return ResponseEntity.ok(version);
    }
    
    /**
     * 更新数据集版本信息
     */
    @PutMapping("/versions/{id}")
    public ResponseEntity<DatasetVersionDTO> updateDatasetVersion(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDatasetVersionRequest request) {
        
        DatasetVersionDTO updatedVersion = datasetVersionService.updateDatasetVersion(id, request, request.getUserId());
        return ResponseEntity.ok(updatedVersion);
    }
    
    /**
     * 删除数据集版本
     */
    @DeleteMapping("/versions/{id}")
    public ResponseEntity<Void> deleteDatasetVersion(
            @PathVariable Long id,
            @Valid @RequestBody DeleteDatasetVersionRequest request) {
        
        datasetVersionService.deleteDatasetVersion(id, request.getUserId());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 克隆数据集版本
     */
    @PostMapping("/versions/{id}/clone")
    public ResponseEntity<DatasetVersionDTO> cloneDatasetVersion(
            @PathVariable Long id,
            @Valid @RequestBody CloneDatasetVersionRequest request) {
        
        DatasetVersionDTO clonedVersion = 
                datasetVersionService.cloneDatasetVersion(id, request.getNewVersionNumber(), 
                        request.getNewName(), request.getDescription(), request.getUserId());
        return new ResponseEntity<>(clonedVersion, HttpStatus.CREATED);
    }
    
    /**
     * 获取数据集版本中的问题列表
     */
    @GetMapping("/versions/{id}/questions")
    public ResponseEntity<List<DatasetQuestionMappingDTO>> getQuestionsInDataset(@PathVariable Long id) {
        List<DatasetQuestionMapping> mappings = mappingRepository.findByDatasetVersionId(id);
        
        List<DatasetQuestionMappingDTO> dtos = mappings.stream()
                .map(this::convertMappingToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * 转换映射实体为DTO
     */
    private DatasetQuestionMappingDTO convertMappingToDTO(DatasetQuestionMapping mapping) {
        return new DatasetQuestionMappingDTO(
                mapping.getId(),
                mapping.getDatasetVersion().getId(),
                mapping.getDatasetVersion().getName(),
                mapping.getStandardQuestion().getId(),
                mapping.getStandardQuestion().getQuestionText(),
                mapping.getOrderInDataset()
        );
    }
} 
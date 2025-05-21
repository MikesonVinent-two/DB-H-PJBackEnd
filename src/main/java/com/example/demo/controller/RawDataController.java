package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.RawAnswerDTO;
import com.example.demo.dto.RawQuestionDTO;
import com.example.demo.dto.RawQuestionWithAnswersDTO;
import com.example.demo.dto.RawQuestionDisplayDTO;
import com.example.demo.entity.RawAnswer;
import com.example.demo.entity.RawQuestion;
import com.example.demo.service.RawDataService;

import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/raw-data")
@CrossOrigin(origins = "*")
public class RawDataController {
    
    private static final Logger logger = LoggerFactory.getLogger(RawDataController.class);
    private final RawDataService rawDataService;
    
    @Autowired
    public RawDataController(RawDataService rawDataService) {
        this.rawDataService = rawDataService;
    }
    
    @PostMapping("/questions")
    public ResponseEntity<RawQuestion> createQuestion(@Valid @RequestBody RawQuestion question) {
        RawQuestion savedQuestion = rawDataService.createQuestion(question);
        return ResponseEntity.ok(savedQuestion);
    }
    
    @PostMapping("/questions-dto")
    public ResponseEntity<RawQuestion> createQuestionFromDTO(@Valid @RequestBody RawQuestionDTO questionDTO) {
        RawQuestion savedQuestion = rawDataService.createQuestionFromDTO(questionDTO);
        return ResponseEntity.ok(savedQuestion);
    }
    
    @PostMapping("/answers")
    public ResponseEntity<RawAnswer> createAnswer(@Valid @RequestBody RawAnswerDTO answerDTO) {
        RawAnswer savedAnswer = rawDataService.createAnswer(answerDTO);
        return ResponseEntity.ok(savedAnswer);
    }
    
    @PostMapping("/questions-with-answers")
    public ResponseEntity<RawQuestion> createQuestionWithAnswers(@Valid @RequestBody RawQuestionWithAnswersDTO dto) {
        RawQuestion savedQuestion = rawDataService.createQuestionWithAnswers(dto);
        return ResponseEntity.ok(savedQuestion);
    }
    
    // 获取所有原始问题（分页）
    @GetMapping("/questions")
    public ResponseEntity<Page<RawQuestionDisplayDTO>> getAllRawQuestions(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(rawDataService.findAllRawQuestions(pageable));
    }
    
    // 根据标准化状态查询（分页）
    @GetMapping("/questions/by-status")
    public ResponseEntity<Page<RawQuestionDisplayDTO>> getRawQuestionsByStatus(
            @RequestParam(required = false, defaultValue = "false") boolean standardized,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(rawDataService.findRawQuestionsByStandardizedStatus(standardized, pageable));
    }
    
    // 根据来源网站查询（分页）
    @GetMapping("/questions/by-source")
    public ResponseEntity<Page<RawQuestionDisplayDTO>> getRawQuestionsBySource(
            @RequestParam String sourceSite,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(rawDataService.findRawQuestionsBySourceSite(sourceSite, pageable));
    }
    
    // 搜索原始问题（分页）
    @GetMapping("/questions/search")
    public ResponseEntity<Page<RawQuestionDisplayDTO>> searchRawQuestions(
            @RequestParam String keyword,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(rawDataService.searchRawQuestions(keyword, pageable));
    }
    
    // 添加测试数据
    @PostMapping("/questions/test-data")
    public ResponseEntity<String> addTestData() {
        try {
            // 创建测试问题1
            RawQuestionDTO question1 = new RawQuestionDTO();
            question1.setSourceUrl("http://example.com/q1");
            question1.setSourceSite("测试网站");
            question1.setTitle("测试问题1");
            question1.setContent("这是一个测试问题的内容");
            question1.setTags(Arrays.asList("测试", "示例"));
            rawDataService.createQuestionFromDTO(question1);
            
            // 创建测试问题2
            RawQuestionDTO question2 = new RawQuestionDTO();
            question2.setSourceUrl("http://example.com/q2");
            question2.setSourceSite("测试网站");
            question2.setTitle("测试问题2");
            question2.setContent("这是另一个测试问题的内容");
            question2.setTags(Arrays.asList("测试", "示例2"));
            rawDataService.createQuestionFromDTO(question2);
            
            return ResponseEntity.ok("测试数据添加成功");
        } catch (Exception e) {
            logger.error("添加测试数据失败", e);
            return ResponseEntity.badRequest().body("添加测试数据失败: " + e.getMessage());
        }
    }
    
    // 根据标签查询问题
    @GetMapping("/questions/by-tags")
    public ResponseEntity<Page<RawQuestionDisplayDTO>> getQuestionsByTags(
            @RequestParam List<String> tags,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        if (tags == null || tags.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(rawDataService.findQuestionsByTags(tags, pageable));
    }
} 
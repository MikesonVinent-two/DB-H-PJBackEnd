package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.RawAnswerDTO;
import com.example.demo.dto.RawQuestionDTO;
import com.example.demo.dto.RawQuestionWithAnswersDTO;
import com.example.demo.entity.RawAnswer;
import com.example.demo.entity.RawQuestion;
import com.example.demo.service.RawDataService;

import jakarta.validation.Valid;

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
} 
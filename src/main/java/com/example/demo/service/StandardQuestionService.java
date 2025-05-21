package com.example.demo.service;

import com.example.demo.dto.StandardQuestionDTO;
import com.example.demo.entity.StandardQuestion;

public interface StandardQuestionService {
    StandardQuestion createStandardQuestion(StandardQuestionDTO questionDTO, Long userId);
} 
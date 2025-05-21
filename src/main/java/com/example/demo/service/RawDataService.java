package com.example.demo.service;

import com.example.demo.dto.RawAnswerDTO;
import com.example.demo.dto.RawQuestionDTO;
import com.example.demo.dto.RawQuestionWithAnswersDTO;
import com.example.demo.entity.RawQuestion;
import com.example.demo.entity.RawAnswer;

public interface RawDataService {
    RawQuestion createQuestion(RawQuestion question);
    RawQuestion createQuestionFromDTO(RawQuestionDTO questionDTO);
    RawAnswer createAnswer(RawAnswerDTO answerDTO);
    RawQuestion createQuestionWithAnswers(RawQuestionWithAnswersDTO dto);
} 
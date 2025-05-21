package com.example.demo.service;

import com.example.demo.dto.RawAnswerDTO;
import com.example.demo.dto.RawQuestionWithAnswersDTO;
import com.example.demo.entity.RawQuestion;
import com.example.demo.entity.RawAnswer;

public interface RawDataService {
    RawQuestion createQuestion(RawQuestion question);
    RawAnswer createAnswer(RawAnswerDTO answerDTO);
    RawQuestion createQuestionWithAnswers(RawQuestionWithAnswersDTO dto);
} 
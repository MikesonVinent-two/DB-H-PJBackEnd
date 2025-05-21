package com.example.demo.controller;

import com.example.demo.dto.LLMModelRegistrationRequest;
import com.example.demo.dto.LLMModelRegistrationResponse;
import com.example.demo.service.LLMModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/llm-models")
@CrossOrigin(origins = "*")
public class LLMModelController {
    
    @Autowired
    private LLMModelService llmModelService;
    
    @PostMapping("/register")
    public ResponseEntity<LLMModelRegistrationResponse> registerModels(
            @Valid @RequestBody LLMModelRegistrationRequest request) {
        LLMModelRegistrationResponse response = llmModelService.registerModels(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
} 
package com.ast.back.modules.plagiarism.application.impl;

import com.ast.back.infra.ai.AiExplanationRequest;
import com.ast.back.modules.ai.application.AiExplanationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class TeacherAiExplanationAsyncDispatcher implements TeacherAiExplanationDispatcher {

    private final AiExplanationService aiExplanationService;

    public TeacherAiExplanationAsyncDispatcher(AiExplanationService aiExplanationService) {
        this.aiExplanationService = aiExplanationService;
    }

    @Override
    @Async("plagiarismJobExecutor")
    public void dispatch(Long explanationId, AiExplanationRequest request) {
        aiExplanationService.completePendingExplanation(explanationId, request);
    }
}

package com.ast.back.modules.plagiarism.application.impl;

import com.ast.back.infra.ai.AiExplanationRequest;

public interface TeacherAiExplanationDispatcher {

    void dispatch(Long explanationId, AiExplanationRequest request);
}

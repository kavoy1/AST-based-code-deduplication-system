package com.ast.back.modules.plagiarism.dto;

import com.ast.back.modules.ai.dto.AiExplanationMode;

public class TeacherAiExplanationCreateRequest {

    private String mode;
    private Boolean includeTeacherNote;

    public AiExplanationMode resolveMode() {
        return AiExplanationMode.fromNullable(mode);
    }

    public boolean resolveIncludeTeacherNote() {
        return Boolean.TRUE.equals(includeTeacherNote);
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Boolean getIncludeTeacherNote() {
        return includeTeacherNote;
    }

    public void setIncludeTeacherNote(Boolean includeTeacherNote) {
        this.includeTeacherNote = includeTeacherNote;
    }
}

package com.ast.back.modules.plagiarism.dto;

public class TeacherPlagiarismPairStatusUpdateRequest {

    private String status;
    private String teacherNote;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTeacherNote() {
        return teacherNote;
    }

    public void setTeacherNote(String teacherNote) {
        this.teacherNote = teacherNote;
    }
}

package com.ast.back.modules.plagiarism.dto;

public record StudentPlagiarismSummary(
        boolean generated,
        String message,
        Integer highestScore,
        String status,
        String teacherNote
) {

    public static StudentPlagiarismSummary notGenerated(String message) {
        return new StudentPlagiarismSummary(false, message, null, null, null);
    }

    public static StudentPlagiarismSummary generated(Integer highestScore, String status, String teacherNote) {
        return new StudentPlagiarismSummary(true, "已生成报告", highestScore, status, teacherNote);
    }
}

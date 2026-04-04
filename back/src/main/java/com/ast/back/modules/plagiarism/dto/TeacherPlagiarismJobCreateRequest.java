package com.ast.back.modules.plagiarism.dto;

public class TeacherPlagiarismJobCreateRequest {

    private Integer thresholdScore = 80;
    private Integer topKPerStudent = 10;
    private String plagiarismMode = "FAST";

    public Integer getThresholdScore() {
        return thresholdScore;
    }

    public void setThresholdScore(Integer thresholdScore) {
        this.thresholdScore = thresholdScore;
    }

    public Integer getTopKPerStudent() {
        return topKPerStudent;
    }

    public void setTopKPerStudent(Integer topKPerStudent) {
        this.topKPerStudent = topKPerStudent;
    }

    public String getPlagiarismMode() {
        return plagiarismMode;
    }

    public void setPlagiarismMode(String plagiarismMode) {
        this.plagiarismMode = plagiarismMode;
    }
}

package com.ast.back.service.dto;

public class TeacherPlagiarismJobCreateRequest {

    private Integer thresholdScore = 80;
    private Integer topKPerStudent = 10;

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
}

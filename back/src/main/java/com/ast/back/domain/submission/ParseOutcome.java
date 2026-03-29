package com.ast.back.domain.submission;

public record ParseOutcome(boolean success, String errorMessage) {

    public static ParseOutcome ok() {
        return new ParseOutcome(true, null);
    }

    public static ParseOutcome failure(String errorMessage) {
        return new ParseOutcome(false, errorMessage);
    }
}

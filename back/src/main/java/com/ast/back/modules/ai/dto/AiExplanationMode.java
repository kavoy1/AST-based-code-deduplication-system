package com.ast.back.modules.ai.dto;

public enum AiExplanationMode {
    CODE_ONLY,
    CODE_WITH_SYSTEM_EVIDENCE;

    public static AiExplanationMode fromNullable(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return CODE_ONLY;
        }
        try {
            return AiExplanationMode.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CODE_ONLY;
        }
    }
}

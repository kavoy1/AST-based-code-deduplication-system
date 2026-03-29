package com.ast.back.service.dto;

import java.util.List;

public record TextSubmissionRequest(List<TextSubmissionEntry> entries) {
}

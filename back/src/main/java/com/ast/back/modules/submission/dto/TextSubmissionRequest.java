package com.ast.back.modules.submission.dto;

import java.util.List;

public record TextSubmissionRequest(List<TextSubmissionEntry> entries) {
}

package com.ast.back.domain.submission;

import org.springframework.web.multipart.MultipartFile;

public interface JavaSubmissionParser {

    ParseOutcome parse(MultipartFile file);
}

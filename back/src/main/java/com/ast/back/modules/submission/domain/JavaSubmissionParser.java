package com.ast.back.modules.submission.domain;

import org.springframework.web.multipart.MultipartFile;

public interface JavaSubmissionParser {

    ParseOutcome parse(MultipartFile file);
}

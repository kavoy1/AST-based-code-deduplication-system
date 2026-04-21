package com.ast.back.modules.submission.domain;

import org.springframework.web.multipart.MultipartFile;

public interface CSubmissionParser extends SubmissionParser {

    CProjectSourceIndex collectSources(MultipartFile file);
}

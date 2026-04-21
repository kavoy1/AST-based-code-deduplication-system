package com.ast.back.modules.submission.domain;

import org.springframework.web.multipart.MultipartFile;

public interface SubmissionParser {

    String language();

    SubmissionPackageKind packageKind();

    default boolean supports(String candidateLanguage, SubmissionPackageKind candidatePackageKind) {
        return language().equalsIgnoreCase(candidateLanguage) && packageKind() == candidatePackageKind;
    }

    ParseOutcome parse(MultipartFile file);
}

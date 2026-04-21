package com.ast.back.modules.submission.domain;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CSubmissionParserImpl implements CSubmissionParser {

    private static final String SINGLE_C_FILE_ONLY_MESSAGE = "单文件提交仅支持 .c 文件";
    private static final String C_SUBMISSION_TYPE_MESSAGE = "C 作业仅支持单个 .c 文件或单个 .zip 工程包";
    private static final String EMPTY_ARCHIVE_MESSAGE = "压缩包中未找到 .c 实现文件";

    private final CProjectSourceCollector sourceCollector;

    public CSubmissionParserImpl(CProjectSourceCollector sourceCollector) {
        this.sourceCollector = sourceCollector;
    }

    @Override
    public String language() {
        return "C";
    }

    @Override
    public SubmissionPackageKind packageKind() {
        return SubmissionPackageKind.SINGLE_SOURCE_FILE;
    }

    @Override
    public boolean supports(String candidateLanguage, SubmissionPackageKind candidatePackageKind) {
        return language().equalsIgnoreCase(candidateLanguage)
                && (candidatePackageKind == SubmissionPackageKind.SINGLE_SOURCE_FILE
                || candidatePackageKind == SubmissionPackageKind.PROJECT_ARCHIVE);
    }

    @Override
    public ParseOutcome parse(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return ParseOutcome.failure(C_SUBMISSION_TYPE_MESSAGE);
        }
        String lowerCaseFilename = originalFilename.toLowerCase();
        if (lowerCaseFilename.endsWith(".c")) {
            return ParseOutcome.ok();
        }
        if (lowerCaseFilename.endsWith(".h")) {
            return ParseOutcome.failure(SINGLE_C_FILE_ONLY_MESSAGE);
        }
        if (!lowerCaseFilename.endsWith(".zip")) {
            return ParseOutcome.failure(C_SUBMISSION_TYPE_MESSAGE);
        }
        CProjectSourceIndex sourceIndex = sourceCollector.collect(file);
        if (!sourceIndex.hasImplementationFiles()) {
            return ParseOutcome.failure(EMPTY_ARCHIVE_MESSAGE);
        }
        return ParseOutcome.ok();
    }

    @Override
    public CProjectSourceIndex collectSources(MultipartFile file) {
        return sourceCollector.collect(file);
    }
}

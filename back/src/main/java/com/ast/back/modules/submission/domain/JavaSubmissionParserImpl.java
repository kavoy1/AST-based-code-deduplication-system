package com.ast.back.modules.submission.domain;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
public class JavaSubmissionParserImpl implements JavaSubmissionParser {

    private static final String JAVA_FILE_ONLY_MESSAGE = "仅支持上传 .java 文件";

    private final JavaParser javaParser = new JavaParser(new ParserConfiguration());

    @Override
    public String language() {
        return "JAVA";
    }

    @Override
    public SubmissionPackageKind packageKind() {
        return SubmissionPackageKind.SINGLE_SOURCE_FILE;
    }

    @Override
    public ParseOutcome parse(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".java")) {
            return ParseOutcome.failure(JAVA_FILE_ONLY_MESSAGE);
        }
        try (InputStream inputStream = file.getInputStream()) {
            ParseResult<CompilationUnit> result = javaParser.parse(inputStream);
            if (result.isSuccessful()) {
                return ParseOutcome.ok();
            }
            String message = result.getProblems().isEmpty()
                    ? "Java 语法解析失败"
                    : result.getProblems().get(0).getVerboseMessage();
            return ParseOutcome.failure(message);
        } catch (IOException e) {
            return ParseOutcome.failure("读取上传文件失败");
        }
    }
}

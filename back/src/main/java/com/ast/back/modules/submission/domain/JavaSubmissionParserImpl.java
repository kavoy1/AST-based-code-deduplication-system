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

    private final JavaParser javaParser = new JavaParser(new ParserConfiguration());

    @Override
    public ParseOutcome parse(MultipartFile file) {
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

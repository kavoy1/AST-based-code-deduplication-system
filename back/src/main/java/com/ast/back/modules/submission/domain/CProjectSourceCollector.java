package com.ast.back.modules.submission.domain;

import com.ast.back.shared.common.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class CProjectSourceCollector {

    private final CProjectArchiveExtractor archiveExtractor;

    public CProjectSourceCollector(CProjectArchiveExtractor archiveExtractor) {
        this.archiveExtractor = archiveExtractor;
    }

    public CProjectSourceIndex collect(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException("缺少上传文件名");
        }
        return collect(originalFilename, () -> readSource(file), () -> archiveExtractor.extract(file));
    }

    public CProjectSourceIndex collect(Path filePath) {
        String filename = filePath.getFileName() == null ? null : filePath.getFileName().toString();
        if (filename == null) {
            throw new BusinessException("缺少上传文件名");
        }
        return collect(filename, () -> readSource(filePath), () -> archiveExtractor.extract(filePath));
    }

    private CProjectSourceIndex collect(String originalFilename, SourceReader sourceReader, ArchiveReader archiveReader) {
        String lowerCaseFilename = originalFilename.toLowerCase();
        if (lowerCaseFilename.endsWith(".zip")) {
            return archiveReader.read();
        }
        if (lowerCaseFilename.endsWith(".c") || lowerCaseFilename.endsWith(".h")) {
            return new CProjectSourceIndex(
                    List.of(new CProjectSourceFile(originalFilename, sourceReader.read())),
                    List.of()
            );
        }
        throw new BusinessException("仅支持收集 .c、.h 或 .zip 文件");
    }

    private String readSource(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("读取 C 源文件失败");
        }
    }

    private String readSource(Path filePath) {
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("读取 C 源文件失败");
        }
    }

    @FunctionalInterface
    private interface SourceReader {
        String read();
    }

    @FunctionalInterface
    private interface ArchiveReader {
        CProjectSourceIndex read();
    }
}

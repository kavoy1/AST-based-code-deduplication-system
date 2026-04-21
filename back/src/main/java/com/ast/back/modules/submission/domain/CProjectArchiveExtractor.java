package com.ast.back.modules.submission.domain;

import com.ast.back.shared.common.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class CProjectArchiveExtractor {

    public CProjectSourceIndex extract(MultipartFile archive) {
        try (InputStream inputStream = archive.getInputStream()) {
            return extract(inputStream);
        } catch (IOException e) {
            throw new BusinessException("读取 C 工程压缩包失败");
        }
    }

    public CProjectSourceIndex extract(Path archivePath) {
        try (InputStream inputStream = Files.newInputStream(archivePath)) {
            return extract(inputStream);
        } catch (IOException e) {
            throw new BusinessException("读取 C 工程压缩包失败");
        }
    }

    private CProjectSourceIndex extract(InputStream inputStream) {
        List<CProjectSourceFile> sourceFiles = new ArrayList<>();
        List<String> ignoredEntries = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zipInputStream.closeEntry();
                    continue;
                }
                String path = normalizeEntryPath(entry.getName());
                byte[] content = zipInputStream.readAllBytes();
                if (isSupportedSourceFile(path)) {
                    sourceFiles.add(new CProjectSourceFile(path, new String(content, StandardCharsets.UTF_8)));
                } else {
                    ignoredEntries.add(path);
                }
                zipInputStream.closeEntry();
            }
            return new CProjectSourceIndex(sourceFiles, ignoredEntries);
        } catch (IOException e) {
            throw new BusinessException("读取 C 工程压缩包失败");
        }
    }

    private boolean isSupportedSourceFile(String path) {
        String lowerCasePath = path.toLowerCase();
        return lowerCasePath.endsWith(".c") || lowerCasePath.endsWith(".h");
    }

    private String normalizeEntryPath(String path) {
        return path.replace('\\', '/');
    }
}

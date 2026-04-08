package com.ast.back.infra.storage;

import com.ast.back.modules.submission.persistence.entity.Submission;
import com.ast.back.shared.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class LocalStorageServiceImpl implements LocalStorageService {

    private final Path rootDir;
    private final int maxFiles;
    private final long maxFileBytes;

    public LocalStorageServiceImpl(
            @Value("${app.storage.root-dir:uploads}") String rootDir,
            @Value("${app.storage.max-files:20}") int maxFiles,
            @Value("${app.storage.max-file-bytes:2097152}") long maxFileBytes
    ) {
        this.rootDir = Paths.get(rootDir).toAbsolutePath().normalize();
        this.maxFiles = maxFiles;
        this.maxFileBytes = maxFileBytes;
    }

    @Override
    public StoredFile store(Long assignmentId, Long studentId, Submission submission, MultipartFile file) {
        if (file.getSize() > maxFileBytes) {
            throw new BusinessException("文件大小超过系统限制");
        }

        String safeFilename = sanitizeFilename(file.getOriginalFilename());
        Path relativePath = Paths.get(
                "assignment-" + assignmentId,
                "student-" + studentId,
                "submission-" + submission.getId(),
                safeFilename
        );
        Path targetPath = rootDir.resolve(relativePath).normalize();

        if (!targetPath.startsWith(rootDir)) {
            throw new BusinessException("非法文件路径");
        }

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
            return new StoredFile(relativePath.toString().replace('\\', '/'), sha256(targetPath), file.getSize());
        } catch (IOException e) {
            throw new BusinessException("保存上传文件失败");
        }
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    @Override
    public String readText(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BusinessException("文件路径不能为空");
        }
        Path targetPath = rootDir.resolve(relativePath).normalize();
        if (!targetPath.startsWith(rootDir)) {
            throw new BusinessException("非法文件路径");
        }
        try {
            return Files.readString(targetPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("读取提交文件失败");
        }
    }

    @Override
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path targetPath = rootDir.resolve(relativePath).normalize();
        if (!targetPath.startsWith(rootDir)) {
            throw new BusinessException("非法文件路径");
        }
        try {
            Files.deleteIfExists(targetPath);
            cleanupEmptyParents(targetPath.getParent());
        } catch (IOException e) {
            throw new BusinessException("删除提交文件失败");
        }
    }

    private String sanitizeFilename(String originalFilename) {
        String filename = originalFilename == null ? "source.java" : originalFilename;
        filename = filename.replace("\\", "_").replace("/", "_").replace("..", "_");
        return filename.isBlank() ? "source.java" : filename;
    }

    private void cleanupEmptyParents(Path directory) throws IOException {
        Path current = directory;
        while (current != null && current.startsWith(rootDir) && !current.equals(rootDir)) {
            if (Files.notExists(current)) {
                current = current.getParent();
                continue;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
                if (stream.iterator().hasNext()) {
                    break;
                }
            }
            Files.deleteIfExists(current);
            current = current.getParent();
        }
    }

    private String sha256(Path filePath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, read);
            }
            StringBuilder builder = new StringBuilder();
            for (byte b : messageDigest.digest()) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 not available", e);
        }
    }
}

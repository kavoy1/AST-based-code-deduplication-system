package com.ast.back.infra.storage;

import com.ast.back.shared.common.BusinessException;
import com.ast.back.infra.storage.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AssignmentMaterialStorageServiceImpl implements AssignmentMaterialStorageService {

    private final Path rootDir;
    private final long maxFileBytes;

    public AssignmentMaterialStorageServiceImpl(
            @Value("${app.storage.root-dir:uploads}") String rootDir,
            @Value("${app.storage.assignment-material-max-file-bytes:20971520}") long maxFileBytes
    ) {
        this.rootDir = Paths.get(rootDir).toAbsolutePath().normalize();
        this.maxFileBytes = maxFileBytes;
    }

    @Override
    public StoredFile store(Long assignmentId, Long materialId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("作业资料不能为空");
        }
        if (file.getSize() > maxFileBytes) {
            throw new BusinessException("作业资料大小超过系统限制");
        }

        String safeFilename = sanitizeFilename(file.getOriginalFilename());
        Path relativePath = Paths.get("assignment-materials", String.valueOf(assignmentId), materialId + "-" + safeFilename);
        Path targetPath = rootDir.resolve(relativePath).normalize();
        if (!targetPath.startsWith(rootDir)) {
            throw new BusinessException("非法附件路径");
        }

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
            return new StoredFile(relativePath.toString().replace('\\', '/'), sha256(targetPath), file.getSize());
        } catch (IOException e) {
            throw new BusinessException("保存作业资料失败");
        }
    }

    @Override
    public Resource loadAsResource(String relativePath) {
        try {
            Path path = rootDir.resolve(relativePath).normalize();
            if (!path.startsWith(rootDir)) {
                throw new BusinessException("非法附件路径");
            }
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("作业资料不存在");
            }
            return resource;
        } catch (IOException e) {
            throw new BusinessException("读取作业资料失败");
        }
    }

    @Override
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(rootDir.resolve(relativePath).normalize());
        } catch (IOException e) {
            throw new BusinessException("删除作业资料失败");
        }
    }

    private String sanitizeFilename(String originalFilename) {
        String filename = originalFilename == null ? "material.bin" : originalFilename;
        filename = filename.replace("\\", "_").replace("/", "_").replace("..", "_");
        return filename.isBlank() ? "material.bin" : filename;
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

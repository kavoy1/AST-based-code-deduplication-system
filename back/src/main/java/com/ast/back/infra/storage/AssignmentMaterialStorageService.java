package com.ast.back.infra.storage;

import com.ast.back.entity.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface AssignmentMaterialStorageService {

    StoredFile store(Long assignmentId, Long materialId, MultipartFile file);

    Resource loadAsResource(String relativePath);

    void delete(String relativePath);
}

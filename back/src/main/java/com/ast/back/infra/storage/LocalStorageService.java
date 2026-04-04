package com.ast.back.infra.storage;

import com.ast.back.infra.storage.StoredFile;
import com.ast.back.modules.submission.persistence.entity.Submission;
import org.springframework.web.multipart.MultipartFile;

public interface LocalStorageService {

    StoredFile store(Long assignmentId, Long studentId, Submission submission, MultipartFile file);

    String readText(String relativePath);
}

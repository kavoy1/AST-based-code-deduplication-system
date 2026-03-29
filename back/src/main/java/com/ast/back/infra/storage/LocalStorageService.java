package com.ast.back.infra.storage;

import com.ast.back.entity.StoredFile;
import com.ast.back.entity.Submission;
import org.springframework.web.multipart.MultipartFile;

public interface LocalStorageService {

    StoredFile store(Long assignmentId, Long studentId, Submission submission, MultipartFile file);
}

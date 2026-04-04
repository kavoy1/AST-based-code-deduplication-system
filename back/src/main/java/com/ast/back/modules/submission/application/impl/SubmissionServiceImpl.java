package com.ast.back.modules.submission.application.impl;

import com.ast.back.shared.common.BusinessException;
import com.ast.back.modules.plagiarism.domain.AstSignatureProfile;
import com.ast.back.modules.plagiarism.domain.JavaAstSignatureExtractor;
import com.ast.back.modules.submission.domain.JavaSubmissionParser;
import com.ast.back.modules.submission.domain.ParseOutcome;
import com.ast.back.modules.assignment.persistence.entity.Assignment;
import com.ast.back.modules.assignment.persistence.entity.AssignmentClass;
import com.ast.back.modules.classmgmt.persistence.entity.ClassStudent;
import com.ast.back.infra.storage.StoredFile;
import com.ast.back.modules.submission.persistence.entity.Submission;
import com.ast.back.modules.submission.persistence.entity.SubmissionFile;
import com.ast.back.modules.plagiarism.persistence.entity.SubmissionProfile;
import com.ast.back.infra.storage.InMemoryMultipartFile;
import com.ast.back.infra.storage.LocalStorageService;
import com.ast.back.modules.assignment.persistence.mapper.AssignmentClassMapper;
import com.ast.back.modules.assignment.persistence.mapper.AssignmentMapper;
import com.ast.back.modules.classmgmt.persistence.mapper.ClassStudentMapper;
import com.ast.back.modules.submission.persistence.mapper.SubmissionFileMapper;
import com.ast.back.modules.submission.persistence.mapper.SubmissionMapper;
import com.ast.back.modules.plagiarism.persistence.mapper.SubmissionProfileMapper;
import com.ast.back.modules.submission.application.SubmissionService;
import com.ast.back.modules.submission.dto.TextSubmissionEntry;
import com.ast.back.modules.submission.dto.TextSubmissionRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private static final String PROFILE_ALGO_VERSION = "AC_BAG_OF_NODES_V1";

    private final AssignmentMapper assignmentMapper;
    private final AssignmentClassMapper assignmentClassMapper;
    private final ClassStudentMapper classStudentMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionFileMapper submissionFileMapper;
    private final SubmissionProfileMapper submissionProfileMapper;
    private final LocalStorageService localStorageService;
    private final JavaSubmissionParser javaSubmissionParser;
    private final JavaAstSignatureExtractor javaAstSignatureExtractor;
    private final ObjectMapper objectMapper;

    public SubmissionServiceImpl(
            AssignmentMapper assignmentMapper,
            AssignmentClassMapper assignmentClassMapper,
            ClassStudentMapper classStudentMapper,
            SubmissionMapper submissionMapper,
            SubmissionFileMapper submissionFileMapper,
            SubmissionProfileMapper submissionProfileMapper,
            LocalStorageService localStorageService,
            JavaSubmissionParser javaSubmissionParser
    ) {
        this.assignmentMapper = assignmentMapper;
        this.assignmentClassMapper = assignmentClassMapper;
        this.classStudentMapper = classStudentMapper;
        this.submissionMapper = submissionMapper;
        this.submissionFileMapper = submissionFileMapper;
        this.submissionProfileMapper = submissionProfileMapper;
        this.localStorageService = localStorageService;
        this.javaSubmissionParser = javaSubmissionParser;
        this.javaAstSignatureExtractor = new JavaAstSignatureExtractor();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public Submission createSubmission(Long studentId, Long assignmentId, List<MultipartFile> files) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }
        Integer classId = resolveSubmissionClassId(studentId, assignmentId, assignment);
        requireApprovedMembership(studentId, classId);
        validateFiles(assignment, files);

        Submission latestSubmission = findLatestSubmission(studentId, assignmentId, classId);
        if (assignment.getAllowResubmit() != null && assignment.getAllowResubmit() == 0 && latestSubmission != null) {
            throw new BusinessException("当前作业不允许重复提交");
        }

        Submission submission = new Submission();
        submission.setAssignmentId(assignmentId);
        submission.setClassId(classId);
        submission.setStudentId(studentId);
        submission.setVersion(latestSubmission == null ? 1 : latestSubmission.getVersion() + 1);
        submission.setSubmitTime(LocalDateTime.now());
        LocalDateTime deadline = assignment.getEndAt() != null ? assignment.getEndAt() : assignment.getDeadline();
        submission.setDeadlineAt(deadline);
        submission.setIsLate(deadline != null && submission.getSubmitTime().isAfter(deadline) ? 1 : 0);
        submission.setIsLatest(1);
        submission.setIsValid(0);
        submission.setParseOkFiles(0);
        submission.setTotalFiles(files.size());
        submissionMapper.insert(submission);

        if (latestSubmission != null) {
            latestSubmission.setIsLatest(0);
            submissionMapper.updateById(latestSubmission);
        }

        int parseOkFiles = 0;
        int totalNodes = 0;
        Map<String, Integer> mergedSignatureCounts = new HashMap<>();
        List<Map<String, String>> parseFailures = new ArrayList<>();
        for (MultipartFile file : files) {
            StoredFile storedFile = localStorageService.store(assignmentId, studentId, submission, file);
            ParseOutcome parseOutcome = javaSubmissionParser.parse(file);
            if (parseOutcome.success()) {
                parseOkFiles++;
                AstSignatureProfile profile = extractProfile(file);
                totalNodes += profile.totalNodes();
                profile.signatureCounts().forEach((key, value) -> mergedSignatureCounts.merge(key, value, Integer::sum));
            } else {
                parseFailures.add(Map.of(
                        "file", file.getOriginalFilename() == null ? "unknown.java" : file.getOriginalFilename(),
                        "reason", parseOutcome.errorMessage() == null ? "Parse failed" : parseOutcome.errorMessage()
                ));
            }
            submissionFileMapper.insert(buildSubmissionFile(submission.getId(), file, storedFile, parseOutcome));
        }

        submission.setParseOkFiles(parseOkFiles);
        submission.setIsValid(parseOkFiles >= 1 ? 1 : 0);
        submissionMapper.updateById(submission);
        submissionProfileMapper.insert(buildSubmissionProfile(submission, totalNodes, mergedSignatureCounts, parseFailures));
        return submission;
    }

    @Override
    public Submission createTextSubmission(Long studentId, Long assignmentId, TextSubmissionRequest request) {
        if (request == null || request.entries() == null || request.entries().isEmpty()) {
            throw new IllegalArgumentException("文本提交内容不能为空");
        }
        List<MultipartFile> files = toMultipartFiles(request.entries());
        return createSubmission(studentId, assignmentId, files);
    }

    @Override
    public List<Submission> listStudentSubmissions(Long studentId, Long assignmentId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }
        Integer classId = resolveSubmissionClassId(studentId, assignmentId, assignment);
        requireApprovedMembership(studentId, classId);
        QueryWrapper<Submission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("assignment_id", assignmentId)
                .eq("student_id", studentId)
                .eq("class_id", classId)
                .orderByDesc("version");
        return submissionMapper.selectList(queryWrapper);
    }

    private void validateFiles(Assignment assignment, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException("请至少上传一个 Java 文件");
        }
        if (assignment.getMaxFiles() != null && files.size() > assignment.getMaxFiles()) {
            throw new BusinessException("上传文件数量超过作业限制");
        }
        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".java")) {
                throw new BusinessException("仅支持上传 .java 文件");
            }
        }
    }

    private Submission findLatestSubmission(Long studentId, Long assignmentId, Integer classId) {
        QueryWrapper<Submission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("assignment_id", assignmentId)
                .eq("student_id", studentId)
                .eq("class_id", classId)
                .eq("is_latest", 1)
                .orderByDesc("version")
                .last("LIMIT 1");
        return submissionMapper.selectOne(queryWrapper);
    }

    private Integer resolveSubmissionClassId(Long studentId, Long assignmentId, Assignment assignment) {
        QueryWrapper<AssignmentClass> wrapper = new QueryWrapper<>();
        wrapper.eq("assignment_id", assignmentId);
        List<Integer> matchedClassIds = assignmentClassMapper.selectList(wrapper).stream()
                .map(AssignmentClass::getClassId)
                .filter(classId -> hasApprovedMembership(studentId, classId))
                .toList();
        if (matchedClassIds.isEmpty()) {
            Integer fallbackClassId = assignment.getClazzId();
            if (fallbackClassId != null) {
                return fallbackClassId;
            }
            throw new BusinessException(403, "无权提交该作业");
        }
        if (matchedClassIds.size() == 1) {
            return matchedClassIds.get(0);
        }
        Integer fallbackClassId = assignment.getClazzId();
        if (fallbackClassId != null && matchedClassIds.contains(fallbackClassId)) {
            return fallbackClassId;
        }
        return matchedClassIds.get(0);
    }

    private void requireApprovedMembership(Long studentId, Integer classId) {
        if (!hasApprovedMembership(studentId, classId)) {
            throw new BusinessException(403, "无权提交该班级作业");
        }
    }

    private boolean hasApprovedMembership(Long studentId, Integer classId) {
        QueryWrapper<ClassStudent> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("student_id", studentId).eq("class_id", classId).eq("status", 1);
        return classStudentMapper.selectOne(queryWrapper) != null;
    }

    private SubmissionFile buildSubmissionFile(
            Long submissionId,
            MultipartFile file,
            StoredFile storedFile,
            ParseOutcome parseOutcome
    ) {
        SubmissionFile submissionFile = new SubmissionFile();
        submissionFile.setSubmissionId(submissionId);
        submissionFile.setFilename(file.getOriginalFilename());
        submissionFile.setStoragePath(storedFile.storagePath());
        submissionFile.setSha256(storedFile.sha256());
        submissionFile.setBytes(storedFile.bytes());
        submissionFile.setParseStatus(parseOutcome.success() ? "OK" : "FAILED");
        submissionFile.setParseError(parseOutcome.success() ? null : parseOutcome.errorMessage());
        return submissionFile;
    }

    private SubmissionProfile buildSubmissionProfile(
            Submission submission,
            int totalNodes,
            Map<String, Integer> signatureCounts,
            List<Map<String, String>> parseFailures
    ) {
        SubmissionProfile profile = new SubmissionProfile();
        profile.setSubmissionId(submission.getId());
        profile.setAlgoVersion(PROFILE_ALGO_VERSION);
        profile.setTotalNodes(totalNodes);
        profile.setSignatureCountsJson(writeJson(signatureCounts));
        profile.setParseFailuresJson(writeJson(parseFailures));
        profile.setCreateTime(LocalDateTime.now());
        profile.setUpdateTime(LocalDateTime.now());
        return profile;
    }

    private AstSignatureProfile extractProfile(MultipartFile file) {
        try {
            String source = new String(file.getBytes(), StandardCharsets.UTF_8);
            return javaAstSignatureExtractor.extract(source);
        } catch (IOException ex) {
            throw new BusinessException("读取上传文件失败");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("提交画像序列化失败", ex);
        }
    }

    private List<MultipartFile> toMultipartFiles(List<TextSubmissionEntry> entries) {
        List<MultipartFile> files = new ArrayList<>();
        Set<String> usedFilenames = new HashSet<>();
        for (TextSubmissionEntry entry : entries) {
            String filename = resolveUniqueJavaFilename(entry.filename(), usedFilenames);
            files.add(toMultipartFile(filename, entry));
        }
        return files;
    }

    private String resolveUniqueJavaFilename(String rawFilename, Set<String> usedFilenames) {
        String filename = normalizeJavaFilename(rawFilename);
        if (usedFilenames.add(filename)) {
            return filename;
        }

        int extensionIndex = filename.lastIndexOf('.');
        String baseName = extensionIndex >= 0 ? filename.substring(0, extensionIndex) : filename;
        String extension = extensionIndex >= 0 ? filename.substring(extensionIndex) : ".java";

        int duplicateIndex = 2;
        String candidate = baseName + "_" + duplicateIndex + extension;
        while (!usedFilenames.add(candidate)) {
            duplicateIndex++;
            candidate = baseName + "_" + duplicateIndex + extension;
        }
        return candidate;
    }

    private String normalizeJavaFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "Main.java";
        }
        String trimmed = filename.trim();
        return trimmed.toLowerCase().endsWith(".java") ? trimmed : trimmed + ".java";
    }

    private MultipartFile toMultipartFile(String filename, TextSubmissionEntry entry) {
        String content = entry.content() == null ? "" : entry.content();
        return new InMemoryMultipartFile(
                "files",
                filename,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}

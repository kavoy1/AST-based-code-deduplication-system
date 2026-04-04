package com.ast.back.modules.plagiarism.application.impl;

import com.ast.back.infra.ai.AiExplanationRequest;
import com.ast.back.shared.common.BusinessException;
import com.ast.back.infra.storage.LocalStorageService;
import com.ast.back.modules.ai.dto.AiExplanationMode;
import com.ast.back.modules.ai.persistence.entity.AiExplanation;
import com.ast.back.modules.assignment.persistence.entity.Assignment;
import com.ast.back.modules.plagiarism.persistence.entity.PlagiarismJob;
import com.ast.back.modules.plagiarism.persistence.entity.SimilarityEvidence;
import com.ast.back.modules.plagiarism.persistence.entity.SimilarityPair;
import com.ast.back.modules.submission.persistence.entity.Submission;
import com.ast.back.modules.submission.persistence.entity.SubmissionFile;
import com.ast.back.modules.ai.persistence.mapper.AiExplanationMapper;
import com.ast.back.modules.assignment.persistence.mapper.AssignmentMapper;
import com.ast.back.modules.plagiarism.persistence.mapper.PlagiarismJobMapper;
import com.ast.back.modules.plagiarism.persistence.mapper.SimilarityEvidenceMapper;
import com.ast.back.modules.plagiarism.persistence.mapper.SimilarityPairMapper;
import com.ast.back.modules.submission.persistence.mapper.SubmissionFileMapper;
import com.ast.back.modules.submission.persistence.mapper.SubmissionMapper;
import com.ast.back.modules.ai.application.AiExplanationService;
import com.ast.back.modules.plagiarism.application.TeacherPlagiarismJobService;
import com.ast.back.modules.plagiarism.domain.TeacherComparableCodeBlock;
import com.ast.back.modules.plagiarism.domain.TeacherComparableCodeBlockExtractor;
import com.ast.back.modules.plagiarism.domain.TeacherComparableCodeSegmentLocator;
import com.ast.back.modules.plagiarism.dto.TeacherCodeHighlightRangeView;
import com.ast.back.modules.plagiarism.dto.TeacherCodePaneView;
import com.ast.back.modules.plagiarism.dto.TeacherCodeSegmentView;
import com.ast.back.modules.plagiarism.dto.TeacherIncomparableSubmissionView;
import com.ast.back.modules.plagiarism.dto.TeacherParseFailureView;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismCodeCompareView;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismEvidenceView;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismJobReport;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismJobStatsView;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismJobSummaryView;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismPairDetailView;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismPairView;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismUnmatchedFileView;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class TeacherPlagiarismJobServiceImpl implements TeacherPlagiarismJobService {

    private static final Set<String> ALLOWED_PAIR_STATUS = Set.of("PENDING", "CONFIRMED", "FALSE_POSITIVE");
    private static final int DEFAULT_TOP_K = 10;
    private static final String FAST_MODE = "FAST";
    private static final String DEEP_MODE = "DEEP";
    private static final String FAST_ALGO_VERSION = "AC_BAG_OF_NODES_V1";
    private static final String DEEP_ALGO_VERSION = "AC_DEEP_AST_V1_PREVIEW";
    private final TeacherComparableCodeBlockExtractor codeBlockExtractor = new TeacherComparableCodeBlockExtractor();
    private final TeacherComparableCodeSegmentLocator codeSegmentLocator = new TeacherComparableCodeSegmentLocator();

    private final AssignmentMapper assignmentMapper;
    private final PlagiarismJobMapper plagiarismJobMapper;
    private final SimilarityPairMapper similarityPairMapper;
    private final SimilarityEvidenceMapper similarityEvidenceMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionFileMapper submissionFileMapper;
    private final AiExplanationMapper aiExplanationMapper;
    private final TeacherPlagiarismJobDispatcher teacherPlagiarismJobDispatcher;
    private final AiExplanationService aiExplanationService;
    private final LocalStorageService localStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TeacherPlagiarismJobServiceImpl(
            AssignmentMapper assignmentMapper,
            PlagiarismJobMapper plagiarismJobMapper,
            SimilarityPairMapper similarityPairMapper,
            SimilarityEvidenceMapper similarityEvidenceMapper,
            SubmissionMapper submissionMapper,
            SubmissionFileMapper submissionFileMapper,
            AiExplanationMapper aiExplanationMapper,
            TeacherPlagiarismJobDispatcher teacherPlagiarismJobDispatcher,
            AiExplanationService aiExplanationService,
            LocalStorageService localStorageService
    ) {
        this.assignmentMapper = assignmentMapper;
        this.plagiarismJobMapper = plagiarismJobMapper;
        this.similarityPairMapper = similarityPairMapper;
        this.similarityEvidenceMapper = similarityEvidenceMapper;
        this.submissionMapper = submissionMapper;
        this.submissionFileMapper = submissionFileMapper;
        this.aiExplanationMapper = aiExplanationMapper;
        this.teacherPlagiarismJobDispatcher = teacherPlagiarismJobDispatcher;
        this.aiExplanationService = aiExplanationService;
        this.localStorageService = localStorageService;
    }

    @Override
    public PlagiarismJob createSkeletonJob(Long teacherId, Long assignmentId, Integer thresholdScore, Integer topKPerStudent, String plagiarismMode) {
        Assignment assignment = requireTeacherAssignment(teacherId, assignmentId);
        LocalDateTime deadline = assignment.getEndAt() != null ? assignment.getEndAt() : assignment.getDeadline();
        if (deadline != null && LocalDateTime.now().isBefore(deadline)) {
            throw new BusinessException("Cannot create plagiarism job before deadline");
        }

        int effectiveThresholdScore = thresholdScore == null ? 80 : thresholdScore;
        int effectiveTopK = topKPerStudent == null ? DEFAULT_TOP_K : topKPerStudent;
        String normalizedMode = normalizePlagiarismMode(plagiarismMode);
        List<Submission> comparableSubmissions = listComparableSubmissions(assignmentId);
        String submissionFingerprint = buildSubmissionFingerprint(comparableSubmissions);
        int comparablePairCount = calculateComparablePairs(comparableSubmissions);
        String configSnapshot = buildConfigSnapshot(
                submissionFingerprint,
                comparableSubmissions.size(),
                comparablePairCount,
                effectiveThresholdScore,
                normalizedMode
        );

        PlagiarismJob reusableJob = findReusableDoneJob(assignmentId, submissionFingerprint, effectiveThresholdScore, normalizedMode);
        if (reusableJob != null) {
            PlagiarismJob job = new PlagiarismJob();
            job.setAssignmentId(assignmentId);
            job.setStatus("DONE");
            job.setParamsJson("{\"thresholdScore\":" + effectiveThresholdScore + ",\"topKPerStudent\":" + effectiveTopK + ",\"plagiarismMode\":\"" + normalizedMode + "\",\"mode\":\"REUSED\",\"sourceJobId\":" + reusableJob.getId() + "}");
            job.setConfigSnapshot(configSnapshot);
            job.setProgressTotal(comparablePairCount);
            job.setProgressDone(comparablePairCount);
            job.setCreateTime(LocalDateTime.now());
            job.setStartTime(LocalDateTime.now());
            job.setEndTime(LocalDateTime.now());
            plagiarismJobMapper.insert(job);
            int clonedPairCount = cloneJobResults(reusableJob.getId(), job.getId(), effectiveThresholdScore);
            job.setConfigSnapshot(buildReusedConfigSnapshot(
                    reusableJob.getConfigSnapshot(),
                    submissionFingerprint,
                    comparableSubmissions.size(),
                    comparablePairCount,
                    effectiveThresholdScore,
                    normalizedMode,
                    reusableJob.getId(),
                    clonedPairCount
            ));
            plagiarismJobMapper.updateById(job);
            return job;
        }

        PlagiarismJob job = new PlagiarismJob();
        job.setAssignmentId(assignmentId);
        job.setStatus("QUEUED");
        job.setParamsJson("{\"thresholdScore\":" + effectiveThresholdScore + ",\"topKPerStudent\":" + effectiveTopK + ",\"plagiarismMode\":\"" + normalizedMode + "\",\"mode\":\"ASYNC\"}");
        job.setConfigSnapshot(configSnapshot);
        job.setProgressTotal(0);
        job.setProgressDone(0);
        job.setCreateTime(LocalDateTime.now());
        plagiarismJobMapper.insert(job);
        teacherPlagiarismJobDispatcher.dispatch(job, effectiveThresholdScore, effectiveTopK, normalizedMode);
        return job;
    }

    @Override
    public List<TeacherPlagiarismJobSummaryView> listJobsByAssignment(Long teacherId, Long assignmentId) {
        requireTeacherAssignment(teacherId, assignmentId);
        QueryWrapper<PlagiarismJob> wrapper = new QueryWrapper<>();
        wrapper.eq("assignment_id", assignmentId).orderByDesc("id");
        return plagiarismJobMapper.selectList(wrapper).stream()
                .map(this::buildJobSummaryView)
                .toList();
    }

    @Override
    public PlagiarismJob getJob(Long teacherId, Long jobId) {
        PlagiarismJob job = plagiarismJobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException("Plagiarism job does not exist");
        }
        requireTeacherAssignment(teacherId, job.getAssignmentId());
        return job;
    }

    @Override
    public TeacherPlagiarismJobReport getReport(
            Long teacherId,
            Long jobId,
            Integer minScore,
            Integer perStudentTopK,
            String statuses,
            String sortBy,
            String sortDirection
    ) {
        PlagiarismJob job = getJob(teacherId, jobId);
        int effectiveMinScore = minScore == null ? 0 : minScore;
        int effectiveTopK = perStudentTopK == null ? DEFAULT_TOP_K : perStudentTopK;

        List<TeacherPlagiarismPairView> filteredPairs = buildFilteredPairViews(
                listPairs(jobId),
                effectiveMinScore,
                effectiveTopK,
                statuses,
                sortBy,
                sortDirection
        );
        List<TeacherIncomparableSubmissionView> incomparableSubmissions = listIncomparableSubmissions(job.getAssignmentId());
        String message = buildReportMessage(job.getStatus(), filteredPairs.isEmpty());

        return new TeacherPlagiarismJobReport(
                jobId,
                job.getStatus(),
                message,
                effectiveMinScore,
                effectiveTopK,
                buildJobStatsView(job),
                filteredPairs,
                incomparableSubmissions
        );
    }

    @Override
    public TeacherPlagiarismPairDetailView getPairDetail(Long teacherId, Long pairId) {
        SimilarityPair pair = requireTeacherPair(teacherId, pairId);
        PairCompareBundle compareBundle = buildPairCompareBundle(pair);
        return new TeacherPlagiarismPairDetailView(
                pair.getId(),
                pair.getJobId(),
                pair.getStudentA(),
                pair.getStudentB(),
                pair.getScore(),
                pair.getStatus(),
                pair.getTeacherNote(),
                listEvidenceViews(pairId),
                getLatestAiExplanation(teacherId, pairId),
                compareBundle.primaryCompare(),
                compareBundle.matchedFilePairs(),
                compareBundle.unmatchedLeftFiles(),
                compareBundle.unmatchedRightFiles(),
                compareBundle.crossFileSegments()
        );
    }

    @Override
    public SimilarityPair updatePairStatus(Long teacherId, Long pairId, String status, String teacherNote) {
        SimilarityPair pair = requireTeacherPair(teacherId, pairId);
        String normalizedStatus = status == null ? null : status.trim().toUpperCase();
        if (normalizedStatus == null || !ALLOWED_PAIR_STATUS.contains(normalizedStatus)) {
            throw new BusinessException("相似结果状态不合法");
        }
        pair.setStatus(normalizedStatus);
        pair.setTeacherNote(teacherNote);
        similarityPairMapper.updateById(pair);
        return pair;
    }

    @Override
    public byte[] exportReportCsv(
            Long teacherId,
            Long jobId,
            Integer minScore,
            Integer perStudentTopK,
            String statuses,
            String sortBy,
            String sortDirection
    ) {
        TeacherPlagiarismJobReport report = getReport(teacherId, jobId, minScore, perStudentTopK, statuses, sortBy, sortDirection);
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("jobId,status,minScore,perStudentTopK,statuses,sortBy,sortDirection,message\r\n");
        builder.append(csv(report.jobId()))
                .append(',').append(csv(report.status()))
                .append(',').append(csv(report.minScore()))
                .append(',').append(csv(report.perStudentTopK()))
                .append(',').append(csv(statuses))
                .append(',').append(csv(normalizeSortBy(sortBy)))
                .append(',').append(csv(normalizeSortDirection(sortDirection)))
                .append(',').append(csv(report.message()))
                .append("\r\n\r\n");

        if (report.jobStats() != null) {
            builder.append("executionMode,reusedFromJobId,comparableSubmissionCount,comparablePairCount,sizeSkippedPairs,bucketSkippedPairs,fullCalculatedPairs,thresholdMatchedPairs\r\n");
            builder.append(csv(report.jobStats().executionMode()))
                    .append(',').append(csv(report.jobStats().reusedFromJobId()))
                    .append(',').append(csv(report.jobStats().comparableSubmissionCount()))
                    .append(',').append(csv(report.jobStats().comparablePairCount()))
                    .append(',').append(csv(report.jobStats().sizeSkippedPairs()))
                    .append(',').append(csv(report.jobStats().bucketSkippedPairs()))
                    .append(',').append(csv(report.jobStats().fullCalculatedPairs()))
                    .append(',').append(csv(report.jobStats().thresholdMatchedPairs()))
                    .append("\r\n\r\n");
        }

        builder.append("pairId,studentA,studentB,score,status,teacherNote\r\n");
        for (TeacherPlagiarismPairView pair : report.pairs()) {
            builder.append(csv(pair.pairId()))
                    .append(',').append(csv(pair.studentA()))
                    .append(',').append(csv(pair.studentB()))
                    .append(',').append(csv(pair.score()))
                    .append(',').append(csv(pair.status()))
                    .append(',').append(csv(pair.teacherNote()))
                    .append("\r\n");
        }

        builder.append("\r\nsubmissionId,classId,studentId,version,reason,parseOkFiles,totalFiles,parseFailures\r\n");
        for (TeacherIncomparableSubmissionView item : report.incomparableSubmissions()) {
            String parseFailureSummary = item.parseFailures().stream()
                    .map(failure -> failure.file() + ":" + failure.reason())
                    .reduce((left, right) -> left + " | " + right)
                    .orElse("");
            builder.append(csv(item.submissionId()))
                    .append(',').append(csv(item.classId()))
                    .append(',').append(csv(item.studentId()))
                    .append(',').append(csv(item.version()))
                    .append(',').append(csv(item.reason()))
                    .append(',').append(csv(item.parseOkFiles()))
                    .append(',').append(csv(item.totalFiles()))
                    .append(',').append(csv(parseFailureSummary))
                    .append("\r\n");
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportReportZip(
            Long teacherId,
            Long jobId,
            Integer minScore,
            Integer perStudentTopK,
            String statuses,
            String sortBy,
            String sortDirection
    ) {
        TeacherPlagiarismJobReport report = getReport(teacherId, jobId, minScore, perStudentTopK, statuses, sortBy, sortDirection);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
                writeZipEntry(zipOutputStream, "01-report-summary.txt", buildReportSummaryText(report, statuses, sortBy, sortDirection));
                writeZipEntry(zipOutputStream, "02-job-stats.csv", buildStatsCsv(report));
                writeZipEntry(zipOutputStream, "03-similarity-pairs.csv", buildPairsCsv(report));
                writeZipEntry(zipOutputStream, "04-incomparable-submissions.csv", buildIncomparableCsv(report));
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export plagiarism report zip", e);
        }
    }

    @Override
    public AiExplanation createAiExplanation(Long teacherId, Long pairId, AiExplanationMode mode, boolean includeTeacherNote) {
        SimilarityPair pair = requireTeacherPair(teacherId, pairId);
        PairCompareBundle compareBundle = buildPairCompareBundle(pair);
        SimilarityEvidence evidence = mode == AiExplanationMode.CODE_WITH_SYSTEM_EVIDENCE ? requirePrimaryEvidence(pairId) : null;
        AiExplanationRequest request = new AiExplanationRequest(
                pair,
                evidence,
                compareBundle.primaryCompare(),
                mode == null ? AiExplanationMode.CODE_ONLY : mode,
                includeTeacherNote,
                includeTeacherNote ? pair.getTeacherNote() : null
        );
        return aiExplanationService.createExplanation(request);
    }

    @Override
    public List<AiExplanation> listAiExplanations(Long teacherId, Long pairId) {
        requireTeacherPair(teacherId, pairId);
        QueryWrapper<AiExplanation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pair_id", pairId).orderByDesc("id");
        return aiExplanationMapper.selectList(queryWrapper);
    }

    @Override
    public AiExplanation getLatestAiExplanation(Long teacherId, Long pairId) {
        requireTeacherPair(teacherId, pairId);
        QueryWrapper<AiExplanation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pair_id", pairId).orderByDesc("id").last("LIMIT 1");
        return aiExplanationMapper.selectOne(queryWrapper);
    }

    private List<Submission> listComparableSubmissions(Long assignmentId) {
        QueryWrapper<Submission> wrapper = new QueryWrapper<>();
        wrapper.eq("assignment_id", assignmentId).eq("is_latest", 1).eq("is_valid", 1)
                .orderByAsc("class_id").orderByAsc("student_id").orderByAsc("id");
        return submissionMapper.selectList(wrapper);
    }

    private int calculateComparablePairs(List<Submission> submissions) {
        Map<Integer, Integer> countByClass = new HashMap<>();
        for (Submission submission : submissions) {
            countByClass.merge(submission.getClassId(), 1, Integer::sum);
        }
        int total = 0;
        for (Integer count : countByClass.values()) {
            total += count * (count - 1) / 2;
        }
        return total;
    }

    private String buildSubmissionFingerprint(List<Submission> submissions) {
        StringBuilder builder = new StringBuilder();
        for (Submission submission : submissions) {
            builder.append(submission.getId()).append(':')
                    .append(submission.getStudentId()).append(':')
                    .append(submission.getClassId()).append(':')
                    .append(submission.getVersion()).append(':')
                    .append(submission.getParseOkFiles()).append(':')
                    .append(submission.getTotalFiles())
                    .append(';');
        }
        return sha256Hex(builder.toString());
    }

    private String buildConfigSnapshot(String submissionFingerprint, int submissionCount, int pairCount, int thresholdScore, String plagiarismMode) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("algoVersion", resolveAlgoVersion(plagiarismMode));
        snapshot.put("plagiarismMode", plagiarismMode);
        snapshot.put("submissionFingerprint", submissionFingerprint);
        snapshot.put("comparableSubmissionCount", submissionCount);
        snapshot.put("comparablePairCount", pairCount);
        snapshot.put("thresholdScore", thresholdScore);
        snapshot.put("executionMode", "ASYNC");
        snapshot.put("executionStats", buildExecutionStatsMap(0, 0, 0, 0));
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build job config snapshot", e);
        }
    }

    private String buildReusedConfigSnapshot(
            String sourceConfigSnapshot,
            String submissionFingerprint,
            int submissionCount,
            int pairCount,
            int thresholdScore,
            String plagiarismMode,
            Long sourceJobId,
            int clonedPairCount
    ) {
        Map<String, Object> snapshot = parseSnapshot(sourceConfigSnapshot);
        if (snapshot == null) {
            snapshot = new HashMap<>();
        } else {
            snapshot = new HashMap<>(snapshot);
        }
        snapshot.put("algoVersion", resolveAlgoVersion(plagiarismMode));
        snapshot.put("plagiarismMode", plagiarismMode);
        snapshot.put("submissionFingerprint", submissionFingerprint);
        snapshot.put("comparableSubmissionCount", submissionCount);
        snapshot.put("comparablePairCount", pairCount);
        snapshot.put("thresholdScore", thresholdScore);
        snapshot.put("executionMode", "REUSED");
        snapshot.put("reusedFromJobId", sourceJobId);

        Map<String, Object> executionStats = toMutableMap(snapshot.get("executionStats"));
        executionStats.putIfAbsent("sizeSkippedPairs", 0);
        executionStats.putIfAbsent("bucketSkippedPairs", 0);
        executionStats.putIfAbsent("fullCalculatedPairs", 0);
        executionStats.put("thresholdMatchedPairs", clonedPairCount);
        snapshot.put("executionStats", executionStats);
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build reused job config snapshot", e);
        }
    }

    private TeacherPlagiarismJobSummaryView buildJobSummaryView(PlagiarismJob job) {
        Map<String, Object> params = parseSnapshot(job.getParamsJson());
        TeacherPlagiarismJobStatsView statsView = buildJobStatsView(job);
        return new TeacherPlagiarismJobSummaryView(
                job.getId(),
                job.getStatus(),
                job.getProgressTotal(),
                job.getProgressDone(),
                job.getCreateTime(),
                toInteger(params == null ? null : params.get("thresholdScore")),
                toInteger(params == null ? null : params.get("topKPerStudent")),
                params == null ? "FAST" : String.valueOf(params.getOrDefault("plagiarismMode", "FAST")),
                statsView.executionMode(),
                statsView.reusedFromJobId(),
                statsView.thresholdMatchedPairs()
        );
    }

    private String normalizePlagiarismMode(String plagiarismMode) {
        String value = plagiarismMode == null ? "" : plagiarismMode.trim().toUpperCase(Locale.ROOT);
        return DEEP_MODE.equals(value) ? DEEP_MODE : FAST_MODE;
    }

    private String resolveJobPlagiarismMode(PlagiarismJob job) {
        Map<String, Object> params = parseSnapshot(job == null ? null : job.getParamsJson());
        Object mode = params == null ? null : params.get("plagiarismMode");
        return normalizePlagiarismMode(mode == null ? null : String.valueOf(mode));
    }

    private String resolveAlgoVersion(String plagiarismMode) {
        return DEEP_MODE.equals(normalizePlagiarismMode(plagiarismMode)) ? DEEP_ALGO_VERSION : FAST_ALGO_VERSION;
    }

    private TeacherPlagiarismJobStatsView buildJobStatsView(PlagiarismJob job) {
        Map<String, Object> snapshot = parseSnapshot(job.getConfigSnapshot());
        Map<String, Object> executionStats = snapshot == null ? new HashMap<>() : toMutableMap(snapshot.get("executionStats"));
        return new TeacherPlagiarismJobStatsView(
                toInteger(snapshot == null ? null : snapshot.get("comparableSubmissionCount")),
                toInteger(snapshot == null ? null : snapshot.get("comparablePairCount")),
                toInteger(executionStats.get("sizeSkippedPairs")),
                toInteger(executionStats.get("bucketSkippedPairs")),
                toInteger(executionStats.get("fullCalculatedPairs")),
                toInteger(executionStats.get("thresholdMatchedPairs")),
                snapshot == null ? "ASYNC" : String.valueOf(snapshot.getOrDefault("executionMode", "ASYNC")),
                toLong(snapshot == null ? null : snapshot.get("reusedFromJobId"))
        );
    }

    private PairCompareBundle buildPairCompareBundle(SimilarityPair pair) {
        PlagiarismJob job = plagiarismJobMapper.selectById(pair.getJobId());
        if (job == null || job.getAssignmentId() == null) {
            return buildEmptyPairCompareBundle(pair);
        }
        String plagiarismMode = resolveJobPlagiarismMode(job);

        QueryWrapper<Submission> wrapper = new QueryWrapper<>();
        wrapper.eq("assignment_id", job.getAssignmentId())
                .eq("is_latest", 1)
                .eq("is_valid", 1)
                .in("student_id", List.of(pair.getStudentA(), pair.getStudentB()))
                .orderByDesc("version")
                .orderByAsc("id");
        List<Submission> submissions = submissionMapper.selectList(wrapper);
        if (submissions.isEmpty()) {
            return buildEmptyPairCompareBundle(pair);
        }

        Map<Long, Submission> submissionByStudent = new LinkedHashMap<>();
        for (Submission submission : submissions) {
            submissionByStudent.putIfAbsent(submission.getStudentId(), submission);
        }

        Submission leftSubmission = submissionByStudent.get(pair.getStudentA());
        Submission rightSubmission = submissionByStudent.get(pair.getStudentB());
        if (leftSubmission == null || rightSubmission == null) {
            return buildEmptyPairCompareBundle(pair);
        }

        List<SubmissionFile> files = submissionFileMapper.selectBySubmissionIds(List.of(leftSubmission.getId(), rightSubmission.getId()));
        Map<Long, List<SubmissionFile>> filesBySubmissionId = new LinkedHashMap<>();
        for (SubmissionFile file : files) {
            filesBySubmissionId.computeIfAbsent(file.getSubmissionId(), key -> new ArrayList<>()).add(file);
        }

        List<FilePaneAssembly> leftFiles = buildFilePaneAssemblies(filesBySubmissionId.get(leftSubmission.getId()), pair.getStudentA());
        List<FilePaneAssembly> rightFiles = buildFilePaneAssemblies(filesBySubmissionId.get(rightSubmission.getId()), pair.getStudentB());
        if (leftFiles.isEmpty() || rightFiles.isEmpty()) {
            TeacherPlagiarismCodeCompareView fallback = buildAggregateCodeCompare(
                    filesBySubmissionId.get(leftSubmission.getId()),
                    filesBySubmissionId.get(rightSubmission.getId()),
                    pair,
                    plagiarismMode
            );
            return new PairCompareBundle(
                    fallback,
                    fallback == null ? List.of() : List.of(fallback),
                    buildUnmatchedFileViews(leftFiles, "left"),
                    buildUnmatchedFileViews(rightFiles, "right"),
                    List.of()
            );
        }

        List<TeacherComparableCodeBlock> leftBlocks = leftFiles.stream().flatMap(item -> item.blocks().stream()).toList();
        List<TeacherComparableCodeBlock> rightBlocks = rightFiles.stream().flatMap(item -> item.blocks().stream()).toList();
        List<TeacherCodeSegmentView> segments = codeSegmentLocator.locate(leftBlocks, rightBlocks, pair.getScore(), plagiarismMode);
        if (segments.isEmpty()) {
            TeacherPlagiarismCodeCompareView fallback = buildAggregateCodeCompare(
                    filesBySubmissionId.get(leftSubmission.getId()),
                    filesBySubmissionId.get(rightSubmission.getId()),
                    pair,
                    plagiarismMode
            );
            return new PairCompareBundle(
                    fallback,
                    fallback == null ? List.of() : List.of(fallback),
                    buildUnmatchedFileViews(leftFiles, "left"),
                    buildUnmatchedFileViews(rightFiles, "right"),
                    List.of()
            );
        }

        Map<String, FilePaneAssembly> leftByFile = new LinkedHashMap<>();
        for (FilePaneAssembly item : leftFiles) {
            leftByFile.put(item.fileName(), item);
        }
        Map<String, FilePaneAssembly> rightByFile = new LinkedHashMap<>();
        for (FilePaneAssembly item : rightFiles) {
            rightByFile.put(item.fileName(), item);
        }

        Map<String, List<TeacherCodeSegmentView>> segmentsByPair = new LinkedHashMap<>();
        for (TeacherCodeSegmentView segment : segments) {
            String key = buildFilePairKey(segment.leftFile(), segment.rightFile());
            segmentsByPair.computeIfAbsent(key, ignored -> new ArrayList<>()).add(segment);
        }

        Map<String, Integer> leftPairUsage = new HashMap<>();
        Map<String, Integer> rightPairUsage = new HashMap<>();
        for (String key : segmentsByPair.keySet()) {
            String[] parts = splitFilePairKey(key);
            leftPairUsage.merge(parts[0], 1, Integer::sum);
            rightPairUsage.merge(parts[1], 1, Integer::sum);
        }

        List<TeacherPlagiarismCodeCompareView> matchedFilePairs = new ArrayList<>();
        List<TeacherPlagiarismCodeCompareView> crossFileSegments = new ArrayList<>();
        Set<String> usedLeftFiles = new LinkedHashSet<>();
        Set<String> usedRightFiles = new LinkedHashSet<>();

        for (Map.Entry<String, List<TeacherCodeSegmentView>> entry : segmentsByPair.entrySet()) {
            String[] parts = splitFilePairKey(entry.getKey());
            String leftFileName = parts[0];
            String rightFileName = parts[1];
            FilePaneAssembly leftFile = leftByFile.get(leftFileName);
            FilePaneAssembly rightFile = rightByFile.get(rightFileName);
            if (leftFile == null || rightFile == null) {
                continue;
            }
            usedLeftFiles.add(leftFileName);
            usedRightFiles.add(rightFileName);
            TeacherPlagiarismCodeCompareView compareView = buildFilePairCompareView(
                    leftFile,
                    rightFile,
                    entry.getValue(),
                    leftPairUsage.getOrDefault(leftFileName, 0) > 1 || rightPairUsage.getOrDefault(rightFileName, 0) > 1
                            ? "CROSS_FILE"
                            : "MATCHED_PAIR"
            );
            if ("CROSS_FILE".equals(compareView.relationType())) {
                crossFileSegments.add(compareView);
            } else {
                matchedFilePairs.add(compareView);
            }
        }

        matchedFilePairs.sort(comparePriorityComparator());
        crossFileSegments.sort(comparePriorityComparator());

        List<TeacherPlagiarismUnmatchedFileView> unmatchedLeftFiles = buildUnmatchedFileViews(
                leftFiles.stream().filter(item -> !usedLeftFiles.contains(item.fileName())).toList(),
                "left"
        );
        List<TeacherPlagiarismUnmatchedFileView> unmatchedRightFiles = buildUnmatchedFileViews(
                rightFiles.stream().filter(item -> !usedRightFiles.contains(item.fileName())).toList(),
                "right"
        );

        TeacherPlagiarismCodeCompareView primaryCompare = !matchedFilePairs.isEmpty()
                ? matchedFilePairs.get(0)
                : (!crossFileSegments.isEmpty() ? crossFileSegments.get(0) : buildAggregateCodeCompare(
                        filesBySubmissionId.get(leftSubmission.getId()),
                        filesBySubmissionId.get(rightSubmission.getId()),
                        pair,
                        plagiarismMode
                ));

        return new PairCompareBundle(primaryCompare, matchedFilePairs, unmatchedLeftFiles, unmatchedRightFiles, crossFileSegments);
    }

    private PairCompareBundle buildEmptyPairCompareBundle(SimilarityPair pair) {
        TeacherPlagiarismCodeCompareView fallback = new TeacherPlagiarismCodeCompareView(
                "empty",
                "暂无可对比代码",
                "MATCHED_PAIR",
                new TeacherCodePaneView("学生 " + pair.getStudentA(), "// 暂无可展示的代码内容", List.of()),
                new TeacherCodePaneView("学生 " + pair.getStudentB(), "// 暂无可展示的代码内容", List.of()),
                List.of()
        );
        return new PairCompareBundle(fallback, List.of(fallback), List.of(), List.of(), List.of());
    }

    private TeacherPlagiarismCodeCompareView buildAggregateCodeCompare(
            List<SubmissionFile> leftFiles,
            List<SubmissionFile> rightFiles,
            SimilarityPair pair,
            String plagiarismMode
    ) {
        CodePaneAssembly leftPane = buildCodePane(leftFiles, pair.getStudentA());
        CodePaneAssembly rightPane = buildCodePane(rightFiles, pair.getStudentB());
        List<TeacherCodeSegmentView> segments = codeSegmentLocator.locate(leftPane.blocks(), rightPane.blocks(), pair.getScore(), plagiarismMode);
        List<TeacherCodeHighlightRangeView> leftHighlights = buildHighlightsFromSegments(segments, true);
        List<TeacherCodeHighlightRangeView> rightHighlights = buildHighlightsFromSegments(segments, false);
        if (leftHighlights.isEmpty() || rightHighlights.isEmpty()) {
            leftHighlights = buildHighlightRanges(leftPane.code(), rightPane.code());
            rightHighlights = buildHighlightRanges(rightPane.code(), leftPane.code());
        }
        return new TeacherPlagiarismCodeCompareView(
                "aggregate",
                "完整代码整体对比",
                "MATCHED_PAIR",
                new TeacherCodePaneView(leftPane.title(), leftPane.code(), leftHighlights),
                new TeacherCodePaneView(rightPane.title(), rightPane.code(), rightHighlights),
                segments
        );
    }

    private List<FilePaneAssembly> buildFilePaneAssemblies(List<SubmissionFile> files, Long studentId) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        return files.stream()
                .filter(file -> "OK".equalsIgnoreCase(file.getParseStatus()) && file.getStoragePath() != null && !file.getStoragePath().isBlank())
                .sorted(Comparator.comparing(SubmissionFile::getFilename, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(file -> buildFilePaneAssembly(file, studentId))
                .filter(Objects::nonNull)
                .toList();
    }

    private FilePaneAssembly buildFilePaneAssembly(SubmissionFile file, Long studentId) {
        String source = safeReadSource(file.getStoragePath());
        List<TeacherComparableCodeBlock> blocks = codeBlockExtractor.extract(file.getFilename(), source, 1);
        TeacherCodePaneView pane = new TeacherCodePaneView(file.getFilename(), source, List.of());
        return new FilePaneAssembly(file.getFilename(), pane, blocks, studentId);
    }

    private TeacherPlagiarismCodeCompareView buildFilePairCompareView(
            FilePaneAssembly leftFile,
            FilePaneAssembly rightFile,
            List<TeacherCodeSegmentView> rawSegments,
            String relationType
    ) {
        List<TeacherCodeSegmentView> labeledSegments = relabelSegments(rawSegments);
        List<TeacherCodeHighlightRangeView> leftHighlights = buildHighlightsFromSegments(labeledSegments, true);
        List<TeacherCodeHighlightRangeView> rightHighlights = buildHighlightsFromSegments(labeledSegments, false);
        if (leftHighlights.isEmpty() || rightHighlights.isEmpty()) {
            leftHighlights = buildHighlightRanges(leftFile.pane().code(), rightFile.pane().code());
            rightHighlights = buildHighlightRanges(rightFile.pane().code(), leftFile.pane().code());
        }

        return new TeacherPlagiarismCodeCompareView(
                buildFilePairKey(leftFile.fileName(), rightFile.fileName()),
                leftFile.fileName() + " ↔ " + rightFile.fileName(),
                relationType,
                new TeacherCodePaneView(leftFile.pane().title(), leftFile.pane().code(), leftHighlights),
                new TeacherCodePaneView(rightFile.pane().title(), rightFile.pane().code(), rightHighlights),
                labeledSegments
        );
    }

    private List<TeacherCodeSegmentView> relabelSegments(List<TeacherCodeSegmentView> segments) {
        List<TeacherCodeSegmentView> normalized = new ArrayList<>();
        for (int index = 0; index < segments.size(); index++) {
            TeacherCodeSegmentView segment = segments.get(index);
            normalized.add(new TeacherCodeSegmentView(
                    segment.id() != null && !segment.id().isBlank() ? segment.id() : "seg-" + (index + 1),
                    segment.label() != null && !segment.label().isBlank() ? segment.label() : "片段 " + (index + 1),
                    segment.summary(),
                    segment.score(),
                    segment.leftFile(),
                    segment.leftStartLine(),
                    segment.leftEndLine(),
                    segment.rightFile(),
                    segment.rightStartLine(),
                    segment.rightEndLine()
            ));
        }
        return normalized;
    }

    private List<TeacherPlagiarismUnmatchedFileView> buildUnmatchedFileViews(List<FilePaneAssembly> files, String side) {
        List<TeacherPlagiarismUnmatchedFileView> result = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            FilePaneAssembly file = files.get(index);
            result.add(new TeacherPlagiarismUnmatchedFileView(
                    side + "-only-" + (index + 1),
                    ("left".equals(side) ? "仅学生 A 包含" : "仅学生 B 包含") + " · " + file.fileName(),
                    file.pane()
            ));
        }
        return result;
    }

    private Comparator<TeacherPlagiarismCodeCompareView> comparePriorityComparator() {
        return Comparator
                .comparingInt((TeacherPlagiarismCodeCompareView view) -> highestSegmentScore(view.segments())).reversed()
                .thenComparing(view -> view.left().title(), Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(view -> view.right().title(), Comparator.nullsLast(String::compareToIgnoreCase));
    }

    private int highestSegmentScore(List<TeacherCodeSegmentView> segments) {
        return segments == null ? 0 : segments.stream()
                .map(TeacherCodeSegmentView::score)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private String buildFilePairKey(String leftFile, String rightFile) {
        return (leftFile == null ? "-" : leftFile) + "::" + (rightFile == null ? "-" : rightFile);
    }

    private String[] splitFilePairKey(String key) {
        String[] parts = String.valueOf(key).split("::", 2);
        if (parts.length == 2) {
            return parts;
        }
        return new String[] { key, "-" };
    }

    private String buildFullCode(List<SubmissionFile> files) {
        if (files == null || files.isEmpty()) {
            return "// 暂无可用于对比的代码文件";
        }
        List<SubmissionFile> orderedFiles = files.stream()
                .filter(file -> "OK".equalsIgnoreCase(file.getParseStatus()) && file.getStoragePath() != null && !file.getStoragePath().isBlank())
                .sorted(Comparator.comparing(SubmissionFile::getFilename, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        if (orderedFiles.isEmpty()) {
            return "// 暂无成功解析的代码文件";
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < orderedFiles.size(); index++) {
            SubmissionFile file = orderedFiles.get(index);
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append("// File: ").append(file.getFilename()).append('\n');
            builder.append(safeReadSource(file.getStoragePath()));
        }
        return builder.toString();
    }
    private CodePaneAssembly buildCodePane(List<SubmissionFile> files, Long studentId) {
        if (files == null || files.isEmpty()) {
            return new CodePaneAssembly("学生 " + studentId, "// 暂无可用于对比的代码文件", List.of());
        }
        List<SubmissionFile> orderedFiles = files.stream()
                .filter(file -> "OK".equalsIgnoreCase(file.getParseStatus()) && file.getStoragePath() != null && !file.getStoragePath().isBlank())
                .sorted(Comparator.comparing(SubmissionFile::getFilename, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        if (orderedFiles.isEmpty()) {
            return new CodePaneAssembly(buildPaneTitle(files, studentId), "// 暂无成功解析的代码文件", List.of());
        }

        StringBuilder builder = new StringBuilder();
        List<TeacherComparableCodeBlock> blocks = new ArrayList<>();
        int currentLine = 1;
        for (SubmissionFile file : orderedFiles) {
            if (builder.length() > 0) {
                builder.append("\n\n");
                currentLine += 2;
            }
            builder.append("// File: ").append(file.getFilename()).append('\n');
            String source = safeReadSource(file.getStoragePath());
            int sourceStartLine = currentLine + 1;
            builder.append(source);
            blocks.addAll(codeBlockExtractor.extract(file.getFilename(), source, sourceStartLine));
            currentLine = sourceStartLine + countLines(source) - 1;
        }
        return new CodePaneAssembly(buildPaneTitle(orderedFiles, studentId), builder.toString(), blocks);
    }

    private String safeReadSource(String storagePath) {
        try {
            return localStorageService.readText(storagePath);
        } catch (BusinessException ex) {
            return "// 读取源代码失败：" + ex.getMessage();
        }
    }

    private String buildPaneTitle(List<SubmissionFile> files, Long studentId) {
        if (files == null || files.isEmpty()) {
            return "学生 " + studentId;
        }
        List<SubmissionFile> orderedFiles = files.stream()
                .sorted(Comparator.comparing(SubmissionFile::getFilename, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        if (orderedFiles.size() == 1) {
            return orderedFiles.get(0).getFilename();
        }
        return orderedFiles.get(0).getFilename() + " 等 " + orderedFiles.size() + " 个文件";
    }

    private List<TeacherCodeHighlightRangeView> buildHighlightRanges(String source, String otherSource) {
        String[] lines = splitLines(source);
        String[] otherLines = splitLines(otherSource);
        if (lines.length == 0 || otherLines.length == 0) {
            return List.of();
        }

        Set<String> sharedNormalizedLines = new HashSet<>();
        for (String line : otherLines) {
            String normalized = normalizeComparableLine(line);
            if (normalized != null) {
                sharedNormalizedLines.add(normalized);
            }
        }

        List<Integer> matchedLineNumbers = new ArrayList<>();
        for (int index = 0; index < lines.length; index++) {
            String normalized = normalizeComparableLine(lines[index]);
            if (normalized != null && sharedNormalizedLines.contains(normalized)) {
                matchedLineNumbers.add(index + 1);
            }
        }
        return compressLineNumbers(matchedLineNumbers);
    }

    private String[] splitLines(String content) {
        if (content == null || content.isBlank()) {
            return new String[0];
        }
        return content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }

    private String normalizeComparableLine(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
            return null;
        }
        String compact = trimmed.replaceAll("\\s+", "");
        if (compact.length() < 8) {
            return null;
        }
        String lower = compact.toLowerCase(Locale.ROOT);
        if (lower.startsWith("package") || lower.startsWith("import")) {
            return null;
        }
        if (compact.matches("[{}();,]+")) {
            return null;
        }
        return compact;
    }

    private List<TeacherCodeHighlightRangeView> compressLineNumbers(List<Integer> lineNumbers) {
        if (lineNumbers.isEmpty()) {
            return List.of();
        }
        List<TeacherCodeHighlightRangeView> ranges = new ArrayList<>();
        int start = lineNumbers.get(0);
        int previous = start;
        for (int index = 1; index < lineNumbers.size(); index++) {
            int current = lineNumbers.get(index);
            if (current == previous + 1) {
                previous = current;
                continue;
            }
            ranges.add(new TeacherCodeHighlightRangeView(start, previous));
            start = current;
            previous = current;
        }
        ranges.add(new TeacherCodeHighlightRangeView(start, previous));
        return ranges;
    }

    private int countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 1;
        }
        return splitLines(content).length;
    }

    private List<TeacherCodeHighlightRangeView> buildHighlightsFromSegments(
            List<TeacherCodeSegmentView> segments,
            boolean useLeft
    ) {
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }
        List<TeacherCodeHighlightRangeView> ranges = new ArrayList<>();
        for (TeacherCodeSegmentView segment : segments) {
            ranges.add(useLeft
                    ? new TeacherCodeHighlightRangeView(segment.leftStartLine(), segment.leftEndLine())
                    : new TeacherCodeHighlightRangeView(segment.rightStartLine(), segment.rightEndLine()));
        }
        return compressRanges(ranges);
    }

    private List<TeacherCodeHighlightRangeView> compressRanges(List<TeacherCodeHighlightRangeView> ranges) {
        if (ranges.isEmpty()) {
            return List.of();
        }
        List<TeacherCodeHighlightRangeView> sortedRanges = ranges.stream()
                .sorted(Comparator.comparing(TeacherCodeHighlightRangeView::startLine)
                        .thenComparing(TeacherCodeHighlightRangeView::endLine))
                .toList();
        List<TeacherCodeHighlightRangeView> compressed = new ArrayList<>();
        int currentStart = sortedRanges.get(0).startLine();
        int currentEnd = sortedRanges.get(0).endLine();
        for (int index = 1; index < sortedRanges.size(); index++) {
            TeacherCodeHighlightRangeView range = sortedRanges.get(index);
            if (range.startLine() <= currentEnd + 1) {
                currentEnd = Math.max(currentEnd, range.endLine());
                continue;
            }
            compressed.add(new TeacherCodeHighlightRangeView(currentStart, currentEnd));
            currentStart = range.startLine();
            currentEnd = range.endLine();
        }
        compressed.add(new TeacherCodeHighlightRangeView(currentStart, currentEnd));
        return compressed;
    }

    private record CodePaneAssembly(
            String title,
            String code,
            List<TeacherComparableCodeBlock> blocks
    ) {
    }

    private record FilePaneAssembly(
            String fileName,
            TeacherCodePaneView pane,
            List<TeacherComparableCodeBlock> blocks,
            Long studentId
    ) {
    }

    private record PairCompareBundle(
            TeacherPlagiarismCodeCompareView primaryCompare,
            List<TeacherPlagiarismCodeCompareView> matchedFilePairs,
            List<TeacherPlagiarismUnmatchedFileView> unmatchedLeftFiles,
            List<TeacherPlagiarismUnmatchedFileView> unmatchedRightFiles,
            List<TeacherPlagiarismCodeCompareView> crossFileSegments
    ) {
        private static PairCompareBundle empty() {
            return new PairCompareBundle(null, List.of(), List.of(), List.of(), List.of());
        }
    }

    private PlagiarismJob findReusableDoneJob(Long assignmentId, String submissionFingerprint, int thresholdScore, String plagiarismMode) {
        QueryWrapper<PlagiarismJob> wrapper = new QueryWrapper<>();
        wrapper.eq("assignment_id", assignmentId).eq("status", "DONE").orderByDesc("id");
        String normalizedMode = normalizePlagiarismMode(plagiarismMode);
        for (PlagiarismJob job : plagiarismJobMapper.selectList(wrapper)) {
            Map<String, Object> snapshot = parseSnapshot(job.getConfigSnapshot());
            if (snapshot == null) {
                continue;
            }
            Object algoVersion = snapshot.get("algoVersion");
            Object fingerprint = snapshot.get("submissionFingerprint");
            Integer previousThreshold = toInteger(snapshot.get("thresholdScore"));
            String previousMode = normalizePlagiarismMode(snapshot.get("plagiarismMode") == null ? null : String.valueOf(snapshot.get("plagiarismMode")));
            if (!resolveAlgoVersion(normalizedMode).equals(algoVersion)) {
                continue;
            }
            if (!normalizedMode.equals(previousMode)) {
                continue;
            }
            if (!submissionFingerprint.equals(fingerprint)) {
                continue;
            }
            if (previousThreshold == null || thresholdScore < previousThreshold) {
                continue;
            }
            return job;
        }
        return null;
    }

    private int cloneJobResults(Long sourceJobId, Long targetJobId, int thresholdScore) {
        QueryWrapper<SimilarityPair> pairWrapper = new QueryWrapper<>();
        pairWrapper.eq("job_id", sourceJobId).ge("score", thresholdScore).orderByDesc("score").orderByAsc("id");
        List<SimilarityPair> sourcePairs = similarityPairMapper.selectList(pairWrapper);
        if (sourcePairs.isEmpty()) {
            return 0;
        }

        List<SimilarityPair> clonedPairs = new ArrayList<>();
        Map<Long, Long> sourceToNewPairId = new HashMap<>();
        for (SimilarityPair sourcePair : sourcePairs) {
            SimilarityPair clonedPair = new SimilarityPair();
            clonedPair.setJobId(targetJobId);
            clonedPair.setStudentA(sourcePair.getStudentA());
            clonedPair.setStudentB(sourcePair.getStudentB());
            clonedPair.setScore(sourcePair.getScore());
            clonedPair.setStatus(sourcePair.getStatus());
            clonedPair.setTeacherNote(sourcePair.getTeacherNote());
            clonedPair.setCreateTime(LocalDateTime.now());
            clonedPairs.add(clonedPair);
        }
        similarityPairMapper.batchInsert(clonedPairs);
        for (int index = 0; index < sourcePairs.size(); index++) {
            sourceToNewPairId.put(sourcePairs.get(index).getId(), clonedPairs.get(index).getId());
        }

        QueryWrapper<SimilarityEvidence> evidenceWrapper = new QueryWrapper<>();
        evidenceWrapper.in("pair_id", sourceToNewPairId.keySet()).orderByDesc("weight").orderByAsc("id");
        List<SimilarityEvidence> sourceEvidences = similarityEvidenceMapper.selectList(evidenceWrapper);
        if (sourceEvidences.isEmpty()) {
            return clonedPairs.size();
        }

        List<SimilarityEvidence> clonedEvidences = new ArrayList<>();
        for (SimilarityEvidence sourceEvidence : sourceEvidences) {
            SimilarityEvidence clonedEvidence = new SimilarityEvidence();
            clonedEvidence.setPairId(sourceToNewPairId.get(sourceEvidence.getPairId()));
            clonedEvidence.setType(sourceEvidence.getType());
            clonedEvidence.setSummary(sourceEvidence.getSummary());
            clonedEvidence.setWeight(sourceEvidence.getWeight());
            clonedEvidence.setPayloadJson(sourceEvidence.getPayloadJson());
            clonedEvidences.add(clonedEvidence);
        }
        similarityEvidenceMapper.batchInsert(clonedEvidences);
        return clonedPairs.size();
    }

    private Map<String, Object> parseSnapshot(String configSnapshot) {
        if (configSnapshot == null || configSnapshot.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(configSnapshot, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Map<String, Object> buildExecutionStatsMap(int sizeSkippedPairs, int bucketSkippedPairs, int fullCalculatedPairs, int thresholdMatchedPairs) {
        Map<String, Object> executionStats = new HashMap<>();
        executionStats.put("sizeSkippedPairs", sizeSkippedPairs);
        executionStats.put("bucketSkippedPairs", bucketSkippedPairs);
        executionStats.put("fullCalculatedPairs", fullCalculatedPairs);
        executionStats.put("thresholdMatchedPairs", thresholdMatchedPairs);
        return executionStats;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMutableMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return new HashMap<>((Map<String, Object>) map);
        }
        return new HashMap<>();
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private List<SimilarityPair> listPairs(Long jobId) {
        QueryWrapper<SimilarityPair> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("job_id", jobId).orderByDesc("score").orderByAsc("id");
        return similarityPairMapper.selectList(queryWrapper);
    }

    private List<TeacherPlagiarismPairView> buildFilteredPairViews(
            List<SimilarityPair> pairs,
            int minScore,
            int perStudentTopK,
            String statuses,
            String sortBy,
            String sortDirection
    ) {
        if (pairs == null || pairs.isEmpty()) {
            return List.of();
        }

        Set<String> statusFilter = parseStatusFilter(statuses);
        Map<Long, Integer> studentPairCount = new HashMap<>();
        List<TeacherPlagiarismPairView> result = new ArrayList<>();
        for (SimilarityPair pair : pairs) {
            if (pair.getScore() == null || pair.getScore() < minScore) {
                continue;
            }
            String normalizedStatus = normalizePairStatus(pair.getStatus());
            if (!statusFilter.isEmpty() && !statusFilter.contains(normalizedStatus)) {
                continue;
            }
            boolean canIncludeA = studentPairCount.getOrDefault(pair.getStudentA(), 0) < perStudentTopK;
            boolean canIncludeB = studentPairCount.getOrDefault(pair.getStudentB(), 0) < perStudentTopK;
            if (!canIncludeA || !canIncludeB) {
                continue;
            }
            result.add(new TeacherPlagiarismPairView(
                    pair.getId(),
                    pair.getStudentA(),
                    pair.getStudentB(),
                    pair.getScore(),
                    normalizedStatus,
                    pair.getTeacherNote()
            ));
            studentPairCount.merge(pair.getStudentA(), 1, Integer::sum);
            studentPairCount.merge(pair.getStudentB(), 1, Integer::sum);
        }
        result.sort(buildPairViewComparator(sortBy, sortDirection));
        return result;
    }

    private List<TeacherPlagiarismEvidenceView> listEvidenceViews(Long pairId) {
        QueryWrapper<SimilarityEvidence> evidenceQuery = new QueryWrapper<>();
        evidenceQuery.eq("pair_id", pairId).orderByDesc("weight").orderByAsc("id");
        return similarityEvidenceMapper.selectList(evidenceQuery).stream()
                .map(evidence -> new TeacherPlagiarismEvidenceView(
                        evidence.getId(),
                        evidence.getType(),
                        evidence.getSummary(),
                        evidence.getWeight(),
                        evidence.getPayloadJson()
                ))
                .toList();
    }

    private List<TeacherIncomparableSubmissionView> listIncomparableSubmissions(Long assignmentId) {
        QueryWrapper<Submission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("assignment_id", assignmentId).eq("is_latest", 1).orderByDesc("submit_time");
        List<Submission> submissions = submissionMapper.selectList(queryWrapper);
        if (submissions == null || submissions.isEmpty()) {
            return List.of();
        }

        List<TeacherIncomparableSubmissionView> result = new ArrayList<>();
        for (Submission submission : submissions) {
            int parseOkFiles = submission.getParseOkFiles() == null ? 0 : submission.getParseOkFiles();
            boolean comparable = submission.getIsValid() != null && submission.getIsValid() == 1 && parseOkFiles >= 1;
            if (comparable) {
                continue;
            }
            result.add(new TeacherIncomparableSubmissionView(
                    submission.getId(),
                    submission.getClassId(),
                    submission.getStudentId(),
                    submission.getVersion(),
                    buildIncomparableReason(submission),
                    parseOkFiles,
                    submission.getTotalFiles(),
                    listParseFailures(submission.getId())
            ));
        }
        return result;
    }

    private List<TeacherParseFailureView> listParseFailures(Long submissionId) {
        QueryWrapper<SubmissionFile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("submission_id", submissionId).orderByAsc("id");
        return submissionFileMapper.selectList(queryWrapper).stream()
                .filter(file -> !"OK".equalsIgnoreCase(file.getParseStatus()))
                .map(file -> new TeacherParseFailureView(
                        file.getFilename(),
                        file.getParseError() == null || file.getParseError().isBlank() ? file.getParseStatus() : file.getParseError()
                ))
                .toList();
    }

    private String buildIncomparableReason(Submission submission) {
        int parseOkFiles = submission.getParseOkFiles() == null ? 0 : submission.getParseOkFiles();
        if (parseOkFiles < 1) {
            return "parse_ok_files=0";
        }
        if (submission.getIsValid() != null && submission.getIsValid() == 0) {
            return "submission_marked_invalid";
        }
        return "not_comparable";
    }

    private SimilarityEvidence requirePrimaryEvidence(Long pairId) {
        QueryWrapper<SimilarityEvidence> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pair_id", pairId).orderByDesc("weight").orderByAsc("id").last("LIMIT 1");
        SimilarityEvidence evidence = similarityEvidenceMapper.selectOne(queryWrapper);
        if (evidence == null) {
            throw new BusinessException("相似结果证据不存在");
        }
        return evidence;
    }

    private SimilarityPair requireTeacherPair(Long teacherId, Long pairId) {
        SimilarityPair pair = similarityPairMapper.selectById(pairId);
        if (pair == null) {
            throw new BusinessException("相似结果不存在");
        }
        PlagiarismJob job = plagiarismJobMapper.selectById(pair.getJobId());
        if (job == null) {
            throw new BusinessException("Plagiarism job does not exist");
        }
        requireTeacherAssignment(teacherId, job.getAssignmentId());
        return pair;
    }

    private Assignment requireTeacherAssignment(Long teacherId, Long assignmentId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("Assignment does not exist");
        }
        if (!teacherId.equals(assignment.getTeacherId())) {
            throw new BusinessException(403, "No permission to access this assignment");
        }
        return assignment;
    }

    private String buildReportMessage(String jobStatus, boolean noPairs) {
        if ("QUEUED".equalsIgnoreCase(jobStatus)) {
            return "plagiarism job is queued";
        }
        if ("RUNNING".equalsIgnoreCase(jobStatus)) {
            return "plagiarism job is still running";
        }
        if ("FAILED".equalsIgnoreCase(jobStatus)) {
            return "plagiarism job failed";
        }
        return noPairs ? "plagiarism job finished with no matched pairs" : "plagiarism job finished";
    }

    private String csv(Object value) {
        if (value == null) {
            return "\"\"";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private Set<String> parseStatusFilter(String statuses) {
        if (statuses == null || statuses.isBlank()) {
            return Set.of();
        }
        Set<String> parsed = new java.util.LinkedHashSet<>();
        for (String item : statuses.split(",")) {
            String normalized = normalizePairStatus(item);
            if (ALLOWED_PAIR_STATUS.contains(normalized)) {
                parsed.add(normalized);
            }
        }
        return parsed;
    }

    private String normalizePairStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSortBy(String sortBy) {
        String normalized = sortBy == null ? "" : sortBy.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "status", "studenta", "studentb", "pairid" -> normalized;
            default -> "score";
        };
    }

    private String normalizeSortDirection(String sortDirection) {
        return "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";
    }

    private Comparator<TeacherPlagiarismPairView> buildPairViewComparator(String sortBy, String sortDirection) {
        String normalizedSortBy = normalizeSortBy(sortBy);
        Comparator<TeacherPlagiarismPairView> comparator = switch (normalizedSortBy) {
            case "status" -> Comparator
                    .comparing(TeacherPlagiarismPairView::status, Comparator.nullsLast(String::compareTo))
                    .thenComparing(TeacherPlagiarismPairView::score, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(TeacherPlagiarismPairView::pairId, Comparator.nullsLast(Long::compareTo));
            case "studenta" -> Comparator
                    .comparing(TeacherPlagiarismPairView::studentA, Comparator.nullsLast(Long::compareTo))
                    .thenComparing(TeacherPlagiarismPairView::score, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(TeacherPlagiarismPairView::pairId, Comparator.nullsLast(Long::compareTo));
            case "studentb" -> Comparator
                    .comparing(TeacherPlagiarismPairView::studentB, Comparator.nullsLast(Long::compareTo))
                    .thenComparing(TeacherPlagiarismPairView::score, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(TeacherPlagiarismPairView::pairId, Comparator.nullsLast(Long::compareTo));
            case "pairid" -> Comparator.comparing(TeacherPlagiarismPairView::pairId, Comparator.nullsLast(Long::compareTo));
            default -> Comparator
                    .comparing(TeacherPlagiarismPairView::score, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(TeacherPlagiarismPairView::pairId, Comparator.nullsLast(Long::compareTo));
        };
        return "asc".equals(normalizeSortDirection(sortDirection)) ? comparator : comparator.reversed();
    }

    private void writeZipEntry(ZipOutputStream zipOutputStream, String fileName, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(fileName));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private String buildReportSummaryText(
            TeacherPlagiarismJobReport report,
            String statuses,
            String sortBy,
            String sortDirection
    ) {
        Map<String, Long> statusCountMap = new LinkedHashMap<>();
        statusCountMap.put("PENDING", report.pairs().stream().filter(item -> "PENDING".equalsIgnoreCase(item.status())).count());
        statusCountMap.put("CONFIRMED", report.pairs().stream().filter(item -> "CONFIRMED".equalsIgnoreCase(item.status())).count());
        statusCountMap.put("FALSE_POSITIVE", report.pairs().stream().filter(item -> "FALSE_POSITIVE".equalsIgnoreCase(item.status())).count());
        StringBuilder builder = new StringBuilder();
        builder.append("Plagiarism report summary").append("\r\n")
                .append("Job ID: ").append(report.jobId()).append("\r\n")
                .append("Job status: ").append(report.status()).append("\r\n")
                .append("Message: ").append(report.message()).append("\r\n")
                .append("Minimum score: ").append(report.minScore()).append("\r\n")
                .append("Per student TopK: ").append(report.perStudentTopK()).append("\r\n")
                .append("Status filter: ").append(statuses == null || statuses.isBlank() ? "ALL" : statuses).append("\r\n")
                .append("Sort by: ").append(normalizeSortBy(sortBy)).append("\r\n")
                .append("Sort direction: ").append(normalizeSortDirection(sortDirection)).append("\r\n")
                .append("Matched pair count: ").append(report.pairs().size()).append("\r\n")
                .append("Incomparable submission count: ").append(report.incomparableSubmissions().size()).append("\r\n");
        if (report.jobStats() != null) {
            builder.append("Execution mode: ").append(report.jobStats().executionMode()).append("\r\n")
                    .append("Comparable submissions: ").append(report.jobStats().comparableSubmissionCount()).append("\r\n")
                    .append("Comparable pairs: ").append(report.jobStats().comparablePairCount()).append("\r\n")
                    .append("Calculated pairs: ").append(report.jobStats().fullCalculatedPairs()).append("\r\n")
                    .append("Threshold matched pairs: ").append(report.jobStats().thresholdMatchedPairs()).append("\r\n");
        }
        builder.append("Status distribution: ")
                .append("PENDING=").append(statusCountMap.get("PENDING")).append(", ")
                .append("CONFIRMED=").append(statusCountMap.get("CONFIRMED")).append(", ")
                .append("FALSE_POSITIVE=").append(statusCountMap.get("FALSE_POSITIVE")).append("\r\n");
        return builder.toString();
    }

    private String buildStatsCsv(TeacherPlagiarismJobReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("jobId,status,minScore,perStudentTopK,message\r\n");
        builder.append(csv(report.jobId()))
                .append(',').append(csv(report.status()))
                .append(',').append(csv(report.minScore()))
                .append(',').append(csv(report.perStudentTopK()))
                .append(',').append(csv(report.message()))
                .append("\r\n");
        if (report.jobStats() != null) {
            builder.append("\r\nexecutionMode,reusedFromJobId,comparableSubmissionCount,comparablePairCount,sizeSkippedPairs,bucketSkippedPairs,fullCalculatedPairs,thresholdMatchedPairs\r\n");
            builder.append(csv(report.jobStats().executionMode()))
                    .append(',').append(csv(report.jobStats().reusedFromJobId()))
                    .append(',').append(csv(report.jobStats().comparableSubmissionCount()))
                    .append(',').append(csv(report.jobStats().comparablePairCount()))
                    .append(',').append(csv(report.jobStats().sizeSkippedPairs()))
                    .append(',').append(csv(report.jobStats().bucketSkippedPairs()))
                    .append(',').append(csv(report.jobStats().fullCalculatedPairs()))
                    .append(',').append(csv(report.jobStats().thresholdMatchedPairs()))
                    .append("\r\n");
        }
        return builder.toString();
    }

    private String buildPairsCsv(TeacherPlagiarismJobReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("pairId,studentA,studentB,score,status,teacherNote\r\n");
        for (TeacherPlagiarismPairView pair : report.pairs()) {
            builder.append(csv(pair.pairId()))
                    .append(',').append(csv(pair.studentA()))
                    .append(',').append(csv(pair.studentB()))
                    .append(',').append(csv(pair.score()))
                    .append(',').append(csv(pair.status()))
                    .append(',').append(csv(pair.teacherNote()))
                    .append("\r\n");
        }
        return builder.toString();
    }

    private String buildIncomparableCsv(TeacherPlagiarismJobReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("submissionId,classId,studentId,version,reason,parseOkFiles,totalFiles,parseFailures\r\n");
        for (TeacherIncomparableSubmissionView item : report.incomparableSubmissions()) {
            String parseFailureSummary = item.parseFailures().stream()
                    .map(failure -> failure.file() + ":" + failure.reason())
                    .reduce((left, right) -> left + " | " + right)
                    .orElse("");
            builder.append(csv(item.submissionId()))
                    .append(',').append(csv(item.classId()))
                    .append(',').append(csv(item.studentId()))
                    .append(',').append(csv(item.version()))
                    .append(',').append(csv(item.reason()))
                    .append(',').append(csv(item.parseOkFiles()))
                    .append(',').append(csv(item.totalFiles()))
                    .append(',').append(csv(parseFailureSummary))
                    .append("\r\n");
        }
        return builder.toString();
    }
}



package com.ast.back.service.impl;

import com.ast.back.common.BusinessException;
import com.ast.back.entity.AiExplanation;
import com.ast.back.entity.Assignment;
import com.ast.back.entity.PlagiarismJob;
import com.ast.back.entity.SimilarityEvidence;
import com.ast.back.entity.SimilarityPair;
import com.ast.back.entity.Submission;
import com.ast.back.entity.SubmissionFile;
import com.ast.back.mapper.AiExplanationMapper;
import com.ast.back.mapper.AssignmentMapper;
import com.ast.back.mapper.PlagiarismJobMapper;
import com.ast.back.mapper.SimilarityEvidenceMapper;
import com.ast.back.mapper.SimilarityPairMapper;
import com.ast.back.mapper.SubmissionFileMapper;
import com.ast.back.mapper.SubmissionMapper;
import com.ast.back.service.AiExplanationService;
import com.ast.back.service.TeacherPlagiarismJobService;
import com.ast.back.service.dto.TeacherIncomparableSubmissionView;
import com.ast.back.service.dto.TeacherParseFailureView;
import com.ast.back.service.dto.TeacherPlagiarismEvidenceView;
import com.ast.back.service.dto.TeacherPlagiarismJobReport;
import com.ast.back.service.dto.TeacherPlagiarismJobStatsView;
import com.ast.back.service.dto.TeacherPlagiarismJobSummaryView;
import com.ast.back.service.dto.TeacherPlagiarismPairDetailView;
import com.ast.back.service.dto.TeacherPlagiarismPairView;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TeacherPlagiarismJobServiceImpl implements TeacherPlagiarismJobService {

    private static final Set<String> ALLOWED_PAIR_STATUS = Set.of("PENDING", "CONFIRMED", "FALSE_POSITIVE");
    private static final int DEFAULT_TOP_K = 10;
    private static final String ALGO_VERSION = "AC_BAG_OF_NODES_V1";

    private final AssignmentMapper assignmentMapper;
    private final PlagiarismJobMapper plagiarismJobMapper;
    private final SimilarityPairMapper similarityPairMapper;
    private final SimilarityEvidenceMapper similarityEvidenceMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionFileMapper submissionFileMapper;
    private final AiExplanationMapper aiExplanationMapper;
    private final TeacherPlagiarismJobDispatcher teacherPlagiarismJobDispatcher;
    private final AiExplanationService aiExplanationService;
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
            AiExplanationService aiExplanationService
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
    }

    @Override
    public PlagiarismJob createSkeletonJob(Long teacherId, Long assignmentId, Integer thresholdScore, Integer topKPerStudent) {
        Assignment assignment = requireTeacherAssignment(teacherId, assignmentId);
        LocalDateTime deadline = assignment.getEndAt() != null ? assignment.getEndAt() : assignment.getDeadline();
        if (deadline != null && LocalDateTime.now().isBefore(deadline)) {
            throw new BusinessException("截止前不能创建查重任务");
        }

        int effectiveThresholdScore = thresholdScore == null ? 80 : thresholdScore;
        int effectiveTopK = topKPerStudent == null ? DEFAULT_TOP_K : topKPerStudent;
        List<Submission> comparableSubmissions = listComparableSubmissions(assignmentId);
        String submissionFingerprint = buildSubmissionFingerprint(comparableSubmissions);
        int comparablePairCount = calculateComparablePairs(comparableSubmissions);
        String configSnapshot = buildConfigSnapshot(submissionFingerprint, comparableSubmissions.size(), comparablePairCount, effectiveThresholdScore);

        PlagiarismJob reusableJob = findReusableDoneJob(assignmentId, submissionFingerprint, effectiveThresholdScore);
        if (reusableJob != null) {
            PlagiarismJob job = new PlagiarismJob();
            job.setAssignmentId(assignmentId);
            job.setStatus("DONE");
            job.setParamsJson("{\"thresholdScore\":" + effectiveThresholdScore + ",\"topKPerStudent\":" + effectiveTopK + ",\"mode\":\"REUSED\",\"sourceJobId\":" + reusableJob.getId() + "}");
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
                    reusableJob.getId(),
                    clonedPairCount
            ));
            plagiarismJobMapper.updateById(job);
            return job;
        }

        PlagiarismJob job = new PlagiarismJob();
        job.setAssignmentId(assignmentId);
        job.setStatus("QUEUED");
        job.setParamsJson("{\"thresholdScore\":" + effectiveThresholdScore + ",\"topKPerStudent\":" + effectiveTopK + ",\"mode\":\"ASYNC\"}");
        job.setConfigSnapshot(configSnapshot);
        job.setProgressTotal(0);
        job.setProgressDone(0);
        job.setCreateTime(LocalDateTime.now());
        plagiarismJobMapper.insert(job);
        teacherPlagiarismJobDispatcher.dispatch(job, effectiveThresholdScore, effectiveTopK);
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
            throw new BusinessException("查重任务不存在");
        }
        requireTeacherAssignment(teacherId, job.getAssignmentId());
        return job;
    }

    @Override
    public TeacherPlagiarismJobReport getReport(Long teacherId, Long jobId, Integer minScore, Integer perStudentTopK) {
        PlagiarismJob job = getJob(teacherId, jobId);
        int effectiveMinScore = minScore == null ? 0 : minScore;
        int effectiveTopK = perStudentTopK == null ? DEFAULT_TOP_K : perStudentTopK;

        List<TeacherPlagiarismPairView> filteredPairs = buildFilteredPairViews(listPairs(jobId), effectiveMinScore, effectiveTopK);
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
        return new TeacherPlagiarismPairDetailView(
                pair.getId(),
                pair.getJobId(),
                pair.getStudentA(),
                pair.getStudentB(),
                pair.getScore(),
                pair.getStatus(),
                pair.getTeacherNote(),
                listEvidenceViews(pairId),
                getLatestAiExplanation(teacherId, pairId)
        );
    }

    @Override
    public SimilarityPair updatePairStatus(Long teacherId, Long pairId, String status, String teacherNote) {
        SimilarityPair pair = requireTeacherPair(teacherId, pairId);
        String normalizedStatus = status == null ? null : status.trim().toUpperCase();
        if (normalizedStatus == null || !ALLOWED_PAIR_STATUS.contains(normalizedStatus)) {
            throw new BusinessException("相似对状态不合法");
        }
        pair.setStatus(normalizedStatus);
        pair.setTeacherNote(teacherNote);
        similarityPairMapper.updateById(pair);
        return pair;
    }

    @Override
    public byte[] exportReportCsv(Long teacherId, Long jobId, Integer minScore, Integer perStudentTopK) {
        TeacherPlagiarismJobReport report = getReport(teacherId, jobId, minScore, perStudentTopK);
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("jobId,status,minScore,perStudentTopK,message\r\n");
        builder.append(csv(report.jobId()))
                .append(',').append(csv(report.status()))
                .append(',').append(csv(report.minScore()))
                .append(',').append(csv(report.perStudentTopK()))
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
    public AiExplanation createAiExplanation(Long teacherId, Long pairId) {
        SimilarityPair pair = requireTeacherPair(teacherId, pairId);
        return aiExplanationService.createExplanation(pair, requirePrimaryEvidence(pairId));
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

    private String buildConfigSnapshot(String submissionFingerprint, int submissionCount, int pairCount, int thresholdScore) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("algoVersion", ALGO_VERSION);
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
            Long sourceJobId,
            int clonedPairCount
    ) {
        Map<String, Object> snapshot = parseSnapshot(sourceConfigSnapshot);
        if (snapshot == null) {
            snapshot = new HashMap<>();
        } else {
            snapshot = new HashMap<>(snapshot);
        }
        snapshot.put("algoVersion", ALGO_VERSION);
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
                statsView.executionMode(),
                statsView.reusedFromJobId(),
                statsView.thresholdMatchedPairs()
        );
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

    private PlagiarismJob findReusableDoneJob(Long assignmentId, String submissionFingerprint, int thresholdScore) {
        QueryWrapper<PlagiarismJob> wrapper = new QueryWrapper<>();
        wrapper.eq("assignment_id", assignmentId).eq("status", "DONE").orderByDesc("id");
        for (PlagiarismJob job : plagiarismJobMapper.selectList(wrapper)) {
            Map<String, Object> snapshot = parseSnapshot(job.getConfigSnapshot());
            if (snapshot == null) {
                continue;
            }
            Object algoVersion = snapshot.get("algoVersion");
            Object fingerprint = snapshot.get("submissionFingerprint");
            Integer previousThreshold = toInteger(snapshot.get("thresholdScore"));
            if (!ALGO_VERSION.equals(algoVersion)) {
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

    private List<TeacherPlagiarismPairView> buildFilteredPairViews(List<SimilarityPair> pairs, int minScore, int perStudentTopK) {
        if (pairs == null || pairs.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> studentPairCount = new HashMap<>();
        List<TeacherPlagiarismPairView> result = new ArrayList<>();
        for (SimilarityPair pair : pairs) {
            if (pair.getScore() == null || pair.getScore() < minScore) {
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
                    pair.getStatus(),
                    pair.getTeacherNote()
            ));
            studentPairCount.merge(pair.getStudentA(), 1, Integer::sum);
            studentPairCount.merge(pair.getStudentB(), 1, Integer::sum);
        }
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
            throw new BusinessException("相似对证据不存在");
        }
        return evidence;
    }

    private SimilarityPair requireTeacherPair(Long teacherId, Long pairId) {
        SimilarityPair pair = similarityPairMapper.selectById(pairId);
        if (pair == null) {
            throw new BusinessException("相似对不存在");
        }
        PlagiarismJob job = plagiarismJobMapper.selectById(pair.getJobId());
        if (job == null) {
            throw new BusinessException("查重任务不存在");
        }
        requireTeacherAssignment(teacherId, job.getAssignmentId());
        return pair;
    }

    private Assignment requireTeacherAssignment(Long teacherId, Long assignmentId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }
        if (!teacherId.equals(assignment.getTeacherId())) {
            throw new BusinessException(403, "无权操作该作业");
        }
        return assignment;
    }

    private String buildReportMessage(String jobStatus, boolean noPairs) {
        if ("QUEUED".equalsIgnoreCase(jobStatus)) {
            return "查重任务已进入队列，请稍后刷新报告";
        }
        if ("RUNNING".equalsIgnoreCase(jobStatus)) {
            return "查重任务运行中，请稍后刷新报告";
        }
        if ("FAILED".equalsIgnoreCase(jobStatus)) {
            return "查重任务执行失败，请检查任务状态";
        }
        return noPairs ? "查重完成，当前无超过阈值的相似对" : "查重完成";
    }

    private String csv(Object value) {
        if (value == null) {
            return "\"\"";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}

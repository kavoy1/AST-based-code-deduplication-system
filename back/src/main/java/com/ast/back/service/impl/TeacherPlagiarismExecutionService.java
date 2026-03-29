package com.ast.back.service.impl;

import com.ast.back.domain.plagiarism.AcSimilarityCalculator;
import com.ast.back.domain.plagiarism.AcSimilarityResult;
import com.ast.back.domain.plagiarism.AstSignatureProfile;
import com.ast.back.domain.plagiarism.JavaAstSignatureExtractor;
import com.ast.back.entity.PlagiarismJob;
import com.ast.back.entity.SimilarityEvidence;
import com.ast.back.entity.SimilarityPair;
import com.ast.back.entity.Submission;
import com.ast.back.entity.SubmissionFile;
import com.ast.back.entity.SubmissionProfile;
import com.ast.back.mapper.PlagiarismJobMapper;
import com.ast.back.mapper.SimilarityEvidenceMapper;
import com.ast.back.mapper.SimilarityPairMapper;
import com.ast.back.mapper.SubmissionFileMapper;
import com.ast.back.mapper.SubmissionMapper;
import com.ast.back.mapper.SubmissionProfileMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TeacherPlagiarismExecutionService {

    private static final int EVIDENCE_TOP_N = 20;
    private static final int PROGRESS_UPDATE_BATCH_SIZE = 50;
    private static final int RESULT_WRITE_BATCH_SIZE = 100;

    private final SubmissionMapper submissionMapper;
    private final SubmissionFileMapper submissionFileMapper;
    private final SubmissionProfileMapper submissionProfileMapper;
    private final SimilarityPairMapper similarityPairMapper;
    private final SimilarityEvidenceMapper similarityEvidenceMapper;
    private final PlagiarismJobMapper plagiarismJobMapper;
    private final JavaAstSignatureExtractor extractor;
    private final AcSimilarityCalculator calculator;
    private final Path storageRootDir;
    private final ObjectMapper objectMapper;

    @Autowired
    public TeacherPlagiarismExecutionService(
            SubmissionMapper submissionMapper,
            SubmissionFileMapper submissionFileMapper,
            SubmissionProfileMapper submissionProfileMapper,
            SimilarityPairMapper similarityPairMapper,
            SimilarityEvidenceMapper similarityEvidenceMapper,
            PlagiarismJobMapper plagiarismJobMapper,
            @Value("${app.storage.root-dir:uploads}") String storageRootDir
    ) {
        this(submissionMapper, submissionFileMapper, submissionProfileMapper, similarityPairMapper, similarityEvidenceMapper, plagiarismJobMapper,
                new JavaAstSignatureExtractor(), new AcSimilarityCalculator(), storageRootDir, new ObjectMapper());
    }

    TeacherPlagiarismExecutionService(
            SubmissionMapper submissionMapper,
            SubmissionFileMapper submissionFileMapper,
            SubmissionProfileMapper submissionProfileMapper,
            SimilarityPairMapper similarityPairMapper,
            SimilarityEvidenceMapper similarityEvidenceMapper,
            PlagiarismJobMapper plagiarismJobMapper,
            JavaAstSignatureExtractor extractor,
            AcSimilarityCalculator calculator,
            String storageRootDir
    ) {
        this(submissionMapper, submissionFileMapper, submissionProfileMapper, similarityPairMapper, similarityEvidenceMapper, plagiarismJobMapper,
                extractor, calculator, storageRootDir, new ObjectMapper());
    }

    TeacherPlagiarismExecutionService(
            SubmissionMapper submissionMapper,
            SubmissionFileMapper submissionFileMapper,
            SubmissionProfileMapper submissionProfileMapper,
            SimilarityPairMapper similarityPairMapper,
            SimilarityEvidenceMapper similarityEvidenceMapper,
            PlagiarismJobMapper plagiarismJobMapper,
            JavaAstSignatureExtractor extractor,
            AcSimilarityCalculator calculator,
            String storageRootDir,
            ObjectMapper objectMapper
    ) {
        this.submissionMapper = submissionMapper;
        this.submissionFileMapper = submissionFileMapper;
        this.submissionProfileMapper = submissionProfileMapper;
        this.similarityPairMapper = similarityPairMapper;
        this.similarityEvidenceMapper = similarityEvidenceMapper;
        this.plagiarismJobMapper = plagiarismJobMapper;
        this.extractor = extractor;
        this.calculator = calculator;
        this.storageRootDir = Paths.get(storageRootDir).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    public void execute(PlagiarismJob job, int thresholdScore, int topKPerStudent) {
        try {
            job.setStatus("RUNNING");
            job.setStartTime(LocalDateTime.now());
            job.setErrorMsg(null);
            plagiarismJobMapper.updateById(job);

            List<Submission> submissions = listComparableSubmissions(job.getAssignmentId());
            Map<Long, SubmissionProfileContext> profiles = buildProfiles(submissions);

            int totalPairs = calculateComparablePairs(submissions);
            job.setProgressTotal(totalPairs);
            job.setProgressDone(0);
            plagiarismJobMapper.updateById(job);

            List<Submission> ordered = submissions.stream()
                    .sorted(Comparator.comparing(Submission::getClassId).thenComparing(Submission::getStudentId))
                    .toList();

            int processedPairs = 0;
            int lastFlushedProgress = 0;
            int sizeSkippedPairs = 0;
            int bucketSkippedPairs = 0;
            int fullCalculatedPairs = 0;
            int thresholdMatchedPairs = 0;
            List<PairInsertBundle> pendingBundles = new ArrayList<>();
            for (int i = 0; i < ordered.size(); i++) {
                for (int j = i + 1; j < ordered.size(); j++) {
                    Submission left = ordered.get(i);
                    Submission right = ordered.get(j);
                    if (!left.getClassId().equals(right.getClassId())) {
                        continue;
                    }

                    SubmissionProfileContext leftContext = profiles.get(left.getId());
                    SubmissionProfileContext rightContext = profiles.get(right.getId());
                    double thresholdAc = thresholdScore / 100.0;
                    double sizeUpperBoundAc = calculateSizeUpperBoundAc(leftContext.profile(), rightContext.profile());
                    if (sizeUpperBoundAc < thresholdAc) {
                        sizeSkippedPairs++;
                        processedPairs++;
                        if (processedPairs - lastFlushedProgress >= PROGRESS_UPDATE_BATCH_SIZE) {
                            job.setProgressDone(processedPairs);
                            plagiarismJobMapper.updateById(job);
                            lastFlushedProgress = processedPairs;
                        }
                        continue;
                    }

                    double bucketUpperBoundAc = calculateBucketUpperBoundAc(leftContext, rightContext);
                    if (bucketUpperBoundAc < thresholdAc) {
                        bucketSkippedPairs++;
                        processedPairs++;
                        if (processedPairs - lastFlushedProgress >= PROGRESS_UPDATE_BATCH_SIZE) {
                            job.setProgressDone(processedPairs);
                            plagiarismJobMapper.updateById(job);
                            lastFlushedProgress = processedPairs;
                        }
                        continue;
                    }

                    fullCalculatedPairs++;
                    AcSimilarityResult result = calculator.calculate(leftContext.profile(), rightContext.profile());
                    int score = (int) Math.round(result.ac() * 100.0);
                    if (result.ac() >= thresholdAc) {
                        thresholdMatchedPairs++;
                        pendingBundles.add(buildPairInsertBundle(job, left, right, leftContext, rightContext, result, score));
                        if (pendingBundles.size() >= RESULT_WRITE_BATCH_SIZE) {
                            flushPairBundles(pendingBundles);
                        }
                    }

                    processedPairs++;
                    if (processedPairs - lastFlushedProgress >= PROGRESS_UPDATE_BATCH_SIZE) {
                        job.setProgressDone(processedPairs);
                        plagiarismJobMapper.updateById(job);
                        lastFlushedProgress = processedPairs;
                    }
                }
            }

            flushPairBundles(pendingBundles);
            job.setProgressDone(processedPairs);
            job.setConfigSnapshot(updateExecutionStatsSnapshot(
                    job.getConfigSnapshot(),
                    sizeSkippedPairs,
                    bucketSkippedPairs,
                    fullCalculatedPairs,
                    thresholdMatchedPairs
            ));
            job.setStatus("DONE");
            job.setEndTime(LocalDateTime.now());
            plagiarismJobMapper.updateById(job);
        } catch (Exception ex) {
            job.setConfigSnapshot(updateExecutionStatsSnapshot(
                    job.getConfigSnapshot(),
                    0,
                    0,
                    0,
                    0
            ));
            job.setStatus("FAILED");
            job.setEndTime(LocalDateTime.now());
            job.setErrorMsg(ex.getMessage());
            plagiarismJobMapper.updateById(job);
            throw ex;
        }
    }

    private List<Submission> listComparableSubmissions(Long assignmentId) {
        QueryWrapper<Submission> wrapper = new QueryWrapper<>();
        wrapper.eq("assignment_id", assignmentId).eq("is_latest", 1).eq("is_valid", 1);
        return submissionMapper.selectList(wrapper);
    }

    private Map<Long, SubmissionProfileContext> buildProfiles(List<Submission> submissions) {
        Map<Long, SubmissionProfileContext> result = new HashMap<>();
        if (submissions.isEmpty()) {
            return result;
        }

        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        QueryWrapper<SubmissionProfile> profileWrapper = new QueryWrapper<>();
        profileWrapper.in("submission_id", submissionIds);
        Map<Long, SubmissionProfile> cachedProfiles = submissionProfileMapper.selectList(profileWrapper).stream()
                .collect(Collectors.toMap(SubmissionProfile::getSubmissionId, profile -> profile, (left, right) -> left));

        List<Submission> missingProfileSubmissions = submissions.stream()
                .filter(submission -> !cachedProfiles.containsKey(submission.getId()))
                .toList();
        Map<Long, List<SubmissionFile>> filesBySubmissionId = loadFilesForSubmissions(missingProfileSubmissions);

        for (Submission submission : submissions) {
            SubmissionProfile cached = cachedProfiles.get(submission.getId());
            if (cached != null) {
                result.put(submission.getId(), fromCachedProfile(cached));
            } else {
                result.put(submission.getId(), buildProfileFromFiles(filesBySubmissionId.getOrDefault(submission.getId(), List.of())));
            }
        }
        return result;
    }

    private Map<Long, List<SubmissionFile>> loadFilesForSubmissions(List<Submission> submissions) {
        if (submissions == null || submissions.isEmpty()) {
            return Map.of();
        }
        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        return submissionFileMapper.selectBySubmissionIds(submissionIds).stream()
                .collect(Collectors.groupingBy(SubmissionFile::getSubmissionId));
    }

    private SubmissionProfileContext fromCachedProfile(SubmissionProfile profile) {
        try {
            Map<String, Integer> counts = objectMapper.readValue(
                    profile.getSignatureCountsJson() == null ? "{}" : profile.getSignatureCountsJson(),
                    new TypeReference<>() {
                    }
            );
            List<Map<String, String>> parseFailures = objectMapper.readValue(
                    profile.getParseFailuresJson() == null ? "[]" : profile.getParseFailuresJson(),
                    new TypeReference<>() {
                    }
            );
            return new SubmissionProfileContext(
                    new AstSignatureProfile(profile.getTotalNodes() == null ? 0 : profile.getTotalNodes(), counts),
                    buildBucketCounts(counts),
                    parseFailures
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse cached submission profile", e);
        }
    }

    private SubmissionProfileContext buildProfileFromFiles(List<SubmissionFile> files) {
        Map<String, Integer> mergedCounts = new HashMap<>();
        int totalNodes = 0;
        List<Map<String, String>> parseFailures = new ArrayList<>();
        for (SubmissionFile file : files) {
            if (!"OK".equalsIgnoreCase(file.getParseStatus())) {
                parseFailures.add(Map.of(
                        "file", file.getFilename(),
                        "reason", file.getParseError() == null ? file.getParseStatus() : file.getParseError()
                ));
                continue;
            }
            String source = readSource(file.getStoragePath());
            AstSignatureProfile profile = extractor.extract(source);
            totalNodes += profile.totalNodes();
            profile.signatureCounts().forEach((key, value) -> mergedCounts.merge(key, value, Integer::sum));
        }
        return new SubmissionProfileContext(
                new AstSignatureProfile(totalNodes, mergedCounts),
                buildBucketCounts(mergedCounts),
                parseFailures
        );
    }

    private String readSource(String relativePath) {
        try {
            return Files.readString(storageRootDir.resolve(relativePath).normalize());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read stored source file", e);
        }
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

    private double calculateSizeUpperBoundAc(AstSignatureProfile left, AstSignatureProfile right) {
        int denominator = left.totalNodes() + right.totalNodes();
        if (denominator == 0) {
            return 0.0;
        }
        int maxMatchedNodes = Math.min(left.totalNodes(), right.totalNodes());
        return (2.0 * maxMatchedNodes) / denominator;
    }

    private double calculateBucketUpperBoundAc(SubmissionProfileContext left, SubmissionProfileContext right) {
        int bucketMatchedNodes = 0;
        for (String bucket : left.bucketCounts().keySet()) {
            int leftCount = left.bucketCounts().getOrDefault(bucket, 0);
            int rightCount = right.bucketCounts().getOrDefault(bucket, 0);
            bucketMatchedNodes += Math.min(leftCount, rightCount);
        }
        int denominator = left.profile().totalNodes() + right.profile().totalNodes();
        return denominator == 0 ? 0.0 : (2.0 * bucketMatchedNodes) / denominator;
    }

    private Map<String, Integer> buildBucketCounts(Map<String, Integer> signatureCounts) {
        Map<String, Integer> bucketCounts = new HashMap<>();
        signatureCounts.forEach((signature, count) -> bucketCounts.merge(toBucket(signature), count, Integer::sum));
        return bucketCounts;
    }

    private String toBucket(String signature) {
        int separatorIndex = signature.indexOf(':');
        return separatorIndex >= 0 ? signature.substring(0, separatorIndex) : signature;
    }

    private String updateExecutionStatsSnapshot(
            String currentSnapshot,
            int sizeSkippedPairs,
            int bucketSkippedPairs,
            int fullCalculatedPairs,
            int thresholdMatchedPairs
    ) {
        try {
            Map<String, Object> snapshot = currentSnapshot == null || currentSnapshot.isBlank()
                    ? new HashMap<>()
                    : objectMapper.readValue(currentSnapshot, new TypeReference<>() {
                    });
            snapshot.putIfAbsent("executionMode", "ASYNC");

            Map<String, Object> executionStats = toMutableMap(snapshot.get("executionStats"));
            executionStats.put("sizeSkippedPairs", sizeSkippedPairs);
            executionStats.put("bucketSkippedPairs", bucketSkippedPairs);
            executionStats.put("fullCalculatedPairs", fullCalculatedPairs);
            executionStats.put("thresholdMatchedPairs", thresholdMatchedPairs);
            snapshot.put("executionStats", executionStats);
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to update execution stats snapshot", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMutableMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return new HashMap<>((Map<String, Object>) map);
        }
        return new HashMap<>();
    }

    private PairInsertBundle buildPairInsertBundle(
            PlagiarismJob job,
            Submission left,
            Submission right,
            SubmissionProfileContext leftContext,
            SubmissionProfileContext rightContext,
            AcSimilarityResult result,
            int score
    ) {
        SimilarityPair pair = new SimilarityPair();
        pair.setJobId(job.getId());
        pair.setStudentA(Math.min(left.getStudentId(), right.getStudentId()));
        pair.setStudentB(Math.max(left.getStudentId(), right.getStudentId()));
        pair.setScore(score);
        pair.setStatus("PENDING");
        pair.setCreateTime(LocalDateTime.now());

        SimilarityEvidence evidence = new SimilarityEvidence();
        evidence.setType("SIGNATURE_OVERLAP_TOP");
        evidence.setSummary("匹配节点数=" + result.matchedNodes() + "，AC=" + String.format("%.4f", result.ac()));
        evidence.setWeight(result.matchedNodes());
        evidence.setPayloadJson(buildPayload(leftContext, rightContext, result));
        return new PairInsertBundle(pair, evidence);
    }

    private void flushPairBundles(List<PairInsertBundle> pendingBundles) {
        if (pendingBundles.isEmpty()) {
            return;
        }
        List<SimilarityPair> pairs = pendingBundles.stream().map(PairInsertBundle::pair).toList();
        similarityPairMapper.batchInsert(pairs);

        List<SimilarityEvidence> evidences = new ArrayList<>();
        for (PairInsertBundle bundle : pendingBundles) {
            if (bundle.pair().getId() == null) {
                throw new IllegalStateException("Batch inserted similarity pair id is missing");
            }
            SimilarityEvidence evidence = bundle.evidence();
            evidence.setPairId(bundle.pair().getId());
            evidences.add(evidence);
        }
        similarityEvidenceMapper.batchInsert(evidences);
        pendingBundles.clear();
    }

    private String buildPayload(SubmissionProfileContext left, SubmissionProfileContext right, AcSimilarityResult result) {
        try {
            List<Map<String, Object>> topN = buildTopSignatures(left.profile(), right.profile(), result);
            Map<String, Object> payload = new HashMap<>();
            payload.put("algoVersion", "AC_BAG_OF_NODES_V1");
            payload.put("schemaVersion", 1);
            payload.put("summary", "Top 命中特征已生成，供教师复核参考");
            payload.put("weight", result.matchedNodes());
            payload.put("totals", Map.of(
                    "N1", left.profile().totalNodes(),
                    "N2", right.profile().totalNodes(),
                    "M", result.matchedNodes(),
                    "AC", result.ac()
            ));
            payload.put("topN", topN);
            payload.put("parseFailures", Map.of(
                    "sideA", left.parseFailures(),
                    "sideB", right.parseFailures()
            ));
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build evidence payload", e);
        }
    }

    private List<Map<String, Object>> buildTopSignatures(
            AstSignatureProfile left,
            AstSignatureProfile right,
            AcSimilarityResult result
    ) {
        List<Map<String, Object>> topN = new ArrayList<>();
        left.signatureCounts().keySet().stream()
                .filter(right.signatureCounts()::containsKey)
                .map(signature -> {
                    int countA = left.signatureCounts().get(signature);
                    int countB = right.signatureCounts().get(signature);
                    int matched = Math.min(countA, countB);
                    Map<String, Object> item = new HashMap<>();
                    item.put("signatureId", Integer.toHexString(signature.hashCode()));
                    item.put("signature", signature);
                    item.put("countA", countA);
                    item.put("countB", countB);
                    item.put("matchedCount", matched);
                    item.put("contribution", matched);
                    item.put("contributionRatio", result.matchedNodes() == 0 ? 0.0 : ((double) matched) / result.matchedNodes());
                    return item;
                })
                .sorted((a, b) -> Integer.compare((Integer) b.get("matchedCount"), (Integer) a.get("matchedCount")))
                .limit(EVIDENCE_TOP_N)
                .forEach(topN::add);
        return topN;
    }

    private record SubmissionProfileContext(
            AstSignatureProfile profile,
            Map<String, Integer> bucketCounts,
            List<Map<String, String>> parseFailures
    ) {
    }

    private record PairInsertBundle(
            SimilarityPair pair,
            SimilarityEvidence evidence
    ) {
    }
}

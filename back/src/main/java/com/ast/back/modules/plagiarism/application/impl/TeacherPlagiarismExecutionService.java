package com.ast.back.modules.plagiarism.application.impl;

import com.ast.back.modules.assignment.persistence.entity.Assignment;
import com.ast.back.modules.assignment.persistence.mapper.AssignmentMapper;
import com.ast.back.modules.plagiarism.domain.AcSimilarityCalculator;
import com.ast.back.modules.plagiarism.domain.AcSimilarityResult;
import com.ast.back.modules.plagiarism.domain.AstSignatureProfile;
import com.ast.back.modules.plagiarism.domain.CAstSignatureExtractor;
import com.ast.back.modules.plagiarism.domain.CDeepAstSignatureExtractor;
import com.ast.back.modules.plagiarism.domain.DeepAstProfile;
import com.ast.back.modules.plagiarism.domain.DeepAstMethodProfile;
import com.ast.back.modules.plagiarism.domain.DeepAstSimilarityCalculator;
import com.ast.back.modules.plagiarism.domain.DeepAstSimilarityResult;
import com.ast.back.modules.plagiarism.domain.JavaAstSignatureExtractor;
import com.ast.back.modules.plagiarism.domain.DeepAstMethodMatch;
import com.ast.back.modules.plagiarism.domain.DeepAstSignatureExtractor;
import com.ast.back.modules.plagiarism.persistence.entity.PlagiarismJob;
import com.ast.back.modules.plagiarism.persistence.entity.SimilarityEvidence;
import com.ast.back.modules.plagiarism.persistence.entity.SimilarityPair;
import com.ast.back.modules.submission.persistence.entity.Submission;
import com.ast.back.modules.submission.persistence.entity.SubmissionFile;
import com.ast.back.modules.submission.domain.CProjectSourceFile;
import com.ast.back.modules.submission.domain.CProjectSourceIndex;
import com.ast.back.modules.submission.domain.CProjectSourceCollector;
import com.ast.back.modules.submission.domain.CProjectArchiveExtractor;
import com.ast.back.modules.plagiarism.persistence.entity.SubmissionProfile;
import com.ast.back.modules.plagiarism.persistence.mapper.PlagiarismJobMapper;
import com.ast.back.modules.plagiarism.persistence.mapper.SimilarityEvidenceMapper;
import com.ast.back.modules.plagiarism.persistence.mapper.SimilarityPairMapper;
import com.ast.back.modules.submission.persistence.mapper.SubmissionFileMapper;
import com.ast.back.modules.submission.persistence.mapper.SubmissionMapper;
import com.ast.back.modules.plagiarism.persistence.mapper.SubmissionProfileMapper;
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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TeacherPlagiarismExecutionService {

    private static final int EVIDENCE_TOP_N = 20;
    private static final int PROGRESS_UPDATE_BATCH_SIZE = 50;
    private static final int RESULT_WRITE_BATCH_SIZE = 100;
    private static final String FAST_MODE = "FAST";
    private static final String DEEP_MODE = "DEEP";
    private static final String FAST_ALGO_VERSION = "AC_BAG_OF_NODES_V3";
    private static final String DEEP_ALGO_VERSION = "AC_DEEP_AST_V2";

    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionFileMapper submissionFileMapper;
    private final SubmissionProfileMapper submissionProfileMapper;
    private final SimilarityPairMapper similarityPairMapper;
    private final SimilarityEvidenceMapper similarityEvidenceMapper;
    private final PlagiarismJobMapper plagiarismJobMapper;
    private final JavaAstSignatureExtractor extractor;
    private final DeepAstSignatureExtractor deepExtractor;
    private final CAstSignatureExtractor cExtractor;
    private final CDeepAstSignatureExtractor cDeepExtractor;
    private final CProjectSourceCollector cProjectSourceCollector;
    private final AcSimilarityCalculator calculator;
    private final DeepAstSimilarityCalculator deepCalculator;
    private final Path storageRootDir;
    private final ObjectMapper objectMapper;

    @Autowired
    public TeacherPlagiarismExecutionService(
            AssignmentMapper assignmentMapper,
            SubmissionMapper submissionMapper,
            SubmissionFileMapper submissionFileMapper,
            SubmissionProfileMapper submissionProfileMapper,
            SimilarityPairMapper similarityPairMapper,
            SimilarityEvidenceMapper similarityEvidenceMapper,
            PlagiarismJobMapper plagiarismJobMapper,
            @Value("${app.storage.root-dir:uploads}") String storageRootDir
    ) {
        this(assignmentMapper, submissionMapper, submissionFileMapper, submissionProfileMapper, similarityPairMapper, similarityEvidenceMapper, plagiarismJobMapper,
                new JavaAstSignatureExtractor(),
                new DeepAstSignatureExtractor(),
                new CAstSignatureExtractor(),
                new CDeepAstSignatureExtractor(),
                new CProjectSourceCollector(new CProjectArchiveExtractor()),
                new AcSimilarityCalculator(),
                new DeepAstSimilarityCalculator(),
                storageRootDir,
                new ObjectMapper());
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
        this(null, submissionMapper, submissionFileMapper, submissionProfileMapper, similarityPairMapper, similarityEvidenceMapper, plagiarismJobMapper,
                extractor,
                new DeepAstSignatureExtractor(),
                new CAstSignatureExtractor(),
                new CDeepAstSignatureExtractor(),
                new CProjectSourceCollector(new CProjectArchiveExtractor()),
                calculator,
                new DeepAstSimilarityCalculator(),
                storageRootDir,
                objectMapper);
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
        this(null, submissionMapper, submissionFileMapper, submissionProfileMapper, similarityPairMapper, similarityEvidenceMapper, plagiarismJobMapper,
                extractor,
                new DeepAstSignatureExtractor(),
                new CAstSignatureExtractor(),
                new CDeepAstSignatureExtractor(),
                new CProjectSourceCollector(new CProjectArchiveExtractor()),
                calculator,
                new DeepAstSimilarityCalculator(),
                storageRootDir,
                new ObjectMapper());
    }

    TeacherPlagiarismExecutionService(
            AssignmentMapper assignmentMapper,
            SubmissionMapper submissionMapper,
            SubmissionFileMapper submissionFileMapper,
            SubmissionProfileMapper submissionProfileMapper,
            SimilarityPairMapper similarityPairMapper,
            SimilarityEvidenceMapper similarityEvidenceMapper,
            PlagiarismJobMapper plagiarismJobMapper,
            JavaAstSignatureExtractor extractor,
            DeepAstSignatureExtractor deepExtractor,
            CAstSignatureExtractor cExtractor,
            CDeepAstSignatureExtractor cDeepExtractor,
            CProjectSourceCollector cProjectSourceCollector,
            AcSimilarityCalculator calculator,
            DeepAstSimilarityCalculator deepCalculator,
            String storageRootDir,
            ObjectMapper objectMapper
    ) {
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.submissionFileMapper = submissionFileMapper;
        this.submissionProfileMapper = submissionProfileMapper;
        this.similarityPairMapper = similarityPairMapper;
        this.similarityEvidenceMapper = similarityEvidenceMapper;
        this.plagiarismJobMapper = plagiarismJobMapper;
        this.extractor = extractor;
        this.deepExtractor = deepExtractor;
        this.cExtractor = cExtractor;
        this.cDeepExtractor = cDeepExtractor;
        this.cProjectSourceCollector = cProjectSourceCollector;
        this.calculator = calculator;
        this.deepCalculator = deepCalculator;
        this.storageRootDir = Paths.get(storageRootDir).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    TeacherPlagiarismExecutionService(
            SubmissionMapper submissionMapper,
            SubmissionFileMapper submissionFileMapper,
            SubmissionProfileMapper submissionProfileMapper,
            SimilarityPairMapper similarityPairMapper,
            SimilarityEvidenceMapper similarityEvidenceMapper,
            PlagiarismJobMapper plagiarismJobMapper,
            JavaAstSignatureExtractor extractor,
            DeepAstSignatureExtractor deepExtractor,
            AcSimilarityCalculator calculator,
            DeepAstSimilarityCalculator deepCalculator,
            String storageRootDir,
            ObjectMapper objectMapper
    ) {
        this(null, submissionMapper, submissionFileMapper, submissionProfileMapper, similarityPairMapper, similarityEvidenceMapper, plagiarismJobMapper,
                extractor,
                deepExtractor,
                new CAstSignatureExtractor(),
                new CDeepAstSignatureExtractor(),
                new CProjectSourceCollector(new CProjectArchiveExtractor()),
                calculator,
                deepCalculator,
                storageRootDir,
                objectMapper);
    }

    public void execute(PlagiarismJob job, int thresholdScore, int topKPerStudent, String plagiarismMode) {
        try {
            job.setStatus("RUNNING");
            job.setStartTime(LocalDateTime.now());
            job.setErrorMsg(null);
            plagiarismJobMapper.updateById(job);

            String normalizedMode = normalizePlagiarismMode(plagiarismMode);
            List<Submission> submissions = listComparableSubmissions(job.getAssignmentId());
            String assignmentLanguage = resolveAssignmentLanguage(job.getAssignmentId());
            Map<Long, SubmissionProfileContext> profiles = buildProfiles(submissions, normalizedMode, assignmentLanguage);

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
                    fullCalculatedPairs++;
                    ModeSimilarityResult result = calculateSimilarity(leftContext, rightContext, normalizedMode);
                    int score = (int) Math.round(result.score() * 100.0);
                    if (result.score() >= thresholdAc) {
                        thresholdMatchedPairs++;
                    }
                    pendingBundles.add(buildPairInsertBundle(job, left, right, leftContext, rightContext, result, score, plagiarismMode));
                    if (pendingBundles.size() >= RESULT_WRITE_BATCH_SIZE) {
                        flushPairBundles(pendingBundles);
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
                    thresholdMatchedPairs,
                    plagiarismMode
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
                    0,
                    plagiarismMode
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

    private Map<Long, SubmissionProfileContext> buildProfiles(
            List<Submission> submissions,
            String plagiarismMode,
            String assignmentLanguage
    ) {
        Map<Long, SubmissionProfileContext> result = new HashMap<>();
        if (submissions.isEmpty()) {
            return result;
        }

        String normalizedMode = normalizePlagiarismMode(plagiarismMode);
        if (DEEP_MODE.equals(normalizedMode)) {
            Map<Long, List<SubmissionFile>> filesBySubmissionId = loadFilesForSubmissions(submissions);
            for (Submission submission : submissions) {
                result.put(
                        submission.getId(),
                        buildDeepProfileFromFiles(filesBySubmissionId.getOrDefault(submission.getId(), List.of()), assignmentLanguage)
                );
            }
            return result;
        }

        if (!"JAVA".equals(assignmentLanguage)) {
            Map<Long, List<SubmissionFile>> filesBySubmissionId = loadFilesForSubmissions(submissions);
            for (Submission submission : submissions) {
                result.put(
                        submission.getId(),
                        buildProfileFromFiles(filesBySubmissionId.getOrDefault(submission.getId(), List.of()), assignmentLanguage)
                );
            }
            return result;
        }

        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        QueryWrapper<SubmissionProfile> profileWrapper = new QueryWrapper<>();
        profileWrapper.in("submission_id", submissionIds);
        Map<Long, SubmissionProfile> cachedProfiles = submissionProfileMapper.selectList(profileWrapper).stream()
                .filter(profile -> FAST_ALGO_VERSION.equals(profile.getAlgoVersion()))
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
                result.put(
                        submission.getId(),
                        buildProfileFromFiles(filesBySubmissionId.getOrDefault(submission.getId(), List.of()), assignmentLanguage)
                );
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
                    parseFailures,
                    null
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse cached submission profile", e);
        }
    }

    private SubmissionProfileContext buildProfileFromFiles(List<SubmissionFile> files, String assignmentLanguage) {
        if ("C".equals(assignmentLanguage)) {
            return buildCProfileFromFiles(files);
        }
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
            try {
                AstSignatureProfile profile = extractor.extract(source);
                totalNodes += profile.totalNodes();
                profile.signatureCounts().forEach((key, value) -> mergedCounts.merge(key, value, Integer::sum));
            } catch (RuntimeException ex) {
                parseFailures.add(Map.of(
                        "file", file.getFilename(),
                        "reason", ex.getMessage() == null ? "AST extraction failed" : ex.getMessage()
                ));
            }
        }
        return new SubmissionProfileContext(
                new AstSignatureProfile(totalNodes, mergedCounts),
                buildBucketCounts(mergedCounts),
                parseFailures,
                null
        );
    }

    private SubmissionProfileContext buildDeepProfileFromFiles(List<SubmissionFile> files, String assignmentLanguage) {
        if ("C".equals(assignmentLanguage)) {
            return buildCDeepProfileFromFiles(files);
        }
        Map<String, Integer> mergedDeepCounts = new HashMap<>();
        Map<String, Integer> mergedCoarseCounts = new HashMap<>();
        List<DeepAstMethodProfile> methodProfiles = new ArrayList<>();
        int deepTotalNodes = 0;
        int coarseTotalNodes = 0;
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
            try {
                DeepAstProfile profile = deepExtractor.extract(source);
                deepTotalNodes += profile.deepTotalNodes();
                coarseTotalNodes += profile.coarseTotalNodes();
                profile.deepSignatureCounts().forEach((key, value) -> mergedDeepCounts.merge(key, value, Integer::sum));
                profile.coarseSignatureCounts().forEach((key, value) -> mergedCoarseCounts.merge(key, value, Integer::sum));
                methodProfiles.addAll(profile.methodProfiles());
            } catch (RuntimeException ex) {
                parseFailures.add(Map.of(
                        "file", file.getFilename(),
                        "reason", ex.getMessage() == null ? "Deep AST extraction failed" : ex.getMessage()
                ));
            }
        }
        DeepAstProfile deepProfile = new DeepAstProfile(
                coarseTotalNodes,
                mergedCoarseCounts,
                deepTotalNodes,
                mergedDeepCounts,
                methodProfiles
        );
        return new SubmissionProfileContext(
                deepProfile.deepProfile(),
                buildBucketCounts(mergedDeepCounts),
                parseFailures,
                deepProfile
        );
    }

    private SubmissionProfileContext buildCProfileFromFiles(List<SubmissionFile> files) {
        List<Map<String, String>> parseFailures = new ArrayList<>();
        CProjectSourceIndex sourceIndex = collectCProjectSources(files, parseFailures);
        if (!sourceIndex.hasImplementationFiles()) {
            throw new IllegalStateException(buildMissingCImplementationMessage(parseFailures, files));
        }
        try {
            AstSignatureProfile profile = cExtractor.extract(sourceIndex);
            return new SubmissionProfileContext(profile, buildBucketCounts(profile.signatureCounts()), parseFailures, null);
        } catch (RuntimeException ex) {
            parseFailures.add(Map.of(
                    "file", resolveCFailureLabel(files),
                    "reason", ex.getMessage() == null ? "C AST extraction failed" : ex.getMessage()
            ));
            throw buildCExtractionFailure("C AST extraction failed", parseFailures, ex);
        }
    }

    private SubmissionProfileContext buildCDeepProfileFromFiles(List<SubmissionFile> files) {
        List<Map<String, String>> parseFailures = new ArrayList<>();
        CProjectSourceIndex sourceIndex = collectCProjectSources(files, parseFailures);
        if (!sourceIndex.hasImplementationFiles()) {
            throw new IllegalStateException(buildMissingCImplementationMessage(parseFailures, files));
        }
        try {
            DeepAstProfile profile = cDeepExtractor.extract(sourceIndex);
            return new SubmissionProfileContext(profile.deepProfile(), buildBucketCounts(profile.deepSignatureCounts()), parseFailures, profile);
        } catch (RuntimeException ex) {
            parseFailures.add(Map.of(
                    "file", resolveCFailureLabel(files),
                    "reason", ex.getMessage() == null ? "C deep AST extraction failed" : ex.getMessage()
            ));
            throw buildCExtractionFailure("C deep AST extraction failed", parseFailures, ex);
        }
    }

    private CProjectSourceIndex collectCProjectSources(List<SubmissionFile> files, List<Map<String, String>> parseFailures) {
        List<CProjectSourceFile> sourceFiles = new ArrayList<>();
        List<String> ignoredEntries = new ArrayList<>();
        for (SubmissionFile file : files) {
            if (!"OK".equalsIgnoreCase(file.getParseStatus())) {
                parseFailures.add(Map.of(
                        "file", file.getFilename(),
                        "reason", file.getParseError() == null ? file.getParseStatus() : file.getParseError()
                ));
                continue;
            }
            try {
                CProjectSourceIndex sourceIndex = cProjectSourceCollector.collect(resolveStoredPath(file.getStoragePath()));
                sourceFiles.addAll(sourceIndex.sourceFiles());
                ignoredEntries.addAll(sourceIndex.ignoredEntries());
            } catch (RuntimeException ex) {
                parseFailures.add(Map.of(
                        "file", file.getFilename(),
                        "reason", ex.getMessage() == null ? "Failed to collect C project sources" : ex.getMessage()
                ));
            }
        }
        return new CProjectSourceIndex(sourceFiles, ignoredEntries);
    }

    private String readSource(String relativePath) {
        try {
            return Files.readString(resolveStoredPath(relativePath));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read stored source file", e);
        }
    }

    private Path resolveStoredPath(String relativePath) {
        return storageRootDir.resolve(relativePath).normalize();
    }

    private String resolveAssignmentLanguage(Long assignmentId) {
        if (assignmentMapper == null) {
            return "JAVA";
        }
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null || assignment.getLanguage() == null || assignment.getLanguage().isBlank()) {
            return "JAVA";
        }
        return assignment.getLanguage().trim().toUpperCase(Locale.ROOT);
    }

    private String resolveCFailureLabel(List<SubmissionFile> files) {
        if (files == null || files.isEmpty()) {
            return "c-project";
        }
        return files.stream()
                .map(SubmissionFile::getFilename)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse("c-project");
    }

    private IllegalStateException buildCExtractionFailure(
            String messagePrefix,
            List<Map<String, String>> parseFailures,
            RuntimeException cause
    ) {
        String failureDetails = parseFailures.stream()
                .map(item -> item.getOrDefault("file", "c-project") + ": " + item.getOrDefault("reason", "unknown"))
                .collect(Collectors.joining("; "));
        String message = failureDetails.isBlank() ? messagePrefix : messagePrefix + " - " + failureDetails;
        return new IllegalStateException(message, cause);
    }

    private String buildMissingCImplementationMessage(
            List<Map<String, String>> parseFailures,
            List<SubmissionFile> files
    ) {
        String baseMessage = "No valid C implementation files available for plagiarism analysis";
        if (parseFailures == null || parseFailures.isEmpty()) {
            return baseMessage + " (" + resolveCFailureLabel(files) + ")";
        }
        String failureDetails = parseFailures.stream()
                .map(item -> item.getOrDefault("file", "c-project") + ": " + item.getOrDefault("reason", "unknown"))
                .collect(Collectors.joining("; "));
        return baseMessage + " - " + failureDetails;
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

    private ModeSimilarityResult calculateSimilarity(
            SubmissionProfileContext left,
            SubmissionProfileContext right,
            String plagiarismMode
    ) {
        if (DEEP_MODE.equals(plagiarismMode) && left.deepProfile() != null && right.deepProfile() != null) {
            DeepAstSimilarityResult result = deepCalculator.calculate(left.deepProfile(), right.deepProfile());
            return new ModeSimilarityResult(
                    result.matchedNodes(),
                    result.finalScore(),
                    result.coarseScore(),
                    result.deepStructuralScore(),
                    result.methodScore(),
                    result.methodMatches()
            );
        }

        AcSimilarityResult result = calculator.calculate(left.profile(), right.profile());
        return new ModeSimilarityResult(
                result.matchedNodes(),
                result.ac(),
                result.ac(),
                result.ac(),
                0.0,
                List.of()
        );
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
            int thresholdMatchedPairs,
            String plagiarismMode
    ) {
        try {
            Map<String, Object> snapshot = currentSnapshot == null || currentSnapshot.isBlank()
                    ? new HashMap<>()
                    : objectMapper.readValue(currentSnapshot, new TypeReference<>() {
                    });
            snapshot.putIfAbsent("executionMode", "ASYNC");
            snapshot.put("plagiarismMode", normalizePlagiarismMode(plagiarismMode));
            snapshot.put("algoVersion", resolveAlgoVersion(plagiarismMode));

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
            ModeSimilarityResult result,
            int score,
            String plagiarismMode
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
        evidence.setSummary(buildEvidenceSummary(result, plagiarismMode));
        evidence.setWeight(result.matchedNodes());
        evidence.setPayloadJson(buildPayload(leftContext, rightContext, result, plagiarismMode));
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

    private String buildPayload(SubmissionProfileContext left, SubmissionProfileContext right, ModeSimilarityResult result, String plagiarismMode) {
        try {
            String normalizedMode = normalizePlagiarismMode(plagiarismMode);
            List<Map<String, Object>> topN = buildTopSignatures(left.profile(), right.profile(), result);
            Map<String, Object> payload = new HashMap<>();
            payload.put("algoVersion", resolveAlgoVersion(plagiarismMode));
            payload.put("plagiarismMode", normalizedMode);
            payload.put("engineType", DEEP_MODE.equals(normalizedMode) ? "DEEP_STRUCTURE" : "FAST_STRUCTURE");
            payload.put("schemaVersion", 2);
            payload.put("summary", DEEP_MODE.equals(normalizedMode)
                    ? "已生成深度结构证据，可辅助教师查看方法结构、路径关系和重点命中。"
                    : "已生成快速结构证据，可辅助教师快速复核。");
            payload.put("weight", result.matchedNodes());
            payload.put("totals", Map.of(
                    "N1", left.profile().totalNodes(),
                    "N2", right.profile().totalNodes(),
                    "M", result.matchedNodes(),
                    "AC", result.score()
            ));
            payload.put("topN", topN);
            payload.put("scoreBreakdown", Map.of(
                    "coarseScore", result.coarseScore(),
                    "deepStructuralScore", result.deepStructuralScore(),
                    "methodScore", result.methodScore(),
                    "finalScore", result.score()
            ));
            payload.put("methodMatches", buildMethodMatchesPayload(result.methodMatches()));
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
            ModeSimilarityResult result
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

    private List<Map<String, Object>> buildMethodMatchesPayload(List<DeepAstMethodMatch> methodMatches) {
        return methodMatches.stream()
                .limit(5)
                .map(match -> Map.<String, Object>of(
                        "leftMethod", match.leftMethod(),
                        "rightMethod", match.rightMethod(),
                        "score", match.score()
                ))
                .toList();
    }

    private String buildEvidenceSummary(ModeSimilarityResult result, String plagiarismMode) {
        if (DEEP_MODE.equals(normalizePlagiarismMode(plagiarismMode))) {
            return "娣卞害缁撴瀯鍛戒腑 " + result.matchedNodes()
                    + " 涓壒寰侊紝缁煎悎鍒?" + String.format("%.4f", result.score());
        }
        return "鍖归厤鑺傜偣鏁?" + result.matchedNodes()
                + "锛孉C=" + String.format("%.4f", result.score());
    }

    private String normalizePlagiarismMode(String plagiarismMode) {
        String value = plagiarismMode == null ? "" : plagiarismMode.trim().toUpperCase();
        return DEEP_MODE.equals(value) ? DEEP_MODE : FAST_MODE;
    }

    private String resolveAlgoVersion(String plagiarismMode) {
        return DEEP_MODE.equals(normalizePlagiarismMode(plagiarismMode)) ? DEEP_ALGO_VERSION : FAST_ALGO_VERSION;
    }

    private record SubmissionProfileContext(
            AstSignatureProfile profile,
            Map<String, Integer> bucketCounts,
            List<Map<String, String>> parseFailures,
            DeepAstProfile deepProfile
    ) {
    }

    private record ModeSimilarityResult(
            int matchedNodes,
            double score,
            double coarseScore,
            double deepStructuralScore,
            double methodScore,
            List<DeepAstMethodMatch> methodMatches
    ) {
    }

    private record PairInsertBundle(
            SimilarityPair pair,
            SimilarityEvidence evidence
    ) {
    }
}

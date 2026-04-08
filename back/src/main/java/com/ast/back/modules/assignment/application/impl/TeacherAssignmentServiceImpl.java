package com.ast.back.modules.assignment.application.impl;

import com.ast.back.shared.common.BusinessException;
import com.ast.back.infra.storage.LocalStorageService;
import com.ast.back.modules.assignment.persistence.entity.Assignment;
import com.ast.back.modules.assignment.persistence.entity.AssignmentClass;
import com.ast.back.modules.assignment.persistence.entity.AssignmentMaterial;
import com.ast.back.modules.assignment.persistence.entity.AssignmentReopenLog;
import com.ast.back.modules.classmgmt.persistence.entity.Clazz;
import com.ast.back.infra.storage.StoredFile;
import com.ast.back.modules.plagiarism.persistence.entity.PlagiarismJob;
import com.ast.back.modules.plagiarism.persistence.entity.SimilarityEvidence;
import com.ast.back.modules.plagiarism.persistence.entity.SimilarityPair;
import com.ast.back.modules.plagiarism.persistence.mapper.PlagiarismJobMapper;
import com.ast.back.modules.plagiarism.persistence.mapper.SimilarityEvidenceMapper;
import com.ast.back.modules.plagiarism.persistence.mapper.SimilarityPairMapper;
import com.ast.back.modules.plagiarism.persistence.mapper.SubmissionProfileMapper;
import com.ast.back.modules.submission.persistence.entity.Submission;
import com.ast.back.modules.submission.persistence.entity.SubmissionFile;
import com.ast.back.infra.storage.AssignmentMaterialStorageService;
import com.ast.back.modules.assignment.persistence.mapper.AssignmentClassMapper;
import com.ast.back.modules.assignment.persistence.mapper.AssignmentMapper;
import com.ast.back.modules.assignment.persistence.mapper.AssignmentMaterialMapper;
import com.ast.back.modules.assignment.persistence.mapper.AssignmentReopenLogMapper;
import com.ast.back.modules.classmgmt.persistence.mapper.ClassMapper;
import com.ast.back.modules.submission.persistence.mapper.SubmissionFileMapper;
import com.ast.back.modules.submission.persistence.mapper.SubmissionMapper;
import com.ast.back.modules.assignment.application.TeacherAssignmentService;
import com.ast.back.modules.assignment.dto.TeacherAssignmentClassView;
import com.ast.back.modules.assignment.dto.TeacherAssignmentCreateRequest;
import com.ast.back.modules.assignment.dto.TeacherAssignmentDetailView;
import com.ast.back.modules.assignment.dto.TeacherAssignmentMaterialView;
import com.ast.back.modules.assignment.dto.TeacherAssignmentPageView;
import com.ast.back.modules.assignment.dto.TeacherAssignmentReopenRequest;
import com.ast.back.modules.assignment.dto.TeacherAssignmentStatsView;
import com.ast.back.modules.assignment.dto.TeacherAssignmentSummaryView;
import com.ast.back.modules.submission.dto.TeacherSubmissionOverview;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TeacherAssignmentServiceImpl implements TeacherAssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final AssignmentClassMapper assignmentClassMapper;
    private final AssignmentMaterialMapper assignmentMaterialMapper;
    private final AssignmentReopenLogMapper assignmentReopenLogMapper;
    private final ClassMapper classMapper;
    private final SubmissionMapper submissionMapper;
    private final SubmissionFileMapper submissionFileMapper;
    private final SubmissionProfileMapper submissionProfileMapper;
    private final PlagiarismJobMapper plagiarismJobMapper;
    private final SimilarityPairMapper similarityPairMapper;
    private final SimilarityEvidenceMapper similarityEvidenceMapper;
    private final AssignmentMaterialStorageService assignmentMaterialStorageService;
    private final LocalStorageService localStorageService;

    public TeacherAssignmentServiceImpl(
            AssignmentMapper assignmentMapper,
            AssignmentClassMapper assignmentClassMapper,
            AssignmentMaterialMapper assignmentMaterialMapper,
            AssignmentReopenLogMapper assignmentReopenLogMapper,
            ClassMapper classMapper,
            SubmissionMapper submissionMapper,
            PlagiarismJobMapper plagiarismJobMapper,
            SubmissionFileMapper submissionFileMapper,
            SubmissionProfileMapper submissionProfileMapper,
            SimilarityPairMapper similarityPairMapper,
            SimilarityEvidenceMapper similarityEvidenceMapper,
            AssignmentMaterialStorageService assignmentMaterialStorageService,
            LocalStorageService localStorageService
    ) {
        this.assignmentMapper = assignmentMapper;
        this.assignmentClassMapper = assignmentClassMapper;
        this.assignmentMaterialMapper = assignmentMaterialMapper;
        this.assignmentReopenLogMapper = assignmentReopenLogMapper;
        this.classMapper = classMapper;
        this.submissionMapper = submissionMapper;
        this.plagiarismJobMapper = plagiarismJobMapper;
        this.submissionFileMapper = submissionFileMapper;
        this.submissionProfileMapper = submissionProfileMapper;
        this.similarityPairMapper = similarityPairMapper;
        this.similarityEvidenceMapper = similarityEvidenceMapper;
        this.assignmentMaterialStorageService = assignmentMaterialStorageService;
        this.localStorageService = localStorageService;
    }

    @Override
    @Transactional
    public Assignment createAssignment(Long teacherId, TeacherAssignmentCreateRequest request) {
        validateCreateRequest(request);
        List<Clazz> classes = requireTeacherClasses(teacherId, request.classIds());

        Assignment assignment = new Assignment();
        applyAssignmentFields(assignment, request);
        assignment.setTeacherId(teacherId);
        assignment.setClazzId(request.classIds().get(0));
        assignment.setCreateTime(LocalDateTime.now());
        assignment.setUpdateTime(LocalDateTime.now());
        assignment.setStatus("PUBLISHED");
        assignment.setMaterialCount(0);
        assignmentMapper.insert(assignment);

        for (Clazz clazz : classes) {
            AssignmentClass assignmentClass = new AssignmentClass();
            assignmentClass.setAssignmentId(assignment.getId());
            assignmentClass.setClassId(clazz.getId());
            assignmentClass.setPublishStatus("PUBLISHED");
            assignmentClass.setCreateTime(LocalDateTime.now());
            assignmentClassMapper.insert(assignmentClass);
        }
        return assignment;
    }

    @Override
    public TeacherAssignmentPageView listAssignments(Long teacherId, String keyword, String status, Integer page, Integer size) {
        QueryWrapper<Assignment> wrapper = new QueryWrapper<>();
        wrapper.eq("teacher_id", teacherId).orderByDesc("create_time");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("title", keyword.trim());
        }
        List<Assignment> assignments = assignmentMapper.selectList(wrapper);
        Map<Long, Boolean> plagiarismAvailability = listPlagiarismAvailability(assignments);
        Map<Long, List<AssignmentClass>> assignmentClassesByAssignmentId = listAssignmentClassesByAssignmentIds(assignments);
        Map<Integer, Clazz> clazzById = listClazzMap(flattenAssignmentClasses(assignmentClassesByAssignmentId.values()));
        Map<Long, List<Submission>> latestSubmissionsByAssignmentId = listLatestSubmissionsByAssignmentIds(assignments);
        Map<Long, Integer> materialCountByAssignmentId = listMaterialCountByAssignmentIds(assignments);
        List<TeacherAssignmentSummaryView> summaries = assignments.stream()
                .map(assignment -> {
                    List<AssignmentClass> assignmentClasses = assignmentClassesByAssignmentId.getOrDefault(assignment.getId(), List.of());
                    List<Submission> latestSubmissions = latestSubmissionsByAssignmentId.getOrDefault(assignment.getId(), List.of());
                    TeacherAssignmentStatsView stats = buildAssignmentStats(assignmentClasses, clazzById, latestSubmissions, null);
                    int materialCount = materialCountByAssignmentId.getOrDefault(assignment.getId(), 0);
                    return new TeacherAssignmentSummaryView(
                            assignment.getId(),
                            assignment.getTitle(),
                            assignment.getLanguage(),
                            assignment.getStartAt(),
                            assignment.getEndAt(),
                            deriveStatus(assignment),
                            stats.classCount(),
                            stats.studentCount(),
                            stats.submittedStudentCount(),
                            stats.unsubmittedStudentCount(),
                            stats.lateSubmissionCount(),
                            materialCount,
                            plagiarismAvailability.getOrDefault(assignment.getId(), false)
                    );
                })
                .filter(summary -> matchesSummaryStatus(summary, status))
                .toList();
        long total = summaries.size();
        List<TeacherAssignmentSummaryView> pagedRecords = summaries.stream()
                .skip((long) (Math.max(page, 1) - 1) * Math.max(size, 1))
                .limit(Math.max(size, 1))
                .toList();
        return new TeacherAssignmentPageView(pagedRecords, total);
    }

    @Override
    public TeacherAssignmentDetailView getAssignmentDetail(Long teacherId, Long assignmentId) {
        Assignment assignment = requireTeacherAssignment(teacherId, assignmentId);
        List<AssignmentClass> assignmentClasses = listAssignmentClasses(assignmentId);
        Map<Integer, Clazz> clazzById = listClazzMap(assignmentClasses);
        TeacherAssignmentStatsView stats = getAssignmentStats(teacherId, assignmentId, null);
        Map<Integer, Long> submittedByClass = listLatestSubmissions(assignmentId).stream()
                .collect(Collectors.groupingBy(Submission::getClassId, Collectors.mapping(Submission::getStudentId, Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size()))));

        List<TeacherAssignmentClassView> classes = assignmentClasses.stream()
                .map(assignmentClass -> {
                    Clazz clazz = clazzById.get(assignmentClass.getClassId());
                    int studentCount = clazz == null || clazz.getStudentCount() == null ? 0 : clazz.getStudentCount();
                    int submittedStudentCount = submittedByClass.getOrDefault(assignmentClass.getClassId(), 0L).intValue();
                    return new TeacherAssignmentClassView(assignmentClass.getClassId(), clazz == null ? "" : clazz.getClassName(), studentCount, submittedStudentCount);
                })
                .toList();

        List<TeacherAssignmentMaterialView> materials = listMaterials(assignmentId).stream()
                .map(material -> new TeacherAssignmentMaterialView(
                        material.getId(),
                        material.getOriginalName(),
                        material.getSizeBytes(),
                        material.getContentType(),
                        "/api/teacher/assignments/" + assignmentId + "/materials/" + material.getId() + "/download"
                ))
                .toList();

        return new TeacherAssignmentDetailView(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getLanguage(),
                assignment.getDescription(),
                assignment.getStartAt(),
                assignment.getEndAt(),
                deriveStatus(assignment),
                assignment.getAllowResubmit() != null && assignment.getAllowResubmit() == 1,
                assignment.getAllowLateSubmit() != null && assignment.getAllowLateSubmit() == 1,
                assignment.getMaxFiles() == null ? 20 : assignment.getMaxFiles(),
                classes,
                materials,
                stats
        );
    }

    @Override
    @Transactional
    public Assignment updateAssignment(Long teacherId, Long assignmentId, TeacherAssignmentCreateRequest request) {
        validateCreateRequest(request);
        Assignment assignment = requireTeacherAssignment(teacherId, assignmentId);
        requireTeacherClasses(teacherId, request.classIds());
        applyAssignmentFields(assignment, request);
        assignment.setClazzId(request.classIds().get(0));
        assignment.setUpdateTime(LocalDateTime.now());
        assignmentMapper.updateById(assignment);

        QueryWrapper<AssignmentClass> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("assignment_id", assignmentId);
        assignmentClassMapper.delete(deleteWrapper);
        for (Integer classId : request.classIds()) {
            AssignmentClass assignmentClass = new AssignmentClass();
            assignmentClass.setAssignmentId(assignmentId);
            assignmentClass.setClassId(classId);
            assignmentClass.setPublishStatus("PUBLISHED");
            assignmentClass.setCreateTime(LocalDateTime.now());
            assignmentClassMapper.insert(assignmentClass);
        }
        return assignment;
    }

    @Override
    @Transactional
    public Assignment reopenAssignment(Long teacherId, Long assignmentId, TeacherAssignmentReopenRequest request) {
        Assignment assignment = requireTeacherAssignment(teacherId, assignmentId);
        if (request == null || request.startAt() == null || request.endAt() == null || !request.startAt().isBefore(request.endAt())) {
            throw new BusinessException("重开时间窗不合法");
        }

        AssignmentReopenLog log = new AssignmentReopenLog();
        log.setAssignmentId(assignmentId);
        log.setOldStartAt(assignment.getStartAt());
        log.setOldEndAt(assignment.getEndAt());
        log.setNewStartAt(request.startAt());
        log.setNewEndAt(request.endAt());
        log.setReason(request.reason());
        log.setOperatorId(teacherId);
        log.setCreatedAt(LocalDateTime.now());
        assignmentReopenLogMapper.insert(log);

        assignment.setStartAt(request.startAt());
        assignment.setEndAt(request.endAt());
        assignment.setDeadline(request.endAt());
        assignment.setStatus("PUBLISHED");
        assignment.setUpdateTime(LocalDateTime.now());
        assignmentMapper.updateById(assignment);
        return assignment;
    }

    @Override
    @Transactional
    public Assignment closeAssignmentNow(Long teacherId, Long assignmentId) {
        Assignment assignment = requireTeacherAssignment(teacherId, assignmentId);
        LocalDateTime now = LocalDateTime.now();
        assignment.setEndAt(now);
        assignment.setDeadline(now);
        assignment.setUpdateTime(now);
        assignmentMapper.updateById(assignment);
        return assignment;
    }

    @Override
    @Transactional
    public void deleteAssignment(Long teacherId, Long assignmentId) {
        requireTeacherAssignment(teacherId, assignmentId);
        clearPlagiarismJobs(assignmentId);
        deleteSubmissionPayload(assignmentId);
        deleteAssignmentResources(assignmentId);
    }

    @Override
    @Transactional
    public List<AssignmentMaterial> uploadMaterials(Long teacherId, Long assignmentId, List<MultipartFile> files) {
        requireTeacherAssignment(teacherId, assignmentId);
        if (files == null || files.isEmpty()) {
            throw new BusinessException("请至少上传一个作业资料文件");
        }

        List<AssignmentMaterial> materials = new ArrayList<>();
        int sortNo = countMaterials(assignmentId) + 1;
        for (MultipartFile file : files) {
            AssignmentMaterial material = new AssignmentMaterial();
            material.setAssignmentId(assignmentId);
            material.setStorageType("LOCAL");
            material.setOriginalName(file.getOriginalFilename());
            material.setContentType(file.getContentType());
            material.setSortNo(sortNo++);
            material.setCreatedBy(teacherId);
            material.setCreatedAt(LocalDateTime.now());
            assignmentMaterialMapper.insert(material);

            StoredFile storedFile = assignmentMaterialStorageService.store(assignmentId, material.getId(), file);
            material.setRelativePath(storedFile.storagePath());
            material.setSha256(storedFile.sha256());
            material.setSizeBytes(storedFile.bytes());
            assignmentMaterialMapper.updateById(material);
            materials.add(material);
        }
        return materials;
    }

    @Override
    public AssignmentMaterial getMaterial(Long teacherId, Long assignmentId, Long materialId) {
        requireTeacherAssignment(teacherId, assignmentId);
        AssignmentMaterial material = assignmentMaterialMapper.selectById(materialId);
        if (material == null || !assignmentId.equals(material.getAssignmentId())) {
            throw new BusinessException("作业资料不存在");
        }
        return material;
    }

    @Override
    public Resource loadMaterialResource(Long teacherId, Long assignmentId, Long materialId) {
        AssignmentMaterial material = getMaterial(teacherId, assignmentId, materialId);
        return assignmentMaterialStorageService.loadAsResource(material.getRelativePath());
    }

    @Override
    @Transactional
    public void deleteMaterial(Long teacherId, Long assignmentId, Long materialId) {
        requireTeacherAssignment(teacherId, assignmentId);
        AssignmentMaterial material = assignmentMaterialMapper.selectById(materialId);
        if (material == null || !assignmentId.equals(material.getAssignmentId())) {
            throw new BusinessException("作业资料不存在");
        }
        assignmentMaterialStorageService.delete(material.getRelativePath());
        assignmentMaterialMapper.deleteById(materialId);
    }

    @Override
    public TeacherAssignmentStatsView getAssignmentStats(Long teacherId, Long assignmentId, Integer classId) {
        requireTeacherAssignment(teacherId, assignmentId);
        List<AssignmentClass> assignmentClasses = listAssignmentClasses(assignmentId);
        Map<Integer, Clazz> clazzById = listClazzMap(assignmentClasses);
        List<Submission> latestSubmissions = listLatestSubmissions(assignmentId).stream()
                .toList();
        return buildAssignmentStats(assignmentClasses, clazzById, latestSubmissions, classId);
    }

    @Override
    public List<TeacherSubmissionOverview> listAssignmentSubmissions(Long teacherId, Long assignmentId) {
        requireTeacherAssignment(teacherId, assignmentId);
        return listLatestSubmissions(assignmentId).stream()
                .sorted((left, right) -> {
                    LocalDateTime leftTime = left.getSubmitTime();
                    LocalDateTime rightTime = right.getSubmitTime();
                    if (leftTime == null && rightTime == null) {
                        return Long.compare(right.getId(), left.getId());
                    }
                    if (leftTime == null) {
                        return 1;
                    }
                    if (rightTime == null) {
                        return -1;
                    }
                    int timeCompare = rightTime.compareTo(leftTime);
                    return timeCompare != 0 ? timeCompare : Long.compare(right.getId(), left.getId());
                })
                .map(submission -> new TeacherSubmissionOverview(
                        submission.getId(),
                        submission.getStudentId(),
                        submission.getIsLate(),
                        submission.getIsValid(),
                        submission.getParseOkFiles(),
                        submission.getTotalFiles(),
                        submission.getSubmitTime()
                ))
                .toList();
    }

    private void validateCreateRequest(TeacherAssignmentCreateRequest request) {
        if (request == null) {
            throw new BusinessException("作业参数不能为空");
        }
        if (request.title() == null || request.title().isBlank() || request.title().length() > 80) {
            throw new BusinessException("作业标题不合法");
        }
        if (request.language() == null || request.language().isBlank()) {
            throw new BusinessException("编程语言不能为空");
        }
        if (request.classIds() == null || request.classIds().isEmpty()) {
            throw new BusinessException("至少选择一个班级");
        }
        if (request.startAt() == null || request.endAt() == null || !request.startAt().isBefore(request.endAt())) {
            throw new BusinessException("作业时间窗不合法");
        }
        if (request.description() != null && request.description().length() > 600) {
            throw new BusinessException("作业说明过长");
        }
        if (request.maxFiles() != null && (request.maxFiles() < 1 || request.maxFiles() > 50)) {
            throw new BusinessException("提交文件数限制不合法");
        }
    }

    private void applyAssignmentFields(Assignment assignment, TeacherAssignmentCreateRequest request) {
        assignment.setTitle(request.title().trim());
        assignment.setLanguage(request.language().trim().toUpperCase());
        assignment.setDescription(request.description());
        assignment.setStartAt(request.startAt());
        assignment.setEndAt(request.endAt());
        assignment.setDeadline(request.endAt());
        assignment.setAllowResubmit(Boolean.TRUE.equals(request.allowResubmit()) ? 1 : 0);
        assignment.setAllowLateSubmit(Boolean.TRUE.equals(request.allowLateSubmit()) ? 1 : 0);
        assignment.setMaxFiles(request.maxFiles() == null ? 20 : request.maxFiles());
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

    private List<Clazz> requireTeacherClasses(Long teacherId, List<Integer> classIds) {
        List<Clazz> classes = classMapper.selectBatchIds(classIds);
        if (classes.size() != classIds.size()) {
            throw new BusinessException("存在无效班级");
        }
        for (Clazz clazz : classes) {
            if (!teacherId.equals(clazz.getTeacherId())) {
                throw new BusinessException(403, "存在不属于当前教师的班级");
            }
        }
        return classes;
    }

    private List<AssignmentClass> listAssignmentClasses(Long assignmentId) {
        QueryWrapper<AssignmentClass> wrapper = new QueryWrapper<>();
        wrapper.eq("assignment_id", assignmentId).orderByAsc("class_id");
        return assignmentClassMapper.selectList(wrapper);
    }

    private Map<Long, List<AssignmentClass>> listAssignmentClassesByAssignmentIds(List<Assignment> assignments) {
        List<Long> assignmentIds = assignments.stream()
                .map(Assignment::getId)
                .filter(Objects::nonNull)
                .toList();
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }
        return assignmentClassMapper.selectList(new QueryWrapper<AssignmentClass>()
                        .in("assignment_id", assignmentIds)
                        .orderByAsc("assignment_id")
                        .orderByAsc("class_id"))
                .stream()
                .collect(Collectors.groupingBy(
                        AssignmentClass::getAssignmentId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private List<AssignmentClass> flattenAssignmentClasses(Collection<List<AssignmentClass>> groupedAssignmentClasses) {
        return groupedAssignmentClasses.stream()
                .flatMap(List::stream)
                .toList();
    }

    private Map<Integer, Clazz> listClazzMap(List<AssignmentClass> assignmentClasses) {
        List<Integer> classIds = assignmentClasses.stream()
                .map(AssignmentClass::getClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return classMapper.selectClassesByIdsWithStudentCount(classIds).stream()
                .collect(Collectors.toMap(Clazz::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private List<AssignmentMaterial> listMaterials(Long assignmentId) {
        QueryWrapper<AssignmentMaterial> wrapper = new QueryWrapper<>();
        wrapper.eq("assignment_id", assignmentId).orderByAsc("sort_no");
        return assignmentMaterialMapper.selectList(wrapper);
    }

    private void deleteSubmissionPayload(Long assignmentId) {
        List<Submission> submissions = submissionMapper.selectList(new QueryWrapper<Submission>()
                .eq("assignment_id", assignmentId));
        if (submissions.isEmpty()) {
            return;
        }

        List<Long> submissionIds = submissions.stream()
                .map(Submission::getId)
                .filter(Objects::nonNull)
                .toList();
        if (submissionIds.isEmpty()) {
            return;
        }

        List<SubmissionFile> files = submissionFileMapper.selectList(new QueryWrapper<SubmissionFile>()
                .in("submission_id", submissionIds));
        for (SubmissionFile file : files) {
            if (file.getStoragePath() != null && !file.getStoragePath().isBlank()) {
                localStorageService.delete(file.getStoragePath());
            }
        }

        submissionFileMapper.delete(new QueryWrapper<SubmissionFile>().in("submission_id", submissionIds));
        submissionProfileMapper.delete(new QueryWrapper<com.ast.back.modules.plagiarism.persistence.entity.SubmissionProfile>()
                .in("submission_id", submissionIds));
        submissionMapper.delete(new QueryWrapper<Submission>().eq("assignment_id", assignmentId));
    }

    private void clearPlagiarismJobs(Long assignmentId) {
        List<PlagiarismJob> jobs = plagiarismJobMapper.selectList(new QueryWrapper<PlagiarismJob>()
                .eq("assignment_id", assignmentId));
        if (jobs.isEmpty()) {
            return;
        }

        List<Long> jobIds = jobs.stream()
                .map(PlagiarismJob::getId)
                .filter(Objects::nonNull)
                .toList();
        List<SimilarityPair> pairs = similarityPairMapper.selectList(new QueryWrapper<SimilarityPair>()
                .in("job_id", jobIds));
        if (!pairs.isEmpty()) {
            List<Long> pairIds = pairs.stream()
                    .map(SimilarityPair::getId)
                    .filter(Objects::nonNull)
                    .toList();
            similarityEvidenceMapper.delete(new QueryWrapper<SimilarityEvidence>().in("pair_id", pairIds));
        }

        similarityPairMapper.delete(new QueryWrapper<SimilarityPair>().in("job_id", jobIds));
        plagiarismJobMapper.delete(new QueryWrapper<PlagiarismJob>().eq("assignment_id", assignmentId));
    }

    private void deleteAssignmentResources(Long assignmentId) {
        List<AssignmentMaterial> materials = listMaterials(assignmentId);
        for (AssignmentMaterial material : materials) {
            if (material.getRelativePath() != null && !material.getRelativePath().isBlank()) {
                assignmentMaterialStorageService.delete(material.getRelativePath());
            }
        }
        assignmentMaterialMapper.delete(new QueryWrapper<AssignmentMaterial>().eq("assignment_id", assignmentId));
        assignmentReopenLogMapper.delete(new QueryWrapper<AssignmentReopenLog>().eq("assignment_id", assignmentId));
        assignmentClassMapper.delete(new QueryWrapper<AssignmentClass>().eq("assignment_id", assignmentId));
        assignmentMapper.deleteById(assignmentId);
    }

    private int countMaterials(Long assignmentId) {
        QueryWrapper<AssignmentMaterial> wrapper = new QueryWrapper<>();
        wrapper.eq("assignment_id", assignmentId);
        Long count = assignmentMaterialMapper.selectCount(wrapper);
        return count == null ? 0 : count.intValue();
    }

    private Map<Long, Integer> listMaterialCountByAssignmentIds(List<Assignment> assignments) {
        List<Long> assignmentIds = assignments.stream()
                .map(Assignment::getId)
                .filter(Objects::nonNull)
                .toList();
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }
        return assignmentMaterialMapper.selectList(new QueryWrapper<AssignmentMaterial>()
                        .in("assignment_id", assignmentIds))
                .stream()
                .collect(Collectors.toMap(
                        AssignmentMaterial::getAssignmentId,
                        material -> 1,
                        Integer::sum,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, Boolean> listPlagiarismAvailability(List<Assignment> assignments) {
        List<Long> assignmentIds = assignments.stream()
                .map(Assignment::getId)
                .filter(Objects::nonNull)
                .toList();
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }

        QueryWrapper<PlagiarismJob> wrapper = new QueryWrapper<>();
        wrapper.in("assignment_id", assignmentIds).select("assignment_id");
        return plagiarismJobMapper.selectList(wrapper).stream()
                .map(PlagiarismJob::getAssignmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(id -> id, id -> true, (left, right) -> left, LinkedHashMap::new));
    }

    private List<Submission> listLatestSubmissions(Long assignmentId) {
        QueryWrapper<Submission> wrapper = new QueryWrapper<>();
        wrapper.eq("assignment_id", assignmentId)
                .eq("is_latest", 1)
                .orderByDesc("submit_time")
                .orderByDesc("id");
        return submissionMapper.selectList(wrapper);
    }

    private Map<Long, List<Submission>> listLatestSubmissionsByAssignmentIds(List<Assignment> assignments) {
        List<Long> assignmentIds = assignments.stream()
                .map(Assignment::getId)
                .filter(Objects::nonNull)
                .toList();
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }
        return submissionMapper.selectList(new QueryWrapper<Submission>()
                        .in("assignment_id", assignmentIds)
                        .eq("is_latest", 1)
                        .orderByDesc("submit_time")
                        .orderByDesc("id"))
                .stream()
                .collect(Collectors.groupingBy(
                        Submission::getAssignmentId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private TeacherAssignmentStatsView buildAssignmentStats(
            List<AssignmentClass> assignmentClasses,
            Map<Integer, Clazz> clazzById,
            List<Submission> latestSubmissions,
            Integer classId
    ) {
        List<AssignmentClass> filteredAssignmentClasses = assignmentClasses;
        if (classId != null) {
            filteredAssignmentClasses = assignmentClasses.stream()
                    .filter(item -> Objects.equals(item.getClassId(), classId))
                    .toList();
        }
        List<Submission> filteredSubmissions = latestSubmissions.stream()
                .filter(submission -> classId == null || Objects.equals(submission.getClassId(), classId))
                .toList();

        int classCount = filteredAssignmentClasses.size();
        int studentCount = filteredAssignmentClasses.stream()
                .map(AssignmentClass::getClassId)
                .map(clazzById::get)
                .filter(Objects::nonNull)
                .map(Clazz::getStudentCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int submittedStudentCount = filteredSubmissions.stream()
                .map(submission -> submission.getClassId() + ":" + submission.getStudentId())
                .collect(Collectors.toSet())
                .size();
        int lateSubmissionCount = (int) filteredSubmissions.stream()
                .filter(submission -> submission.getIsLate() != null && submission.getIsLate() == 1)
                .count();
        int latestSubmissionCount = filteredSubmissions.size();
        return new TeacherAssignmentStatsView(
                classCount,
                studentCount,
                submittedStudentCount,
                Math.max(studentCount - submittedStudentCount, 0),
                lateSubmissionCount,
                latestSubmissionCount
        );
    }

    private boolean matchesSummaryStatus(TeacherAssignmentSummaryView summary, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        return normalizeSummaryStatus(summary.status()).equalsIgnoreCase(normalizeSummaryStatus(status));
    }

    private String normalizeSummaryStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        return switch (normalized) {
            case "CLOSED", "ENDED" -> "ENDED";
            case "PUBLISHED", "ACTIVE" -> "ACTIVE";
            case "DRAFT" -> "DRAFT";
            case "ARCHIVED" -> "ARCHIVED";
            default -> normalized;
        };
    }

    private String deriveStatus(Assignment assignment) {
        if (assignment.getStatus() != null && !assignment.getStatus().isBlank()) {
            if ("ARCHIVED".equalsIgnoreCase(assignment.getStatus()) || "DRAFT".equalsIgnoreCase(assignment.getStatus())) {
                return assignment.getStatus();
            }
        }
        LocalDateTime now = LocalDateTime.now();
        if (assignment.getEndAt() != null && now.isAfter(assignment.getEndAt())) {
            return "CLOSED";
        }
        return "PUBLISHED";
    }
}

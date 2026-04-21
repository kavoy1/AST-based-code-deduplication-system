import request from './request'
import {
  formatDateTimeForApi,
  normalizeAssignmentDetail,
  normalizeAssignmentSummary,
  normalizeJob,
  normalizePairDetail,
  normalizeReport,
  normalizeSubmission
} from '../views/teacher/assignmentMappers'

function buildAssignmentPayload(payload) {
  return {
    title: payload.title,
    language: payload.language,
    classIds: payload.classIds,
    startAt: formatDateTimeForApi(payload.startAt),
    endAt: formatDateTimeForApi(payload.endAt),
    description: payload.description,
    allowResubmit: payload.allowResubmit,
    allowLateSubmit: payload.allowLateSubmit,
    maxFiles: payload.maxFiles
  }
}

function buildReportParams(params = {}) {
  const normalizedStatuses = Array.isArray(params.statuses)
    ? params.statuses.filter(Boolean)
    : params.statuses
      ? String(params.statuses).split(',').map(item => item.trim()).filter(Boolean)
      : []
  return {
    minScore: params.minScore ?? 0,
    perStudentTopK: params.perStudentTopK ?? 0,
    statuses: normalizedStatuses.length ? normalizedStatuses.join(',') : undefined,
    sortBy: params.sortBy || 'score',
    sortDirection: params.sortDirection || 'desc'
  }
}

function findLatestJobByMode(jobs = [], plagiarismMode) {
  if (!plagiarismMode) return jobs[0] || null
  const normalizedMode = String(plagiarismMode || 'FAST').trim().toUpperCase()
  return jobs.find((job) => String(job?.plagiarismMode || 'FAST').trim().toUpperCase() === normalizedMode) || null
}

export async function fetchTeacherAssignments(params = {}) {
  const data = await request.get('/teacher/assignments', { params })
  const records = Array.isArray(data) ? data : Array.isArray(data?.records) ? data.records : []
  const total = Number(data?.total ?? data?.count ?? data?.pagination?.total)
  return {
    records: records.map(normalizeAssignmentSummary),
    total: Number.isFinite(total) && total > 0 ? total : records.length
  }
}

export async function createTeacherAssignment(payload) {
  return request.post('/teacher/assignments', buildAssignmentPayload(payload))
}

export async function updateTeacherAssignment(assignmentId, payload) {
  return request.put(`/teacher/assignments/${assignmentId}`, buildAssignmentPayload(payload))
}

export async function deleteTeacherAssignment(assignmentId) {
  return request.delete(`/teacher/assignments/${assignmentId}`)
}

export async function fetchTeacherAssignmentDetail(assignmentId) {
  const data = await request.get(`/teacher/assignments/${assignmentId}`)
  return normalizeAssignmentDetail(data)
}

export async function reopenTeacherAssignment(assignmentId, payload) {
  return request.post(`/teacher/assignments/${assignmentId}/reopen`, {
    startAt: formatDateTimeForApi(payload.startAt),
    endAt: formatDateTimeForApi(payload.endAt),
    reason: payload.reason || ''
  })
}

export async function closeTeacherAssignmentNow(assignmentId) {
  return request.post(`/teacher/assignments/${assignmentId}/close-now`)
}

export async function archiveTeacherAssignment(assignmentId) {
  return request.post(`/teacher/assignments/${assignmentId}/archive`)
}

export async function restoreTeacherAssignment(assignmentId) {
  return request.post(`/teacher/assignments/${assignmentId}/restore`)
}

export async function fetchTeacherAssignmentSubmissions(assignmentId) {
  const data = await request.get(`/teacher/assignments/${assignmentId}/submissions`)
  const records = Array.isArray(data) ? data : Array.isArray(data?.records) ? data.records : []
  return records.map(normalizeSubmission)
}

export async function fetchTeacherAssignmentStats(assignmentId, classId) {
  return request.get(`/teacher/assignments/${assignmentId}/stats`, {
    params: classId ? { classId } : {}
  })
}

export async function uploadTeacherAssignmentMaterials(assignmentId, files) {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  return request.post(`/teacher/assignments/${assignmentId}/materials`, formData)
}

export async function deleteTeacherAssignmentMaterial(assignmentId, materialId) {
  return request.delete(`/teacher/assignments/${assignmentId}/materials/${materialId}`)
}

export function getTeacherAssignmentMaterialDownloadUrl(assignmentId, materialId) {
  return `/api/teacher/assignments/${assignmentId}/materials/${materialId}/download`
}

export async function createTeacherPlagiarismJob(assignmentId, payload) {
  const data = await request.post(`/teacher/assignments/${assignmentId}/plagiarism-jobs`, {
    thresholdScore: payload.thresholdScore ?? 80,
    topKPerStudent: payload.topKPerStudent ?? 0,
    plagiarismMode: payload.plagiarismMode || 'FAST'
  })
  return normalizeJob(data)
}

export async function fetchTeacherPlagiarismJobs(assignmentId) {
  const data = await request.get(`/teacher/assignments/${assignmentId}/plagiarism-jobs`)
  const records = Array.isArray(data) ? data : Array.isArray(data?.records) ? data.records : []
  return records.map(normalizeJob)
}

export async function fetchTeacherPlagiarismJob(jobId) {
  const data = await request.get(`/teacher/plagiarism-jobs/${jobId}`)
  return normalizeJob(data)
}

export async function fetchTeacherPlagiarismReport(jobId, params = {}) {
  const data = await request.get(`/teacher/plagiarism-jobs/${jobId}/report`, {
    params: buildReportParams(params)
  })
  return normalizeReport(data)
}

export async function fetchTeacherAssignmentPlagiarism(assignmentId, params = {}, plagiarismMode = null) {
  const jobs = await fetchTeacherPlagiarismJobs(assignmentId)
  const latestJob = findLatestJobByMode(jobs, plagiarismMode)
  const report = latestJob
    ? await fetchTeacherPlagiarismReport(latestJob.id, params)
    : {
        jobId: null,
        status: '',
        message: '',
        minScore: params.minScore ?? 0,
        perStudentTopK: params.perStudentTopK ?? 0,
        jobStats: null,
        pairs: [],
        incomparableSubmissions: []
      }

  return {
    jobs,
    activeJobId: latestJob?.id || null,
    report
  }
}

export async function exportTeacherPlagiarismReport(jobId, params = {}) {
  return request.get(`/teacher/plagiarism-jobs/${jobId}/export`, {
    params: {
      ...buildReportParams(params),
      format: params.format || 'csv'
    },
    responseType: 'blob'
  })
}

export async function fetchTeacherPairDetail(pairId) {
  const data = await request.get(`/teacher/similarity-pairs/${pairId}`)
  return normalizePairDetail(data)
}

export async function updateTeacherPairStatus(pairId, payload) {
  return request.post(`/teacher/similarity-pairs/${pairId}/status`, payload)
}

export async function createTeacherAiExplanation(pairId, payload = {}) {
  return request.post(`/teacher/similarity-pairs/${pairId}/ai-explanations`, {
    mode: payload.mode || 'CODE_ONLY',
    includeTeacherNote: Boolean(payload.includeTeacherNote)
  })
}

export async function fetchTeacherAiExplanationHistory(pairId) {
  const data = await request.get(`/teacher/similarity-pairs/${pairId}/ai-explanations`)
  return Array.isArray(data) ? data : []
}

export async function fetchLatestTeacherAiExplanation(pairId) {
  return request.get(`/teacher/similarity-pairs/${pairId}/ai-explanations/latest`)
}

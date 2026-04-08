import request from './request'
import { normalizeStudentCurrentSubmission } from '../views/student/studentSubmissionHelpers'

function toArray(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  return []
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function resolveAssignmentEndAt(raw = {}) {
  return raw.endAt || raw.deadline || ''
}

function inferAssignmentStatus(raw = {}) {
  const now = Date.now()
  const startAt = raw.startAt ? new Date(raw.startAt).getTime() : null
  const endAt = resolveAssignmentEndAt(raw) ? new Date(resolveAssignmentEndAt(raw)).getTime() : null
  const statusValue = String(raw.status || '').toLowerCase()

  if (['closed', 'ended', 'finished'].includes(statusValue)) return 'ended'
  if (startAt && startAt > now) return 'pending'
  if (endAt && endAt < now) return 'ended'
  return 'active'
}

export function normalizeStudentClass(raw = {}) {
  return {
    id: raw.id,
    className: raw.className || '未命名班级',
    courseName: raw.courseName || '未设置课程',
    teacherId: raw.teacherId || null,
    teacherName: raw.teacherName || '未提供',
    joinTime: raw.joinTime || '',
    joinTimeLabel: formatDateTime(raw.joinTime),
    status: Number(raw.status ?? 0),
    statusText: Number(raw.status) === 1 ? '已加入' : '审核中'
  }
}

export function normalizeStudentAssignment(raw = {}, context = {}) {
  const status = inferAssignmentStatus(raw)
  const endAt = resolveAssignmentEndAt(raw)
  return {
    id: raw.id,
    title: raw.title || '未命名作业',
    language: raw.language || 'Java',
    description: raw.description || '',
    startAt: raw.startAt || '',
    endAt,
    startAtLabel: formatDateTime(raw.startAt),
    endAtLabel: formatDateTime(endAt),
    status,
    statusText: status === 'active' ? '进行中' : status === 'pending' ? '未开始' : '已结束',
    statusTagType: status === 'active' ? 'success' : status === 'pending' ? 'warning' : 'info',
    allowResubmit: Number(raw.allowResubmit ?? 0) === 1,
    allowLateSubmit: Number(raw.allowLateSubmit ?? 0) === 1,
    maxFiles: Number(raw.maxFiles ?? 0) || 1,
    materialCount: Number(raw.materialCount ?? 0) || 0,
    classId: context.classId ?? null,
    className: context.className || '',
    canSubmit: status !== 'ended'
  }
}

export async function fetchStudentClasses() {
  const data = await request.get('/student/classes')
  return toArray(data).map(normalizeStudentClass)
}

export async function joinStudentClass(inviteCode) {
  return request.post('/student/classes/join', null, { params: { inviteCode } })
}

export async function fetchStudentAssignmentsByClass(classId, context = {}) {
  const data = await request.get(`/student/classes/${classId}/assignments`)
  return toArray(data).map((item) => normalizeStudentAssignment(item, context))
}

export async function fetchStudentAssignmentDetail(assignmentId, context = {}) {
  const data = await request.get(`/student/assignments/${assignmentId}`)
  return normalizeStudentAssignment(data, context)
}

export async function submitStudentAssignmentFiles(assignmentId, files = []) {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  return request.post(`/student/assignments/${assignmentId}/submissions`, formData)
}

export async function submitStudentAssignmentText(assignmentId, entries = []) {
  const payloadEntries = entries
    .map((entry) => ({
      filename: (entry.filename || '').trim(),
      content: entry.content || ''
    }))
    .filter((entry) => entry.content.trim())

  return request.post(`/student/assignments/${assignmentId}/submissions/text`, {
    entries: payloadEntries
  })
}

export async function fetchStudentCurrentSubmission(assignmentId) {
  const data = await request.get(`/student/assignments/${assignmentId}/current-submission`)
  return data ? normalizeStudentCurrentSubmission(data) : null
}

export async function fetchStudentPlagiarismSummary(assignmentId) {
  const data = await request.get(`/student/assignments/${assignmentId}/plagiarism-summary`)
  return {
    generated: Boolean(data?.generated),
    message: data?.message || '',
    highestScore: Number(data?.highestScore ?? 0) || 0,
    status: data?.status || '',
    teacherNote: data?.teacherNote || ''
  }
}

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

let draftSeed = 0

function nextDraftId() {
  draftSeed += 1
  return `draft-${draftSeed}`
}

function buildDefaultFilename(index) {
  return index === 0 ? 'Main.java' : `Main_${index + 1}.java`
}

function formatBytes(value) {
  const bytes = Number(value || 0)
  if (!bytes) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  const kb = bytes / 1024
  if (kb < 1024) return `${kb.toFixed(kb >= 10 ? 0 : 1)} KB`
  const mb = kb / 1024
  return `${mb.toFixed(mb >= 10 ? 0 : 1)} MB`
}

function buildPreview(content) {
  const cleaned = String(content || '').replace(/\s+/g, ' ').trim()
  if (!cleaned) return '暂无代码内容'
  return cleaned.length > 220 ? `${cleaned.slice(0, 220)}...` : cleaned
}

function ensureJavaFilename(filename) {
  const trimmed = String(filename || '').trim()
  if (!trimmed) return ''
  return trimmed.toLowerCase().endsWith('.java') ? trimmed : `${trimmed}.java`
}

export function normalizeDraftEntries(entries = []) {
  const used = new Set()
  return entries
    .filter((entry) => String(entry?.content || '').trim())
    .map((entry, index) => {
      const sourceFilename = ensureJavaFilename(entry?.filename) || buildDefaultFilename(index)
      let candidate = sourceFilename
      let duplicateIndex = 2
      while (used.has(candidate.toLowerCase())) {
        const extensionIndex = sourceFilename.lastIndexOf('.')
        const baseName = extensionIndex >= 0 ? sourceFilename.slice(0, extensionIndex) : sourceFilename
        const extension = extensionIndex >= 0 ? sourceFilename.slice(extensionIndex) : '.java'
        candidate = `${baseName}_${duplicateIndex}${extension}`
        duplicateIndex += 1
      }
      used.add(candidate.toLowerCase())
      return {
        filename: candidate,
        content: entry.content
      }
    })
}

export function normalizeStudentCurrentSubmission(raw = {}) {
  const files = Array.isArray(raw.files)
    ? raw.files.map((file) => ({
        filename: file.filename || '',
        bytes: Number(file.bytes ?? 0) || 0,
        parseStatus: file.parseStatus || '',
        parseError: file.parseError || '',
        content: file.content || ''
      }))
    : []

  return {
    id: raw.id ?? null,
    assignmentId: raw.assignmentId ?? null,
    classId: raw.classId ?? null,
    studentId: raw.studentId ?? null,
    submitTime: raw.submitTime || '',
    submitTimeLabel: formatDateTime(raw.submitTime),
    isValid: Number(raw.isValid ?? 0) === 1,
    parseOkFiles: Number(raw.parseOkFiles ?? 0) || 0,
    totalFiles: Number(raw.totalFiles ?? 0) || 0,
    isLate: Number(raw.isLate ?? 0) === 1,
    deadlineAt: raw.deadlineAt || '',
    deadlineAtLabel: formatDateTime(raw.deadlineAt),
    files
  }
}

export function buildCodeEntriesFromCurrentSubmission(currentSubmission) {
  const files = Array.isArray(currentSubmission?.files) ? currentSubmission.files : []
  const drafts = files
    .filter((file) => String(file.filename || '').toLowerCase().endsWith('.java'))
    .sort((left, right) => {
      const leftIsMain = String(left.filename || '').toLowerCase() === 'main.java'
      const rightIsMain = String(right.filename || '').toLowerCase() === 'main.java'
      if (leftIsMain && !rightIsMain) return -1
      if (!leftIsMain && rightIsMain) return 1
      return String(left.filename || '').localeCompare(String(right.filename || ''), 'zh-CN', { sensitivity: 'base' })
    })
    .map((file) => ({
      id: nextDraftId(),
      filename: file.filename || '',
      content: file.content || ''
    }))

  return drafts.length ? drafts : [{ id: nextDraftId(), filename: 'Main.java', content: '' }]
}

export function buildSubmissionFileCards(currentSubmission) {
  const files = Array.isArray(currentSubmission?.files) ? currentSubmission.files : []

  return files.map((file) => {
    const parseOk = String(file.parseStatus || '').toUpperCase() === 'OK'
    const content = String(file.content || '')
    return {
      filename: file.filename || '',
      bytesLabel: formatBytes(file.bytes),
      parseStatus: file.parseStatus || '',
      parseLabel: parseOk ? '可解析' : '解析失败',
      parseTone: parseOk ? 'success' : 'danger',
      parseError: file.parseError || '',
      preview: file.parseError || buildPreview(content),
      content,
      hasContent: Boolean(content.trim())
    }
  })
}

export function createEmptyCodeEntry(index = 0) {
  return {
    id: nextDraftId(),
    filename: buildDefaultFilename(index),
    content: ''
  }
}

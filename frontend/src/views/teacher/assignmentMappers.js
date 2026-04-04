function parseDate(value) {
  if (!value) return null
  if (value instanceof Date) return value
  const normalized = typeof value === 'string' ? value.replace(' ', 'T') : value
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatDateTime(value) {
  const date = parseDate(value)
  if (!date) return ''
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  const second = `${date.getSeconds()}`.padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

export function formatDateTimeForApi(value) {
  const date = parseDate(value)
  if (!date) return ''
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')}T${`${date.getHours()}`.padStart(2, '0')}:${`${date.getMinutes()}`.padStart(2, '0')}:${`${date.getSeconds()}`.padStart(2, '0')}`
}

export function formatDateTimeForInput(value) {
  const formatted = formatDateTimeForApi(value)
  return formatted ? formatted.slice(0, 16) : ''
}

export function normalizeClasses(payload) {
  const list = Array.isArray(payload?.records) ? payload.records : Array.isArray(payload) ? payload : []
  return list.map((item) => ({
    id: Number(item.id),
    name: item.className || item.name || `班级 ${item.id}`,
    studentCount: Number(item.studentCount || item.studentNum || 0)
  }))
}

function normalizeAssignmentStatus(status, endAt) {
  const raw = String(status || '').toUpperCase()
  if (raw === 'CLOSED' || raw === 'ENDED') {
    return { value: 'ended', label: '已结束' }
  }
  if (raw === 'PUBLISHED') {
    const endDate = parseDate(endAt)
    if (endDate && endDate.getTime() < Date.now()) {
      return { value: 'ended', label: '已结束' }
    }
    return { value: 'active', label: '进行中' }
  }
  if (raw === 'DRAFT') {
    return { value: 'draft', label: '草稿' }
  }
  return { value: raw.toLowerCase() || 'unknown', label: raw || '未知' }
}

export function normalizeAssignmentSummary(item) {
  const status = normalizeAssignmentStatus(item.status, item.endAt)
  const classCount = Number(item.classCount || 0)
  const studentCount = Number(item.studentCount || 0)
  const submittedCount = Number(item.submittedStudentCount || 0)
  const classIds = Array.isArray(item.classIds)
    ? item.classIds.map((value) => Number(value))
    : Array.isArray(item.classes)
      ? item.classes.map((entry) => Number(entry.classId || entry.id)).filter(Boolean)
      : []
  const classNames = Array.isArray(item.classNames)
    ? item.classNames
    : Array.isArray(item.classes)
      ? item.classes.map((entry) => entry.className || entry.name).filter(Boolean)
      : []
  return {
    id: item.id,
    title: item.title || '',
    language: item.language || 'JAVA',
    startAt: item.startAt,
    endAt: item.endAt,
    startTime: formatDateTime(item.startAt),
    endTime: formatDateTime(item.endAt),
    status: status.value,
    statusLabel: status.label,
    classCount,
    classIds,
    classNames,
    classNamesText: classCount ? `共 ${classCount} 个班级` : '未配置班级',
    studentCount,
    submittedCount,
    unsubmittedCount: Number(item.unsubmittedStudentCount || 0),
    lateSubmissionCount: Number(item.lateSubmissionCount || 0),
    materialCount: Number(item.materialCount || 0),
    progress: studentCount > 0 ? Math.round((submittedCount / studentCount) * 100) : 0,
    hasPlagiarismJob: Boolean(item.hasPlagiarismJob)
  }
}

export function normalizeAssignmentDetail(item) {
  const status = normalizeAssignmentStatus(item.status, item.endAt)
  const classes = Array.isArray(item.classes) ? item.classes.map((entry) => ({
    classId: Number(entry.classId),
    className: entry.className,
    studentCount: Number(entry.studentCount || 0),
    submittedStudentCount: Number(entry.submittedStudentCount || 0)
  })) : []

  return {
    id: item.id,
    title: item.title || '',
    language: item.language || 'JAVA',
    description: item.description || '',
    startAt: item.startAt,
    endAt: item.endAt,
    startTime: formatDateTime(item.startAt),
    endTime: formatDateTime(item.endAt),
    status: status.value,
    statusLabel: status.label,
    allowResubmit: Boolean(item.allowResubmit),
    allowLateSubmit: Boolean(item.allowLateSubmit),
    maxFiles: Number(item.maxFiles || 0),
    classes,
    classIds: classes.map((entry) => entry.classId),
    classNamesText: classes.length ? classes.map((entry) => entry.className).join('、') : '未配置班级',
    materials: Array.isArray(item.materials) ? item.materials.map((material) => ({
      id: material.id,
      originalName: material.originalName,
      sizeBytes: Number(material.sizeBytes || 0),
      contentType: material.contentType || '-',
      downloadUrl: material.downloadUrl || ''
    })) : [],
    stats: item.stats || null
  }
}

export function normalizeSubmission(item) {
  return {
    submissionId: Number(item.submissionId),
    studentId: Number(item.studentId),
    studentName: item.studentName || `学生 ${item.studentId}`,
    studentNumber: String(item.studentNumber || item.studentId || ''),
    version: Number(item.version || 0),
    parseOkFiles: Number(item.parseOkFiles || 0),
    totalFiles: Number(item.totalFiles || 0),
    fileCount: Number(item.totalFiles || 0),
    isLate: Number(item.isLate || 0) === 1,
    isValid: Number(item.isValid || 0) === 1,
    lastSubmittedAt: item.lastSubmittedAt ? formatDateTime(item.lastSubmittedAt) : ''
  }
}

export function normalizeJob(job) {
  let threshold = Number(job?.thresholdScore || 80)
  let topK = Number(job?.topKPerStudent || 10)
  let plagiarismMode = String(job?.plagiarismMode || 'FAST').toUpperCase()
  if (job?.paramsJson) {
    try {
      const params = JSON.parse(job.paramsJson)
      threshold = Number(params.thresholdScore || threshold)
      topK = Number(params.topKPerStudent || topK)
      plagiarismMode = String(params.plagiarismMode || plagiarismMode || 'FAST').toUpperCase()
    } catch {}
  }

  return {
    id: Number(job.id || job.jobId),
    status: job.status || 'UNKNOWN',
    progressDone: Number(job.progressDone || 0),
    progressTotal: Number(job.progressTotal || 0),
    createTime: formatDateTime(job.createTime),
    threshold,
    topK,
    plagiarismMode,
    executionMode: job.executionMode || 'ASYNC',
    reusedFromJobId: job.reusedFromJobId ? Number(job.reusedFromJobId) : null,
    thresholdMatchedPairs: Number(job.thresholdMatchedPairs || 0),
    errorMsg: job.errorMsg || ''
  }
}

export function normalizeReport(report) {
  const jobStats = report.jobStats ? {
    comparableSubmissionCount: Number(report.jobStats.comparableSubmissionCount || 0),
    comparablePairCount: Number(report.jobStats.comparablePairCount || 0),
    sizeSkippedPairs: Number(report.jobStats.sizeSkippedPairs || 0),
    bucketSkippedPairs: Number(report.jobStats.bucketSkippedPairs || 0),
    fullCalculatedPairs: Number(report.jobStats.fullCalculatedPairs || 0),
    thresholdMatchedPairs: Number(report.jobStats.thresholdMatchedPairs || 0),
    executionMode: report.jobStats.executionMode || 'ASYNC',
    reusedFromJobId: report.jobStats.reusedFromJobId ? Number(report.jobStats.reusedFromJobId) : null
  } : null

  return {
    jobId: report.jobId,
    status: report.status || '',
    message: report.message || '',
    minScore: Number(report.minScore || 0),
    perStudentTopK: Number(report.perStudentTopK || 10),
    jobStats,
    pairs: Array.isArray(report.pairs) ? report.pairs.map((item) => ({
      pairId: Number(item.pairId),
      studentA: String(item.studentA),
      studentB: String(item.studentB),
      score: Number(item.score || 0),
      status: item.status || 'PENDING',
      teacherNote: item.teacherNote || ''
    })) : [],
    incomparableSubmissions: Array.isArray(report.incomparableSubmissions) ? report.incomparableSubmissions.map((item) => ({
      submissionId: Number(item.submissionId),
      classId: Number(item.classId),
      studentId: Number(item.studentId),
      version: Number(item.version || 0),
      reason: item.reason || '',
      parseOkFiles: Number(item.parseOkFiles || 0),
      totalFiles: Number(item.totalFiles || 0),
      parseFailures: Array.isArray(item.parseFailures) ? item.parseFailures : []
    })) : []
  }
}

export function normalizePairDetail(detail) {
  return {
    pairId: Number(detail.pairId),
    jobId: Number(detail.jobId),
    studentA: String(detail.studentA),
    studentB: String(detail.studentB),
    score: Number(detail.score || 0),
    status: detail.status || 'PENDING',
    teacherNote: detail.teacherNote || '',
    evidences: Array.isArray(detail.evidences) ? detail.evidences : [],
    latestAiExplanation: detail.latestAiExplanation || null,
    codeCompare: detail.codeCompare || null,
    matchedFilePairs: Array.isArray(detail.matchedFilePairs) ? detail.matchedFilePairs : [],
    unmatchedLeftFiles: Array.isArray(detail.unmatchedLeftFiles) ? detail.unmatchedLeftFiles : [],
    unmatchedRightFiles: Array.isArray(detail.unmatchedRightFiles) ? detail.unmatchedRightFiles : [],
    crossFileSegments: Array.isArray(detail.crossFileSegments) ? detail.crossFileSegments : []
  }
}

export function safeParseJson(value) {
  if (!value) return null
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

export function humanizeSignature(signature) {
  const text = String(signature || '')
  if (!text) return '结构命中'
  if (text.startsWith('BinaryExpr:')) return `二元运算 ${text.replace('BinaryExpr:', '')}`
  if (text.startsWith('UnaryExpr:')) return `一元运算 ${text.replace('UnaryExpr:', '')}`
  if (text.startsWith('Assign:')) return `赋值结构 ${text.replace('Assign:', '')}`
  if (text.startsWith('Literal:')) return `字面量类型 ${text.replace('Literal:', '')}`
  if (text.startsWith('Call:')) return `方法调用 ${text.replace('Call:', '')}`
  if (text.startsWith('If:')) return text.includes('else=1') ? 'if-else 分支结构' : 'if 分支结构'
  if (text.startsWith('ForEach')) return 'foreach 循环结构'
  if (text.startsWith('ForRange')) return '范围循环结构'
  if (text.startsWith('For:')) return `for 循环 ${text.replace('For:', '')}`
  if (text === 'While') return 'while 循环结构'
  if (text === 'DoWhile') return 'do-while 循环结构'
  if (text === 'TryCatch') return '异常捕获结构'
  if (text === 'Switch') return 'switch 分支结构'
  if (text === 'Return') return 'return 返回结构'
  if (text === 'Throw') return 'throw 抛出结构'
  return text
}

export function buildPairRisk(score) {
  const value = Number(score || 0)
  if (value >= 90) return { label: '高风险', tone: 'danger', hint: '建议老师优先复核' }
  if (value >= 80) return { label: '重点复核', tone: 'warning', hint: '结构相似度较高' }
  return { label: '一般关注', tone: 'neutral', hint: '建议结合证据再判断' }
}

export function summarizeEvidenceList(evidences = []) {
  return evidences.map((item) => {
    const payload = safeParseJson(item.payloadJson)
    const topMatches = Array.isArray(payload?.topN) ? payload.topN.slice(0, 80).map((entry) => ({
      label: humanizeSignature(entry.signature || entry.signatureId),
      matchedCount: Number(entry.matchedCount || 0),
      contributionRatio: Number(entry.contributionRatio || 0)
    })) : []
    const totals = payload?.totals
      ? {
          N1: Number(payload.totals.N1 || 0),
          N2: Number(payload.totals.N2 || 0),
          M: Number(payload.totals.M || 0),
          AC: Number(payload.totals.AC || 0)
        }
      : null
    const parseFailures = payload?.parseFailures || null
    return {
      id: item.id,
      type: item.type,
      summary: item.summary || '',
      weight: Number(item.weight || 0),
      topMatches,
      totals,
      parseFailures,
      payload
    }
  })
}

export function formatBytes(value) {
  const size = Number(value || 0)
  if (size >= 1024 * 1024 * 1024) return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  if (size >= 1024) return `${Math.round(size / 1024)} KB`
  return `${size} B`
}

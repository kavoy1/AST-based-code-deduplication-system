export function safeParseJsonText(value) {
  if (!value) return null
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

export function formatAiModeLabel(mode) {
  return mode === 'CODE_WITH_SYSTEM_EVIDENCE' ? '代码 + 系统证据' : '仅代码'
}

export function formatAiLevelLabel(level) {
  const map = {
    HIGH: '高相似',
    MEDIUM: '中高相似',
    LOW: '低相似'
  }
  return map[String(level || '').trim().toUpperCase()] || '待判断'
}

export function formatAiConfidenceLabel(confidence) {
  const map = {
    HIGH: '高把握',
    MEDIUM: '中等把握',
    LOW: '低把握'
  }
  return map[String(confidence || '').trim().toUpperCase()] || '把握度待定'
}

function normalizePercent(value) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return 0
  return Math.max(0, Math.min(100, Math.round(numeric)))
}

function ensureList(value) {
  return Array.isArray(value)
    ? value.map(item => String(item || '').trim()).filter(Boolean)
    : []
}

function buildScoreText(structured) {
  const estimatedSimilarity = normalizePercent(structured.estimatedSimilarity)
  const rangeMin = normalizePercent(structured.rangeMin ?? estimatedSimilarity)
  const rangeMax = normalizePercent(structured.rangeMax ?? estimatedSimilarity)
  const useRange = structured.useRange === true || rangeMin !== rangeMax
  return useRange ? `${Math.min(rangeMin, rangeMax)}%-${Math.max(rangeMin, rangeMax)}%` : `${estimatedSimilarity}%`
}

function hasStructuredSimilarity(structured) {
  if (!structured || typeof structured !== 'object') return false
  return ['estimatedSimilarity', 'rangeMin', 'rangeMax'].some((key) => {
    const value = structured[key]
    return value !== undefined && value !== null && value !== ''
  })
}

function buildStatusLabel(status) {
  if (status === 'FAILED') return '生成失败'
  if (status === 'GENERATING') return '生成中'
  return '已生成'
}

export function normalizeAiExplanationRecord(item = {}, options = {}) {
  const requestPayload = safeParseJsonText(item.requestPayload) || {}
  const responsePayload = safeParseJsonText(item.responsePayload) || {}
  const structured = responsePayload.structured || {}
  const hasAiScore = hasStructuredSimilarity(structured)
  const isGenerating = String(item.status || '').trim().toUpperCase() === 'GENERATING'
  const estimatedSimilarity = normalizePercent(structured.estimatedSimilarity ?? (hasAiScore ? requestPayload.systemScore ?? options.systemScore ?? 0 : 0))
  const rangeMin = normalizePercent(structured.rangeMin ?? estimatedSimilarity)
  const rangeMax = normalizePercent(structured.rangeMax ?? estimatedSimilarity)
  const useRange = structured.useRange === true || rangeMin !== rangeMax
  const coreEvidence = ensureList(structured.coreEvidence)
  const differenceAdjustments = ensureList(structured.differenceAdjustments)
  const systemEvidenceEffects = ensureList(structured.systemEvidenceEffects)
  const mode = requestPayload.mode || 'CODE_ONLY'
  const createdAt = typeof options.formatDateTime === 'function'
    ? options.formatDateTime(item.createTime)
    : item.createTime || ''

  return {
    id: item.id,
    status: item.status || 'SUCCESS',
    statusLabel: buildStatusLabel(item.status),
    createdAt,
    mode,
    modeLabel: formatAiModeLabel(mode),
    includeTeacherNote: Boolean(requestPayload.includeTeacherNote),
    model: item.model || '',
    estimatedSimilarity,
    rangeMin,
    rangeMax,
    useRange,
    scoreText: isGenerating && !hasAiScore ? '评估中' : buildScoreText({ estimatedSimilarity, rangeMin, rangeMax, useRange }),
    confidence: String(structured.confidence || '').trim().toUpperCase() || 'MEDIUM',
    confidenceLabel: formatAiConfidenceLabel(structured.confidence),
    level: String(structured.level || '').trim().toUpperCase() || 'MEDIUM',
    levelLabel: formatAiLevelLabel(structured.level),
    summary: structured.summary || item.result || (item.status === 'FAILED' ? '本次 AI 解释生成失败。' : 'AI 正在根据当前模式生成结构化评分。'),
    coreEvidence,
    differenceAdjustments,
    systemEvidenceEffects,
    lowerBoundReason: structured.lowerBoundReason || '',
    upperBoundReason: structured.upperBoundReason || '',
    systemScore: normalizePercent(structured.systemScore ?? requestPayload.systemScore ?? options.systemScore ?? 0),
    diffDirection: structured.diffDirection || 'MATCHED',
    errorMsg: item.errorMsg || ''
  }
}

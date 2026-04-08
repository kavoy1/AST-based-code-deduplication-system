function normalizeLineNumber(value) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric) || numeric <= 0) return 0
  return Math.round(numeric)
}

function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function decodeHtmlEntities(value) {
  return String(value || '')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&amp;/g, '&')
}

function findMatchingRange(lineNumber, ranges = []) {
  const current = normalizeLineNumber(lineNumber)
  if (!current || !Array.isArray(ranges)) return null
  return ranges.find((range) => range && current >= range.startLine && current <= range.endLine) || null
}

export function normalizeSourceCode(code = '') {
  const raw = String(code || '')
  const decoded = decodeHtmlEntities(raw)

  const hasTokenArtifacts =
    /<span[^>]*class=["']token\b/i.test(decoded) ||
    /<\/span>/i.test(decoded) ||
    /class\s*=\s*["']?token\s+\w+["']?\s*>/i.test(decoded) ||
    /["']token\s+\w+["']\s*>/i.test(decoded)

  if (!hasTokenArtifacts) return raw

  return decoded
    .replace(/<\/?span[^>]*>/gi, '')
    .replace(/class\s*=\s*["']?token\s+\w+["']?\s*>/gi, '')
    .replace(/["']token\s+\w+["']\s*>/gi, '')
}

function highlightCode(line) {
  const placeholders = []
  const reserve = (html) => {
    const token = `__CODE_TOKEN_${placeholders.length}__`
    placeholders.push({ token, html })
    return token
  }

  let result = escapeHtml(line)
    .replace(/(\/\/.*)/g, (match) => reserve(`<span class="token comment">${match}</span>`))
    .replace(/("[^"]*"|'[^']*')/g, (match) => reserve(`<span class="token string">${match}</span>`))
    .replace(
      /\b(public|private|protected|class|static|void|int|double|float|long|boolean|return|new|if|else|for|while|try|catch|throw)\b/g,
      '<span class="token keyword">$1</span>'
    )

  placeholders.forEach(({ token, html }) => {
    result = result.replace(token, html)
  })

  return result
}

export function normalizeHighlightRanges(items = []) {
  if (!Array.isArray(items)) return []
  return items
    .map((item) => ({
      startLine: normalizeLineNumber(item.startLine),
      endLine: normalizeLineNumber(item.endLine)
    }))
    .filter((item) => item.startLine && item.endLine)
}

export function normalizeCodeCompare(compare = {}) {
  const left = {
    title: compare.left?.title || '',
    code: normalizeSourceCode(compare.left?.code || ''),
    highlights: normalizeHighlightRanges(compare.left?.highlights)
  }

  const right = {
    title: compare.right?.title || '',
    code: normalizeSourceCode(compare.right?.code || ''),
    highlights: normalizeHighlightRanges(compare.right?.highlights)
  }

  const segments = Array.isArray(compare.segments)
    ? compare.segments.map((segment, index) => ({
        id: segment.id || `seg-${index + 1}`,
        label: segment.label || `片段 ${index + 1}`,
        summary: segment.summary || '',
        score: normalizeLineNumber(segment.score),
        blockType: segment.blockType || '',
        leftMethodKey: segment.leftMethodKey || '',
        rightMethodKey: segment.rightMethodKey || '',
        leftFile: segment.leftFile || left.title || '',
        leftStartLine: normalizeLineNumber(segment.leftStartLine),
        leftEndLine: normalizeLineNumber(segment.leftEndLine),
        rightFile: segment.rightFile || right.title || '',
        rightStartLine: normalizeLineNumber(segment.rightStartLine),
        rightEndLine: normalizeLineNumber(segment.rightEndLine)
      }))
    : []

  return { left, right, segments }
}

export function normalizeUnmatchedFile(item = {}, side = 'left', index = 0) {
  return {
    key: item.key || `${side}-only-${index + 1}`,
    label: item.label || (side === 'left' ? `仅学生A存在 ${index + 1}` : `仅学生B存在 ${index + 1}`),
    pane: {
      title: item.pane?.title || '',
      code: normalizeSourceCode(item.pane?.code || ''),
      highlights: normalizeHighlightRanges(item.pane?.highlights)
    }
  }
}

export function getSegmentRange(segment, side) {
  if (!segment) return null
  const startLine = normalizeLineNumber(side === 'left' ? segment.leftStartLine : segment.rightStartLine)
  const endLine = normalizeLineNumber(side === 'left' ? segment.leftEndLine : segment.rightEndLine)
  if (!startLine || !endLine) return null
  return {
    startLine: Math.min(startLine, endLine),
    endLine: Math.max(startLine, endLine)
  }
}

export function getLineHighlightMeta(lineNumber, activeRange, allRanges = []) {
  const current = normalizeLineNumber(lineNumber)
  const normalizedActiveRange = activeRange
    ? {
        startLine: normalizeLineNumber(activeRange.startLine),
        endLine: normalizeLineNumber(activeRange.endLine)
      }
    : null
  const matchingRange = findMatchingRange(current, allRanges)
  const activeMatch = findMatchingRange(current, normalizedActiveRange ? [normalizedActiveRange] : [])
  const targetRange = activeMatch || matchingRange

  return {
    state: activeMatch ? 'active' : matchingRange ? 'related' : 'none',
    isRangeStart: Boolean(targetRange && current === targetRange.startLine),
    isRangeEnd: Boolean(targetRange && current === targetRange.endLine),
    isActiveStart: Boolean(activeMatch && current === activeMatch.startLine),
    isActiveEnd: Boolean(activeMatch && current === activeMatch.endLine)
  }
}

export function getLineHighlightState(lineNumber, activeRange, allRanges = []) {
  return getLineHighlightMeta(lineNumber, activeRange, allRanges).state
}

export function buildCodeLines(code = '') {
  return normalizeSourceCode(code)
    .split(/\r?\n/)
    .map((line, index) => ({
      number: index + 1,
      html: highlightCode(line)
    }))
}

const RAIL_MIN_OFFSET = 18
const RAIL_MAX_OFFSET = 86
const RAIL_MIN_GAP = 7

function clampRailOffset(value) {
  return Math.max(RAIL_MIN_OFFSET, Math.min(RAIL_MAX_OFFSET, value))
}

function spreadRailOffsets(offsets = []) {
  if (!offsets.length) return []

  const next = [...offsets]

  for (let index = 1; index < next.length; index += 1) {
    next[index] = Math.max(next[index], next[index - 1] + RAIL_MIN_GAP)
  }

  if (next[next.length - 1] > RAIL_MAX_OFFSET) {
    next[next.length - 1] = RAIL_MAX_OFFSET
    for (let index = next.length - 2; index >= 0; index -= 1) {
      next[index] = Math.min(next[index], next[index + 1] - RAIL_MIN_GAP)
    }
  }

  next[0] = Math.max(next[0], RAIL_MIN_OFFSET)
  for (let index = 1; index < next.length; index += 1) {
    next[index] = Math.max(next[index], next[index - 1] + RAIL_MIN_GAP)
  }

  return next.map((item) => clampRailOffset(Math.round(item)))
}

export function buildSegmentRailMarkers(segments = [], totalLines = 1) {
  const safeTotal = Math.max(1, Number(totalLines) || 1)
  const markers = segments.map((segment, index) => {
    const referenceLine = Math.max(
      1,
      normalizeLineNumber(segment.leftStartLine) || normalizeLineNumber(segment.rightStartLine) || 1
    )
    const progress = safeTotal <= 1 ? 0 : (referenceLine - 1) / safeTotal
    return {
      index,
      segment,
      rawTopOffset: clampRailOffset(RAIL_MIN_OFFSET + progress * (RAIL_MAX_OFFSET - RAIL_MIN_OFFSET))
    }
  })

  const sorted = [...markers].sort((left, right) => left.rawTopOffset - right.rawTopOffset)
  const spreadOffsets = spreadRailOffsets(sorted.map((item) => item.rawTopOffset))
  const offsetMap = new Map(sorted.map((item, index) => [item.index, spreadOffsets[index]]))

  return markers.map((item) => ({
    segment: item.segment,
    topOffset: offsetMap.get(item.index) ?? Math.round(item.rawTopOffset)
  }))
}

export function formatSegmentBlockType(blockType = '') {
  const value = String(blockType || '').toUpperCase()
  if (!value) return '结构块'
  if (value === 'METHOD') return '方法块'
  if (value === 'IF_THEN') return 'if 分支块'
  if (value === 'IF_ELSE') return 'if-else 分支块'
  if (value === 'ELSE_IF') return 'else-if 分支块'
  if (value === 'FOR') return 'for 循环块'
  if (value === 'FOREACH') return 'foreach 循环块'
  if (value === 'WHILE') return 'while 循环块'
  if (value === 'DO_WHILE') return 'do-while 循环块'
  if (value === 'TRY') return 'try 结构块'
  if (value === 'CATCH') return 'catch 结构块'
  if (value === 'SWITCH') return 'switch 分支块'
  if (value === 'CASE_GROUP') return 'case 分组块'
  return `${blockType} 结构块`
}

export function buildSegmentMeta(segment = {}) {
  const items = []
  const blockLabel = formatSegmentBlockType(segment.blockType)
  if (blockLabel) items.push(blockLabel)
  if (segment.leftMethodKey && segment.rightMethodKey) {
    items.push(`${segment.leftMethodKey} ↔ ${segment.rightMethodKey}`)
  }
  return items.join(' · ')
}

export function buildSegmentLineSummary(segment = {}) {
  const leftRange = getSegmentRange(segment, 'left')
  const rightRange = getSegmentRange(segment, 'right')
  const items = []

  if (leftRange) items.push(`左 ${leftRange.startLine}-${leftRange.endLine}`)
  if (rightRange) items.push(`右 ${rightRange.startLine}-${rightRange.endLine}`)

  return items.join(' / ')
}

export function buildInactiveSegmentList(segments = [], activeSegmentId = '') {
  if (!Array.isArray(segments) || !segments.length) return []
  const targetId = String(activeSegmentId || '').trim()

  return segments
    .map((segment, index) => ({
      index: index + 1,
      segment
    }))
    .filter((item) => !targetId || item.segment?.id !== targetId)
}

export function buildCompareTabs(detail = {}) {
  const tabs = []

  ;(detail.matchedFilePairs || []).forEach((item, index) => {
    tabs.push({
      key: item.key || `matched-${index + 1}`,
      type: 'matched',
      title: item.label || `匹配对 ${index + 1}`,
      subtitle: `${item.left?.title || '-'} ↔ ${item.right?.title || '-'}`,
      compare: normalizeCodeCompare(item)
    })
  })

  ;(detail.crossFileSegments || []).forEach((item, index) => {
    tabs.push({
      key: item.key || `cross-${index + 1}`,
      type: 'cross',
      title: item.label || `跨文件片段 ${index + 1}`,
      subtitle: `${item.left?.title || '-'} ↔ ${item.right?.title || '-'}`,
      compare: normalizeCodeCompare(item)
    })
  })

  ;(detail.unmatchedLeftFiles || []).forEach((item, index) => {
    const normalized = normalizeUnmatchedFile(item, 'left', index)
    tabs.push({
      key: normalized.key,
      type: 'left-only',
      title: normalized.label,
      subtitle: normalized.pane.title || '仅左侧文件',
      compare: normalizeCodeCompare({
        left: normalized.pane,
        right: { title: '', code: '', highlights: [] },
        segments: []
      })
    })
  })

  ;(detail.unmatchedRightFiles || []).forEach((item, index) => {
    const normalized = normalizeUnmatchedFile(item, 'right', index)
    tabs.push({
      key: normalized.key,
      type: 'right-only',
      title: normalized.label,
      subtitle: normalized.pane.title || '仅右侧文件',
      compare: normalizeCodeCompare({
        left: { title: '', code: '', highlights: [] },
        right: normalized.pane,
        segments: []
      })
    })
  })

  if (!tabs.length && detail.codeCompare) {
    tabs.push({
      key: 'primary',
      type: 'matched',
      title: '主对比',
      subtitle: `${detail.codeCompare.left?.title || '-'} ↔ ${detail.codeCompare.right?.title || '-'}`,
      compare: normalizeCodeCompare(detail.codeCompare)
    })
  }

  return tabs
}

export function buildCompareTabFilters(tabs = []) {
  const safeTabs = Array.isArray(tabs) ? tabs : []
  const matchedCount = safeTabs.filter((item) => item?.type === 'matched').length
  const crossCount = safeTabs.filter((item) => item?.type === 'cross').length
  const singleCount = safeTabs.filter((item) => item?.type === 'left-only' || item?.type === 'right-only').length

  return [
    { value: 'all', label: '全部', count: safeTabs.length },
    { value: 'matched', label: '匹配对', count: matchedCount },
    { value: 'cross', label: '跨文件', count: crossCount },
    { value: 'single', label: '单侧文件', count: singleCount }
  ].filter((item) => item.value === 'all' || item.count > 0)
}

export function filterCompareTabs(tabs = [], filter = 'all') {
  const safeTabs = Array.isArray(tabs) ? tabs : []
  const safeFilter = String(filter || 'all').trim()

  if (safeFilter === 'matched') return safeTabs.filter((item) => item?.type === 'matched')
  if (safeFilter === 'cross') return safeTabs.filter((item) => item?.type === 'cross')
  if (safeFilter === 'single') return safeTabs.filter((item) => item?.type === 'left-only' || item?.type === 'right-only')

  return safeTabs
}

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

export function getLineHighlightState(lineNumber, activeRange, allRanges = []) {
  const current = normalizeLineNumber(lineNumber)
  if (activeRange && current >= activeRange.startLine && current <= activeRange.endLine) {
    return 'active'
  }
  if (Array.isArray(allRanges) && allRanges.some((range) => range && current >= range.startLine && current <= range.endLine)) {
    return 'related'
  }
  return 'none'
}

export function buildCodeLines(code = '') {
  return normalizeSourceCode(code)
    .split(/\r?\n/)
    .map((line, index) => ({
      number: index + 1,
      html: highlightCode(line)
    }))
}

export function buildSegmentRailMarkers(segments = [], totalLines = 1) {
  const safeTotal = Math.max(1, Number(totalLines) || 1)
  return segments.map((segment) => {
    const referenceLine = Math.max(
      1,
      normalizeLineNumber(segment.leftStartLine) || normalizeLineNumber(segment.rightStartLine) || 1
    )
    return {
      segment,
      topOffset: Math.max(9, Math.min(91, Math.round(((referenceLine - 1) / safeTotal) * 100)))
    }
  })
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

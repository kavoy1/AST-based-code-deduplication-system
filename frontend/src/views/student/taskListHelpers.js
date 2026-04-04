export function filterAssignmentsByClass(assignments = [], classId = 'all') {
  if (!classId || classId === 'all') return assignments
  return assignments.filter((item) => String(item.classId) === String(classId))
}

export function buildCountdownLabel(endAt, now = Date.now()) {
  if (!endAt) return ''
  const target = new Date(endAt).getTime()
  if (Number.isNaN(target) || target <= now) return ''

  let diff = Math.floor((target - now) / 1000)
  const days = Math.floor(diff / 86400)
  diff -= days * 86400
  const hours = Math.floor(diff / 3600)
  diff -= hours * 3600
  const minutes = Math.floor(diff / 60)
  const seconds = diff - minutes * 60

  const hh = String(hours).padStart(2, '0')
  const mm = String(minutes).padStart(2, '0')
  const ss = String(seconds).padStart(2, '0')

  if (days > 0) {
    return `剩余 ${days}天 ${hh}:${mm}:${ss}`
  }

  return `剩余 ${hh}:${mm}:${ss}`
}

export function paginateAssignments(assignments = [], currentPage = 1, pageSize = 3) {
  const safePageSize = Math.max(1, Number(pageSize) || 1)
  const safePage = Math.max(1, Number(currentPage) || 1)
  const start = (safePage - 1) * safePageSize

  return {
    total: assignments.length,
    items: assignments.slice(start, start + safePageSize)
  }
}

export function buildPaginationMeta(total = 0, currentPage = 1, pageSize = 3) {
  const safeTotal = Math.max(0, Number(total) || 0)
  const safePageSize = Math.max(1, Number(pageSize) || 1)
  const safePage = Math.max(1, Number(currentPage) || 1)
  const totalPages = Math.max(1, Math.ceil(safeTotal / safePageSize))

  if (safeTotal === 0) {
    return {
      totalPages,
      from: 0,
      to: 0
    }
  }

  const from = Math.min((safePage - 1) * safePageSize + 1, safeTotal)
  const to = Math.min(from + safePageSize - 1, safeTotal)

  return {
    totalPages,
    from,
    to
  }
}

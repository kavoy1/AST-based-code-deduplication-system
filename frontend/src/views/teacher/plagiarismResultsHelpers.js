export function getPairRank(currentPage = 1, pageSize = 10, index = 0) {
  const safePage = Math.max(1, Number(currentPage) || 1)
  const safePageSize = Math.max(1, Number(pageSize) || 1)
  const safeIndex = Math.max(0, Number(index) || 0)
  return (safePage - 1) * safePageSize + safeIndex + 1
}

export function formatStudentDisplayLabel(studentId, studentMap = {}) {
  const profile = studentMap?.[studentId] || studentMap?.[String(studentId)] || null

  if (profile?.name && profile?.number) return `${profile.name} (${profile.number})`
  if (profile?.name) return profile.name
  if (profile?.number) return `学号 ${profile.number}`

  return `学生 ${studentId || '-'}`
}

export function buildPairStudentTitle(pair = {}, studentMap = {}) {
  return `${formatStudentDisplayLabel(pair?.studentA, studentMap)} → ${formatStudentDisplayLabel(pair?.studentB, studentMap)}`
}

export function getPairRankTone(rank = 0) {
  if (rank === 1) return 'top-1'
  if (rank === 2) return 'top-2'
  if (rank === 3) return 'top-3'
  return 'default'
}

export function shouldShowResultsLaunchAction(assignmentId) {
  return !String(assignmentId || '').trim()
}

export function getLatestJobByMode(jobs = [], mode = 'FAST') {
  const normalizedMode = String(mode || 'FAST').trim().toUpperCase()
  return jobs.find((job) => String(job?.plagiarismMode || 'FAST').trim().toUpperCase() === normalizedMode) || null
}

export function filterPairsByRiskView(pairs = [], riskView = 'all', threshold = 80) {
  if (riskView !== 'high-risk') {
    return pairs
  }
  const safeThreshold = Number.isFinite(Number(threshold)) ? Number(threshold) : 80
  return pairs.filter((pair) => Number(pair?.score || 0) >= safeThreshold)
}

export function countHighRiskPairs(pairs = [], threshold = 80) {
  return filterPairsByRiskView(pairs, 'high-risk', threshold).length
}

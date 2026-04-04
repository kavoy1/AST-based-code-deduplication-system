export function getPairRank(currentPage = 1, pageSize = 10, index = 0) {
  const safePage = Math.max(1, Number(currentPage) || 1)
  const safePageSize = Math.max(1, Number(pageSize) || 1)
  const safeIndex = Math.max(0, Number(index) || 0)
  return (safePage - 1) * safePageSize + safeIndex + 1
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

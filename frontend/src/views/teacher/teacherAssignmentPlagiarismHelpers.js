export function getAssignmentDirectoryCountLabel(total = 0) {
  const safeTotal = Math.max(0, Number(total) || 0)
  return `${safeTotal} 份作业`
}

export function shouldShowAssignmentDirectoryPagination(total = 0, pageSize = 10) {
  const safeTotal = Math.max(0, Number(total) || 0)
  const safePageSize = Math.max(1, Number(pageSize) || 10)
  return safeTotal > safePageSize
}

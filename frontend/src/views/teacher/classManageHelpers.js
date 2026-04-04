export function getInviteCountLabel(total = 0) {
  const safeTotal = Math.max(0, Number(total) || 0)
  return `共 ${safeTotal} 个班级邀请码`
}

export function shouldShowInvitePagination(total = 0, pageSize = 10) {
  const safeTotal = Math.max(0, Number(total) || 0)
  const safePageSize = Math.max(1, Number(pageSize) || 10)
  return safeTotal > safePageSize
}

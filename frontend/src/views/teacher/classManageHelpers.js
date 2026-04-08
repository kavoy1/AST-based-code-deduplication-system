export function getInviteCountLabel(total = 0) {
  const safeTotal = Math.max(0, Number(total) || 0)
  return `共 ${safeTotal} 个班级邀请码`
}

export function shouldShowInvitePagination(total = 0, pageSize = 10) {
  const safeTotal = Math.max(0, Number(total) || 0)
  const safePageSize = Math.max(1, Number(pageSize) || 10)
  return safeTotal > safePageSize
}

function splitMergedClassNames(value) {
  return String(value || '')
    .split('、')
    .map((item) => item.trim())
    .filter(Boolean)
}

export function buildTeacherStudentClassOptions(rows = []) {
  return Array.from(new Set(
    rows.flatMap((row) => splitMergedClassNames(row.class_name))
  )).sort((left, right) => left.localeCompare(right, 'zh-CN', { sensitivity: 'base' }))
}

export function filterTeacherStudents(rows = [], filters = {}) {
  const keyword = String(filters.keyword || '').trim().toLowerCase()
  const college = String(filters.college || '').trim()
  const className = String(filters.className || '').trim()

  return rows.filter((row) => {
    const matchesKeyword = !keyword || [
      row.student_number,
      row.username,
      row.nickname,
      row.email
    ].filter(Boolean).join(' ').toLowerCase().includes(keyword)

    const matchesCollege = !college || String(row.college || '') === college
    const matchesClass = !className || splitMergedClassNames(row.class_name).includes(className)

    return matchesKeyword && matchesCollege && matchesClass
  })
}

const APPLICATION_RESOLVED_KEYWORDS = [
  'approved',
  'passed',
  'success',
  'done',
  'resolved',
  'accepted',
  '已通过',
  '已拒绝',
  '已解决',
  'rejected',
  'refused'
]

function normalizeRows(rows = []) {
  return Array.isArray(rows) ? rows : []
}

export function getTeacherApplicationStatusText(row) {
  const raw = String(row?.status || row?.applyStatus || row?.reviewStatus || row?.result || '').toLowerCase()
  return APPLICATION_RESOLVED_KEYWORDS.some((item) => raw.includes(item.toLowerCase())) ? '已解决' : '未解决'
}

export function isTeacherApplicationResolved(row) {
  return getTeacherApplicationStatusText(row) === '已解决'
}

export function sortTeacherApplications(rows = []) {
  return [...normalizeRows(rows)].sort((left, right) => {
    const statusDelta = Number(isTeacherApplicationResolved(right)) - Number(isTeacherApplicationResolved(left))
    if (statusDelta !== 0) return statusDelta
    return new Date(right.applyTime || 0).getTime() - new Date(left.applyTime || 0).getTime()
  })
}

export function buildTeacherApplicationClassOptions(rows = []) {
  return Array.from(new Set(
    normalizeRows(rows)
      .map((row) => String(row?.className || '').trim())
      .filter(Boolean)
  )).sort((left, right) => left.localeCompare(right, 'zh-CN', { sensitivity: 'base' }))
}

export function filterTeacherApplications(rows = [], filters = {}) {
  const keyword = String(filters.keyword || '').trim().toLowerCase()
  const status = String(filters.status || 'all').trim()
  const className = String(filters.className || '').trim()

  return normalizeRows(rows).filter((item) => {
    const matchesStatus = status === 'all'
      || (status === 'resolved' && isTeacherApplicationResolved(item))
      || (status === 'pending' && !isTeacherApplicationResolved(item))
    const matchesClass = !className || String(item?.className || '') === className
    const source = [
      item?.studentNumber,
      item?.nickname,
      item?.username,
      item?.className
    ].filter(Boolean).join(' ').toLowerCase()
    const matchesKeyword = !keyword || source.includes(keyword)

    return matchesStatus && matchesClass && matchesKeyword
  })
}

export function paginateTeacherRows(rows = [], page = 1, pageSize = 10) {
  const safePage = Math.max(1, Number(page) || 1)
  const safePageSize = Math.max(1, Number(pageSize) || 10)
  const start = (safePage - 1) * safePageSize

  return normalizeRows(rows).slice(start, start + safePageSize)
}

export function getSelectableTeacherApplications(rows = []) {
  return normalizeRows(rows).filter((item) => !isTeacherApplicationResolved(item))
}

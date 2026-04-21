function isPlaceholderStudentName(value = '') {
  const text = String(value || '').trim()
  if (!text) return false
  return /^学生\s*\d+$/u.test(text) || /^student[_\s-]*\d+$/iu.test(text)
}

export function resolveRosterStudentName(student = {}) {
  const nickname = String(student?.nickname || student?.studentName || '').trim()
  const username = String(student?.username || '').trim()
  if (nickname && !isPlaceholderStudentName(nickname)) {
    return nickname
  }
  if (username && !isPlaceholderStudentName(username)) {
    return username
  }
  return nickname || username || '未命名学生'
}

export function mapRosterStudent(student, className = '') {
  return {
    studentId: Number(student?.id || student?.user_id || student?.userId || 0),
    studentName: resolveRosterStudentName(student),
    className
  }
}

export function buildSubmittedRoster(rows = [], studentProfileMap = new Map()) {
  const latestMap = new Map()
  rows.forEach((item) => {
    if (!item?.studentId) return
    const current = latestMap.get(item.studentId)
    const nextTime = item.lastSubmittedAt || ''
    const currentTime = current?.lastSubmittedAt || ''
    const profile = studentProfileMap.get(Number(item.studentId)) || null

    if (!current || nextTime >= currentTime) {
      latestMap.set(item.studentId, {
        studentId: Number(item.studentId),
        studentName: profile?.studentName || resolveRosterStudentName(item),
        lastSubmittedAt: item.lastSubmittedAt || '',
        className: profile?.className || ''
      })
    }
  })
  return Array.from(latestMap.values()).sort((left, right) => {
    const timeLeft = left.lastSubmittedAt || ''
    const timeRight = right.lastSubmittedAt || ''
    return timeRight.localeCompare(timeLeft)
  })
}

export function sortRosterByName(items = []) {
  return [...items].sort((left, right) => String(left.studentName || '').localeCompare(String(right.studentName || ''), 'zh-CN'))
}

function toNumber(value) {
  return Number(value || 0)
}

function sumWeeklyVisits(weeklyVisits = []) {
  return (weeklyVisits || []).reduce((sum, item) => sum + toNumber(item), 0)
}

function getPercent(part, total) {
  if (!total) return 0
  return Math.max(0, Math.min(100, Math.round((toNumber(part) / toNumber(total)) * 100)))
}

export function getStatusTone(status) {
  const normalized = String(status || '').trim()
  return ['运行正常', '正常', '杩愯姝ｅ父'].includes(normalized) ? 'healthy' : 'warning'
}

export function buildAdminCommandCenterViewModel(stats = {}, feedbackSummary = {}) {
  const userCount = toNumber(stats.userCount)
  const studentCount = toNumber(stats.studentCount)
  const teacherCount = toNumber(stats.teacherCount)
  const adminCount = toNumber(stats.adminCount)
  const noticeCount = toNumber(stats.noticeCount)
  const feedbackTotal = toNumber(feedbackSummary.total)
  const feedbackPending = toNumber(feedbackSummary.pending)
  const feedbackResolved = toNumber(feedbackSummary.resolved)
  const statusLabel = stats.status || '状态未知'
  const statusTone = getStatusTone(statusLabel)
  const weeklyVisitsTotal = sumWeeklyVisits(stats.weeklyVisits)

  return {
    hero: {
      eyebrow: 'SYSTEM COMMAND',
      title: '系统治理驾驶舱',
      description: '首页只保留系统状态、待处理反馈和快捷入口，先判断风险，再进入处理动作。',
      statusLabel,
      statusTone,
      statusMeta: statusTone === 'healthy' ? '服务、存储与接口链路稳定' : '建议优先检查异常链路',
      statusChip: statusTone === 'healthy' ? 'ONLINE' : 'ALERT',
      inlineStats: [
        { key: 'users', label: '用户', value: String(userCount) },
        { key: 'notices', label: '公告', value: String(noticeCount) },
        { key: 'visits', label: '本周访问', value: String(weeklyVisitsTotal) }
      ]
    },
    primaryCards: [
      {
        key: 'feedback',
        title: '待处理反馈',
        subtitle: '优先确认是否需要人工介入',
        value: feedbackPending,
        valueSuffix: '条',
        ringPercent: getPercent(feedbackResolved, Math.max(feedbackTotal, feedbackResolved + feedbackPending)),
        highlights: [
          { key: 'resolved', label: '已解决', value: feedbackResolved, tone: 'mint' },
          { key: 'pending', label: '待处理', value: feedbackPending, tone: 'amber' }
        ],
        footnote: `总计 ${feedbackTotal} 条反馈`
      },
      {
        key: 'quick-actions',
        title: '快捷入口',
        subtitle: '保留三个最常用的管理动作',
        actions: [
          {
            key: 'users',
            label: '用户治理',
            meta: `${userCount} 个账号`,
            route: '/admin/users',
            tone: 'ice'
          },
          {
            key: 'feedback',
            label: '反馈处理',
            meta: `${feedbackPending} 条待处理`,
            route: '/admin/feedbacks',
            tone: 'amber'
          },
          {
            key: 'settings',
            label: '参数策略',
            meta: `学生 ${studentCount} / 教师 ${teacherCount} / 管理员 ${adminCount}`,
            route: '/admin/settings',
            tone: 'violet'
          }
        ]
      }
    ]
  }
}

import { reactive, readonly } from 'vue'
import { ElNotification } from 'element-plus'
import router from '@/router'
import { fetchTeacherPlagiarismJob } from '@/api/teacherAssignments'

const STORAGE_KEY = 'teacher_plagiarism_monitor_v1'
const POLL_INTERVAL_MS = 5000
const TERMINAL_STATUSES = new Set(['DONE', 'FAILED'])

export const TEACHER_PLAGIARISM_MONITOR_FINISHED_EVENT = 'teacher-plagiarism-monitor-finished'

const state = reactive({
  initialized: false,
  collapsed: false,
  tasks: []
})

let pollTimer = null
let polling = false

function persistState() {
  const snapshot = {
    collapsed: state.collapsed,
    tasks: state.tasks
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot))
}

function restoreState() {
  try {
    const snapshot = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    state.collapsed = Boolean(snapshot.collapsed)
    state.tasks = Array.isArray(snapshot.tasks) ? snapshot.tasks : []
  } catch {
    state.collapsed = false
    state.tasks = []
  }
}

function ensureInitialized() {
  if (state.initialized) return
  restoreState()
  state.initialized = true
  ensurePolling()
}

function clearPolling() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

function schedulePolling() {
  clearPolling()
  if (!state.tasks.some((task) => !TERMINAL_STATUSES.has(task.status))) {
    return
  }
  pollTimer = window.setTimeout(() => {
    pollActiveJobs()
  }, POLL_INTERVAL_MS)
}

function ensurePolling() {
  if (typeof window === 'undefined') return
  schedulePolling()
}

function buildTaskKey(jobId) {
  return String(jobId)
}

function normalizeTrackedJob({ assignmentId, assignmentTitle, job }) {
  return {
    key: buildTaskKey(job.id),
    jobId: Number(job.id),
    assignmentId: Number(assignmentId),
    assignmentTitle: assignmentTitle || `作业 ${assignmentId}`,
    status: job.status || 'UNKNOWN',
    progressDone: Number(job.progressDone || 0),
    progressTotal: Number(job.progressTotal || 0),
    threshold: Number(job.threshold || 80),
    topK: Number(job.topK || 10),
    executionMode: job.executionMode || 'ASYNC',
    reusedFromJobId: job.reusedFromJobId ? Number(job.reusedFromJobId) : null,
    thresholdMatchedPairs: Number(job.thresholdMatchedPairs || 0),
    createTime: job.createTime || '',
    errorMsg: job.errorMsg || '',
    lastUpdatedAt: Date.now(),
    notifiedTerminalStatus: job.notifiedTerminalStatus || null
  }
}

function upsertTask(taskPayload, { allowTerminalInsert = false } = {}) {
  const task = normalizeTrackedJob(taskPayload)
  const index = state.tasks.findIndex((item) => item.key === task.key)
  if (index >= 0) {
    state.tasks[index] = {
      ...state.tasks[index],
      ...task,
      assignmentTitle: task.assignmentTitle || state.tasks[index].assignmentTitle
    }
  } else if (allowTerminalInsert || !TERMINAL_STATUSES.has(task.status)) {
    state.tasks.unshift(task)
  } else {
    return null
  }
  sortTasks()
  persistState()
  ensurePolling()
  return state.tasks.find((item) => item.key === task.key) || null
}

function sortTasks() {
  state.tasks = [...state.tasks].sort((left, right) => {
    const leftActive = TERMINAL_STATUSES.has(left.status) ? 1 : 0
    const rightActive = TERMINAL_STATUSES.has(right.status) ? 1 : 0
    if (leftActive !== rightActive) return leftActive - rightActive
    return Number(right.lastUpdatedAt || 0) - Number(left.lastUpdatedAt || 0)
  })
}

function registerTeacherPlagiarismJobs({ assignmentId, assignmentTitle, jobs }) {
  ensureInitialized()
  jobs.forEach((job) => {
    upsertTask({ assignmentId, assignmentTitle, job }, { allowTerminalInsert: false })
  })
}

function trackTeacherPlagiarismJob({ assignmentId, assignmentTitle, job }) {
  ensureInitialized()
  const tracked = upsertTask({ assignmentId, assignmentTitle, job }, { allowTerminalInsert: true })
  if (tracked && TERMINAL_STATUSES.has(tracked.status)) {
    notifyTerminalState(tracked)
  }
}

function dismissTeacherPlagiarismJob(jobId) {
  ensureInitialized()
  state.tasks = state.tasks.filter((task) => task.jobId !== Number(jobId))
  persistState()
  ensurePolling()
}

function toggleTeacherPlagiarismMonitorCollapsed() {
  ensureInitialized()
  state.collapsed = !state.collapsed
  persistState()
}

function notifyTerminalState(task) {
  if (!task || task.notifiedTerminalStatus === task.status) return

  const title = task.status === 'DONE' ? '查重任务已完成' : '查重任务执行失败'
  const message = task.status === 'DONE'
    ? `${task.assignmentTitle}（Job #${task.jobId}）已完成，可点此查看报告。`
    : `${task.assignmentTitle}（Job #${task.jobId}）失败：${task.errorMsg || '请稍后重试或查看任务详情。'}`

  ElNotification({
    title,
    message,
    type: task.status === 'DONE' ? 'success' : 'error',
    duration: 6000,
    onClick: () => {
      openTeacherPlagiarismJob(task)
    }
  })

  task.notifiedTerminalStatus = task.status
  task.lastUpdatedAt = Date.now()
  persistState()

  window.dispatchEvent(new CustomEvent(TEACHER_PLAGIARISM_MONITOR_FINISHED_EVENT, {
    detail: {
      assignmentId: task.assignmentId,
      jobId: task.jobId,
      status: task.status
    }
  }))
}

async function pollActiveJobs() {
  if (polling) return
  const activeTasks = state.tasks.filter((task) => !TERMINAL_STATUSES.has(task.status))
  if (!activeTasks.length) {
    clearPolling()
    return
  }

  polling = true
  try {
    const results = await Promise.all(activeTasks.map(async (task) => {
      try {
        const latestJob = await fetchTeacherPlagiarismJob(task.jobId)
        return { task, latestJob }
      } catch {
        return null
      }
    }))

    results.filter(Boolean).forEach(({ task, latestJob }) => {
      const tracked = upsertTask({
        assignmentId: task.assignmentId,
        assignmentTitle: task.assignmentTitle,
        job: {
          ...task,
          ...latestJob,
          id: task.jobId,
          executionMode: task.executionMode,
          reusedFromJobId: task.reusedFromJobId,
          thresholdMatchedPairs: task.thresholdMatchedPairs
        }
      }, { allowTerminalInsert: true })

      if (tracked && TERMINAL_STATUSES.has(tracked.status)) {
        notifyTerminalState(tracked)
      }
    })
  } finally {
    polling = false
    ensurePolling()
  }
}

function openTeacherPlagiarismJob(task) {
  if (!task?.assignmentId) return
  router.push({
    path: '/teacher/assignments',
    query: {
      tab: 'plagiarism',
      assignmentId: task.assignmentId
    }
  })
}

export function useTeacherPlagiarismMonitor() {
  ensureInitialized()
  return {
    state: readonly(state),
    registerTeacherPlagiarismJobs,
    trackTeacherPlagiarismJob,
    dismissTeacherPlagiarismJob,
    toggleTeacherPlagiarismMonitorCollapsed,
    openTeacherPlagiarismJob
  }
}

const STORAGE_KEY = 'teacher_plagiarism_jobs_v1'

export const TEACHER_PLAGIARISM_MONITOR_FINISHED_EVENT = 'teacher-plagiarism-monitor-finished'

function readState() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  } catch {
    return {}
  }
}

function writeState(state) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

function normalizeJob(job = {}) {
  return {
    id: job.id,
    status: job.status || 'QUEUED',
    progressDone: Number(job.progressDone || 0),
    progressTotal: Number(job.progressTotal || 0),
    thresholdMatchedPairs: Number(job.thresholdMatchedPairs || 0),
    errorMsg: job.errorMsg || ''
  }
}

function dispatchFinished(detail) {
  window.dispatchEvent(new CustomEvent(TEACHER_PLAGIARISM_MONITOR_FINISHED_EVENT, { detail }))
}

export function useTeacherPlagiarismMonitor() {
  function registerTeacherPlagiarismJobs({ assignmentId, assignmentTitle, jobs = [] }) {
    if (!assignmentId) return
    const state = readState()
    state[String(assignmentId)] = {
      assignmentId: String(assignmentId),
      assignmentTitle: assignmentTitle || `作业 ${assignmentId}`,
      jobs: jobs.map(normalizeJob)
    }
    writeState(state)
  }

  function trackTeacherPlagiarismJob({ assignmentId, assignmentTitle, job }) {
    if (!assignmentId || !job?.id) return
    const state = readState()
    const key = String(assignmentId)
    const current = state[key] || {
      assignmentId: key,
      assignmentTitle: assignmentTitle || `作业 ${assignmentId}`,
      jobs: []
    }

    const normalized = normalizeJob(job)
    const index = current.jobs.findIndex((item) => String(item.id) === String(normalized.id))
    if (index >= 0) current.jobs[index] = normalized
    else current.jobs.unshift(normalized)

    current.assignmentTitle = assignmentTitle || current.assignmentTitle
    state[key] = current
    writeState(state)

    if (normalized.status === 'DONE' || normalized.status === 'FAILED') {
      dispatchFinished({
        assignmentId: key,
        assignmentTitle: current.assignmentTitle,
        jobId: normalized.id,
        status: normalized.status,
        errorMsg: normalized.errorMsg || ''
      })
    }
  }

  return {
    registerTeacherPlagiarismJobs,
    trackTeacherPlagiarismJob
  }
}

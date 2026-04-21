export function isOverviewLaunchDisabled(assignment = {}) {
  const status = String(assignment?.status || '').toLowerCase()
  return status === 'active' || status === 'archived'
}

export function canOverviewCloseNow(assignment = {}) {
  return String(assignment?.status || '').toLowerCase() === 'active'
}

export function getEndedAssignmentOverviewPrimaryAction(assignment = {}) {
  if (assignment?.hasPlagiarismJob) {
    const archived = String(assignment?.status || '').toLowerCase() === 'archived'
    return {
      event: 'results',
      label: '查看结果',
      message: archived ? '当前作业已归档，仅保留结果查看。' : '当前作业已有查重结果，可以直接进入查看。'
    }
  }

  return {
    event: 'launch',
    label: '发起查重',
    message: '当前作业还没有查重结果，可以直接发起查重。'
  }
}

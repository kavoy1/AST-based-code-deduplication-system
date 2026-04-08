export function isOverviewLaunchDisabled(assignment = {}) {
  return String(assignment?.status || '').toLowerCase() === 'active'
}

export function canOverviewCloseNow(assignment = {}) {
  return String(assignment?.status || '').toLowerCase() === 'active'
}

export function getEndedAssignmentOverviewPrimaryAction(assignment = {}) {
  if (assignment?.hasPlagiarismJob) {
    return {
      event: 'results',
      label: '查看结果',
      message: '当前作业已归档，仅保留结果查看'
    }
  }

  return {
    event: 'launch',
    label: '发起查重',
    message: '当前作业未发起查重，可以直接前往发起查重'
  }
}

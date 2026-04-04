export function isOverviewLaunchDisabled(assignment = {}) {
  return String(assignment?.status || '').toLowerCase() === 'active'
}

export function getEndedAssignmentOverviewPrimaryAction(assignment = {}) {
  if (assignment?.hasPlagiarismJob) {
    return {
      event: 'results',
      label: '查看结果',
      message: '当前作业已结束，老师可以直接查看历史查重结果。'
    }
  }

  return {
    event: 'launch',
    label: '发起查重',
    message: '当前作业还没有查重记录，可以直接开始查重。'
  }
}

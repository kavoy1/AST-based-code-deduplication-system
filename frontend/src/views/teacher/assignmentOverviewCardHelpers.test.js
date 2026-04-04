import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getEndedAssignmentOverviewPrimaryAction,
  isOverviewLaunchDisabled
} from './assignmentOverviewCardHelpers.js'

test('isOverviewLaunchDisabled blocks launch for active assignments only', () => {
  assert.equal(isOverviewLaunchDisabled({ status: 'active' }), true)
  assert.equal(isOverviewLaunchDisabled({ status: 'draft' }), false)
  assert.equal(isOverviewLaunchDisabled({ status: 'ended' }), false)
})

test('getEndedAssignmentOverviewPrimaryAction exposes launch for ended assignments without plagiarism jobs', () => {
  assert.deepEqual(
    getEndedAssignmentOverviewPrimaryAction({ status: 'ended', hasPlagiarismJob: false }),
    {
      event: 'launch',
      label: '发起查重',
      message: '当前作业未发起查重，可直接前往发起查重'
    }
  )
})

test('getEndedAssignmentOverviewPrimaryAction keeps result entry for ended assignments with plagiarism jobs', () => {
  assert.deepEqual(
    getEndedAssignmentOverviewPrimaryAction({ status: 'ended', hasPlagiarismJob: true }),
    {
      event: 'results',
      label: '查看结果',
      message: '当前作业已归档，仅保留结果查看'
    }
  )
})

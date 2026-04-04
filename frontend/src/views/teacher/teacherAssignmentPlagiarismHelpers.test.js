import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getAssignmentDirectoryCountLabel,
  shouldShowAssignmentDirectoryPagination
} from './teacherAssignmentPlagiarismHelpers.js'

test('getAssignmentDirectoryCountLabel formats directory totals', () => {
  assert.equal(getAssignmentDirectoryCountLabel(0), '0 份作业')
  assert.equal(getAssignmentDirectoryCountLabel(27), '27 份作业')
})

test('shouldShowAssignmentDirectoryPagination only enables pagination when needed', () => {
  assert.equal(shouldShowAssignmentDirectoryPagination(0, 10), false)
  assert.equal(shouldShowAssignmentDirectoryPagination(10, 10), false)
  assert.equal(shouldShowAssignmentDirectoryPagination(11, 10), true)
})

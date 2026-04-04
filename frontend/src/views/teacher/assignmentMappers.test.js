import test from 'node:test'
import assert from 'node:assert/strict'

import { normalizeAssignmentSummary } from './assignmentMappers.js'

test('normalizeAssignmentSummary keeps plagiarism availability from summary payload', () => {
  const summary = normalizeAssignmentSummary({
    id: 8,
    title: '实验一',
    language: 'JAVA',
    status: 'PUBLISHED',
    startAt: '2026-04-01 08:00:00',
    endAt: '2026-04-10 23:59:59',
    classCount: 1,
    studentCount: 30,
    submittedStudentCount: 12,
    unsubmittedStudentCount: 18,
    lateSubmissionCount: 1,
    materialCount: 2,
    hasPlagiarismJob: true
  })

  assert.equal(summary.hasPlagiarismJob, true)
})

test('normalizeAssignmentSummary defaults plagiarism availability to false', () => {
  const summary = normalizeAssignmentSummary({
    id: 9,
    title: '实验二',
    language: 'JAVA',
    status: 'PUBLISHED',
    startAt: '2026-04-01 08:00:00',
    endAt: '2026-04-10 23:59:59',
    classCount: 1,
    studentCount: 30,
    submittedStudentCount: 12,
    unsubmittedStudentCount: 18,
    lateSubmissionCount: 1,
    materialCount: 2
  })

  assert.equal(summary.hasPlagiarismJob, false)
})

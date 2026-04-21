import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildPairStudentTitle,
  countHighRiskPairs,
  filterPairsByRiskView,
  formatStudentDisplayLabel,
  getLatestJobByMode,
  getPairRank,
  getPairRankTone,
  shouldShowResultsLaunchAction
} from './plagiarismResultsHelpers.js'

test('getPairRank returns absolute rank for current page item', () => {
  assert.equal(getPairRank(1, 10, 0), 1)
  assert.equal(getPairRank(2, 10, 0), 11)
  assert.equal(getPairRank(3, 10, 4), 25)
})

test('getPairRankTone highlights only top three ranks', () => {
  assert.equal(getPairRankTone(1), 'top-1')
  assert.equal(getPairRankTone(2), 'top-2')
  assert.equal(getPairRankTone(3), 'top-3')
  assert.equal(getPairRankTone(4), 'default')
})

test('shouldShowResultsLaunchAction hides launch button inside assignment result subpages by default', () => {
  assert.equal(shouldShowResultsLaunchAction('1'), true)
  assert.equal(shouldShowResultsLaunchAction(12), true)
  assert.equal(shouldShowResultsLaunchAction(''), true)
  assert.equal(shouldShowResultsLaunchAction(null), true)
  assert.equal(shouldShowResultsLaunchAction('1', { status: 'archived' }), false)
})

test('getLatestJobByMode returns the latest job for a specific plagiarism mode', () => {
  const jobs = [
    { jobId: 9, plagiarismMode: 'DEEP' },
    { jobId: 8, plagiarismMode: 'FAST' },
    { jobId: 7, plagiarismMode: 'DEEP' }
  ]

  assert.deepEqual(getLatestJobByMode(jobs, 'FAST'), jobs[1])
  assert.deepEqual(getLatestJobByMode(jobs, 'DEEP'), jobs[0])
  assert.equal(getLatestJobByMode(jobs, 'UNKNOWN'), null)
})

test('formatStudentDisplayLabel prefers roster names over raw student ids', () => {
  const studentMap = {
    4: { name: '张三', number: '2026000004' },
    6: { name: '李四', number: '2026000006' },
    7: { name: '', number: '2026000007' }
  }

  assert.equal(formatStudentDisplayLabel(4, studentMap), '张三 (2026000004)')
  assert.equal(formatStudentDisplayLabel('6', studentMap), '李四 (2026000006)')
  assert.equal(formatStudentDisplayLabel(7, studentMap), '学号 2026000007')
  assert.equal(formatStudentDisplayLabel(8, studentMap), '学生 8')
})

test('buildPairStudentTitle renders both sides with student names when available', () => {
  const title = buildPairStudentTitle(
    { studentA: 4, studentB: 6 },
    {
      4: { name: '张三', number: '2026000004' },
      6: { name: '李四', number: '2026000006' }
    }
  )

  assert.equal(title, '张三 (2026000004) → 李四 (2026000006)')
})

test('filterPairsByRiskView keeps all pairs by default and can focus on high risk pairs', () => {
  const pairs = [
    { pairId: 1, score: 95 },
    { pairId: 2, score: 82 },
    { pairId: 3, score: 67 }
  ]

  assert.deepEqual(filterPairsByRiskView(pairs, 'all', 80), pairs)
  assert.deepEqual(filterPairsByRiskView(pairs, 'high-risk', 80), pairs.slice(0, 2))
})

test('countHighRiskPairs counts only pairs at or above the threshold', () => {
  const pairs = [
    { pairId: 1, score: 95 },
    { pairId: 2, score: 80 },
    { pairId: 3, score: 79 }
  ]

  assert.equal(countHighRiskPairs(pairs, 80), 2)
  assert.equal(countHighRiskPairs(pairs, 90), 1)
})

import test from 'node:test'
import assert from 'node:assert/strict'

import {
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

test('shouldShowResultsLaunchAction hides launch button inside assignment result subpages', () => {
  assert.equal(shouldShowResultsLaunchAction('1'), false)
  assert.equal(shouldShowResultsLaunchAction(12), false)
  assert.equal(shouldShowResultsLaunchAction(''), true)
  assert.equal(shouldShowResultsLaunchAction(null), true)
})

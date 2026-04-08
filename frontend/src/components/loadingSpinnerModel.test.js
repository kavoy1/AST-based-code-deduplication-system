import test from 'node:test'
import assert from 'node:assert/strict'

import { buildSpinnerSegments } from './loadingSpinnerModel.js'

test('buildSpinnerSegments returns three animated bars with staggered timing', () => {
  const segments = buildSpinnerSegments()

  assert.equal(segments.length, 3)
  assert.deepEqual(segments[0], { height: 20, delay: 0, marginInline: 0 })
  assert.deepEqual(segments[1], { height: 35, delay: 0.25, marginInline: 5 })
  assert.deepEqual(segments[2], { height: 20, delay: 0.5, marginInline: 0 })
})

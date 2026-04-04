import test from 'node:test'
import assert from 'node:assert/strict'

import { buildCountdownLabel, buildPaginationMeta, filterAssignmentsByClass, paginateAssignments } from './taskListHelpers.js'

test('filterAssignmentsByClass returns all items for all class filter', () => {
  const assignments = [
    { id: 1, classId: 11 },
    { id: 2, classId: 22 }
  ]

  const result = filterAssignmentsByClass(assignments, 'all')

  assert.equal(result.length, 2)
})

test('buildCountdownLabel formats active assignment remaining time', () => {
  const result = buildCountdownLabel('2026-04-03T00:00:00+08:00', new Date('2026-04-01T10:20:30+08:00').getTime())

  assert.equal(result, '剩余 1天 13:39:30')
})

test('paginateAssignments returns current page slice and total', () => {
  const assignments = Array.from({ length: 8 }, (_, index) => ({ id: index + 1 }))

  const result = paginateAssignments(assignments, 2, 3)

  assert.equal(result.total, 8)
  assert.deepEqual(result.items.map((item) => item.id), [4, 5, 6])
})

test('buildPaginationMeta returns visible range and page count', () => {
  const result = buildPaginationMeta(8, 2, 3)

  assert.deepEqual(result, {
    totalPages: 3,
    from: 4,
    to: 6
  })
})

test('paginateAssignments supports compact two-item pages', () => {
  const assignments = Array.from({ length: 5 }, (_, index) => ({ id: index + 1 }))

  const result = paginateAssignments(assignments, 2, 2)

  assert.equal(result.total, 5)
  assert.deepEqual(result.items.map((item) => item.id), [3, 4])
})

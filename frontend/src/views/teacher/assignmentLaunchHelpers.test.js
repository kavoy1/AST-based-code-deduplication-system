import test from 'node:test'
import assert from 'node:assert/strict'

import { mergeAssignmentRosterContext } from './assignmentLaunchHelpers.js'

test('mergeAssignmentRosterContext fills missing class context from detail payload', () => {
  const merged = mergeAssignmentRosterContext(
    {
      id: 8,
      title: 'Java 查重',
      classIds: [],
      classNames: []
    },
    {
      id: 8,
      classes: [
        { classId: 11, className: '软工 1 班' },
        { classId: 12, className: '软工 2 班' }
      ],
      classNamesText: '软工 1 班、软工 2 班'
    }
  )

  assert.deepEqual(merged.classIds, [11, 12])
  assert.deepEqual(merged.classNames, ['软工 1 班', '软工 2 班'])
  assert.equal(merged.classNamesText, '软工 1 班、软工 2 班')
})

test('mergeAssignmentRosterContext keeps existing summary context when already complete', () => {
  const merged = mergeAssignmentRosterContext(
    {
      id: 8,
      title: 'Java 查重',
      classIds: [11],
      classNames: ['软工 1 班'],
      classNamesText: '共 1 个班级'
    },
    {
      id: 8,
      classes: [
        { classId: 12, className: '软工 2 班' }
      ],
      classNamesText: '软工 2 班'
    }
  )

  assert.deepEqual(merged.classIds, [11])
  assert.deepEqual(merged.classNames, ['软工 1 班'])
  assert.equal(merged.classNamesText, '共 1 个班级')
})

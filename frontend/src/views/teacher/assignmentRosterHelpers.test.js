import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildSubmittedRoster,
  mapRosterStudent,
  resolveRosterStudentName
} from './assignmentRosterHelpers.js'

test('resolveRosterStudentName prefers nickname and falls back to username', () => {
  assert.equal(resolveRosterStudentName({ nickname: '张三', username: 'zhangsan' }), '张三')
  assert.equal(resolveRosterStudentName({ nickname: '   ', username: 'lisi' }), 'lisi')
  assert.equal(resolveRosterStudentName({ nickname: '学生 8', username: '王小明' }), '王小明')
  assert.equal(resolveRosterStudentName({}), '未命名学生')
})

test('mapRosterStudent keeps class name and omits student number from roster card content model', () => {
  const item = mapRosterStudent({
    id: 8,
    nickname: '测试学生',
    studentNumber: '2026000008'
  }, '软工 1 班')

  assert.equal(item.studentId, 8)
  assert.equal(item.studentName, '测试学生')
  assert.equal(item.className, '软工 1 班')
  assert.equal('studentNumber' in item, false)
})

test('buildSubmittedRoster reuses mapped student names by student id', () => {
  const roster = buildSubmittedRoster([
    {
      studentId: 8,
      studentName: '',
      lastSubmittedAt: '2026-04-20 16:18:55'
    }
  ], new Map([
    [8, { studentName: '王小明', className: '软工 1 班' }]
  ]))

  assert.equal(roster[0].studentName, '王小明')
  assert.equal(roster[0].className, '软工 1 班')
})

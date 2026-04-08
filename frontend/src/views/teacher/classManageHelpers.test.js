import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildTeacherApplicationClassOptions,
  filterTeacherApplications,
  getSelectableTeacherApplications,
  getTeacherApplicationStatusText,
  buildTeacherStudentClassOptions,
  filterTeacherStudents,
  getInviteCountLabel,
  paginateTeacherRows,
  shouldShowInvitePagination,
  sortTeacherApplications
} from './classManageHelpers.js'

test('getInviteCountLabel formats invite totals for the footer', () => {
  assert.equal(getInviteCountLabel(0), '共 0 个班级邀请码')
  assert.equal(getInviteCountLabel(12), '共 12 个班级邀请码')
})

test('shouldShowInvitePagination only shows pagination when one page is not enough', () => {
  assert.equal(shouldShowInvitePagination(0, 10), false)
  assert.equal(shouldShowInvitePagination(10, 10), false)
  assert.equal(shouldShowInvitePagination(11, 10), true)
  assert.equal(shouldShowInvitePagination(25, 20), true)
})

test('buildTeacherStudentClassOptions expands merged class names into unique options', () => {
  const options = buildTeacherStudentClassOptions([
    { class_name: '软工1班、软工2班' },
    { class_name: '软工2班、软工3班' },
    { class_name: '' }
  ])

  assert.deepEqual(options, ['软工1班', '软工2班', '软工3班'])
})

test('filterTeacherStudents keeps only rows inside teacher scope filters', () => {
  const rows = filterTeacherStudents(
    [
      { student_number: '2026000001', username: 'student_test', nickname: '测试学生', college: '计算机学院', class_name: '软工1班、软工2班' },
      { student_number: '2026000002', username: 'student_test_2', nickname: '测试学生2', college: '数学学院', class_name: '数科1班' }
    ],
    {
      keyword: '测试学生',
      college: '计算机学院',
      className: '软工2班'
    }
  )

  assert.equal(rows.length, 1)
  assert.equal(rows[0].student_number, '2026000001')
})

test('getTeacherApplicationStatusText marks resolved and pending applications', () => {
  assert.equal(getTeacherApplicationStatusText({ status: 'approved' }), '已解决')
  assert.equal(getTeacherApplicationStatusText({ reviewStatus: 'rejected' }), '已解决')
  assert.equal(getTeacherApplicationStatusText({ status: 'pending' }), '未解决')
})

test('sortTeacherApplications keeps resolved rows first and newer items ahead inside each group', () => {
  const rows = sortTeacherApplications([
    { id: 1, status: 'approved', applyTime: '2026-04-04T10:00:00Z' },
    { id: 2, status: 'pending', applyTime: '2026-04-04T09:00:00Z' },
    { id: 3, status: 'pending', applyTime: '2026-04-04T11:00:00Z' },
    { id: 4, status: 'rejected', applyTime: '2026-04-04T12:00:00Z' }
  ])

  assert.deepEqual(rows.map((row) => row.id), [4, 1, 3, 2])
})

test('buildTeacherApplicationClassOptions returns unique class names', () => {
  const rows = buildTeacherApplicationClassOptions([
    { className: 'Class A' },
    { className: 'Class B' },
    { className: 'Class A' },
    { className: '' }
  ])

  assert.deepEqual(rows, ['Class A', 'Class B'])
})

test('filterTeacherApplications applies status, class and keyword filters together', () => {
  const rows = filterTeacherApplications(
    [
      { id: 1, status: 'pending', studentNumber: '2026000001', nickname: 'Alice', username: 'alice', className: 'Class A' },
      { id: 2, status: 'approved', studentNumber: '2026000002', nickname: 'Bob', username: 'bob', className: 'Class B' },
      { id: 3, status: 'pending', studentNumber: '2026000003', nickname: 'Cindy', username: 'cindy', className: 'Class B' }
    ],
    {
      status: 'pending',
      className: 'Class B',
      keyword: 'cindy'
    }
  )

  assert.deepEqual(rows.map((row) => row.id), [3])
})

test('paginateTeacherRows returns only the current page rows', () => {
  const rows = paginateTeacherRows(
    [
      { id: 1 },
      { id: 2 },
      { id: 3 },
      { id: 4 },
      { id: 5 }
    ],
    2,
    2
  )

  assert.deepEqual(rows.map((row) => row.id), [3, 4])
})

test('getSelectableTeacherApplications keeps only pending rows on the current page', () => {
  const rows = getSelectableTeacherApplications([
    { id: 1, status: 'pending' },
    { id: 2, status: 'approved' },
    { id: 3, reviewStatus: 'rejected' },
    { id: 4, status: 'pending' }
  ])

  assert.deepEqual(rows.map((row) => row.id), [1, 4])
})

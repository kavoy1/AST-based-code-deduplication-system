import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildAdminCommandCenterViewModel,
  getStatusTone
} from './adminCommandCenterHelpers.js'

test('buildAdminCommandCenterViewModel keeps only three homepage blocks', () => {
  const viewModel = buildAdminCommandCenterViewModel(
    {
      userCount: 18,
      studentCount: 12,
      teacherCount: 5,
      adminCount: 1,
      noticeCount: 6,
      status: '运行正常',
      weeklyVisits: [4, 7, 9, 12]
    },
    {
      total: 8,
      pending: 2,
      resolved: 6
    }
  )

  assert.equal(viewModel.hero.statusLabel, '运行正常')
  assert.equal(viewModel.hero.statusTone, 'healthy')
  assert.equal(viewModel.hero.inlineStats.length, 3)
  assert.equal(viewModel.hero.inlineStats[0].label, '用户')
  assert.equal(viewModel.hero.inlineStats[0].value, '18')

  assert.equal(viewModel.primaryCards.length, 2)
  assert.equal(viewModel.primaryCards[0].key, 'feedback')
  assert.equal(viewModel.primaryCards[0].value, 2)
  assert.equal(viewModel.primaryCards[0].ringPercent, 75)
  assert.equal(viewModel.primaryCards[0].highlights.length, 2)

  assert.equal(viewModel.primaryCards[1].key, 'quick-actions')
  assert.equal(viewModel.primaryCards[1].actions.length, 3)
  assert.equal(viewModel.primaryCards[1].actions[0].label, '用户治理')
  assert.equal(viewModel.primaryCards[1].actions[0].meta, '18 个账号')
  assert.equal(viewModel.primaryCards[1].actions[1].meta, '2 条待处理')
})

test('getStatusTone marks non-healthy statuses as warning', () => {
  assert.equal(getStatusTone('运行正常'), 'healthy')
  assert.equal(getStatusTone('正常'), 'healthy')
  assert.equal(getStatusTone('维护中'), 'warning')
  assert.equal(getStatusTone(''), 'warning')
})

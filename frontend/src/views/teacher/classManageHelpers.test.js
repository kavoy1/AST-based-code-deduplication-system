import test from 'node:test'
import assert from 'node:assert/strict'

import { getInviteCountLabel, shouldShowInvitePagination } from './classManageHelpers.js'

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

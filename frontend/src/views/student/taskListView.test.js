import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const viewSource = readFileSync(new URL('./TaskList.vue', import.meta.url), 'utf8')

test('student task cards do not render preview description copy in list view', () => {
  assert.equal(viewSource.includes('暂无作业说明，点击查看作业进入详情。'), false)
  assert.equal(viewSource.includes('student-assignment-card__description'), false)
})

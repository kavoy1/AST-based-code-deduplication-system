import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const summaryFile = readFileSync(new URL('./SimilarityPairSummary.vue', import.meta.url), 'utf8')

test('SimilarityPairSummary keeps key user-facing labels readable in Chinese', () => {
  assert.match(summaryFile, /处理总结/)
  assert.match(summaryFile, /查重命中明细/)
  assert.match(summaryFile, /教师确认信息/)
  assert.match(summaryFile, /证据摘要/)
  assert.match(summaryFile, /生成 AI 解释/)
})

test('SimilarityPairSummary does not keep known mojibake labels', () => {
  assert.doesNotMatch(summaryFile, /璇佹嵁鎽樿/)
  assert.doesNotMatch(summaryFile, /鏌ラ噸鍛戒腑鏄庣粏/)
  assert.doesNotMatch(summaryFile, /澶勭悊鎬荤粨/)
})

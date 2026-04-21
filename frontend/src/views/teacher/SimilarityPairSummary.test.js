import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const summaryFile = readFileSync(new URL('./SimilarityPairSummary.vue', import.meta.url), 'utf8')

test('SimilarityPairSummary shows structured AI similarity judgment sections', () => {
  assert.match(summaryFile, /AI判定相似度/)
  assert.match(summaryFile, /核心依据/)
  assert.match(summaryFile, /差异修正/)
  assert.match(summaryFile, /为什么不是更高/)
  assert.match(summaryFile, /为什么不是更低/)
})

test('SimilarityPairSummary distinguishes code-only and system-evidence modes', () => {
  assert.match(summaryFile, /仅代码/)
  assert.match(summaryFile, /代码 \+ 系统证据/)
  assert.match(summaryFile, /系统证据如何影响结果/)
})

test('SimilarityPairSummary still reminds teachers which model is active', () => {
  assert.match(summaryFile, /当前模型：/)
  assert.match(summaryFile, /currentAiRuntimeText/)
})

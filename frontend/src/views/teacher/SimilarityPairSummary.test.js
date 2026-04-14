import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const summaryFile = readFileSync(new URL('./SimilarityPairSummary.vue', import.meta.url), 'utf8')

test('SimilarityPairSummary keeps key user-facing labels readable in Chinese', () => {
  assert.match(summaryFile, /AI 解释/)
  assert.match(summaryFile, /AI 对话记录/)
  assert.match(summaryFile, /生成解释/)
})

test('SimilarityPairSummary reminds teachers which model is currently active', () => {
  assert.match(summaryFile, /当前模型：/)
  assert.match(summaryFile, /currentAiModel/)
})

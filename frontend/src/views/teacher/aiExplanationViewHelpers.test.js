import test from 'node:test'
import assert from 'node:assert/strict'
import { formatAiLevelLabel, normalizeAiExplanationRecord } from './aiExplanationViewHelpers.js'

test('normalizeAiExplanationRecord returns range display and evidence sections', () => {
  const view = normalizeAiExplanationRecord({
    status: 'SUCCESS',
    requestPayload: JSON.stringify({ mode: 'CODE_ONLY' }),
    responsePayload: JSON.stringify({
      structured: {
        estimatedSimilarity: 82,
        rangeMin: 78,
        rangeMax: 85,
        useRange: true,
        level: 'HIGH',
        confidence: 'MEDIUM',
        summary: '核心算法一致',
        coreEvidence: ['排序流程一致'],
        differenceAdjustments: ['数据结构不同'],
        lowerBoundReason: '至少存在结构一致',
        upperBoundReason: '仍有改写',
        systemEvidenceEffects: []
      }
    })
  })

  assert.equal(view.scoreText, '78%-85%')
  assert.equal(view.modeLabel, '仅代码')
  assert.equal(view.levelLabel, '高相似')
  assert.deepEqual(view.coreEvidence, ['排序流程一致'])
  assert.deepEqual(view.systemEvidenceEffects, [])
})

test('normalizeAiExplanationRecord keeps system evidence adjustments in evidence mode', () => {
  const view = normalizeAiExplanationRecord({
    status: 'SUCCESS',
    requestPayload: JSON.stringify({ mode: 'CODE_WITH_SYSTEM_EVIDENCE', systemScore: 90 }),
    responsePayload: JSON.stringify({
      structured: {
        estimatedSimilarity: 88,
        rangeMin: 85,
        rangeMax: 90,
        useRange: true,
        level: 'HIGH',
        confidence: 'HIGH',
        summary: '综合判断高度相似',
        coreEvidence: ['主流程一致'],
        differenceAdjustments: ['部分函数拆分不同'],
        lowerBoundReason: '核心流程足够接近',
        upperBoundReason: '仍有模块拆分差异',
        systemEvidenceEffects: ['AC 相似系数抬高了综合判断']
      }
    })
  })

  assert.equal(view.modeLabel, '代码 + 系统证据')
  assert.equal(view.scoreText, '85%-90%')
  assert.equal(view.systemScore, 90)
  assert.deepEqual(view.systemEvidenceEffects, ['AC 相似系数抬高了综合判断'])
})

test('normalizeAiExplanationRecord does not show fallback percentage while generating', () => {
  const view = normalizeAiExplanationRecord({
    status: 'GENERATING',
    requestPayload: JSON.stringify({ mode: 'CODE_ONLY', systemScore: 100 }),
    responsePayload: JSON.stringify({})
  })

  assert.equal(view.scoreText, '评估中')
  assert.equal(view.systemScore, 100)
})

test('formatAiLevelLabel returns readable Chinese labels', () => {
  assert.equal(formatAiLevelLabel('HIGH'), '高相似')
  assert.equal(formatAiLevelLabel('MEDIUM'), '中高相似')
  assert.equal(formatAiLevelLabel('LOW'), '低相似')
})

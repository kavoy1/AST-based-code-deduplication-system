import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildTeacherDecisionSummary,
  normalizeJob,
  normalizePairDetail,
  normalizeReport,
  summarizeEvidenceList
} from './assignmentMappers.js'

test('normalizeJob preserves unlimited topK and exposes threshold aliases', () => {
  const job = normalizeJob({
    id: 12,
    thresholdScore: 80,
    topKPerStudent: 0,
    plagiarismMode: 'deep'
  })

  assert.equal(job.threshold, 80)
  assert.equal(job.thresholdScore, 80)
  assert.equal(job.topK, 0)
  assert.equal(job.topKPerStudent, 0)
  assert.equal(job.plagiarismMode, 'DEEP')
})

test('normalizeJob respects zero topK from paramsJson', () => {
  const job = normalizeJob({
    id: 18,
    thresholdScore: 90,
    topKPerStudent: 10,
    paramsJson: JSON.stringify({
      thresholdScore: 75,
      topKPerStudent: 0,
      plagiarismMode: 'FAST'
    })
  })

  assert.equal(job.threshold, 75)
  assert.equal(job.topK, 0)
})

test('normalizeReport preserves zero minScore and zero topK', () => {
  const report = normalizeReport({
    jobId: 7,
    minScore: 0,
    perStudentTopK: 0,
    pairs: [],
    incomparableSubmissions: []
  })

  assert.equal(report.minScore, 0)
  assert.equal(report.perStudentTopK, 0)
})

test('summarizeEvidenceList falls back to readable generated title when summary is garbled', () => {
  const items = summarizeEvidenceList([
    {
      id: 1,
      type: 'SIGNATURE_OVERLAP_TOP',
      summary: '閹诲綊鍘ら敍灞灸佸?868 C=1.0000',
      weight: 1804,
      payloadJson: JSON.stringify({
        totals: { M: 1804, AC: 1 },
        topN: [{ signature: 'ForEach', matchedCount: 108 }]
      })
    }
  ])

  assert.match(items[0].title, /foreach/i)
  assert.equal(items[0].summary, '')
})

test('buildTeacherDecisionSummary recommends direct confirm for scores at threshold', () => {
  const summary = buildTeacherDecisionSummary({
    score: 85,
    evidences: [
      {
        title: '鏂规硶璋冪敤 arity=1',
        weight: 868,
        totals: { M: 868, AC: 1 },
        topMatches: [
          { label: '鏂规硶璋冪敤 arity=1' },
          { label: '瀛楅潰閲忕被鍨?STR' },
          { label: 'return 杩斿洖缁撴瀯' }
        ]
      }
    ]
  })

  assert.equal(summary.tone, 'confirm')
  assert.equal(summary.primaryAction, 'CONFIRMED')
  assert.match(summary.title, /寤鸿鐩存帴纭/)
  assert.match(summary.reasons[0], /85%/)
  assert.match(summary.reasons[1], /868/)
})

test('buildTeacherDecisionSummary recommends review below confirm threshold', () => {
  const summary = buildTeacherDecisionSummary({
    score: 74,
    evidences: [
      {
        title: 'if 鍒嗘敮缁撴瀯',
        weight: 32,
        totals: { M: 32, AC: 0.78 },
        topMatches: [{ label: 'if 鍒嗘敮缁撴瀯' }]
      }
    ]
  })

  assert.equal(summary.tone, 'review')
  assert.equal(summary.primaryAction, 'PENDING')
  assert.match(summary.title, /寤鸿缁х画澶嶆牳/)
})

test('normalizePairDetail keeps current AI runtime fields for teacher reminders', () => {
  const detail = normalizePairDetail({
    pairId: 1,
    jobId: 9,
    studentA: '2026000001',
    studentB: '2026000002',
    score: 97,
    currentAiModel: 'qwen3.6-plus',
    currentAiProvider: 'QWEN'
  })

  assert.equal(detail.currentAiModel, 'qwen3.6-plus')
  assert.equal(detail.currentAiProvider, 'QWEN')
})

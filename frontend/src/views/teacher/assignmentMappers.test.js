import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildTeacherDecisionSummary,
  normalizeJob,
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
      summary: '鎻归厤锛屾ā寮?868 C=1.0000',
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
        title: '方法调用 arity=1',
        weight: 868,
        totals: { M: 868, AC: 1 },
        topMatches: [
          { label: '方法调用 arity=1' },
          { label: '字面量类型 STR' },
          { label: 'return 返回结构' }
        ]
      }
    ]
  })

  assert.equal(summary.tone, 'confirm')
  assert.equal(summary.primaryAction, 'CONFIRMED')
  assert.match(summary.title, /建议直接确认/)
  assert.match(summary.reasons[0], /85%/)
  assert.match(summary.reasons[1], /868/)
})

test('buildTeacherDecisionSummary recommends review below confirm threshold', () => {
  const summary = buildTeacherDecisionSummary({
    score: 74,
    evidences: [
      {
        title: 'if 分支结构',
        weight: 32,
        totals: { M: 32, AC: 0.78 },
        topMatches: [{ label: 'if 分支结构' }]
      }
    ]
  })

  assert.equal(summary.tone, 'review')
  assert.equal(summary.primaryAction, 'PENDING')
  assert.match(summary.title, /建议继续复核/)
})

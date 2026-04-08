import test from 'node:test'
import assert from 'node:assert/strict'

import { normalizeJob, normalizeReport } from './assignmentMappers.js'

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

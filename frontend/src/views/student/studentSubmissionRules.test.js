import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildStudentSubmissionPolicy,
  normalizeStudentAssignmentLanguage
} from './studentSubmissionRules.js'

test('normalizeStudentAssignmentLanguage keeps JAVA and C, falls back to JAVA', () => {
  assert.equal(normalizeStudentAssignmentLanguage('JAVA'), 'JAVA')
  assert.equal(normalizeStudentAssignmentLanguage('java'), 'JAVA')
  assert.equal(normalizeStudentAssignmentLanguage('C'), 'C')
  assert.equal(normalizeStudentAssignmentLanguage('python'), 'JAVA')
  assert.equal(normalizeStudentAssignmentLanguage(''), 'JAVA')
})

test('buildStudentSubmissionPolicy keeps Java multi-file and text editing workflow', () => {
  const policy = buildStudentSubmissionPolicy({
    language: 'JAVA',
    maxFiles: 5,
    allowResubmit: true
  })

  assert.equal(policy.language, 'JAVA')
  assert.equal(policy.supportsTextMode, true)
  assert.equal(policy.effectiveFileLimit, 5)
  assert.equal(policy.fileAccept, '.java')
  assert.match(policy.uploadTitle, /Java/i)
  assert.match(policy.ruleSummary, /覆盖旧内容/)
})

test('buildStudentSubmissionPolicy constrains C to one source file or one zip package', () => {
  const policy = buildStudentSubmissionPolicy({
    language: 'C',
    maxFiles: 9,
    allowResubmit: false
  })

  assert.equal(policy.language, 'C')
  assert.equal(policy.supportsTextMode, false)
  assert.equal(policy.effectiveFileLimit, 1)
  assert.equal(policy.fileAccept, '.c,.zip')
  assert.match(policy.uploadTip, /\.zip/)
  assert.match(policy.maxFilesSummary, /1/)
  assert.match(policy.detailGuidance, /\.c/)
})

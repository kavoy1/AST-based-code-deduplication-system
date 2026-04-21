import test from 'node:test'
import assert from 'node:assert/strict'

import {
  normalizeTeacherAssignmentLanguage,
  supportedTeacherAssignmentLanguages,
  teacherAssignmentLanguageHint
} from './teacherAssignmentEditorHelpers.js'

test('supportedTeacherAssignmentLanguages exposes JAVA and C only', () => {
  assert.deepEqual(supportedTeacherAssignmentLanguages, ['JAVA', 'C'])
})

test('normalizeTeacherAssignmentLanguage falls back to JAVA for unsupported values', () => {
  assert.equal(normalizeTeacherAssignmentLanguage('JAVA'), 'JAVA')
  assert.equal(normalizeTeacherAssignmentLanguage('C'), 'C')
  assert.equal(normalizeTeacherAssignmentLanguage('PYTHON'), 'JAVA')
  assert.equal(normalizeTeacherAssignmentLanguage(''), 'JAVA')
})

test('teacherAssignmentLanguageHint explains current publish scope', () => {
  assert.match(teacherAssignmentLanguageHint, /JAVA/)
  assert.match(teacherAssignmentLanguageHint, /C/)
})

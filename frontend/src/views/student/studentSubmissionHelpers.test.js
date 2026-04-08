import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildCodeEntriesFromCurrentSubmission,
  normalizeDraftEntries,
  normalizeStudentCurrentSubmission
} from './studentSubmissionHelpers.js'

test('normalizeStudentCurrentSubmission keeps only current submission summary fields', () => {
  const submission = normalizeStudentCurrentSubmission({
    id: 90,
    assignmentId: 20,
    classId: 3,
    studentId: 1001,
    submitTime: '2026-04-04T09:30:00',
    isValid: 1,
    parseOkFiles: 2,
    totalFiles: 2,
    isLate: 0,
    deadlineAt: '2026-04-13T00:00:00',
    files: [
      {
        filename: 'Main.java',
        bytes: 128,
        parseStatus: 'OK',
        parseError: null,
        content: 'public class Main {}'
      }
    ]
  })

  assert.equal(submission.id, 90)
  assert.equal(submission.totalFiles, 2)
  assert.equal(submission.files[0].filename, 'Main.java')
  assert.equal('version' in submission, false)
})

test('buildCodeEntriesFromCurrentSubmission maps current files into editable drafts', () => {
  const entries = buildCodeEntriesFromCurrentSubmission({
    files: [
      { filename: 'Utils.java', content: 'class Utils {}' },
      { filename: 'Main.java', content: 'public class Main {}' }
    ]
  })

  assert.equal(entries.length, 2)
  assert.deepEqual(entries.map((item) => item.filename), ['Main.java', 'Utils.java'])
  assert.equal(entries[0].content, 'public class Main {}')
})

test('normalizeDraftEntries appends java suffix and resolves duplicate filenames', () => {
  const entries = normalizeDraftEntries([
    { filename: 'Main', content: 'public class Main {}' },
    { filename: 'main.java', content: 'class AnotherMain {}' },
    { filename: '', content: 'class Helper {}' },
    { filename: 'Ignore.java', content: '   ' }
  ])

  assert.deepEqual(entries, [
    { filename: 'Main.java', content: 'public class Main {}' },
    { filename: 'main_2.java', content: 'class AnotherMain {}' },
    { filename: 'Main_3.java', content: 'class Helper {}' }
  ])
})

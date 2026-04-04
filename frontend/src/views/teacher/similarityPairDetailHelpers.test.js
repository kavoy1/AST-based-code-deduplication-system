import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildCodeLines,
  buildCompareTabs,
  buildSegmentRailMarkers,
  getLineHighlightState,
  normalizeCodeCompare,
  normalizeSourceCode
} from './similarityPairDetailHelpers.js'

test('normalizeCodeCompare keeps normalized segments and numeric line ranges', () => {
  const compare = normalizeCodeCompare({
    left: {
      title: 'ScoreUtils.java',
      code: 'public class ScoreUtils {}',
      highlights: [{ startLine: '3', endLine: '8' }]
    },
    right: {
      title: 'ScoreHelper.java',
      code: 'public class ScoreHelper {}',
      highlights: [{ startLine: 4, endLine: 9 }]
    },
    segments: [
      {
        id: '',
        label: '',
        summary: '平均值逻辑',
        score: '93',
        leftFile: 'ScoreUtils.java',
        leftStartLine: '3',
        leftEndLine: '8',
        rightFile: 'ScoreHelper.java',
        rightStartLine: 4,
        rightEndLine: 10
      }
    ]
  })

  assert.equal(compare.left.highlights[0].startLine, 3)
  assert.equal(compare.right.highlights[0].endLine, 9)
  assert.equal(compare.segments[0].id, 'seg-1')
  assert.equal(compare.segments[0].label, '片段 1')
  assert.equal(compare.segments[0].score, 93)
  assert.equal(compare.segments[0].leftStartLine, 3)
  assert.equal(compare.segments[0].rightEndLine, 10)
})

test('getLineHighlightState distinguishes active and passive segment ranges', () => {
  const active = { startLine: 4, endLine: 8 }
  const all = [{ startLine: 4, endLine: 8 }, { startLine: 12, endLine: 16 }]

  assert.equal(getLineHighlightState(5, active, all), 'active')
  assert.equal(getLineHighlightState(13, active, all), 'related')
  assert.equal(getLineHighlightState(2, active, all), 'none')
})

test('buildCodeLines preserves line numbers and basic keyword highlighting', () => {
  const lines = buildCodeLines('public class Demo {\n  return 0;\n}')

  assert.equal(lines.length, 3)
  assert.equal(lines[0].number, 1)
  assert.match(lines[0].html, /token keyword/)
  assert.match(lines[1].html, /return/)
})

test('normalizeSourceCode strips token html fragments from broken payload', () => {
  const badCode = '"token keyword">public static int calc(int[] arr) {\n  class="token keyword">return 0;\n}'
  const normalized = normalizeSourceCode(badCode)

  assert.doesNotMatch(normalized, /token keyword/)
  assert.match(normalized, /public static int calc/)
  assert.match(normalized, /return 0;/)
})

test('buildCodeLines keeps source code readable instead of exposing token markup text', () => {
  const lines = buildCodeLines('public static double getAverage(int[] scores) {\n  return scores.length;\n}')

  assert.equal(lines.length, 3)
  assert.doesNotMatch(lines[0].html, /class=&lt;span/)
  assert.match(lines[0].html, /public/)
  assert.match(lines[0].html, /getAverage/)
})

test('buildSegmentRailMarkers maps segment start lines onto vertical rail positions', () => {
  const markers = buildSegmentRailMarkers(
    [
      { id: 'seg-1', label: '片段 1', summary: 'A', leftStartLine: 10, leftEndLine: 20, rightStartLine: 12, rightEndLine: 22 },
      { id: 'seg-2', label: '片段 2', summary: 'B', leftStartLine: 60, leftEndLine: 72, rightStartLine: 64, rightEndLine: 76 }
    ],
    100
  )

  assert.equal(markers.length, 2)
  assert.equal(markers[0].segment.id, 'seg-1')
  assert.equal(markers[0].topOffset, 9)
  assert.equal(markers[1].topOffset, 59)
})

test('buildCompareTabs groups matched pairs, cross-file fragments and unmatched files', () => {
  const tabs = buildCompareTabs({
    matchedFilePairs: [
      {
        key: 'pair-1',
        label: '匹配对 1',
        relationType: 'MATCHED_PAIR',
        left: { title: 'Utils.java', code: 'class A {}', highlights: [] },
        right: { title: 'Main.java', code: 'class B {}', highlights: [] },
        segments: []
      }
    ],
    crossFileSegments: [
      {
        key: 'cross-1',
        label: '跨文件片段 1',
        relationType: 'CROSS_FILE',
        left: { title: 'A.java', code: 'class A {}', highlights: [] },
        right: { title: 'B.java', code: 'class B {}', highlights: [] },
        segments: []
      }
    ],
    unmatchedLeftFiles: [
      {
        key: 'left-only-1',
        label: '仅学生A存在 1',
        pane: { title: 'OnlyLeft.java', code: 'class OnlyLeft {}', highlights: [] }
      }
    ],
    unmatchedRightFiles: [
      {
        key: 'right-only-1',
        label: '仅学生B存在 1',
        pane: { title: 'OnlyRight.java', code: 'class OnlyRight {}', highlights: [] }
      }
    ]
  })

  assert.equal(tabs.length, 4)
  assert.equal(tabs[0].type, 'matched')
  assert.equal(tabs[1].type, 'cross')
  assert.equal(tabs[2].type, 'left-only')
  assert.equal(tabs[3].type, 'right-only')
  assert.equal(tabs[2].compare.left.title, 'OnlyLeft.java')
  assert.equal(tabs[3].compare.right.title, 'OnlyRight.java')
})

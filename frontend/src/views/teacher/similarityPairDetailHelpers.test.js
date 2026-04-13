import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildCompareTabFilters,
  buildCodeLines,
  buildCompareTabs,
  filterCompareTabs,
  buildSegmentLineSummary,
  buildSegmentMeta,
  buildSegmentRailMarkers,
  formatSegmentBlockType,
  getLineHighlightMeta,
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
        summary: 'average loop',
        score: '93',
        blockType: 'FOREACH',
        leftMethodKey: 'getAverage#1(param1)',
        rightMethodKey: 'calcAverage#1(param1)',
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
  assert.equal(compare.segments[0].score, 93)
  assert.equal(compare.segments[0].blockType, 'FOREACH')
  assert.equal(compare.segments[0].leftMethodKey, 'getAverage#1(param1)')
  assert.equal(compare.segments[0].rightMethodKey, 'calcAverage#1(param1)')
  assert.equal(compare.segments[0].leftStartLine, 3)
  assert.equal(compare.segments[0].rightEndLine, 10)
  assert.ok(compare.segments[0].label)
})

test('getLineHighlightState distinguishes active and passive segment ranges', () => {
  const active = { startLine: 4, endLine: 8 }
  const all = [{ startLine: 4, endLine: 8 }, { startLine: 12, endLine: 16 }]

  assert.equal(getLineHighlightState(5, active, all), 'active')
  assert.equal(getLineHighlightState(13, active, all), 'related')
  assert.equal(getLineHighlightState(2, active, all), 'none')
})

test('getLineHighlightMeta exposes active boundaries for segment ranges', () => {
  const active = { startLine: 4, endLine: 8 }
  const all = [{ startLine: 4, endLine: 8 }, { startLine: 12, endLine: 16 }]

  const activeStart = getLineHighlightMeta(4, active, all)
  const relatedEnd = getLineHighlightMeta(16, active, all)
  const none = getLineHighlightMeta(2, active, all)

  assert.equal(activeStart.state, 'active')
  assert.equal(activeStart.isRangeStart, true)
  assert.equal(activeStart.isRangeEnd, false)
  assert.equal(activeStart.isActiveStart, true)
  assert.equal(activeStart.isActiveEnd, false)

  assert.equal(relatedEnd.state, 'related')
  assert.equal(relatedEnd.isRangeStart, false)
  assert.equal(relatedEnd.isRangeEnd, true)
  assert.equal(relatedEnd.isActiveStart, false)
  assert.equal(relatedEnd.isActiveEnd, false)

  assert.equal(none.state, 'none')
  assert.equal(none.isRangeStart, false)
  assert.equal(none.isRangeEnd, false)
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

test('buildSegmentRailMarkers maps segment start lines into an inner rail band', () => {
  const markers = buildSegmentRailMarkers(
    [
      { id: 'seg-1', label: 'segment 1', summary: 'A', leftStartLine: 10, leftEndLine: 20, rightStartLine: 12, rightEndLine: 22 },
      { id: 'seg-2', label: 'segment 2', summary: 'B', leftStartLine: 60, leftEndLine: 72, rightStartLine: 64, rightEndLine: 76 }
    ],
    100
  )

  assert.equal(markers.length, 2)
  assert.equal(markers[0].segment.id, 'seg-1')
  assert.equal(markers[0].topOffset, 24)
  assert.equal(markers[1].topOffset, 58)
})

test('buildSegmentRailMarkers keeps nearby markers separated by a readable minimum gap', () => {
  const markers = buildSegmentRailMarkers(
    [
      { id: 'seg-1', label: 'segment 1', summary: 'A', leftStartLine: 10, leftEndLine: 20 },
      { id: 'seg-2', label: 'segment 2', summary: 'B', leftStartLine: 11, leftEndLine: 21 },
      { id: 'seg-3', label: 'segment 3', summary: 'C', leftStartLine: 12, leftEndLine: 22 }
    ],
    100
  )

  assert.equal(markers.length, 3)
  assert.ok(markers[1].topOffset - markers[0].topOffset >= 7)
  assert.ok(markers[2].topOffset - markers[1].topOffset >= 7)
  assert.ok(markers.every((item) => item.topOffset >= 18 && item.topOffset <= 86))
})

test('formatSegmentBlockType and buildSegmentMeta expose readable deep block labels', () => {
  assert.match(formatSegmentBlockType('FOREACH'), /foreach/i)
  assert.match(formatSegmentBlockType('IF_ELSE'), /if-else/i)
  assert.ok(formatSegmentBlockType(''))

  const meta = buildSegmentMeta({
    blockType: 'FOREACH',
    leftMethodKey: 'getAverage#1(param1)',
    rightMethodKey: 'calcAverage#1(param1)'
  })

  assert.match(meta, /foreach/i)
  assert.match(meta, /getAverage#1/)
  assert.match(meta, /calcAverage#1/)
})

test('buildSegmentLineSummary keeps both side line ranges visible', () => {
  const text = buildSegmentLineSummary({
    leftStartLine: 3,
    leftEndLine: 8,
    rightStartLine: 4,
    rightEndLine: 10
  })

  assert.match(text, /3-8/)
  assert.match(text, /4-10/)
})

test('buildCompareTabs groups matched pairs, cross-file fragments and unmatched files', () => {
  const tabs = buildCompareTabs({
    matchedFilePairs: [
      {
        key: 'pair-1',
        label: 'pair 1',
        relationType: 'MATCHED_PAIR',
        left: { title: 'Utils.java', code: 'class A {}', highlights: [] },
        right: { title: 'Main.java', code: 'class B {}', highlights: [] },
        segments: []
      }
    ],
    crossFileSegments: [
      {
        key: 'cross-1',
        label: 'cross 1',
        relationType: 'CROSS_FILE',
        left: { title: 'A.java', code: 'class A {}', highlights: [] },
        right: { title: 'B.java', code: 'class B {}', highlights: [] },
        segments: []
      }
    ],
    unmatchedLeftFiles: [
      {
        key: 'left-only-1',
        label: 'left only 1',
        pane: { title: 'OnlyLeft.java', code: 'class OnlyLeft {}', highlights: [] }
      }
    ],
    unmatchedRightFiles: [
      {
        key: 'right-only-1',
        label: 'right only 1',
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

test('buildCompareTabFilters summarizes tab counts by compare type', () => {
  const filters = buildCompareTabFilters([
    { key: 'a', type: 'matched' },
    { key: 'b', type: 'cross' },
    { key: 'c', type: 'cross' },
    { key: 'd', type: 'left-only' },
    { key: 'e', type: 'right-only' }
  ])

  assert.deepEqual(filters, [
    { value: 'all', label: '全部', count: 5 },
    { value: 'matched', label: '匹配对', count: 1 },
    { value: 'cross', label: '跨文件', count: 2 },
    { value: 'single', label: '单侧文件', count: 2 }
  ])
})

test('filterCompareTabs narrows compare tabs by selected filter', () => {
  const tabs = [
    { key: 'a', type: 'matched' },
    { key: 'b', type: 'cross' },
    { key: 'c', type: 'left-only' },
    { key: 'd', type: 'right-only' }
  ]

  assert.deepEqual(filterCompareTabs(tabs, 'all').map((item) => item.key), ['a', 'b', 'c', 'd'])
  assert.deepEqual(filterCompareTabs(tabs, 'matched').map((item) => item.key), ['a'])
  assert.deepEqual(filterCompareTabs(tabs, 'cross').map((item) => item.key), ['b'])
  assert.deepEqual(filterCompareTabs(tabs, 'single').map((item) => item.key), ['c', 'd'])
})

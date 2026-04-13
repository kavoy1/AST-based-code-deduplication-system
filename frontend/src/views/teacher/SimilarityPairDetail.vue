<template>
  <section v-if="pairDetail" ref="pageRootRef" class="compare-page">
    <div class="compare-page__shell">
      <header class="compare-topbar">
        <div class="compare-topbar__left">
          <AppBackButton label="返回结果列表" @click="goBack" />
          <div class="compare-topbar__heading">
            <p class="compare-topbar__eyebrow">相似代码对比</p>
            <h1>完整代码对比</h1>
          </div>
        </div>

        <div class="compare-topbar__center">
          <button type="button" class="compare-topbar__summary" @click="goSummary">处理总结</button>
          <div class="compare-score-card">
            <span>相似度</span>
            <strong>{{ pairDetail.score }}%</strong>
          </div>
        </div>

        <div class="compare-topbar__spacer" aria-hidden="true"></div>
      </header>

      <div v-if="compareTabs.length" class="compare-tabs-shell">
        <div class="compare-tabs-filter">
          <button
            v-for="item in compareTabFilters"
            :key="item.value"
            type="button"
            class="compare-tabs-filter__item"
            :class="{ 'is-active': compareTabFilter === item.value }"
            @click="compareTabFilter = item.value"
          >
            <span>{{ item.label }}</span>
            <small>{{ item.count }}</small>
          </button>
        </div>

        <div class="compare-tabs">
        <button
          v-for="tab in filteredCompareTabs"
          :key="tab.key"
          type="button"
          class="compare-tabs__item"
          :class="[
            `is-${tab.type}`,
            { 'is-active': tab.key === activeTabKey }
          ]"
          @click="selectCompareTab(tab)"
        >
          <div class="compare-tabs__top">
            <span class="compare-tabs__type">{{ tabTypeLabel(tab.type) }}</span>
            <span class="compare-tabs__badge">{{ compareTabBadge(tab) }}</span>
          </div>
          <div class="compare-tabs__pair" :title="tab.subtitle">
            <span class="compare-tabs__file">{{ compareTabFileTitle(tab, 'left') }}</span>
            <span class="compare-tabs__arrow">{{ tab.type === 'left-only' || tab.type === 'right-only' ? '·' : '↔' }}</span>
            <span class="compare-tabs__file">{{ compareTabFileTitle(tab, 'right') }}</span>
          </div>
          <small class="compare-tabs__meta">{{ compareTabMeta(tab) }}</small>
        </button>
        </div>
      </div>

      <section v-if="hasCompareData" class="compare-stage">
        <div class="compare-stage__editors">
          <article class="editor-card">
            <header class="editor-card__header">
              <div class="editor-card__file">
                <span class="editor-card__chip">{{ leftPaneTitle }}</span>
                <strong>{{ leftPaneOwnerLabel }}</strong>
              </div>
              <div class="editor-card__lights">
                <span class="is-red"></span>
                <span class="is-yellow"></span>
                <span class="is-green"></span>
              </div>
            </header>

            <div ref="leftPaneBodyRef" class="editor-card__body">
              <template v-if="leftCodeLines.length">
                <div
                  v-for="line in leftCodeLines"
                  :key="`left-${line.number}`"
                  class="editor-line"
                  :class="lineHighlightClass('left', line.number)"
                  :data-line="line.number"
                >
                  <span class="editor-line__number">{{ line.number }}</span>
                  <div class="editor-line__content">
                    <span v-if="lineHighlightBadge('left', line.number)" class="editor-line__badge">
                      {{ lineHighlightBadge('left', line.number) }}
                    </span>
                    <code class="editor-line__code" v-html="line.html"></code>
                  </div>
                </div>
              </template>
              <div v-else class="editor-card__empty">当前这一侧没有可对比的文件内容</div>
            </div>
          </article>

          <article class="editor-card">
            <header class="editor-card__header">
              <div class="editor-card__file">
                <span class="editor-card__chip">{{ rightPaneTitle }}</span>
                <strong>{{ rightPaneOwnerLabel }}</strong>
              </div>
              <div class="editor-card__lights">
                <span class="is-red"></span>
                <span class="is-yellow"></span>
                <span class="is-green"></span>
              </div>
            </header>

            <div ref="rightPaneBodyRef" class="editor-card__body">
              <template v-if="rightCodeLines.length">
                <div
                  v-for="line in rightCodeLines"
                  :key="`right-${line.number}`"
                  class="editor-line"
                  :class="lineHighlightClass('right', line.number)"
                  :data-line="line.number"
                >
                  <span class="editor-line__number">{{ line.number }}</span>
                  <div class="editor-line__content">
                    <span v-if="lineHighlightBadge('right', line.number)" class="editor-line__badge">
                      {{ lineHighlightBadge('right', line.number) }}
                    </span>
                    <code class="editor-line__code" v-html="line.html"></code>
                  </div>
                </div>
              </template>
              <div v-else class="editor-card__empty">当前这一侧没有可对比的文件内容</div>
            </div>
          </article>
        </div>

        <section v-if="segments.length" class="segment-rail" :class="{ 'is-noticeable': showRailHint }">
          <div class="segment-rail__header">
            <div class="segment-rail__badge">定位</div>
            <button v-if="showRailHint" type="button" class="segment-rail__hint" @click="dismissRailHint">
              点击节点快速定位
            </button>
          </div>
          <div class="segment-rail__body">
            <div class="segment-rail__line" :style="segmentRailTrackStyle"></div>
            <el-tooltip
              v-for="item in segmentRailMarkers"
              :key="item.segment.id"
              placement="left"
              effect="light"
            >
              <template #content>
                <div class="segment-rail__tooltip">
                  <strong>{{ item.segment.label }}</strong>
                  <span>{{ item.segment.summary }}</span>
                  <small v-if="buildSegmentMeta(item.segment)">{{ buildSegmentMeta(item.segment) }}</small>
                  <small>{{ buildSegmentLineSummary(item.segment) }}</small>
                </div>
              </template>
              <button
                type="button"
                class="segment-rail__dot"
                :class="{ 'is-active': item.segment.id === activeSegment?.id }"
                :style="{ top: `${item.topOffset}%` }"
                :aria-label="`定位到 ${item.segment.label}`"
                @click="selectSegment(item.segment)"
              >
                <span class="segment-rail__dot-core"></span>
              </button>
            </el-tooltip>
          </div>
        </section>
      </section>

      <el-empty v-else class="compare-empty" description="当前没有可展示的代码对比内容">
        <template #default>
          <p class="compare-empty__text">系统暂时没有拿到完整代码，你可以先返回结果页查看其他相似对。</p>
          <div class="compare-empty__action">
            <AppBackButton label="返回结果列表" @click="goBack" />
          </div>
        </template>
      </el-empty>
    </div>
  </section>

  <section v-else ref="pageRootRef" class="compare-page compare-page--empty">
    <div class="compare-page__shell compare-page__shell--empty">
      <el-empty description="未找到相似对详情">
        <div class="compare-empty__action">
          <AppBackButton label="返回结果列表" @click="goBack" />
        </div>
      </el-empty>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppBackButton from '../../components/AppBackButton.vue'
import {
  fetchTeacherAssignmentDetail,
  fetchTeacherAssignmentSubmissions,
  fetchTeacherPairDetail
} from '../../api/teacherAssignments'
import request from '../../api/request'
import { formatStudentDisplayLabel } from './plagiarismResultsHelpers'
import {
  buildSegmentMeta,
  buildSegmentLineSummary,
  buildCompareTabFilters,
  buildCodeLines,
  buildCompareTabs,
  buildSegmentRailMarkers,
  filterCompareTabs,
  formatSegmentBlockType,
  getLineHighlightMeta,
  getSegmentRange
} from './similarityPairDetailHelpers'

const route = useRoute()
const router = useRouter()

const pairDetail = ref(null)
const studentMap = ref({})
const activeSegmentId = ref('')
const activeTabKey = ref('')
const compareTabFilter = ref('all')
const leftPaneBodyRef = ref(null)
const rightPaneBodyRef = ref(null)
const pageRootRef = ref(null)
const showRailHint = ref(false)
let railHintTimer = null

const compareTabs = computed(() => buildCompareTabs(pairDetail.value || {}))
const compareTabFilters = computed(() => buildCompareTabFilters(compareTabs.value))
const filteredCompareTabs = computed(() => filterCompareTabs(compareTabs.value, compareTabFilter.value))
const currentTab = computed(() => {
  if (!filteredCompareTabs.value.length) return null
  return filteredCompareTabs.value.find((tab) => tab.key === activeTabKey.value) || filteredCompareTabs.value[0]
})
const codeCompare = computed(() => currentTab.value?.compare || null)
const segments = computed(() => codeCompare.value?.segments || [])
const activeSegment = computed(() => {
  if (!segments.value.length) return null
  return segments.value.find((segment) => segment.id === activeSegmentId.value) || segments.value[0]
})

const hasCompareData = computed(
  () => Boolean(codeCompare.value?.left?.code?.length || codeCompare.value?.right?.code?.length)
)

const studentALabel = computed(() => buildStudentLabel(pairDetail.value?.studentA))
const studentBLabel = computed(() => buildStudentLabel(pairDetail.value?.studentB))
const leftPaneOwnerLabel = computed(() => (currentTab.value?.type === 'right-only' ? '无对应文件' : studentALabel.value))
const rightPaneOwnerLabel = computed(() => (currentTab.value?.type === 'left-only' ? '无对应文件' : studentBLabel.value))
const leftPaneTitle = computed(() => codeCompare.value?.left?.title || '学生 A 代码')
const rightPaneTitle = computed(() => codeCompare.value?.right?.title || '学生 B 代码')

const leftHighlightRanges = computed(() =>
  segments.value.length
    ? segments.value.map((segment) => getSegmentRange(segment, 'left')).filter(Boolean)
    : codeCompare.value?.left?.highlights || []
)
const rightHighlightRanges = computed(() =>
  segments.value.length
    ? segments.value.map((segment) => getSegmentRange(segment, 'right')).filter(Boolean)
    : codeCompare.value?.right?.highlights || []
)
const activeLeftRange = computed(() => getSegmentRange(activeSegment.value, 'left'))
const activeRightRange = computed(() => getSegmentRange(activeSegment.value, 'right'))

const leftCodeLines = computed(() => buildCodeLines(codeCompare.value?.left?.code || ''))
const rightCodeLines = computed(() => buildCodeLines(codeCompare.value?.right?.code || ''))
const segmentRailMarkers = computed(() =>
  buildSegmentRailMarkers(segments.value, Math.max(leftCodeLines.value.length, rightCodeLines.value.length, 1))
)
const segmentRailTrackStyle = computed(() => {
  if (!segmentRailMarkers.value.length) {
    return {
      top: '18%',
      bottom: '16%'
    }
  }

  const offsets = segmentRailMarkers.value.map((item) => item.topOffset)
  const minOffset = Math.min(...offsets)
  const maxOffset = Math.max(...offsets)

  return {
    top: `${Math.max(12, minOffset - 8)}%`,
    bottom: `${Math.max(12, 100 - (maxOffset + 8))}%`
  }
})

watch(compareTabs, async (tabs) => {
  if (!tabs.length) {
    compareTabFilter.value = 'all'
    activeTabKey.value = ''
    activeSegmentId.value = ''
    return
  }
  if (!compareTabFilters.value.some((item) => item.value === compareTabFilter.value)) {
    compareTabFilter.value = 'all'
  }
  if (!tabs.some((tab) => tab.key === activeTabKey.value)) {
    activeTabKey.value = tabs[0].key
  }
  await syncActiveSegment('auto')
})

watch(filteredCompareTabs, async (tabs) => {
  if (!tabs.length) {
    activeTabKey.value = ''
    activeSegmentId.value = ''
    return
  }

  if (!tabs.some((tab) => tab.key === activeTabKey.value)) {
    activeTabKey.value = tabs[0].key
  }
})

watch(activeTabKey, async () => {
  await syncActiveSegment('auto')
})

onMounted(loadPage)
onBeforeUnmount(clearRailHintTimer)

async function loadPage() {
  resetViewportPosition()
  const detail = await fetchTeacherPairDetail(route.params.pairId)
  pairDetail.value = detail

  await nextTick()
  resetViewportPosition()

  if (compareTabs.value.length) {
    activeTabKey.value = compareTabs.value[0].key
    await syncActiveSegment('auto')
  }

  const assignmentId = route.query.assignmentId
  if (!assignmentId) return

  try {
    const [assignmentDetail, submissions] = await Promise.all([
      fetchTeacherAssignmentDetail(assignmentId).catch(() => null),
      fetchTeacherAssignmentSubmissions(assignmentId).catch(() => [])
    ])
    studentMap.value = await loadStudentMap(assignmentDetail, submissions)
  } catch {
    studentMap.value = {}
  }
}

async function syncActiveSegment(behavior = 'auto') {
  dismissRailHint()
  await nextTick()
  resetPaneScroll()
  if (!segments.value.length) {
    activeSegmentId.value = ''
    return
  }
  activeSegmentId.value = segments.value[0].id
  showRailHintOnce()
  await nextTick()
  scrollToSegment(segments.value[0], behavior)
}

function resetPaneScroll() {
  ;[leftPaneBodyRef.value, rightPaneBodyRef.value].forEach((container) => {
    if (!container) return
    container.scrollTop = 0
    container.scrollLeft = 0
  })
}

function showRailHintOnce() {
  clearRailHintTimer()
  showRailHint.value = segments.value.length > 0
  if (!showRailHint.value) return
  railHintTimer = setTimeout(() => {
    showRailHint.value = false
    railHintTimer = null
  }, 3600)
}

function dismissRailHint() {
  showRailHint.value = false
  clearRailHintTimer()
}

function clearRailHintTimer() {
  if (railHintTimer) {
    clearTimeout(railHintTimer)
    railHintTimer = null
  }
}

function resetViewportPosition() {
  if (typeof window !== 'undefined') {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  }

  const containers = [
    document.scrollingElement,
    document.documentElement,
    document.body,
    document.querySelector('.teacher-stage-main'),
    document.querySelector('.workspace-stage-shell'),
    pageRootRef.value
  ].filter(Boolean)

  containers.forEach((container) => {
    if (typeof container.scrollTo === 'function') {
      container.scrollTo({ top: 0, left: 0, behavior: 'auto' })
    } else {
      container.scrollTop = 0
      container.scrollLeft = 0
    }
  })
}

function buildStudentLabel(studentId) {
  return formatStudentDisplayLabel(studentId, studentMap.value)
}

function mapStudentProfile(student) {
  return {
    studentId: Number(student?.id || student?.user_id || student?.userId || student?.studentId || 0),
    name: student?.nickname || student?.username || student?.studentName || '',
    number: String(student?.student_number || student?.studentNumber || '')
  }
}

async function loadStudentMap(assignmentDetail, submissions = []) {
  const classIds = Array.isArray(assignmentDetail?.classIds) ? assignmentDetail.classIds : []
  const classGroups = await Promise.all(
    classIds.map(async (classId) => {
      try {
        const rows = await request.get(`/teacher/classes/${classId}/students`)
        return Array.isArray(rows) ? rows : []
      } catch {
        return []
      }
    })
  )

  const profiles = {}

  classGroups.flat().forEach((student) => {
    const profile = mapStudentProfile(student)
    if (!profile.studentId) return
    profiles[profile.studentId] = {
      name: profile.name,
      number: profile.number
    }
  })

  submissions.forEach((student) => {
    const profile = mapStudentProfile(student)
    if (!profile.studentId) return
    profiles[profile.studentId] = {
      name: profiles[profile.studentId]?.name || profile.name,
      number: profiles[profile.studentId]?.number || profile.number
    }
  })

  return profiles
}

function tabTypeLabel(type) {
  if (type === 'matched') return '匹配对'
  if (type === 'cross') return '跨文件'
  if (type === 'left-only') return '仅A存在'
  if (type === 'right-only') return '仅B存在'
  return '主对比'
}

function compareTabFileTitle(tab, side) {
  const title = side === 'left' ? tab?.compare?.left?.title : tab?.compare?.right?.title

  if (title) return title
  if (side === 'left') return '左侧无文件'
  return '右侧无文件'
}

function compareTabBadge(tab) {
  if (tab?.type === 'left-only') return '左侧'
  if (tab?.type === 'right-only') return '右侧'

  const count = Array.isArray(tab?.compare?.segments) ? tab.compare.segments.length : 0
  return count ? `${count} 块` : '对比'
}

function compareTabMeta(tab) {
  if (tab?.type === 'left-only') return '仅左侧存在该文件，右侧没有对应代码。'
  if (tab?.type === 'right-only') return '仅右侧存在该文件，左侧没有对应代码。'

  const count = Array.isArray(tab?.compare?.segments) ? tab.compare.segments.length : 0
  return count ? `${count} 个结构块，点击查看完整代码对比。` : '点击查看当前文件对的完整代码对比。'
}

function lineHighlightClass(side, lineNumber) {
  const meta = getLineHighlightMeta(
    lineNumber,
    side === 'left' ? activeLeftRange.value : activeRightRange.value,
    side === 'left' ? leftHighlightRanges.value : rightHighlightRanges.value
  )
  return {
    'is-highlight-active': meta.state === 'active',
    'is-highlight-related': meta.state === 'related',
    'is-highlight-start': meta.isRangeStart,
    'is-highlight-end': meta.isRangeEnd,
    'is-highlight-active-start': meta.isActiveStart,
    'is-highlight-active-end': meta.isActiveEnd
  }
}

function lineHighlightBadge(side, lineNumber) {
  if (!activeSegment.value) return ''
  const meta = getLineHighlightMeta(
    lineNumber,
    side === 'left' ? activeLeftRange.value : activeRightRange.value,
    side === 'left' ? leftHighlightRanges.value : rightHighlightRanges.value
  )
  if (!meta.isActiveStart) return ''

  const items = [formatSegmentBlockType(activeSegment.value.blockType)]
  if (activeSegment.value.score) items.push(`${activeSegment.value.score}%`)
  return items.join(' · ')
}

function selectCompareTab(tab) {
  if (!tab || tab.key === activeTabKey.value) return
  activeTabKey.value = tab.key
}

function selectSegment(segment) {
  if (!segment) return
  dismissRailHint()
  activeSegmentId.value = segment.id
  nextTick(() => scrollToSegment(segment, 'smooth'))
}

function scrollToSegment(segment, behavior = 'smooth') {
  if (!segment) return
  scrollPaneToLine(leftPaneBodyRef.value, segment.leftStartLine, behavior)
  scrollPaneToLine(rightPaneBodyRef.value, segment.rightStartLine, behavior)
}

function scrollPaneToLine(container, lineNumber, behavior) {
  if (!container || !lineNumber) return
  const line = container.querySelector(`[data-line="${lineNumber}"]`)
  if (!line) return
  line.scrollIntoView({ block: 'center', behavior })
}

function goBack() {
  const assignmentId = route.query.assignmentId
  router.push(
    assignmentId
      ? `/teacher/assignments/${assignmentId}/plagiarism/results`
      : '/teacher/assignments/plagiarism/results'
  )
}

function goSummary() {
  const assignmentId = route.query.assignmentId
  router.push({
    path: `/teacher/similarity-pairs/${route.params.pairId}/summary`,
    query: assignmentId ? { assignmentId } : {}
  })
}
</script>

<style scoped>
.compare-page {
  height: calc(100vh - 32px);
  min-height: calc(100vh - 32px);
  max-height: calc(100vh - 32px);
  min-width: 0;
  padding: 14px 18px 18px;
  border-radius: 32px;
  background:
    radial-gradient(circle at top right, rgba(140, 165, 255, 0.16), transparent 28%),
    linear-gradient(180deg, rgba(249, 251, 255, 0.98), rgba(241, 245, 251, 0.96));
  border: 1px solid rgba(214, 222, 236, 0.8);
  overflow: hidden;
  box-sizing: border-box;
}

.compare-page--empty {
  display: grid;
  place-items: center;
}

.compare-page__shell {
  height: 100%;
  min-height: 0;
  min-width: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 14px;
  padding: 12px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(216, 224, 237, 0.88);
  box-shadow: 0 22px 44px rgba(184, 196, 214, 0.14);
  overflow: hidden;
}

.compare-page__shell--empty {
  place-items: center;
}

.compare-topbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.compare-topbar__left,
.compare-topbar__center {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.compare-topbar__left {
  justify-content: flex-start;
}

.compare-topbar__center {
  justify-content: center;
}

.compare-topbar__spacer {
  min-width: 0;
}

.compare-topbar__heading {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.compare-topbar__eyebrow {
  margin: 0;
  color: #8b96aa;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.compare-topbar__heading h1 {
  margin: 0;
  color: #121826;
  font-size: 28px;
  line-height: 1.1;
}

.compare-topbar__summary {
  height: 42px;
  padding: 0 16px;
  border: 1px solid rgba(208, 217, 231, 0.96);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.85);
  color: #31405b;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.compare-topbar__summary:hover {
  transform: translateY(-1px);
  border-color: rgba(124, 143, 255, 0.5);
  box-shadow: 0 12px 24px rgba(168, 182, 214, 0.18);
}

.compare-score-card {
  display: grid;
  gap: 4px;
  min-width: 96px;
  padding: 12px 18px;
  border-radius: 22px;
  background: linear-gradient(180deg, #1d2740, #273554);
  color: #fff;
  text-align: center;
}

.compare-score-card span {
  font-size: 12px;
  color: rgba(220, 229, 255, 0.78);
}

.compare-score-card strong {
  font-size: 22px;
  line-height: 1;
}

.compare-tabs-shell {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.compare-tabs-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.compare-tabs-filter__item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(216, 224, 239, 0.92);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: #5f6f8c;
  cursor: pointer;
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease, color 160ms ease, background 160ms ease;
}

.compare-tabs-filter__item:hover {
  transform: translateY(-1px);
  border-color: rgba(124, 143, 255, 0.44);
  box-shadow: 0 10px 18px rgba(168, 182, 214, 0.14);
}

.compare-tabs-filter__item.is-active {
  border-color: rgba(86, 104, 232, 0.34);
  background: linear-gradient(180deg, rgba(243, 246, 255, 0.98), rgba(236, 241, 255, 0.94));
  color: #22314f;
}

.compare-tabs-filter__item span {
  font-size: 13px;
  font-weight: 700;
}

.compare-tabs-filter__item small {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(226, 233, 250, 0.92);
  color: #5f6f8c;
  font-size: 11px;
  font-weight: 800;
}

.compare-tabs-filter__item.is-active small {
  background: rgba(86, 104, 232, 0.14);
  color: #3b4fc0;
}

.compare-tabs {
  display: flex;
  gap: 12px;
  min-width: 0;
  overflow-x: auto;
  padding: 2px 2px 8px;
  scroll-snap-type: x proximity;
}

.compare-tabs__item {
  display: grid;
  grid-template-rows: auto auto auto;
  gap: 10px;
  flex: 0 0 clamp(218px, 18vw, 248px);
  min-width: 218px;
  padding: 14px 16px;
  border: 1px solid rgba(220, 227, 240, 0.92);
  border-radius: 18px;
  background: rgba(248, 250, 255, 0.88);
  color: #46546d;
  text-align: left;
  cursor: pointer;
  scroll-snap-align: start;
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease, background 160ms ease;
}

.compare-tabs__item:hover {
  transform: translateY(-1px);
  border-color: rgba(120, 139, 255, 0.42);
  box-shadow: 0 14px 24px rgba(167, 180, 212, 0.18);
}

.compare-tabs__item.is-active {
  border-color: rgba(86, 104, 232, 0.38);
  background: linear-gradient(180deg, rgba(243, 246, 255, 0.98), rgba(236, 241, 255, 0.94));
  box-shadow: 0 14px 28px rgba(167, 180, 212, 0.24);
}

.compare-tabs__item.is-matched.is-active { box-shadow: 0 14px 28px rgba(113, 145, 255, 0.22); }
.compare-tabs__item.is-cross.is-active { box-shadow: 0 14px 28px rgba(255, 170, 74, 0.22); }
.compare-tabs__item.is-left-only.is-active,
.compare-tabs__item.is-right-only.is-active { box-shadow: 0 14px 28px rgba(122, 143, 174, 0.18); }

.compare-tabs__type {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #7a86a0;
}

.compare-tabs__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.compare-tabs__badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(231, 236, 252, 0.92);
  color: #5c6f97;
  font-size: 11px;
  font-weight: 700;
}

.compare-tabs__pair {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: start;
  gap: 8px;
  min-height: 48px;
}

.compare-tabs__file {
  display: -webkit-box;
  overflow: hidden;
  color: #162033;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow-wrap: anywhere;
}

.compare-tabs__arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding-top: 2px;
  color: #7c8aa7;
  font-size: 14px;
  font-weight: 700;
}

.compare-tabs__meta {
  display: -webkit-box;
  overflow: hidden;
  color: #6f7c92;
  font-size: 12px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.compare-stage {
  min-height: 0;
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 88px;
  gap: 14px;
}

.compare-stage__editors {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.editor-card {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  border-radius: 26px;
  overflow: hidden;
  background: linear-gradient(180deg, #11192b, #172136);
  border: 1px solid rgba(41, 54, 84, 0.96);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.editor-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(43, 57, 89, 0.92);
  background: rgba(25, 34, 55, 0.96);
}

.editor-card__file {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.editor-card__chip {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 8px 12px;
  border-radius: 12px;
  background: rgba(73, 97, 183, 0.38);
  color: #dce4ff;
  font-size: 13px;
  font-weight: 700;
}

.editor-card__file strong {
  color: #f4f7ff;
  font-size: 15px;
  line-height: 1.2;
}

.editor-card__lights {
  display: flex;
  align-items: center;
  gap: 8px;
}

.editor-card__lights span {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.editor-card__lights .is-red { background: #ff5f57; }
.editor-card__lights .is-yellow { background: #febc2e; }
.editor-card__lights .is-green { background: #28c840; }

.editor-card__body {
  min-width: 0;
  min-height: 0;
  overflow: auto;
  padding: 12px 0;
  background: linear-gradient(180deg, rgba(13, 19, 33, 0.98), rgba(17, 24, 40, 0.98));
}

.editor-card__empty {
  display: grid;
  place-items: center;
  min-height: 100%;
  padding: 24px;
  color: rgba(202, 214, 244, 0.62);
  font-size: 14px;
  text-align: center;
}

.editor-line {
  position: relative;
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  align-items: start;
  gap: 18px;
  padding: 0 18px;
  min-height: 32px;
  transition: background 160ms ease, box-shadow 160ms ease;
}

.editor-line__number {
  color: #5b6780;
  text-align: right;
  font-size: 13px;
  line-height: 32px;
  user-select: none;
}

.editor-line__content {
  min-width: 0;
}

.editor-line__badge {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  margin: 4px 0 4px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(255, 214, 224, 0.18);
  border: 1px solid rgba(255, 143, 170, 0.26);
  color: #ffd8e1;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.editor-line__code {
  display: block;
  color: #dbe6ff;
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  line-height: 32px;
  white-space: pre-wrap;
  word-break: break-word;
}

.editor-line.is-highlight-related {
  background: rgba(157, 88, 108, 0.16);
  box-shadow: inset 3px 0 0 rgba(224, 113, 141, 0.5);
}

.editor-line.is-highlight-active {
  background: rgba(190, 68, 96, 0.34);
  box-shadow: inset 4px 0 0 rgba(255, 149, 176, 0.88);
}

.editor-line.is-highlight-start {
  box-shadow:
    inset 3px 0 0 rgba(224, 113, 141, 0.5),
    inset 0 1px 0 rgba(255, 193, 207, 0.34);
}

.editor-line.is-highlight-end {
  box-shadow:
    inset 3px 0 0 rgba(224, 113, 141, 0.5),
    inset 0 -1px 0 rgba(255, 193, 207, 0.34);
}

.editor-line.is-highlight-active.is-highlight-start {
  box-shadow:
    inset 4px 0 0 rgba(255, 149, 176, 0.88),
    inset 0 1px 0 rgba(255, 214, 224, 0.72);
}

.editor-line.is-highlight-active.is-highlight-end {
  box-shadow:
    inset 4px 0 0 rgba(255, 149, 176, 0.88),
    inset 0 -1px 0 rgba(255, 214, 224, 0.72);
}

.editor-line.is-highlight-active-start::after,
.editor-line.is-highlight-active-end::after {
  content: '';
  position: absolute;
  left: 18px;
  right: 18px;
  height: 1px;
  background: rgba(255, 214, 224, 0.72);
}

.editor-line.is-highlight-active-start::after {
  top: 0;
}

.editor-line.is-highlight-active-end::after {
  bottom: 0;
}

.editor-line :deep(.token.keyword) { color: #c6a8ff; }
.editor-line :deep(.token.string) { color: #a5e28f; }
.editor-line :deep(.token.comment) { color: #7d8bab; }

.segment-rail {
  position: relative;
  min-width: 72px;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 14px;
  padding: 16px 0 18px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(210, 220, 244, 0.92);
  box-shadow: 0 18px 30px rgba(179, 189, 207, 0.16);
}

.segment-rail.is-noticeable {
  box-shadow:
    0 0 0 1px rgba(122, 143, 255, 0.28),
    0 0 26px rgba(122, 143, 255, 0.18),
    0 16px 28px rgba(179, 189, 207, 0.18);
}

.segment-rail__header {
  display: grid;
  justify-items: center;
  gap: 10px;
}

.segment-rail__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 44px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(112, 132, 255, 0.1);
  color: #5a6fb8;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.segment-rail__hint {
  max-width: 132px;
  padding: 8px 12px;
  border: none;
  border-radius: 12px;
  background: rgba(18, 24, 38, 0.92);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(18, 24, 38, 0.16);
}

.segment-rail__body {
  position: relative;
  min-height: 0;
}

.segment-rail__line {
  position: absolute;
  top: 18%;
  bottom: 16%;
  left: 50%;
  width: 2px;
  transform: translateX(-50%);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(195, 205, 234, 0.16), rgba(126, 145, 255, 0.4), rgba(195, 205, 234, 0.16));
}

.segment-rail__dot {
  position: absolute;
  left: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 999px;
  background: rgba(122, 143, 255, 0.14);
  transform: translate(-50%, -50%);
  cursor: pointer;
  transition: transform 160ms ease, box-shadow 160ms ease, background 160ms ease;
}

.segment-rail__dot-core {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: rgba(122, 143, 255, 0.7);
  transition: transform 160ms ease, background 160ms ease;
}

.segment-rail__dot:hover {
  transform: translate(-50%, -50%) scale(1.12);
  box-shadow: 0 10px 20px rgba(58, 89, 255, 0.16);
}

.segment-rail__dot:hover .segment-rail__dot-core {
  background: rgba(58, 89, 255, 0.96);
}

.segment-rail__dot.is-active {
  box-shadow:
    0 0 0 1px rgba(91, 113, 255, 0.08),
    0 12px 22px rgba(91, 113, 255, 0.18);
}

.segment-rail__dot.is-active .segment-rail__dot-core {
  width: 16px;
  height: 16px;
  background: #121826;
  box-shadow: 0 0 0 5px rgba(91, 113, 255, 0.12);
}

.segment-rail__tooltip {
  display: grid;
  gap: 4px;
  max-width: 220px;
}

.segment-rail__tooltip strong {
  color: #121826;
  font-size: 13px;
}

.segment-rail__tooltip span,
.segment-rail__tooltip small {
  color: #5f708c;
  line-height: 1.5;
}

.compare-empty {
  display: grid;
  place-items: center;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px dashed rgba(206, 216, 232, 0.92);
}

.compare-empty__text {
  margin: 0;
  color: #6f7c92;
  line-height: 1.7;
}

.compare-empty__action {
  margin-top: 18px;
}

@media (max-width: 1280px) {
  .compare-stage {
    grid-template-columns: 1fr;
  }

  .compare-stage__editors {
    grid-template-columns: 1fr;
  }

  .segment-rail {
    display: none;
  }
}

@media (max-width: 960px) {
  .compare-page {
    height: auto;
    min-height: calc(100vh - 32px);
    max-height: none;
  }

  .compare-page__shell {
    grid-template-rows: auto auto auto;
  }

  .compare-topbar {
    grid-template-columns: 1fr;
  }

  .compare-topbar,
  .compare-topbar__left,
  .compare-topbar__center {
    flex-direction: column;
    align-items: stretch;
  }

  .compare-topbar__center {
    align-items: flex-start;
  }

  .compare-topbar__spacer {
    display: none;
  }

  .editor-card {
    min-height: 420px;
  }
}
</style>

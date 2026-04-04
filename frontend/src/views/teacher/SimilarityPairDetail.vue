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

      <div v-if="compareTabs.length" class="compare-tabs">
        <button
          v-for="tab in compareTabs"
          :key="tab.key"
          type="button"
          class="compare-tabs__item"
          :class="[
            `is-${tab.type}`,
            { 'is-active': tab.key === activeTabKey }
          ]"
          @click="selectCompareTab(tab)"
        >
          <span class="compare-tabs__type">{{ tabTypeLabel(tab.type) }}</span>
          <strong>{{ tab.title }}</strong>
          <small>{{ tab.subtitle }}</small>
        </button>
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
                  <code class="editor-line__code" v-html="line.html"></code>
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
                  <code class="editor-line__code" v-html="line.html"></code>
                </div>
              </template>
              <div v-else class="editor-card__empty">当前这一侧没有可对比的文件内容</div>
            </div>
          </article>
        </div>

        <aside v-if="segments.length" class="segment-rail" :class="{ 'is-noticeable': showRailHint }">
          <div class="segment-rail__badge">片段定位</div>
          <button v-if="showRailHint" type="button" class="segment-rail__hint" @click="dismissRailHint">
            点击圆点快速定位
          </button>
          <div class="segment-rail__line"></div>
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
                <small>
                  左 {{ item.segment.leftStartLine }}-{{ item.segment.leftEndLine }} /
                  右 {{ item.segment.rightStartLine }}-{{ item.segment.rightEndLine }}
                </small>
              </div>
            </template>
            <button
              type="button"
              class="segment-rail__dot"
              :class="{ 'is-active': item.segment.id === activeSegment?.id }"
              :style="{ top: `${item.topOffset}%` }"
              :aria-label="`定位到 ${item.segment.label}`"
              @click="selectSegment(item.segment)"
            ></button>
          </el-tooltip>
        </aside>
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
  fetchTeacherAssignmentSubmissions,
  fetchTeacherPairDetail
} from '../../api/teacherAssignments'
import {
  buildCodeLines,
  buildCompareTabs,
  buildSegmentRailMarkers,
  getLineHighlightState,
  getSegmentRange
} from './similarityPairDetailHelpers'

const route = useRoute()
const router = useRouter()

const pairDetail = ref(null)
const studentMap = ref({})
const activeSegmentId = ref('')
const activeTabKey = ref('')
const leftPaneBodyRef = ref(null)
const rightPaneBodyRef = ref(null)
const pageRootRef = ref(null)
const showRailHint = ref(false)
let railHintTimer = null

const compareTabs = computed(() => buildCompareTabs(pairDetail.value || {}))
const currentTab = computed(() => {
  if (!compareTabs.value.length) return null
  return compareTabs.value.find((tab) => tab.key === activeTabKey.value) || compareTabs.value[0]
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

watch(compareTabs, async (tabs) => {
  if (!tabs.length) {
    activeTabKey.value = ''
    activeSegmentId.value = ''
    return
  }
  if (!tabs.some((tab) => tab.key === activeTabKey.value)) {
    activeTabKey.value = tabs[0].key
  }
  await syncActiveSegment('auto')
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
    const submissions = await fetchTeacherAssignmentSubmissions(assignmentId)
    studentMap.value = submissions.reduce((result, item) => {
      result[item.studentId] = {
        name: item.studentName || '',
        number: item.studentNumber || ''
      }
      return result
    }, {})
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
  const profile = studentMap.value?.[studentId]
  if (profile?.name && profile?.number) return `${profile.name} (${profile.number})`
  if (profile?.name) return profile.name
  if (profile?.number) return `学号 ${profile.number}`
  return `学生 ${studentId || '-'}`
}

function tabTypeLabel(type) {
  if (type === 'matched') return '匹配对'
  if (type === 'cross') return '跨文件'
  if (type === 'left-only') return '仅A存在'
  if (type === 'right-only') return '仅B存在'
  return '主对比'
}

function lineHighlightClass(side, lineNumber) {
  const state = getLineHighlightState(
    lineNumber,
    side === 'left' ? activeLeftRange.value : activeRightRange.value,
    side === 'left' ? leftHighlightRanges.value : rightHighlightRanges.value
  )
  return {
    'is-highlight-active': state === 'active',
    'is-highlight-related': state === 'related'
  }
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

.compare-tabs {
  display: flex;
  gap: 10px;
  min-width: 0;
  overflow-x: auto;
  padding-bottom: 4px;
}

.compare-tabs__item {
  display: grid;
  gap: 2px;
  min-width: 180px;
  padding: 12px 14px;
  border: 1px solid rgba(220, 227, 240, 0.92);
  border-radius: 18px;
  background: rgba(248, 250, 255, 0.88);
  color: #46546d;
  text-align: left;
  cursor: pointer;
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

.compare-tabs__item strong {
  color: #162033;
  font-size: 14px;
}

.compare-tabs__item small {
  color: #6f7c92;
  font-size: 12px;
  line-height: 1.4;
}

.compare-stage {
  min-height: 0;
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 72px;
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
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  align-items: start;
  gap: 18px;
  padding: 0 18px;
  min-height: 32px;
}

.editor-line__number {
  color: #5b6780;
  text-align: right;
  font-size: 13px;
  line-height: 32px;
  user-select: none;
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
  background: rgba(157, 88, 108, 0.18);
}

.editor-line.is-highlight-active {
  background: rgba(190, 68, 96, 0.36);
}

.editor-line :deep(.token.keyword) { color: #c6a8ff; }
.editor-line :deep(.token.string) { color: #a5e28f; }
.editor-line :deep(.token.comment) { color: #7d8bab; }

.segment-rail {
  position: relative;
  min-width: 64px;
  min-height: 0;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(219, 226, 238, 0.9);
  box-shadow: 0 16px 28px rgba(179, 189, 207, 0.18);
}

.segment-rail.is-noticeable {
  box-shadow:
    0 0 0 1px rgba(122, 143, 255, 0.28),
    0 0 26px rgba(122, 143, 255, 0.18),
    0 16px 28px rgba(179, 189, 207, 0.18);
}

.segment-rail__badge {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(245, 247, 252, 0.96);
  color: #667389;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.segment-rail__hint {
  position: absolute;
  top: 42px;
  right: calc(100% + 10px);
  padding: 8px 12px;
  border: none;
  border-radius: 12px;
  background: rgba(18, 24, 38, 0.92);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}

.segment-rail__hint::after {
  content: '';
  position: absolute;
  top: 50%;
  right: -6px;
  width: 12px;
  height: 12px;
  background: rgba(18, 24, 38, 0.92);
  transform: translateY(-50%) rotate(45deg);
}

.segment-rail__line {
  position: absolute;
  top: 58px;
  bottom: 18px;
  left: 50%;
  width: 2px;
  transform: translateX(-50%);
  background: linear-gradient(180deg, rgba(192, 203, 225, 0.2), rgba(122, 143, 255, 0.4), rgba(192, 203, 225, 0.2));
}

.segment-rail__dot {
  position: absolute;
  left: 50%;
  width: 16px;
  height: 16px;
  border: none;
  border-radius: 999px;
  background: rgba(122, 143, 255, 0.55);
  transform: translate(-50%, -50%);
  cursor: pointer;
  transition: transform 160ms ease, box-shadow 160ms ease, background 160ms ease;
}

.segment-rail__dot:hover {
  transform: translate(-50%, -50%) scale(1.12);
  background: rgba(58, 89, 255, 0.96);
  box-shadow: 0 10px 20px rgba(58, 89, 255, 0.24);
}

.segment-rail__dot.is-active {
  background: #121826;
  box-shadow:
    0 0 0 6px rgba(91, 113, 255, 0.12),
    0 12px 22px rgba(91, 113, 255, 0.26);
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

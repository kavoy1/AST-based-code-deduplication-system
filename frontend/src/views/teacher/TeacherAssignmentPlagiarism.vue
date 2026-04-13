<template>
  <section class="results-page">
    <template v-if="!selectedAssignmentId">
      <div class="results-page__shell results-page__shell--directory">
        <div class="results-page__toolbar results-page__toolbar--minimal">
          <div class="results-page__directory-tools">
            <el-input
              v-model="assignmentKeyword"
              class="results-page__search"
              clearable
              placeholder="按作业名称搜索"
              @clear="handleAssignmentSearch"
              @keyup.enter="handleAssignmentSearch"
            />
            <el-button @click="handleAssignmentSearch">搜索</el-button>
          </div>

          <div class="results-page__directory-meta">
            <span v-if="showAssignmentPagination" class="results-page__page-meta">
              第 {{ assignmentPage }} / {{ assignmentPageCount }} 页
            </span>
            <span class="results-page__count">{{ assignmentDirectoryCountLabel }}</span>
          </div>
        </div>

        <div v-if="loading" class="results-page__loading">
          <LoadingSpinner label="正在加载作业列表…" />
        </div>

        <div v-else-if="assignmentOptions.length" class="results-page__grid results-page__grid--directory">
          <button
            v-for="item in assignmentOptions"
            :key="item.id"
            type="button"
            class="assignment-card assignment-card--compact"
            @click="openAssignment(item.id)"
            >
              <div class="assignment-card__top">
                <span class="assignment-card__lang">{{ item.language || '作业' }}</span>
                <span class="assignment-card__status" :class="`is-${item.status}`">{{ item.statusLabel }}</span>
              </div>

              <div class="assignment-card__body">
                <strong class="assignment-card__title">{{ item.title }}</strong>
                <p class="assignment-card__meta">{{ item.classNamesText || '未关联班级' }}</p>
              </div>

              <div class="assignment-card__stats assignment-card__stats--compact">
                <div>
                  <span>班级数</span>
                  <strong>{{ item.classCount || item.classIds?.length || 0 }}</strong>
                </div>
                <div>
                  <span>截止时间</span>
                  <strong>{{ item.endTime || '未设置' }}</strong>
                </div>
                <div>
                  <span>已交人数</span>
                  <strong>{{ item.submittedCount }}/{{ item.studentCount || 0 }}</strong>
                </div>
              </div>

              <div class="assignment-card__footer">
                <span class="assignment-card__footer-label">查看结果</span>
                <span class="assignment-card__footer-arrow">→</span>
              </div>
            </button>
          </div>

        <el-empty
          v-else
          class="results-page__empty"
          :description="assignmentKeyword.trim() ? '没有匹配到对应作业' : '当前还没有已结束的作业可查看'"
        />

        <footer v-if="showAssignmentPagination" class="results-page__footer">
          <el-pagination
            :current-page="assignmentPage"
            background
            layout="total, sizes, prev, pager, next"
            :page-size="assignmentPageSize"
            :page-sizes="[10, 20, 50]"
            :total="assignmentTotal"
            @current-change="handleAssignmentPageChange"
            @size-change="handleAssignmentSizeChange"
          />
        </footer>
      </div>
    </template>

    <template v-else>
      <div class="results-page__shell results-page__shell--pairs">
        <div class="results-page__toolbar results-page__toolbar--pairs">
          <div class="results-page__toolbar-lead">
            <AppBackButton label="返回作业列表" @click="backToAssignments" />
            <div class="results-page__toolbar-copy">
              <span class="results-page__eyebrow">{{ modeLabel }}查重结果</span>
              <strong>{{ assignment?.title || selectedSummary?.title || '当前作业' }}</strong>
            </div>
          </div>
          <div class="results-page__toolbar-actions">
            <label class="switch results-page__mode-switch" aria-label="切换查重模式">
              <input
                type="checkbox"
                :checked="plagiarismMode === 'DEEP'"
                @change="handleModeToggle($event.target.checked ? 'DEEP' : 'FAST')"
              >
              <span>快速</span>
              <span>精细</span>
            </label>
            <el-button v-if="showResultsLaunchAction" type="primary" round @click="goToLaunchPage">发起查重</el-button>
          </div>
        </div>

        <div v-if="loading" class="results-page__loading">
          <LoadingSpinner label="正在加载查重结果…" />
        </div>

        <template v-else-if="!activeJobId">
          <el-empty :description="`${modeLabel}查重当前还没有结果`">
            <el-button v-if="showResultsLaunchAction" type="primary" round @click="goToLaunchPage">去发起查重</el-button>
          </el-empty>
        </template>

        <template v-else-if="pairs.length">
          <div class="results-page__pairs-body">
            <div class="pair-filter-bar">
              <button
                type="button"
                class="pair-filter-chip"
                :class="{ 'is-active': riskView === 'all' }"
                @click="riskView = 'all'"
              >
                全部结果
                <strong>{{ pairs.length }}</strong>
              </button>
              <button
                type="button"
                class="pair-filter-chip"
                :class="{ 'is-active': riskView === 'high-risk' }"
                @click="riskView = 'high-risk'"
              >
                高风险
                <strong>{{ highRiskPairCount }}</strong>
              </button>
              <span class="pair-filter-hint">
                当前以 {{ activeThresholdScore }} 分标记高风险，全部结果完整保留。
              </span>
            </div>

            <div class="pair-list">
              <button
                v-for="(pair, index) in pagedPairs"
                :key="pair.pairId"
                type="button"
                class="pair-card"
                :class="`is-${pairTone(index)}`"
                @click="goToPairDetail(pair)"
              >
                <div class="pair-card__rank-box" :class="`is-${pairTone(index)}`">
                  <span class="pair-card__rank-number">{{ pairRank(index) }}</span>
                </div>

                <div class="pair-card__main">
                  <div class="pair-card__headline">
                    <span class="pair-card__order" :class="`is-${pairTone(index)}`">
                      {{ pairRank(index) <= 3 ? `TOP ${pairRank(index)}` : `#${pairRank(index)}` }}
                    </span>
                    <strong class="pair-card__title">{{ pairStudentTitle(pair) }}</strong>
                  </div>

                  <p class="pair-card__meta">
                    {{ pairStatusLabel(pair.status) }} · 点击后进入相似结果详情页查看完整代码对比
                  </p>
                </div>

                <div class="pair-card__aside">
                  <strong class="pair-card__score">{{ pair.score }}%</strong>
                  <span class="pair-card__action">进入详情</span>
                </div>
              </button>
            </div>
          </div>

          <footer class="results-page__footer results-page__footer--pairs">
            <span class="results-page__count">{{ modeLabel }}模式下共 {{ visiblePairs.length }} 条结果</span>
            <div class="results-page__footer-actions">
              <el-pagination
                v-if="visiblePairs.length > pairPageSize"
                v-model:current-page="currentPage"
                background
                layout="prev, pager, next"
                :page-size="pairPageSize"
                :total="visiblePairs.length"
              />
            </div>
          </footer>
        </template>

        <template v-else>
          <el-empty description="当前作业还没有可展示的相似结果" />
        </template>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchTeacherAssignments,
  fetchTeacherAssignmentDetail,
  fetchTeacherAssignmentPlagiarism
} from '../../api/teacherAssignments'
import request from '../../api/request'
import {
  buildPairStudentTitle,
  countHighRiskPairs,
  filterPairsByRiskView,
  getPairRank,
  getPairRankTone,
  shouldShowResultsLaunchAction
} from './plagiarismResultsHelpers'
import {
  getAssignmentDirectoryCountLabel,
  shouldShowAssignmentDirectoryPagination
} from './teacherAssignmentPlagiarismHelpers'
import AppBackButton from '../../components/AppBackButton.vue'
import LoadingSpinner from '../../components/LoadingSpinner.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const assignment = ref(null)
const assignmentOptions = ref([])
const selectedAssignmentId = ref(String(route.params.assignmentId || ''))
const jobs = ref([])
const pairs = ref([])
const studentMap = ref({})
const plagiarismMode = ref(resolvePlagiarismMode(route.query.mode))
const activeJobId = ref('')
const currentPage = ref(1)
const riskView = ref('all')
const assignmentKeyword = ref('')
const assignmentPage = ref(1)
const assignmentPageSize = ref(10)
const assignmentTotal = ref(0)
const pairPageSize = 10

const selectedSummary = computed(
  () => assignmentOptions.value.find((item) => String(item.id) === String(selectedAssignmentId.value)) || null
)

const assignmentPageCount = computed(() => Math.max(1, Math.ceil(assignmentTotal.value / assignmentPageSize.value)))
const assignmentDirectoryCountLabel = computed(() => getAssignmentDirectoryCountLabel(assignmentTotal.value))
const showAssignmentPagination = computed(() =>
  shouldShowAssignmentDirectoryPagination(assignmentTotal.value, assignmentPageSize.value)
)
const modeLabel = computed(() => (plagiarismMode.value === 'DEEP' ? '精细' : '快速'))
const latestModeJob = computed(() => {
  const normalizedMode = String(plagiarismMode.value || 'FAST').trim().toUpperCase()
  return jobs.value.find((job) => String(job?.plagiarismMode || 'FAST').trim().toUpperCase() === normalizedMode) || null
})
const activeThresholdScore = computed(() => Number(latestModeJob.value?.thresholdScore ?? latestModeJob.value?.threshold ?? 80))
const visiblePairs = computed(() => filterPairsByRiskView(pairs.value, riskView.value, activeThresholdScore.value))
const highRiskPairCount = computed(() => countHighRiskPairs(pairs.value, activeThresholdScore.value))

const pagedPairs = computed(() => {
  const start = (currentPage.value - 1) * pairPageSize
  return visiblePairs.value.slice(start, start + pairPageSize)
})

const showResultsLaunchAction = computed(() => shouldShowResultsLaunchAction(selectedAssignmentId.value))

watch(
  () => route.params.assignmentId,
  async (value) => {
    selectedAssignmentId.value = String(value || '')
    currentPage.value = 1
    if (!selectedAssignmentId.value) {
      await Promise.all([loadAssignmentOptions(), loadPage()])
      return
    }
    await loadPage()
  }
)

watch(
  () => route.query.mode,
  async (value) => {
    const nextMode = resolvePlagiarismMode(value)
    if (nextMode === plagiarismMode.value) return
    plagiarismMode.value = nextMode
    currentPage.value = 1
    await loadPage()
  }
)

watch(
  () => pairs.value.length,
  () => {
    currentPage.value = 1
  }
)

watch(
  () => riskView.value,
  () => {
    currentPage.value = 1
  }
)

onMounted(async () => {
  if (!selectedAssignmentId.value) {
    await loadAssignmentOptions()
  }
  await loadPage()
})

async function loadAssignmentOptions() {
  const result = await fetchTeacherAssignments({
    page: assignmentPage.value,
    size: assignmentPageSize.value,
    keyword: assignmentKeyword.value.trim() || undefined,
    status: 'ended'
  })
  assignmentOptions.value = Array.isArray(result?.records) ? result.records : []
  assignmentTotal.value = Number(result?.total || 0)
}

async function loadPage() {
  loading.value = true
  try {
    if (!selectedAssignmentId.value) {
      assignment.value = null
      jobs.value = []
      pairs.value = []
      studentMap.value = {}
      activeJobId.value = ''
      return
    }

    const [detail, plagiarismResult] = await Promise.all([
      fetchTeacherAssignmentDetail(selectedAssignmentId.value),
      fetchTeacherAssignmentPlagiarism(selectedAssignmentId.value, {
        minScore: 0,
        perStudentTopK: 0,
        sortBy: 'score',
        sortDirection: 'desc'
      }, plagiarismMode.value)
    ])

    assignment.value = detail
    jobs.value = plagiarismResult?.jobs || []
    activeJobId.value = plagiarismResult?.activeJobId ? String(plagiarismResult.activeJobId) : ''
    pairs.value = Array.isArray(plagiarismResult?.report?.pairs) ? plagiarismResult.report.pairs : []
    riskView.value = 'all'
    studentMap.value = await loadAssignmentStudentMap(detail)
  } finally {
    loading.value = false
  }
}

function pairRank(index) {
  return getPairRank(currentPage.value, pairPageSize, index)
}

function pairTone(index) {
  return getPairRankTone(pairRank(index))
}

function pairStudentTitle(pair) {
  return buildPairStudentTitle(pair, studentMap.value)
}

function openAssignment(assignmentId) {
  router.push(`/teacher/assignments/${assignmentId}/plagiarism/results`)
}

function backToAssignments() {
  router.push('/teacher/assignments/plagiarism/results')
}

async function handleAssignmentSearch() {
  assignmentPage.value = 1
  loading.value = true
  try {
    await loadAssignmentOptions()
  } finally {
    loading.value = false
  }
}

async function handleAssignmentPageChange(value) {
  assignmentPage.value = value
  loading.value = true
  try {
    await loadAssignmentOptions()
  } finally {
    loading.value = false
  }
}

async function handleAssignmentSizeChange(value) {
  assignmentPageSize.value = value
  assignmentPage.value = 1
  loading.value = true
  try {
    await loadAssignmentOptions()
  } finally {
    loading.value = false
  }
}

function goToLaunchPage() {
  if (!selectedAssignmentId.value) {
    router.push('/teacher/assignments/plagiarism/run')
    return
  }
  router.push({
    path: `/teacher/assignments/${selectedAssignmentId.value}/plagiarism/run`,
    query: { mode: plagiarismMode.value }
  })
}

async function handleModeToggle(mode) {
  const nextMode = resolvePlagiarismMode(mode)
  if (nextMode === plagiarismMode.value) return
  plagiarismMode.value = nextMode
  currentPage.value = 1
  await router.replace({
    path: route.path,
    query: { ...route.query, mode: nextMode }
  })
  await loadPage()
}

function goToPairDetail(pair) {
  router.push({
    path: `/teacher/similarity-pairs/${pair.pairId}`,
    query: {
      assignmentId: selectedAssignmentId.value,
      jobId: activeJobId.value || ''
    }
  })
}

function pairStatusLabel(status) {
  const statusMap = {
    PENDING: '待老师复核',
    CONFIRMED: '老师已确认',
    FALSE_POSITIVE: '判定为误报'
  }
  return statusMap[status] || '待处理'
}

function mapRosterStudent(student) {
  return {
    studentId: Number(student?.id || student?.user_id || student?.userId || 0),
    name: student?.nickname || student?.username || '',
    number: String(student?.student_number || student?.studentNumber || student?.user_id || student?.userId || '')
  }
}

async function loadAssignmentStudentMap(detail) {
  const classIds = Array.isArray(detail?.classIds) ? detail.classIds : []
  if (!classIds.length) return {}

  const groups = await Promise.all(
    classIds.map(async (classId) => {
      try {
        const rows = await request.get(`/teacher/classes/${classId}/students`)
        return Array.isArray(rows) ? rows : []
      } catch {
        return []
      }
    })
  )

  return groups.flat().reduce((result, student) => {
    const profile = mapRosterStudent(student)
    if (!profile.studentId) return result
    result[profile.studentId] = {
      name: profile.name,
      number: profile.number
    }
    return result
  }, {})
}

function resolvePlagiarismMode(rawMode) {
  return String(rawMode || '').trim().toUpperCase() === 'DEEP' ? 'DEEP' : 'FAST'
}
</script>

<style scoped>
.results-page {
  height: 100%;
  min-height: 0;
  display: grid;
  gap: 16px;
  padding: 18px;
  border-radius: 32px;
  background:
    radial-gradient(circle at top right, rgba(138, 167, 255, 0.12), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 255, 0.96));
  border: 1px solid rgba(214, 222, 236, 0.75);
  box-sizing: border-box;
  overflow: hidden;
}

.results-page__shell {
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 14px;
  padding: 18px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(214, 222, 236, 0.8);
  box-shadow: 0 18px 40px rgba(187, 197, 214, 0.12);
  overflow: hidden;
}

.results-page__shell--directory {
  gap: 12px;
}

.results-page__shell--pairs {
  gap: 10px;
}

.results-page__toolbar,
.assignment-card__top,
.assignment-card__stats {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.results-page__toolbar--minimal {
  justify-content: space-between;
  padding: 0 4px;
}

.results-page__toolbar--pairs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(223, 229, 239, 0.72);
}

.results-page__toolbar-lead {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.results-page__toolbar-copy {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.results-page__eyebrow {
  color: #6e87aa;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.results-page__toolbar-copy strong {
  color: #121826;
  font-size: 22px;
  line-height: 1.14;
  letter-spacing: -0.03em;
}

.results-page__directory-tools,
.results-page__directory-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.results-page__directory-tools {
  min-width: 0;
  flex: 1;
}

.results-page__directory-meta {
  justify-content: flex-end;
}

.results-page__toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.results-page__page-meta {
  color: #8a96a8;
  font-size: 13px;
  font-weight: 600;
}

.results-page__back {
  appearance: none;
  border: none;
  background: transparent;
  color: #7e8ba0;
  font-size: 14px;
  padding: 0;
  margin: 0;
  cursor: pointer;
}

.results-page__search {
  width: 280px;
  max-width: 100%;
}

.results-page__mode-switch {
  --_switch-bg-clr: #d7e7f2;
  --_switch-padding: 4px;
  --_slider-bg-clr: rgba(12, 74, 110, 0.65);
  --_slider-bg-clr-on: rgba(12, 74, 110, 1);
  --_slider-txt-clr: #ffffff;
  --_label-padding: 0.8rem 1.5rem;
  --_switch-easing: cubic-bezier(0.47, 1.64, 0.41, 0.8);
}

.switch {
  color: white;
  width: fit-content;
  justify-content: center;
  position: relative;
  border-radius: 9999px;
  cursor: pointer;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  isolation: isolate;
}

.switch input[type='checkbox'] {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}

.switch > span {
  display: grid;
  min-width: 92px;
  place-content: center;
  transition: opacity 300ms ease-in-out 150ms;
  padding: var(--_label-padding);
  color: var(--_slider-txt-clr);
  font-size: 14px;
  font-weight: 700;
}

.switch::before,
.switch::after {
  content: '';
  position: absolute;
  border-radius: inherit;
  transition: inset 150ms ease-in-out;
}

.switch::before {
  background-color: var(--_slider-bg-clr);
  inset: var(--_switch-padding) 50% var(--_switch-padding) var(--_switch-padding);
  transition:
    inset 500ms var(--_switch-easing),
    background-color 500ms ease-in-out;
  z-index: -1;
  box-shadow:
    inset 0 1px 1px rgba(0, 0, 0, 0.3),
    0 1px rgba(255, 255, 255, 0.3);
}

.switch::after {
  background-color: var(--_switch-bg-clr);
  inset: 0;
  z-index: -2;
}

.switch:focus-within::after {
  inset: -0.25rem;
}

.switch:has(input:checked):hover > span:first-of-type,
.switch:has(input:not(:checked)):hover > span:last-of-type {
  opacity: 1;
  transition-delay: 0ms;
  transition-duration: 100ms;
}

.switch:has(input:checked):hover::before {
  inset: var(--_switch-padding) var(--_switch-padding) var(--_switch-padding) 45%;
}

.switch:has(input:not(:checked)):hover::before {
  inset: var(--_switch-padding) 45% var(--_switch-padding) var(--_switch-padding);
}

.switch:has(input:checked)::before {
  background-color: var(--_slider-bg-clr-on);
  inset: var(--_switch-padding) var(--_switch-padding) var(--_switch-padding) 50%;
}

.switch > span:last-of-type,
.switch > input:checked + span:first-of-type {
  opacity: 0.75;
}

.switch > input:checked ~ span:last-of-type {
  opacity: 1;
}

.results-page__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 108px;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(18, 24, 38, 0.05);
  color: #66778f;
  font-size: 13px;
  font-weight: 700;
}

.results-page__loading {
  padding: 36px 0;
  text-align: center;
  color: #7c8aa0;
}

.results-page__pairs-body {
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
}

.results-page__grid {
  min-height: 0;
  display: grid;
  gap: 14px;
}

.results-page__grid--directory {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-auto-rows: auto;
  align-content: start;
  align-items: start;
}

.assignment-card,
.pair-card {
  appearance: none;
  -webkit-appearance: none;
  width: 100%;
  border: none;
  outline: none;
  font: inherit;
  text-align: left;
  box-sizing: border-box;
}

.assignment-card {
  display: grid;
  grid-template-rows: auto auto auto auto;
  gap: 18px;
  padding: 24px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid rgba(226, 231, 241, 0.95);
  box-shadow: 0 10px 26px rgba(187, 197, 214, 0.08);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  overflow: hidden;
}

.assignment-card--compact {
  min-height: 232px;
  height: auto;
  align-content: start;
}

.assignment-card:hover,
.pair-card:hover {
  transform: translateY(-2px);
  border-color: rgba(116, 130, 255, 0.28);
  box-shadow: 0 16px 30px rgba(183, 194, 214, 0.14);
}

.assignment-card__lang,
.assignment-card__status,
.pair-card__order,
.pair-card__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.assignment-card__lang {
  padding: 6px 11px;
  background: rgba(18, 24, 38, 0.04);
  color: #6a7990;
}

.assignment-card__status {
  padding: 6px 11px;
  background: rgba(90, 119, 255, 0.08);
  color: #4d68db;
}

.assignment-card__status.is-ended {
  background: rgba(18, 24, 38, 0.08);
  color: #5e6c80;
}

.assignment-card__body {
  display: grid;
  gap: 8px;
}

.assignment-card__title {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: #121826;
  font-size: 28px;
  line-height: 1.15;
  letter-spacing: -0.03em;
}

.assignment-card--compact .assignment-card__title {
  font-size: 20px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.assignment-card__meta {
  margin: 0;
  color: #7b879a;
  font-size: 13px;
  line-height: 1.4;
}

.assignment-card__stats {
  align-items: stretch;
}

.assignment-card__stats--compact {
  gap: 10px;
}

.assignment-card__stats > div {
  flex: 1;
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid rgba(233, 237, 245, 0.9);
  border-radius: 16px;
  background: #fafbfe;
}

.assignment-card__stats span {
  color: #8c98ab;
  font-size: 12px;
  font-weight: 500;
}

.assignment-card__stats strong {
  color: #121826;
  font-size: 15px;
  line-height: 1.3;
  word-break: break-word;
}

.assignment-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: auto;
  padding-top: 4px;
}

.assignment-card__footer-label {
  color: #7b879a;
  font-size: 13px;
  font-weight: 600;
}

.assignment-card__footer-arrow {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(18, 24, 38, 0.06);
  color: #121826;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0;
}

.pair-list {
  min-height: 0;
  display: grid;
  gap: 12px;
  align-content: start;
  overflow: auto;
  padding-right: 4px;
  padding-bottom: 4px;
}

.pair-filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding-bottom: 2px;
}

.pair-filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid rgba(214, 222, 236, 0.95);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #607089;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.pair-filter-chip strong {
  color: #121826;
  font-size: 14px;
}

.pair-filter-chip:hover,
.pair-filter-chip.is-active {
  transform: translateY(-1px);
  border-color: rgba(96, 118, 255, 0.35);
  box-shadow: 0 12px 24px rgba(173, 185, 213, 0.18);
}

.pair-filter-chip.is-active {
  background: linear-gradient(180deg, rgba(239, 244, 255, 0.98), rgba(233, 240, 255, 0.94));
  color: #4056c7;
}

.pair-filter-hint {
  color: #7b879a;
  font-size: 13px;
  line-height: 1.6;
}

.pair-card {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  border-radius: 24px;
  background: linear-gradient(180deg, #ffffff, #f7f9ff);
  border: 1px solid rgba(214, 222, 236, 0.95);
  box-shadow: 0 14px 28px rgba(187, 197, 214, 0.1);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.pair-card.is-top-1 {
  border-color: rgba(231, 67, 91, 0.28);
  box-shadow: 0 18px 36px rgba(231, 67, 91, 0.12);
}

.pair-card.is-top-2 {
  border-color: rgba(255, 190, 42, 0.34);
  box-shadow: 0 18px 36px rgba(255, 190, 42, 0.11);
}

.pair-card.is-top-3 {
  border-color: rgba(85, 115, 255, 0.28);
  box-shadow: 0 18px 36px rgba(85, 115, 255, 0.1);
}

.pair-card__rank-box {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  border-radius: 22px;
  border: 1px solid rgba(214, 222, 236, 0.9);
  background: #fff;
  color: #61718a;
}

.pair-card__rank-box.is-top-1 {
  background: linear-gradient(180deg, rgba(255, 95, 122, 0.14), rgba(255, 95, 122, 0.08));
  border-color: rgba(231, 67, 91, 0.34);
  color: #d43b53;
}

.pair-card__rank-box.is-top-2 {
  background: linear-gradient(180deg, rgba(255, 200, 62, 0.16), rgba(255, 200, 62, 0.08));
  border-color: rgba(239, 181, 32, 0.36);
  color: #c98907;
}

.pair-card__rank-box.is-top-3 {
  background: linear-gradient(180deg, rgba(103, 126, 255, 0.16), rgba(103, 126, 255, 0.08));
  border-color: rgba(85, 115, 255, 0.34);
  color: #4c63e6;
}

.pair-card__rank-number {
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
}

.pair-card__main {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.pair-card__headline {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pair-card__order {
  padding: 6px 10px;
  background: rgba(18, 24, 38, 0.06);
  color: #6d7d92;
}

.pair-card__order.is-top-1 {
  background: rgba(231, 67, 91, 0.1);
  color: #d43b53;
}

.pair-card__order.is-top-2 {
  background: rgba(255, 190, 42, 0.15);
  color: #c98907;
}

.pair-card__order.is-top-3 {
  background: rgba(85, 115, 255, 0.12);
  color: #4c63e6;
}

.pair-card__title {
  color: #121826;
  font-size: 17px;
  line-height: 1.3;
}

.pair-card__meta {
  margin: 0;
  color: #7b879a;
  font-size: 13px;
  line-height: 1.55;
}

.pair-card__aside {
  display: grid;
  justify-items: end;
  gap: 10px;
}

.pair-card__score {
  color: #121826;
  font-size: 34px;
  line-height: 1;
  letter-spacing: -0.04em;
}

.pair-card__action {
  padding: 10px 14px;
  background: #121826;
  color: #fff;
}

.results-page__footer {
  display: flex;
  justify-content: center;
  padding-top: 2px;
}

.results-page__footer--pairs {
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding-top: 4px;
}

.results-page__footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  margin-left: auto;
}

.results-page__footer--single {
  justify-content: flex-end;
}

.results-page__empty {
  align-self: center;
}

@media (max-width: 1280px) {
  .pair-card {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .pair-card__aside {
    grid-column: 1 / -1;
    justify-items: start;
    grid-auto-flow: column;
    justify-content: space-between;
    align-items: center;
  }
}

@media (max-width: 960px) {
  .results-page__toolbar,
  .assignment-card__top,
  .assignment-card__stats {
    flex-direction: column;
    align-items: stretch;
  }

  .results-page__directory-meta {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .results-page__grid--directory {
    grid-template-columns: 1fr;
  }

  .results-page__toolbar-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .results-page__toolbar-lead {
    flex-direction: column;
    align-items: flex-start;
  }

  .assignment-card__footer {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .pair-card {
    grid-template-columns: 1fr;
  }

  .pair-card__rank-box {
    width: 60px;
    height: 60px;
  }

  .pair-card__aside,
  .results-page__footer--pairs {
    grid-auto-flow: row;
    justify-items: stretch;
  }

  .results-page__footer-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
    margin-left: 0;
  }
}
</style>




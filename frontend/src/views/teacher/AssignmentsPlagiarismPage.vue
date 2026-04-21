<template>
  <section class="plagiarism-launch-page">
    <article class="plagiarism-launch-frame">
      <div class="plagiarism-launch-frame__topbar">
        <AppBackButton label="返回作业列表" @click="router.push('/teacher/assignments')" />

        <div class="plagiarism-launch-frame__steps">
          <span
            v-for="(label, index) in launchSteps"
            :key="label"
            class="plagiarism-launch-frame__step"
            :class="{ active: launchStep === index + 1, done: launchStep > index + 1 }"
          >
            {{ index + 1 }} · {{ label }}
          </span>
        </div>
      </div>

      <div class="plagiarism-launch-frame__content">
        <transition name="launch-slide" mode="out-in">
          <section v-if="launchStep === 1" key="intro" class="plagiarism-launch-section">
            <div class="plagiarism-launch-section__split plagiarism-launch-section__split--intro">
              <div class="plagiarism-launch-section__main">
                <span class="plagiarism-launch-section__eyebrow">开始之前</span>
                <h2>先确认这次要查哪份作业</h2>
                <p class="plagiarism-launch-section__lead">
                  选择后，系统会按这份作业下的班级和最新提交版本执行查重。
                </p>

                <el-select
                  v-model="selectedAssignmentId"
                  class="plagiarism-launch-section__select"
                  popper-class="plagiarism-launch-select-popper"
                  placeholder="请选择作业"
                  @change="handleAssignmentChange"
                >
                  <el-option v-for="item in assignmentOptions" :key="item.id" :label="item.title" :value="String(item.id)" />
                </el-select>

                <div class="plagiarism-launch-section__meta" v-if="currentAssignment">
                  <span>{{ currentAssignment.title }}</span>
                  <span>{{ currentAssignment.classNamesText || '已关联班级' }}</span>
                </div>
              </div>

              <aside class="plagiarism-launch-section__aside">
                <div class="plagiarism-launch-section__term">
                  <strong>相似度</strong>
                  <p>分数越高，说明两份代码整体更像，老师更值得优先查看。</p>
                </div>
                <div class="plagiarism-launch-section__term">
                  <strong>最低分数</strong>
                  <p>相当于筛选线，只展示不低于这个分数的结果，推荐先用 80。</p>
                </div>
                <div class="plagiarism-launch-section__term">
                  <strong>每人保留</strong>
                  <p>每个学生最多保留多少条结果，避免列表过长，推荐先用 10。</p>
                </div>
              </aside>
            </div>
          </section>

          <section v-else-if="launchStep === 2" key="roster" class="plagiarism-launch-section">
            <div class="plagiarism-launch-section__center plagiarism-launch-section__center--wide">
              <div class="plagiarism-launch-section__title-row">
                <h2>先确认提交名单</h2>
              </div>
              <p class="plagiarism-launch-section__lead">
                左侧是已提交并会参与查重的学生，右侧是当前仍未提交的学生。确认名单后选择模式即可发起。
              </p>

              <div class="plagiarism-launch-roster" v-loading="rosterLoading">
                <section class="plagiarism-launch-roster__panel">
                  <header class="plagiarism-launch-roster__header">
                    <div>
                      <h3>已提交</h3>
                      <p>{{ submittedRoster.length }} 人</p>
                    </div>
                    <span class="plagiarism-launch-roster__badge plagiarism-launch-roster__badge--ok">参与查重</span>
                  </header>

                  <div class="plagiarism-launch-roster__body">
                    <article
                      v-for="item in submittedRoster"
                      :key="`submitted-${item.studentId}`"
                      class="plagiarism-launch-roster__item plagiarism-launch-roster__item--submitted"
                    >
                      <div>
                        <strong>{{ item.studentName }}</strong>
                        <span>{{ item.studentNumber || '学号未记录' }}</span>
                      </div>
                      <div class="plagiarism-launch-roster__meta">
                        <span>{{ item.className || '班级未记录' }}</span>
                        <span>{{ item.lastSubmittedAt || '提交时间未记录' }}</span>
                      </div>
                    </article>

                    <div v-if="!submittedRoster.length && !rosterLoading" class="plagiarism-launch-roster__empty">
                      这份作业当前还没有可参与查重的提交。
                    </div>
                  </div>
                </section>

                <section class="plagiarism-launch-roster__panel">
                  <header class="plagiarism-launch-roster__header">
                    <div>
                      <h3>未提交</h3>
                      <p>{{ pendingRoster.length }} 人</p>
                    </div>
                    <span class="plagiarism-launch-roster__badge plagiarism-launch-roster__badge--pending">待补交</span>
                  </header>

                  <div class="plagiarism-launch-roster__body">
                    <article
                      v-for="item in pendingRoster"
                      :key="`pending-${item.studentId}`"
                      class="plagiarism-launch-roster__item plagiarism-launch-roster__item--pending"
                    >
                      <div>
                        <strong>{{ item.studentName }}</strong>
                        <span>{{ item.studentNumber || '学号未记录' }}</span>
                      </div>
                      <div class="plagiarism-launch-roster__meta">
                        <span>{{ item.className || '班级未记录' }}</span>
                        <span>尚未提交</span>
                      </div>
                    </article>

                    <div v-if="!pendingRoster.length && !rosterLoading" class="plagiarism-launch-roster__empty">
                      当前班级成员已全部提交。
                    </div>
                  </div>
                </section>
              </div>
            </div>
          </section>

          <section v-else-if="launchStep === 3" key="params" class="plagiarism-launch-section">
            <div class="plagiarism-launch-section__center">
              <div class="plagiarism-launch-section__title-row">
                <h2>选择本次查重模式</h2>
                <TeacherExplainTip
                  title="系统自动策略"
                  content="系统会自动计算并保存全部两两对比结果，不再要求老师手动设置阈值或 TopK。默认以 80 分标记高风险，便于老师优先复核。"
                  recommendation="你只需要决定先用快速还是精细模式。"
                  label="查看自动策略说明"
                />
              </div>
              <p class="plagiarism-launch-section__lead">
                这一步不需要手动调参。系统会保留全部结果，并自动标记高风险 pair。
              </p>

              <div class="plagiarism-launch-section__recommend">
                系统自动策略：保存 <strong>全部两两对比</strong>，高风险参考线 <strong>80</strong>
              </div>

              <div class="plagiarism-launch-section__mode-block">
                <div class="plagiarism-launch-section__mode-title">
                  <span>查重模式</span>
                  <TeacherExplainTip
                    title="查重模式说明"
                    content="快速查重适合先把高风险结果筛出来；精细查重会走更细的结构链路，结果更谨慎，但会更慢一些。"
                    recommendation="第一次建议先用快速查重；需要重点复核时再切到精细查重。"
                    label="查看查重模式说明"
                  />
                </div>

                <label class="switch plagiarism-launch-section__mode-switch" aria-label="切换查重模式">
                  <input
                    type="checkbox"
                    :checked="plagiarismMode === 'DEEP'"
                    @change="handleModeToggle($event.target.checked ? 'DEEP' : 'FAST')"
                  >
                  <span>快速</span>
                  <span>精细</span>
                </label>

                <p class="plagiarism-launch-section__mode-copy">
                  {{ plagiarismMode === 'DEEP' ? '精细查重会做更细的结构复核，耗时更长。' : '快速查重适合先筛出高风险结果，出结果更快。' }}
                </p>
              </div>

              <p class="plagiarism-launch-section__tip">
                系统会自动按这份作业关联的全部班级处理，只比较每位学生当前最新、能正常参与查重的提交版本，并完整保存所有 pair。
              </p>
            </div>
          </section>

          <section v-else key="confirm" class="plagiarism-launch-section">
            <div class="plagiarism-launch-section__center">
              <h2>最后确认一次</h2>
              <p class="plagiarism-launch-section__lead">
                确认无误后发起查重，任务会在后台运行，你可以稍后进入结果页查看。
              </p>

              <div class="plagiarism-launch-section__summary">
                <div class="plagiarism-launch-section__summary-item">
                  <span>当前作业</span>
                  <strong>{{ currentAssignment?.title || '未选择' }}</strong>
                </div>
                <div class="plagiarism-launch-section__summary-item">
                  <span>班级范围</span>
                  <strong>{{ currentAssignment?.classNamesText || '默认全部关联班级' }}</strong>
                </div>
                <div class="plagiarism-launch-section__summary-item">
                  <span>已提交 / 未提交</span>
                  <strong>{{ submittedRoster.length }} / {{ pendingRoster.length }}</strong>
                </div>
                <div class="plagiarism-launch-section__summary-item">
                  <span>结果范围</span>
                  <strong>全部两两对比</strong>
                </div>
                <div class="plagiarism-launch-section__summary-item">
                  <span>高风险参考线</span>
                  <strong>{{ thresholdScore }} 分</strong>
                </div>
                <div class="plagiarism-launch-section__summary-item">
                  <span>查重模式</span>
                  <strong>{{ plagiarismMode === 'DEEP' ? '精细查重' : '快速查重' }}</strong>
                </div>
              </div>

              <p class="plagiarism-launch-section__tip">
                {{ latestModeJob ? `${plagiarismMode === 'DEEP' ? '精细' : '快速'}模式已有结果，再次发起会覆盖上一份。最近状态：${latestModeJob.status}，${latestModeJob.createTime || '时间未记录'}` : `当前还没有${plagiarismMode === 'DEEP' ? '精细' : '快速'}查重结果。` }}
              </p>
            </div>
          </section>
        </transition>
      </div>

      <div class="plagiarism-launch-frame__footer">
        <button
          v-if="launchStep > 1"
          type="button"
          class="plagiarism-launch-frame__plain-action"
          @click="goPrevStep"
        >
          上一步
        </button>

        <button
          v-if="launchStep < 4"
          type="button"
          class="learn-more"
          :disabled="launchStep === 1 && !selectedAssignmentId"
          @click="goNextStep"
        >
          <span class="circle" aria-hidden="true">
            <span class="icon arrow"></span>
          </span>
          <span class="button-text">下一步</span>
        </button>

        <el-button
          v-else
          type="primary"
          round
          size="large"
          :loading="jobLoading"
          :disabled="!selectedAssignmentId"
          @click="handleCreateJob"
        >
          {{ latestModeJob ? '确认覆盖发起' : '确认发起' }}
        </el-button>
      </div>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import TeacherExplainTip from './components/TeacherExplainTip.vue'
import AppBackButton from '../../components/AppBackButton.vue'
import { getLatestJobByMode } from './plagiarismResultsHelpers'
import request from '../../api/request'
import {
  createTeacherPlagiarismJob,
  fetchTeacherAssignments,
  fetchTeacherAssignmentSubmissions,
  fetchTeacherPlagiarismJobs
} from '../../api/teacherAssignments'

const route = useRoute()
const router = useRouter()

const launchSteps = ['选择作业', '查看提交名单', '选择模式', '确认发起']
const launchStep = ref(1)
const assignmentOptions = ref([])
const selectedAssignmentId = ref(route.params.assignmentId ? String(route.params.assignmentId) : '')
const jobs = ref([])
const thresholdScore = ref(80)
const topKPerStudent = ref(0)
const plagiarismMode = ref(resolvePlagiarismMode(route.query.mode))
const jobLoading = ref(false)
const rosterLoading = ref(false)
const submittedRoster = ref([])
const pendingRoster = ref([])

const currentAssignment = computed(() => assignmentOptions.value.find((item) => String(item.id) === selectedAssignmentId.value) || null)
const latestModeJob = computed(() => getLatestJobByMode(jobs.value, plagiarismMode.value))

async function loadAssignments() {
  const result = await fetchTeacherAssignments({ page: 1, size: 100, status: 'ended' })
  assignmentOptions.value = Array.isArray(result?.records) ? result.records : []

  if (!selectedAssignmentId.value && assignmentOptions.value.length) {
    selectedAssignmentId.value = String(assignmentOptions.value[0].id)
  }
}

async function loadJobs() {
  if (!selectedAssignmentId.value) {
    jobs.value = []
    return
  }

  try {
    jobs.value = await fetchTeacherPlagiarismJobs(Number(selectedAssignmentId.value))
  } catch {
    jobs.value = []
  }
}

function mapRosterStudent(student, className = '') {
  return {
    studentId: Number(student?.id || student?.user_id || student?.userId || 0),
    studentName: student?.nickname || student?.username || '未命名学生',
    studentNumber: String(student?.student_number || student?.studentNumber || student?.user_id || student?.userId || ''),
    className
  }
}

function buildSubmittedRoster(rows = [], classNameMap = new Map()) {
  const latestMap = new Map()
  rows.forEach((item) => {
    if (!item?.studentId) return
    const current = latestMap.get(item.studentId)
    const nextTime = item.lastSubmittedAt || ''
    const currentTime = current?.lastSubmittedAt || ''
    if (!current || nextTime >= currentTime) {
      latestMap.set(item.studentId, {
        studentId: Number(item.studentId),
        studentName: item.studentName || `学生 ${item.studentId}`,
        studentNumber: String(item.studentNumber || item.studentId || ''),
        lastSubmittedAt: item.lastSubmittedAt || '',
        className: classNameMap.get(Number(item.studentId)) || ''
      })
    }
  })
  return Array.from(latestMap.values()).sort((left, right) => {
    const timeLeft = left.lastSubmittedAt || ''
    const timeRight = right.lastSubmittedAt || ''
    return timeRight.localeCompare(timeLeft)
  })
}

async function loadSubmissionRoster() {
  if (!selectedAssignmentId.value || !currentAssignment.value) {
    submittedRoster.value = []
    pendingRoster.value = []
    return
  }

  rosterLoading.value = true
  try {
    const [submissions, classStudentGroups] = await Promise.all([
      fetchTeacherAssignmentSubmissions(Number(selectedAssignmentId.value)).catch(() => []),
      Promise.all(
        (currentAssignment.value.classIds || []).map(async (classId) => {
          try {
            const rows = await request.get(`/teacher/classes/${classId}/students`)
            return Array.isArray(rows) ? rows : []
          } catch {
            return []
          }
        })
      )
    ])

    const classNames = currentAssignment.value.classNames || []
    const studentMap = new Map()
    const classNameMap = new Map()
    classStudentGroups.forEach((group, index) => {
      const className = classNames[index] || `班级 ${index + 1}`
      group.forEach((student) => {
        const mapped = mapRosterStudent(student, className)
        if (!mapped.studentId) return
        studentMap.set(mapped.studentId, mapped)
        classNameMap.set(mapped.studentId, className)
      })
    })

    const submitted = buildSubmittedRoster(submissions, classNameMap)
    const submittedIds = new Set(submitted.map((item) => Number(item.studentId)))
    const pending = Array.from(studentMap.values())
      .filter((item) => !submittedIds.has(Number(item.studentId)))
      .sort((left, right) => String(left.studentNumber).localeCompare(String(right.studentNumber)))

    submittedRoster.value = submitted
    pendingRoster.value = pending
  } finally {
    rosterLoading.value = false
  }
}

async function handleAssignmentChange() {
  if (!selectedAssignmentId.value) {
    jobs.value = []
    submittedRoster.value = []
    pendingRoster.value = []
    return
  }

  router.replace(`/teacher/assignments/${selectedAssignmentId.value}/plagiarism/run`)
  await Promise.all([loadJobs(), loadSubmissionRoster()])
}

async function handleModeToggle(mode) {
  plagiarismMode.value = resolvePlagiarismMode(mode)
  await router.replace({
    path: route.path,
    query: { ...route.query, mode: plagiarismMode.value }
  })
}

async function goNextStep() {
  if (launchStep.value === 1 && !selectedAssignmentId.value) {
    ElMessage.warning('请先选择一份作业')
    return
  }

  if (launchStep.value === 1) {
    await loadSubmissionRoster()
  }

  if (launchStep.value < 4) {
    launchStep.value += 1
  }
}

function goPrevStep() {
  if (launchStep.value > 1) {
    launchStep.value -= 1
  }
}

async function handleCreateJob() {
  if (!selectedAssignmentId.value) return

  jobLoading.value = true
  try {
    await createTeacherPlagiarismJob(Number(selectedAssignmentId.value), {
      thresholdScore: thresholdScore.value,
      topKPerStudent: topKPerStudent.value,
      plagiarismMode: plagiarismMode.value
    })
    ElMessage.success(`${plagiarismMode.value === 'DEEP' ? '精细' : '快速'}查重已发起，新结果会覆盖该模式之前的结果`)
    router.push({
      path: `/teacher/assignments/${selectedAssignmentId.value}/plagiarism/results`,
      query: { mode: plagiarismMode.value }
    })
  } catch {
    ElMessage.error('创建查重任务失败')
  } finally {
    jobLoading.value = false
  }
}

function resolvePlagiarismMode(rawMode) {
  return String(rawMode || '').trim().toUpperCase() === 'DEEP' ? 'DEEP' : 'FAST'
}

onMounted(async () => {
  await loadAssignments()
  await Promise.all([loadJobs(), loadSubmissionRoster()])
})
</script>

<style scoped>
.plagiarism-launch-page {
  min-height: calc(100vh - 180px);
}

.plagiarism-launch-frame {
  display: flex;
  min-height: calc(100vh - 180px);
  flex-direction: column;
  padding: 36px 46px 34px;
  border-radius: 32px;
  border: 1px solid rgba(207, 217, 235, 0.65);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 22px 44px rgba(191, 201, 214, 0.14);
}

.plagiarism-launch-frame__topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 48px;
}

.plagiarism-launch-frame__plain-action,
.learn-more {
  position: relative;
  display: inline-block;
  cursor: pointer;
  outline: none;
  border: 0;
  vertical-align: middle;
  text-decoration: none;
  background: transparent;
  padding: 0;
  font-size: inherit;
  font-family: inherit;
}

.plagiarism-launch-frame__steps {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  gap: 18px;
  margin: 0;
  padding-top: 4px;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
}

.plagiarism-launch-frame__steps::-webkit-scrollbar {
  display: none;
}

.plagiarism-launch-frame__step {
  position: relative;
  padding: 0 0 20px;
  color: #96a1b4;
  font-size: 16px;
  font-weight: 700;
  text-align: right;
}

.plagiarism-launch-frame__step:not(:last-child)::after {
  content: '→';
  position: absolute;
  right: -18px;
  top: 50%;
  transform: translateY(-50%);
  color: #b0b9c8;
}

.plagiarism-launch-frame__step.active,
.plagiarism-launch-frame__step.done {
  color: #1b2333;
}

.plagiarism-launch-frame__step.active {
  text-decoration: underline;
  text-underline-offset: 8px;
}

.plagiarism-launch-frame__content {
  flex: 1;
  display: flex;
  align-items: stretch;
  justify-content: center;
  min-height: 0;
  padding: 18px 0 8px;
}

.plagiarism-launch-section {
  width: min(1360px, 100%);
}

.plagiarism-launch-section__center {
  display: grid;
  justify-items: center;
  text-align: center;
  gap: 22px;
  min-height: 100%;
  align-content: center;
}

.plagiarism-launch-section__center--wide {
  justify-items: stretch;
  text-align: left;
  align-content: start;
}

.plagiarism-launch-section__split {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 420px);
  align-items: start;
  gap: 64px;
  width: 100%;
  min-height: 100%;
}

.plagiarism-launch-section__split--intro {
  grid-template-columns: minmax(0, 720px) minmax(340px, 420px);
  justify-content: space-between;
  padding: 42px 10px 24px 26px;
}

.plagiarism-launch-section__main {
  display: grid;
  justify-items: start;
  text-align: left;
  gap: 24px;
  max-width: 760px;
  padding-left: 0;
}

.plagiarism-launch-section__aside {
  display: grid;
  gap: 16px;
  align-content: start;
  justify-self: end;
  width: 100%;
  padding-top: 12px;
}

.plagiarism-launch-section__eyebrow {
  color: #8b98ae;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.plagiarism-launch-section__center h2 {
  margin: 0;
  color: #162236;
  font-size: 34px;
}

.plagiarism-launch-section__main h2 {
  margin: 0;
  color: #162236;
  font-size: 54px;
  line-height: 1.16;
  letter-spacing: -0.03em;
}

.plagiarism-launch-section__title-row {
  display: inline-flex;
  align-items: center;
  gap: 14px;
}

.plagiarism-launch-section__lead {
  margin: 0;
  max-width: 680px;
  color: #75839c;
  font-size: 18px;
  line-height: 1.95;
}

.plagiarism-launch-section__select {
  width: min(680px, 100%);
  margin: 10px 0 4px;
}

.plagiarism-launch-section__select :deep(.el-select__wrapper) {
  min-height: 68px;
  padding: 0 22px;
  border-radius: 28px;
  border: 1px solid rgba(201, 212, 229, 0.9);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 255, 0.95));
  box-shadow: 0 18px 34px rgba(162, 177, 203, 0.15);
}

.plagiarism-launch-section__select :deep(.el-select__placeholder),
.plagiarism-launch-section__select :deep(.el-select__selected-item) {
  color: #20304a;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.plagiarism-launch-section__select :deep(.el-select__caret) {
  color: #8c97a7;
  font-size: 18px;
}

.plagiarism-launch-section__meta {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  gap: 12px;
}

.plagiarism-launch-section__meta span,
.plagiarism-launch-section__recommend {
  padding: 12px 22px;
  border-radius: 999px;
  background: rgba(239, 243, 250, 0.9);
  color: #55657f;
  font-size: 15px;
}

.plagiarism-launch-section__term {
  padding: 24px 24px 22px;
  border-radius: 26px;
  background: rgba(249, 251, 255, 0.96);
  border: 1px solid rgba(220, 228, 240, 0.86);
  box-shadow: 0 14px 26px rgba(196, 207, 224, 0.1);
  color: #617188;
  line-height: 1.78;
}

.plagiarism-launch-section__term strong {
  display: block;
  margin-bottom: 12px;
  color: #182236;
  font-size: 22px;
}

.plagiarism-launch-section__term p,
.plagiarism-launch-section__tip {
  margin: 0;
}

.plagiarism-launch-section__params {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 36px;
  margin-top: 8px;
}

.plagiarism-launch-section__mode-block {
  display: grid;
  gap: 16px;
  width: min(760px, 100%);
}

.plagiarism-launch-section__mode-title {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #182236;
  font-size: 18px;
  font-weight: 700;
}

.plagiarism-launch-section__mode-switch {
  --_switch-bg-clr: #d7e7f2;
  --_switch-padding: 4px;
  --_slider-bg-clr: rgba(12, 74, 110, 0.65);
  --_slider-bg-clr-on: rgba(12, 74, 110, 1);
  --_slider-txt-clr: #ffffff;
  --_label-padding: 0.95rem 2rem;
  --_switch-easing: cubic-bezier(0.47, 1.64, 0.41, 0.8);
  margin: 0 auto;
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
  min-width: 104px;
  place-content: center;
  transition: opacity 300ms ease-in-out 150ms;
  padding: var(--_label-padding);
  color: var(--_slider-txt-clr);
  font-size: 15px;
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

.plagiarism-launch-section__mode-copy {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
  text-align: center;
}

.plagiarism-launch-section__param {
  display: grid;
  justify-items: center;
  gap: 12px;
  min-width: 220px;
}

.plagiarism-launch-section__param-title {
  display: inline-flex;
  align-items: center;
  color: #182236;
  font-size: 18px;
  font-weight: 700;
}

.plagiarism-launch-section__summary {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 6px;
}

.plagiarism-launch-section__summary-item {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(245, 247, 252, 0.86);
  text-align: left;
}

.plagiarism-launch-section__summary-item span {
  color: #8b97aa;
  font-size: 13px;
}

.plagiarism-launch-section__summary-item strong {
  color: #162236;
  font-size: 20px;
  word-break: break-word;
}

.plagiarism-launch-roster {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.plagiarism-launch-roster__panel {
  display: flex;
  min-height: 420px;
  flex-direction: column;
  border-radius: 28px;
  border: 1px solid rgba(217, 225, 237, 0.88);
  background: rgba(249, 251, 255, 0.9);
  box-shadow: 0 16px 30px rgba(196, 207, 224, 0.12);
  overflow: hidden;
}

.plagiarism-launch-roster__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 24px 20px;
  border-bottom: 1px solid rgba(225, 232, 242, 0.9);
}

.plagiarism-launch-roster__header h3 {
  margin: 0;
  color: #182236;
  font-size: 24px;
}

.plagiarism-launch-roster__header p {
  margin: 8px 0 0;
  color: #8794aa;
  font-size: 14px;
}

.plagiarism-launch-roster__badge {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
}

.plagiarism-launch-roster__badge--ok {
  background: rgba(88, 199, 132, 0.12);
  color: #2d8f54;
}

.plagiarism-launch-roster__badge--pending {
  background: rgba(255, 181, 72, 0.14);
  color: #b86b00;
}

.plagiarism-launch-roster__body {
  display: grid;
  align-content: start;
  gap: 12px;
  min-height: 0;
  padding: 20px 24px 24px;
  overflow: auto;
}

.plagiarism-launch-roster__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 22px;
  border: 1px solid rgba(221, 229, 239, 0.92);
  background: rgba(255, 255, 255, 0.9);
}

.plagiarism-launch-roster__item strong {
  display: block;
  color: #182236;
  font-size: 17px;
}

.plagiarism-launch-roster__item span {
  color: #7f8ca3;
  font-size: 14px;
}

.plagiarism-launch-roster__meta {
  display: grid;
  justify-items: end;
  gap: 6px;
  text-align: right;
}

.plagiarism-launch-roster__item--submitted {
  border-color: rgba(164, 216, 184, 0.7);
}

.plagiarism-launch-roster__item--pending {
  border-color: rgba(245, 212, 161, 0.74);
}

.plagiarism-launch-roster__empty {
  display: grid;
  place-items: center;
  min-height: 220px;
  color: #8d99af;
  font-size: 15px;
}

.plagiarism-launch-section__tip {
  color: #7f8ca3;
  font-size: 15px;
  line-height: 1.8;
}

.plagiarism-launch-frame__footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  margin-top: 0;
  padding-top: 24px;
  padding-bottom: 24px;
}

.plagiarism-launch-frame__plain-action {
  color: #7d889b;
  font-size: 15px;
  font-weight: 600;
}

.learn-more {
  width: 12rem;
  height: auto;
}

.learn-more:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.learn-more .circle {
  transition: all 0.45s cubic-bezier(0.65, 0, 0.076, 1);
  position: relative;
  display: block;
  margin: 0;
  width: 3rem;
  height: 3rem;
  background: #282936;
  border-radius: 1.625rem;
}

.learn-more .circle .icon {
  transition: all 0.45s cubic-bezier(0.65, 0, 0.076, 1);
  position: absolute;
  top: 0;
  bottom: 0;
  margin: auto;
  background: #fff;
}

.learn-more .circle .icon.arrow {
  left: 0.625rem;
  width: 1.125rem;
  height: 0.125rem;
  background: none;
}

.learn-more .circle .icon.arrow::before {
  position: absolute;
  content: '';
  top: -0.29rem;
  right: 0.0625rem;
  width: 0.625rem;
  height: 0.625rem;
  border-top: 0.125rem solid #fff;
  border-right: 0.125rem solid #fff;
  transform: rotate(45deg);
}

.learn-more .button-text {
  transition: all 0.45s cubic-bezier(0.65, 0, 0.076, 1);
  position: absolute;
  inset: 0;
  padding: 0.75rem 0;
  margin: 0 0 0 1.85rem;
  color: #282936;
  font-weight: 700;
  line-height: 1.6;
  text-align: center;
}

.learn-more:not(:disabled):hover .circle {
  width: 100%;
}

.learn-more:not(:disabled):hover .circle .icon.arrow {
  background: #fff;
  transform: translate(1rem, 0);
}

.learn-more:not(:disabled):hover .button-text {
  color: #fff;
}

.launch-slide-enter-active,
.launch-slide-leave-active {
  transition: all 0.28s ease;
}

.launch-slide-enter-from {
  opacity: 0;
  transform: translateX(24px);
}

.launch-slide-leave-to {
  opacity: 0;
  transform: translateX(-24px);
}

@media (max-width: 960px) {
  .plagiarism-launch-frame {
    padding: 24px 20px 28px;
  }

  .plagiarism-launch-frame__topbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .plagiarism-launch-frame__steps {
    justify-content: flex-start;
    gap: 16px;
    padding: 0 8px 4px;
  }

  .plagiarism-launch-section__split,
  .plagiarism-launch-section__summary,
  .plagiarism-launch-roster {
    grid-template-columns: 1fr;
  }

  .plagiarism-launch-section__split--intro {
    padding: 16px 0 0;
  }

  .plagiarism-launch-section__main {
    justify-items: center;
    text-align: center;
    padding-left: 0;
    max-width: none;
  }

  .plagiarism-launch-section__aside {
    justify-self: stretch;
  }
}
</style>

<style>
.plagiarism-launch-select-popper.el-select__popper {
  border-radius: 24px;
  border: 1px solid rgba(214, 223, 235, 0.96);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 22px 48px rgba(166, 180, 204, 0.22);
  padding: 10px;
}

.plagiarism-launch-select-popper .el-popper__arrow::before {
  border: 1px solid rgba(214, 223, 235, 0.96);
  background: rgba(255, 255, 255, 0.98);
}

.plagiarism-launch-select-popper .el-select-dropdown__item {
  min-height: 48px;
  border-radius: 16px;
  padding: 0 18px;
  color: #24324a;
  font-size: 17px;
  font-weight: 600;
  line-height: 48px;
}

.plagiarism-launch-select-popper .el-select-dropdown__item.is-hovering {
  background: rgba(241, 245, 255, 0.9);
}

.plagiarism-launch-select-popper .el-select-dropdown__item.is-selected {
  background: rgba(232, 239, 255, 0.95);
  color: #3d5ddc;
}
</style>

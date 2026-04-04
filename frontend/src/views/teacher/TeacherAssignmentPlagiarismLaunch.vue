<template>
  <section class="plagiarism-launch-page">
    <header class="plagiarism-launch-page__hero">
      <div>
        <p class="plagiarism-launch-page__eyebrow">Launch Console</p>
        <h1>发起查重</h1>
        <p>{{ assignment?.title || '当前作业' }}：先了解系统，再设置参数，最后确认后再开始。</p>
      </div>
    </header>

    <nav class="plagiarism-launch-steps" aria-label="查重流程">
      <button
        v-for="(step, index) in steps"
        :key="step.key"
        type="button"
        class="plagiarism-launch-steps__item"
        :class="{ active: currentStepIndex === index, done: currentStepIndex > index }"
        @click="jumpToStep(index)"
      >
        <span class="plagiarism-launch-steps__index">{{ index + 1 }}</span>
        <strong>{{ step.label }}</strong>
      </button>
    </nav>

    <section class="plagiarism-launch-wizard">
      <div class="plagiarism-launch-wizard__viewport">
        <div class="plagiarism-launch-wizard__track" :style="wizardTrackStyle">
          <article class="plagiarism-launch-slide plagiarism-launch-slide--intro">
            <div class="plagiarism-launch-slide__main">
              <div>
                <p class="plagiarism-launch-slide__lead">开始前，先用老师能看懂的话把这套系统讲清楚。</p>
                <h2>这次查重会做什么</h2>
                <p class="plagiarism-launch-slide__description">
                  系统会先把学生代码整理成结构特征，再比较彼此是否接近，帮助老师更快找到值得重点查看的结果。
                </p>
              </div>

              <div class="plagiarism-launch-glossary">
                <article v-for="item in glossaryItems" :key="item.title" class="plagiarism-launch-glossary__item">
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.content }}</p>
                </article>
              </div>

              <div class="plagiarism-launch-slide__cta">
                <button type="button" class="cssbuttons-io-button" @click="goNextStep">
                  发起查重
                  <span class="icon">
                    <svg viewBox="0 0 320 512" aria-hidden="true">
                      <path
                        d="M285.5 273l-194 194c-9.4 9.4-24.6 9.4-33.9 0l-22.6-22.6c-9.5-9.5-9.3-25 .4-34.3L188.5 256 35.4 102.9c-9.8-9.3-10-24.8-.4-34.3L57.6 46c9.4-9.4 24.6-9.4 33.9 0l194 194c9.3 9.4 9.3 24.6 0 34z"
                      />
                    </svg>
                  </span>
                </button>
              </div>

              <div class="plagiarism-launch-slide__footer plagiarism-launch-slide__footer--center">
                <button type="button" class="wizard-link-button" @click="goNextStep">
                  <p data-text="下一步">下一步</p>
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" aria-hidden="true">
                    <path
                      fill="currentColor"
                      d="M438.6 278.6c12.5-12.5 12.5-32.8 0-45.3l-160-160c-12.5-12.5-32.8-12.5-45.3 0s-12.5 32.8 0 45.3L338.7 224H32c-17.7 0-32 14.3-32 32s14.3 32 32 32h306.7L233.4 393.4c-12.5 12.5-12.5 32.8 0 45.3s32.8 12.5 45.3 0l160-160z"
                    />
                  </svg>
                </button>
              </div>
            </div>
          </article>

          <article class="plagiarism-launch-slide">
            <div class="plagiarism-launch-slide__main">
              <div class="plagiarism-launch-slide__header">
                <div>
                  <h2>设置参数</h2>
                  <p>参数不多，只保留老师真正会用到的内容。</p>
                </div>
                <div class="plagiarism-launch-slide__recommend">
                  <span>推荐值</span>
                  <strong>最低分数 80 · 每人保留 10</strong>
                </div>
              </div>

              <section class="plagiarism-launch-scope">
                <div class="plagiarism-launch-scope__title">
                  <strong>本次查重班级</strong>
                  <TeacherExplainTip
                    title="查重班级"
                    content="当前版本会对这份作业已发布的所有班级分别执行查重，不会把不同班级的学生混在一起比较。"
                    recommendation="当前页面先展示参与范围，后续再扩展成可选班级。"
                  />
                </div>
                <div class="plagiarism-launch-scope__tags">
                  <span v-for="item in assignmentClasses" :key="item.classId" class="plagiarism-launch-scope__tag">
                    {{ item.className }}
                  </span>
                  <span v-if="!assignmentClasses.length" class="plagiarism-launch-scope__tag is-empty">暂无班级信息</span>
                </div>
              </section>

              <div class="plagiarism-launch-params">
                <article class="plagiarism-launch-param-card">
                  <div class="plagiarism-launch-param-card__head">
                    <strong>最低分数</strong>
                    <TeacherExplainTip
                      title="最低分数"
                      content="它决定结果页先显示哪些结果。值越高，结果越少，但通常更值得老师优先查看。"
                      recommendation="推荐值：80"
                    />
                  </div>
                  <el-input-number v-model="form.thresholdScore" :min="0" :max="100" />
                  <p>适合先聚焦明显可疑的结果。</p>
                </article>

                <article class="plagiarism-launch-param-card">
                  <div class="plagiarism-launch-param-card__head">
                    <strong>每人保留</strong>
                    <TeacherExplainTip
                      title="每人保留"
                      content="表示每个学生最多保留多少条最值得老师查看的结果，避免结果页太长。"
                      recommendation="推荐值：10"
                    />
                  </div>
                  <el-input-number v-model="form.topKPerStudent" :min="1" :max="50" />
                  <p>适合控制结果页长度，减轻老师负担。</p>
                </article>
              </div>

              <div class="plagiarism-launch-slide__footer plagiarism-launch-slide__footer--spread">
                <el-button round @click="goPrevStep">上一步</el-button>
                <button type="button" class="wizard-link-button" @click="goNextStep">
                  <p data-text="下一步">下一步</p>
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" aria-hidden="true">
                    <path
                      fill="currentColor"
                      d="M438.6 278.6c12.5-12.5 12.5-32.8 0-45.3l-160-160c-12.5-12.5-32.8-12.5-45.3 0s-12.5 32.8 0 45.3L338.7 224H32c-17.7 0-32 14.3-32 32s14.3 32 32 32h306.7L233.4 393.4c-12.5 12.5-12.5 32.8 0 45.3s32.8 12.5 45.3 0l160-160z"
                    />
                  </svg>
                </button>
              </div>
            </div>
          </article>

          <article class="plagiarism-launch-slide">
            <div class="plagiarism-launch-slide__main">
              <div>
                <h2>确认总览</h2>
                <p class="plagiarism-launch-slide__description">最后再看一遍，确认无误后就开始查重。</p>
              </div>

              <div class="plagiarism-launch-summary">
                <article class="plagiarism-launch-summary__card">
                  <span>作业名称</span>
                  <strong>{{ assignment?.title || '未选择作业' }}</strong>
                </article>
                <article class="plagiarism-launch-summary__card">
                  <span>参与班级</span>
                  <strong>{{ assignmentClasses.length }} 个班级</strong>
                </article>
                <article class="plagiarism-launch-summary__card">
                  <span>最低分数</span>
                  <strong>{{ form.thresholdScore }}</strong>
                </article>
                <article class="plagiarism-launch-summary__card">
                  <span>每人保留</span>
                  <strong>{{ form.topKPerStudent }}</strong>
                </article>
                <article class="plagiarism-launch-summary__card">
                  <span>最近任务</span>
                  <strong>{{ jobs.length ? `任务 #${jobs[0].id}` : '暂无任务' }}</strong>
                </article>
                <article class="plagiarism-launch-summary__card">
                  <span>最近状态</span>
                  <strong>{{ jobs.length ? jobStatusLabel(jobs[0].status) : '未执行' }}</strong>
                </article>
              </div>

              <div class="plagiarism-launch-summary__note">
                系统会按班级分别执行比较，并把结果整理到“查看结果”页。AI 只做解释，不参与最终判定。
              </div>

              <div class="plagiarism-launch-slide__footer plagiarism-launch-slide__footer--spread">
                <el-button round @click="goPrevStep">上一步</el-button>
                <el-button type="primary" round :loading="submitting" @click="handleCreatePlagiarismJob">确认并发起</el-button>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createTeacherPlagiarismJob,
  fetchTeacherAssignments,
  fetchTeacherAssignmentDetail,
  fetchTeacherPlagiarismJobs
} from '../../api/teacherAssignments'
import TeacherExplainTip from './components/TeacherExplainTip.vue'
import { useTeacherPlagiarismMonitor } from '../../composables/useTeacherPlagiarismMonitor'

const route = useRoute()
const router = useRouter()
const { registerTeacherPlagiarismJobs, trackTeacherPlagiarismJob } = useTeacherPlagiarismMonitor()

const steps = [
  { key: 'intro', label: '认识查重' },
  { key: 'params', label: '设置参数' },
  { key: 'confirm', label: '确认发起' }
]

const glossaryItems = [
  { title: '相似度', content: '分数越高，说明两份代码在整体结构上越接近。' },
  { title: '最低分数', content: '决定结果页优先显示哪些结果，适合先过滤掉太低的结果。' },
  { title: '每人保留', content: '限制每个学生最多保留多少条结果，避免老师被长列表打扰。' },
  { title: 'AI 解释', content: 'AI 只帮老师解释结果，不直接替老师做最终判断。' }
]

const assignmentOptions = ref([])
const assignment = ref(null)
const jobs = ref([])
const selectedAssignmentId = ref(String(route.params.assignmentId || ''))
const currentStepIndex = ref(0)
const submitting = ref(false)

const form = ref({
  thresholdScore: 80,
  topKPerStudent: 10
})

const assignmentClasses = computed(() => assignment.value?.classes || [])
const wizardTrackStyle = computed(() => ({
  transform: `translateX(-${currentStepIndex.value * 100}%)`
}))

watch(
  () => route.params.assignmentId,
  async (value) => {
    selectedAssignmentId.value = String(value || '')
    currentStepIndex.value = 0
    await loadPage()
  }
)

onMounted(async () => {
  await loadAssignmentOptions()
  await loadPage()
})

async function loadAssignmentOptions() {
  const result = await fetchTeacherAssignments({ page: 1, size: 100 })
  assignmentOptions.value = result.records || []
}

async function loadPage() {
  const assignmentId = await resolveAssignmentId()
  if (!assignmentId) {
    assignment.value = null
    jobs.value = []
    return
  }

  const [detail, jobList] = await Promise.all([
    fetchTeacherAssignmentDetail(assignmentId),
    fetchTeacherPlagiarismJobs(assignmentId)
  ])

  assignment.value = detail
  jobs.value = jobList
  selectedAssignmentId.value = String(assignmentId)
  registerTeacherPlagiarismJobs({
    assignmentId,
    assignmentTitle: detail.title,
    jobs: jobList
  })
}

async function resolveAssignmentId() {
  const routeAssignmentId = String(route.params.assignmentId || '').trim()
  if (routeAssignmentId) return routeAssignmentId
  if (!assignmentOptions.value.length) return ''
  const fallbackId = String(selectedAssignmentId.value || assignmentOptions.value[0]?.id || '')
  if (!fallbackId) return ''
  if (route.path === '/teacher/assignments/plagiarism/run') {
    await router.replace(`/teacher/assignments/${fallbackId}/plagiarism/run`)
  }
  return fallbackId
}

function jumpToStep(index) {
  currentStepIndex.value = index
}

function goNextStep() {
  currentStepIndex.value = Math.min(currentStepIndex.value + 1, steps.length - 1)
}

function goPrevStep() {
  currentStepIndex.value = Math.max(currentStepIndex.value - 1, 0)
}

async function handleCreatePlagiarismJob() {
  if (!assignment.value?.id) return
  submitting.value = true
  try {
    const job = await createTeacherPlagiarismJob(assignment.value.id, form.value)
    trackTeacherPlagiarismJob({
      assignmentId: assignment.value.id,
      assignmentTitle: assignment.value.title,
      job
    })
    ElMessage.success(`已创建查重任务 #${job.id}`)
    router.push(`/teacher/assignments/${assignment.value.id}/plagiarism/results`)
  } finally {
    submitting.value = false
  }
}

function jobStatusLabel(status) {
  const map = {
    QUEUED: '等待执行',
    RUNNING: '正在查重',
    DONE: '查重完成',
    FAILED: '执行失败'
  }
  return map[status] || status || '-'
}
</script>

<style scoped>
.plagiarism-launch-page {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 14px;
  padding: 20px 22px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top left, rgba(110, 93, 255, 0.1), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(246, 249, 255, 0.93));
  border: 1px solid rgba(207, 217, 235, 0.6);
  overflow: hidden;
}

.plagiarism-launch-page__eyebrow {
  margin: 0 0 8px;
  color: #785fff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.plagiarism-launch-page__hero h1,
.plagiarism-launch-slide h2 {
  margin: 0;
  color: #121826;
}

.plagiarism-launch-page__hero h1 {
  font-size: 34px;
}

.plagiarism-launch-page__hero p,
.plagiarism-launch-slide__description,
.plagiarism-launch-slide__lead,
.plagiarism-launch-glossary__item p,
.plagiarism-launch-param-card p,
.plagiarism-launch-summary__note {
  margin: 8px 0 0;
  color: #70819a;
  line-height: 1.6;
}

.plagiarism-launch-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  align-items: center;
}

.plagiarism-launch-steps__item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  border: 1px solid rgba(207, 217, 235, 0.72);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.86);
  cursor: pointer;
  text-align: left;
}

.plagiarism-launch-steps__item:not(:last-child)::after {
  content: '→';
  position: absolute;
  right: -18px;
  top: 50%;
  transform: translateY(-50%);
  color: #a18adb;
  font-size: 20px;
  font-weight: 700;
}

.plagiarism-launch-steps__index {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(18, 24, 38, 0.08);
  color: #121826;
  font-weight: 700;
}

.plagiarism-launch-steps__item strong {
  color: #42526a;
  font-size: 16px;
}

.plagiarism-launch-steps__item.active,
.plagiarism-launch-steps__item.done {
  border-color: rgba(123, 82, 185, 0.22);
  background: rgba(163, 112, 240, 0.1);
}

.plagiarism-launch-wizard {
  min-height: 0;
  overflow: hidden;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(207, 217, 235, 0.62);
  box-shadow: 0 14px 34px rgba(179, 191, 216, 0.12);
}

.plagiarism-launch-wizard__viewport {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.plagiarism-launch-wizard__track {
  display: flex;
  width: 300%;
  height: 100%;
  transition: transform 0.4s ease;
}

.plagiarism-launch-slide {
  width: 100%;
  min-width: 100%;
  height: 100%;
  padding: 28px;
}

.plagiarism-launch-slide__main {
  height: 100%;
  display: grid;
  grid-template-rows: auto auto auto 1fr auto;
  gap: 18px;
}

.plagiarism-launch-slide--intro .plagiarism-launch-slide__main {
  align-content: stretch;
}

.plagiarism-launch-slide__lead {
  margin: 0;
  color: #7b52b9;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.plagiarism-launch-glossary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.plagiarism-launch-glossary__item,
.plagiarism-launch-param-card,
.plagiarism-launch-summary__card,
.plagiarism-launch-scope {
  padding: 18px;
  border-radius: 22px;
  background: rgba(248, 250, 255, 0.92);
  border: 1px solid rgba(207, 217, 235, 0.56);
}

.plagiarism-launch-glossary__item strong,
.plagiarism-launch-param-card strong,
.plagiarism-launch-summary__card strong,
.plagiarism-launch-scope__title strong {
  color: #121826;
}

.plagiarism-launch-slide__cta {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

.cssbuttons-io-button {
  background: #a370f0;
  color: white;
  font-family: inherit;
  padding: 0.35em;
  padding-left: 1.2em;
  font-size: 17px;
  font-weight: 500;
  border-radius: 0.9em;
  border: none;
  letter-spacing: 0.05em;
  display: flex;
  align-items: center;
  box-shadow: inset 0 0 1.6em -0.6em #714da6;
  overflow: hidden;
  position: relative;
  height: 2.8em;
  padding-right: 3.3em;
  cursor: pointer;
}

.cssbuttons-io-button .icon {
  background: white;
  margin-left: 1em;
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 2.2em;
  width: 2.2em;
  border-radius: 0.7em;
  box-shadow: 0.1em 0.1em 0.6em 0.2em #7b52b9;
  right: 0.3em;
  transition: all 0.3s;
}

.cssbuttons-io-button:hover .icon {
  width: calc(100% - 0.6em);
}

.cssbuttons-io-button .icon svg {
  width: 1.1em;
  transition: transform 0.3s;
  color: #7b52b9;
  fill: currentColor;
}

.cssbuttons-io-button:hover .icon svg {
  transform: translateX(0.1em);
}

.cssbuttons-io-button:active .icon {
  transform: scale(0.95);
}

.plagiarism-launch-slide__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.plagiarism-launch-slide__recommend {
  display: grid;
  justify-items: end;
  gap: 4px;
}

.plagiarism-launch-slide__recommend span,
.plagiarism-launch-param-card p,
.plagiarism-launch-summary__card span,
.plagiarism-launch-summary__note,
.plagiarism-launch-scope__tag.is-empty {
  color: #7d8ca3;
}

.plagiarism-launch-slide__recommend strong {
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(163, 112, 240, 0.12);
  color: #6a45c8;
}

.plagiarism-launch-scope {
  display: grid;
  gap: 12px;
}

.plagiarism-launch-scope__title,
.plagiarism-launch-param-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.plagiarism-launch-scope__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.plagiarism-launch-scope__tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(18, 24, 38, 0.06);
  color: #42526a;
  font-weight: 600;
}

.plagiarism-launch-params {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.plagiarism-launch-param-card {
  display: grid;
  gap: 12px;
}

.plagiarism-launch-slide__footer {
  display: flex;
  align-items: center;
  margin-top: auto;
}

.plagiarism-launch-slide__footer--center {
  justify-content: center;
}

.plagiarism-launch-slide__footer--spread {
  justify-content: space-between;
}

.wizard-link-button {
  padding: 0;
  margin: 0;
  border: none;
  background: none;
  cursor: pointer;
  --primary-color: #111;
  --hovered-color: #c84747;
  position: relative;
  display: flex;
  font-weight: 600;
  font-size: 20px;
  gap: 0.5rem;
  align-items: center;
}

.wizard-link-button p {
  margin: 0;
  position: relative;
  font-size: 20px;
  color: var(--primary-color);
}

.wizard-link-button::after {
  position: absolute;
  content: '';
  width: 0;
  left: 0;
  bottom: -7px;
  background: var(--hovered-color);
  height: 2px;
  transition: 0.3s ease-out;
}

.wizard-link-button p::before {
  position: absolute;
  content: attr(data-text);
  width: 0%;
  inset: 0;
  color: var(--hovered-color);
  overflow: hidden;
  transition: 0.3s ease-out;
}

.wizard-link-button:hover::after {
  width: 100%;
}

.wizard-link-button:hover p::before {
  width: 100%;
}

.wizard-link-button:hover svg {
  transform: translateX(4px);
  color: var(--hovered-color);
}

.wizard-link-button svg {
  color: var(--primary-color);
  position: relative;
  width: 15px;
  transition: 0.2s;
  transition-delay: 0.2s;
}

.plagiarism-launch-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.plagiarism-launch-summary__card {
  display: grid;
  gap: 8px;
}

.plagiarism-launch-summary__note {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(18, 24, 38, 0.04);
}

@media (max-width: 1320px) {
  .plagiarism-launch-steps,
  .plagiarism-launch-glossary,
  .plagiarism-launch-params,
  .plagiarism-launch-summary {
    grid-template-columns: 1fr;
  }

  .plagiarism-launch-slide__header,
  .plagiarism-launch-slide__footer--spread {
    flex-direction: column;
    align-items: flex-start;
  }

  .plagiarism-launch-steps__item:not(:last-child)::after {
    content: '↓';
    right: auto;
    left: 50%;
    top: auto;
    bottom: -18px;
    transform: translateX(-50%);
  }
}
</style>

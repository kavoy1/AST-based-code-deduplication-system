<template>
  <div class="workspace-page student-assignment-detail-page">
    <WorkspacePanel soft>
      <div v-if="loading" class="workspace-empty">
        <LoadingSpinner label="正在加载作业详情..." />
      </div>

      <WorkspaceEmpty
        v-else-if="!assignment.id"
        title="未找到作业"
        description="当前作业不存在，或你暂时没有访问权限。"
      />

      <template v-else>
        <div class="student-assignment-detail__hero">
          <div class="student-assignment-detail__hero-main">
            <div class="student-assignment-detail__hero-head">
              <AppBackButton label="返回我的作业" @click="goBack" />
              <div class="student-assignment-detail__hero-copy">
                <p class="student-assignment-detail__eyebrow">{{ className || '当前班级' }} · {{ assignment.language }}</p>
                <h1>{{ assignment.title }}</h1>
              </div>
            </div>
            <p>{{ assignment.description || '当前作业暂无详细说明，请按照要求完成并按时提交。' }}</p>
          </div>
          <div class="student-assignment-detail__hero-side">
            <el-tag :type="assignment.statusTagType" round size="large">{{ assignment.statusText }}</el-tag>
          </div>
        </div>

        <div class="student-assignment-detail__meta-grid">
          <article class="student-assignment-detail__meta-card">
            <span>开始时间</span>
            <strong>{{ assignment.startAtLabel }}</strong>
          </article>
          <article class="student-assignment-detail__meta-card">
            <span>截止时间</span>
            <strong>{{ assignment.endAtLabel }}</strong>
          </article>
          <article class="student-assignment-detail__meta-card">
            <span>文件上限</span>
            <strong>最多 {{ assignment.maxFiles }} 个</strong>
          </article>
          <article class="student-assignment-detail__meta-card">
            <span>提交规则</span>
            <strong>{{ assignment.allowResubmit ? '允许覆盖提交' : '仅允许提交一次' }}</strong>
          </article>
        </div>

        <div class="student-assignment-detail__submit-bar" :class="{ 'student-assignment-detail__submit-bar--c': submissionPolicy.language === 'C' }">
          <div>
            <h3>准备好后就可以去提交</h3>
            <p>进入提交页后，你可以选择上传文件或直接编辑文本，系统只保留当前最后一份提交。</p>
          </div>
          <div class="student-assignment-detail__submit-hint">{{ submissionPolicy.detailGuidance }}</div>
          <el-button type="primary" round :disabled="!assignment.canSubmit" @click="goSubmit">
            {{ assignment.canSubmit ? '前往提交' : '当前不可提交' }}
          </el-button>
        </div>
      </template>
    </WorkspacePanel>

    <div class="student-assignment-detail__grid">
      <WorkspacePanel title="个人查重摘要" subtitle="老师完成查重后，这里会展示与你相关的摘要结果。" compact>
        <div v-if="summaryLoading" class="workspace-empty">
          <LoadingSpinner label="正在加载查重摘要..." />
        </div>

        <div v-else class="student-summary-card">
          <template v-if="plagiarismSummary.generated">
            <div class="student-summary-card__score">{{ plagiarismSummary.highestScore }}%</div>
            <div class="student-summary-card__status">{{ plagiarismSummary.status || '已生成摘要' }}</div>
            <p>{{ plagiarismSummary.message || '老师已完成当前作业查重，你可以先查看摘要结果。' }}</p>
            <div v-if="plagiarismSummary.teacherNote" class="student-summary-card__note">
              教师备注：{{ plagiarismSummary.teacherNote }}
            </div>
          </template>

          <WorkspaceEmpty
            v-else
            title="暂无查重摘要"
            :description="plagiarismSummary.message || '老师尚未完成当前作业查重任务。'"
          />
        </div>
      </WorkspacePanel>

      <WorkspacePanel title="当前提交摘要" subtitle="这里统一展示你的最后一次提交状态，完整代码需要点击文件查看。" compact>
        <div v-if="submissionLoading" class="workspace-empty">
          <LoadingSpinner label="正在加载提交摘要..." />
        </div>

        <div v-else class="student-current-card">
          <div class="student-current-card__grid">
            <article class="student-current-card__stat">
              <span>当前状态</span>
              <strong>{{ assignment.canSubmit ? '可以提交' : '当前不可提交' }}</strong>
            </article>
            <article class="student-current-card__stat">
              <span>允许覆盖</span>
              <strong>{{ assignment.allowResubmit ? '是' : '否' }}</strong>
            </article>
            <article class="student-current-card__stat">
              <span>最后提交时间</span>
              <strong>{{ currentSubmission?.submitTimeLabel || '暂无提交' }}</strong>
            </article>
            <article class="student-current-card__stat">
              <span>解析结果</span>
              <strong>{{ currentSubmission ? `${currentSubmission.parseOkFiles}/${currentSubmission.totalFiles}` : '-' }}</strong>
            </article>
            <article class="student-current-card__stat">
              <span>文件数</span>
              <strong>{{ currentSubmission?.totalFiles || 0 }}</strong>
            </article>
            <article class="student-current-card__stat">
              <span>提交结果</span>
              <strong>{{ currentSubmission ? (currentSubmission.isLate ? '迟交' : '按时提交') : '尚未提交' }}</strong>
            </article>
          </div>

          <WorkspaceEmpty
            v-if="!currentSubmission"
            title="暂无当前提交"
            description="你还没有提交过当前作业。"
          />

          <div v-else class="student-current-card__files">
            <div class="student-current-card__files-head">
              <div>
                <h4>已保留文件</h4>
                <p>点击文件卡片查看自己最后一次提交的完整代码。</p>
              </div>
            </div>

            <button
              v-for="file in submissionFileCards"
              :key="file.filename"
              type="button"
              class="student-current-card__file"
              @click="openFilePreview(file)"
            >
              <div class="student-current-card__file-head">
                <strong>{{ file.filename }}</strong>
                <el-tag size="small" :type="file.parseTone">{{ file.parseLabel }}</el-tag>
              </div>

              <div class="student-current-card__file-meta">
                <span>{{ file.bytesLabel }}</span>
                <span>{{ file.hasContent ? '点击查看完整代码' : '无可查看代码' }}</span>
              </div>

              <p>{{ file.preview }}</p>
            </button>
          </div>
        </div>
      </WorkspacePanel>
    </div>

    <el-drawer
      v-model="codePreviewVisible"
      :title="selectedSubmissionFile?.filename || '代码预览'"
      size="52%"
      destroy-on-close
    >
      <div v-if="selectedSubmissionFile" class="student-code-preview">
        <div class="student-code-preview__meta">
          <el-tag size="small" :type="selectedSubmissionFile.parseTone">{{ selectedSubmissionFile.parseLabel }}</el-tag>
          <span>{{ selectedSubmissionFile.bytesLabel }}</span>
        </div>

        <div v-if="selectedSubmissionFile.parseError" class="student-code-preview__error">
          {{ selectedSubmissionFile.parseError }}
        </div>

        <pre v-else class="student-code-preview__content"><code>{{ selectedSubmissionFile.content || '暂无代码内容' }}</code></pre>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchStudentAssignmentDetail,
  fetchStudentCurrentSubmission,
  fetchStudentPlagiarismSummary
} from '../../api/student'
import AppBackButton from '../../components/AppBackButton.vue'
import LoadingSpinner from '../../components/LoadingSpinner.vue'
import WorkspaceEmpty from '../../components/workspace/WorkspaceEmpty.vue'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import { buildSubmissionFileCards } from './studentSubmissionHelpers'
import { buildStudentSubmissionPolicy } from './studentSubmissionRules'

const route = useRoute()
const router = useRouter()

const assignment = ref({})
const currentSubmission = ref(null)
const plagiarismSummary = ref({
  generated: false,
  message: '',
  highestScore: 0,
  status: '',
  teacherNote: ''
})
const loading = ref(false)
const summaryLoading = ref(false)
const submissionLoading = ref(false)
const codePreviewVisible = ref(false)
const selectedSubmissionFile = ref(null)

const assignmentId = computed(() => route.params.assignmentId)
const classId = computed(() => route.query.classId || '')
const className = computed(() => route.query.className || '')
const submissionFileCards = computed(() => buildSubmissionFileCards(currentSubmission.value))
const submissionPolicy = computed(() => buildStudentSubmissionPolicy(assignment.value))

async function loadAssignment() {
  loading.value = true
  try {
    const detail = await fetchStudentAssignmentDetail(assignmentId.value, {
      classId: classId.value,
      className: className.value
    })
    assignment.value = {
      ...detail,
      maxFiles: buildStudentSubmissionPolicy(detail).effectiveFileLimit
    }
  } finally {
    loading.value = false
  }
}

async function loadCurrentSubmission() {
  submissionLoading.value = true
  try {
    currentSubmission.value = await fetchStudentCurrentSubmission(assignmentId.value)
    selectedSubmissionFile.value = buildSubmissionFileCards(currentSubmission.value)[0] || null
  } finally {
    submissionLoading.value = false
  }
}

async function loadPlagiarismSummary() {
  summaryLoading.value = true
  try {
    plagiarismSummary.value = await fetchStudentPlagiarismSummary(assignmentId.value)
  } catch (error) {
    plagiarismSummary.value = {
      generated: false,
      message: typeof error === 'string' ? error : '老师尚未完成当前作业查重任务。',
      highestScore: 0,
      status: '',
      teacherNote: ''
    }
  } finally {
    summaryLoading.value = false
  }
}

function openFilePreview(file) {
  selectedSubmissionFile.value = file
  codePreviewVisible.value = true
}

function goBack() {
  if (classId.value) {
    router.push({
      path: '/student/tasks',
      query: {
        classId: classId.value,
        className: className.value
      }
    })
    return
  }
  router.push('/student/tasks')
}

function goSubmit() {
  router.push({
    path: `/student/assignments/${assignmentId.value}/submit`,
    query: {
      classId: classId.value,
      className: className.value
    }
  })
}

onMounted(async () => {
  await Promise.all([
    loadAssignment(),
    loadCurrentSubmission(),
    loadPlagiarismSummary()
  ])
})
</script>

<style scoped>
.student-assignment-detail-page {
  gap: 16px;
}

.student-assignment-detail__hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.student-assignment-detail__hero-main {
  display: grid;
  gap: 10px;
  max-width: 880px;
}

.student-assignment-detail__hero-head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.student-assignment-detail__hero-copy {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.student-assignment-detail__eyebrow {
  margin: 0;
  color: #456da8;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.student-assignment-detail__hero-copy h1 {
  margin: 0;
  font-size: 40px;
  line-height: 1.04;
}

.student-assignment-detail__hero-main p {
  margin: 0;
  color: var(--text-body);
  font-size: 16px;
  line-height: 1.65;
}

.student-assignment-detail__hero-side {
  display: grid;
  justify-items: end;
  align-content: start;
  gap: 14px;
}

.student-assignment-detail__meta-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.student-assignment-detail__meta-card,
.student-current-card__stat {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(109, 128, 166, 0.12);
}

.student-assignment-detail__meta-card span,
.student-current-card__stat span {
  color: var(--text-muted);
}

.student-assignment-detail__meta-card strong,
.student-current-card__stat strong {
  font-size: 20px;
  line-height: 1.2;
}

.student-assignment-detail__submit-bar {
  margin-top: 18px;
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px solid rgba(71, 115, 173, 0.14);
  background: linear-gradient(135deg, rgba(71, 115, 173, 0.08), rgba(255, 255, 255, 0.9));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.student-assignment-detail__submit-bar h3,
.student-assignment-detail__submit-bar p {
  margin: 0;
}

.student-assignment-detail__submit-bar h3 {
  margin-bottom: 6px;
  font-size: 18px;
}

.student-assignment-detail__submit-bar p {
  color: var(--text-body);
  line-height: 1.55;
}

.student-assignment-detail__submit-bar--c p:first-of-type {
  display: none;
}

.student-assignment-detail__submit-hint {
  max-width: 340px;
  padding: 10px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  color: #36527a;
  line-height: 1.6;
}

.student-assignment-detail__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(340px, 0.95fr);
  gap: 16px;
  align-items: start;
}

.student-summary-card {
  display: grid;
  align-content: start;
  gap: 12px;
}

.student-summary-card__score {
  font-size: 56px;
  font-weight: 800;
  line-height: 1;
  color: var(--text-title);
}

.student-summary-card__status {
  color: #456da8;
  font-weight: 700;
}

.student-summary-card p,
.student-summary-card__note {
  margin: 0;
  color: var(--text-body);
  line-height: 1.75;
}

.student-summary-card__note {
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(71, 115, 173, 0.08);
}

.student-current-card {
  display: grid;
  gap: 12px;
}

.student-current-card__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.student-current-card__files {
  display: grid;
  gap: 10px;
}

.student-current-card__files-head h4,
.student-current-card__files-head p {
  margin: 0;
}

.student-current-card__files-head h4 {
  font-size: 18px;
}

.student-current-card__files-head p {
  margin-top: 6px;
  color: var(--text-muted);
  line-height: 1.5;
}

.student-current-card__file {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(109, 128, 166, 0.14);
  background: rgba(255, 255, 255, 0.78);
  display: grid;
  gap: 10px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.student-current-card__file:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 26px rgba(100, 118, 152, 0.1);
  border-color: rgba(71, 115, 173, 0.18);
}

.student-current-card__file-head,
.student-current-card__file-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.student-current-card__file-meta {
  color: var(--text-muted);
  font-size: 13px;
}

.student-current-card__file p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.6;
}

.student-code-preview {
  display: grid;
  gap: 14px;
  min-height: 100%;
}

.student-code-preview__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-muted);
  font-size: 13px;
}

.student-code-preview__error {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(250, 232, 232, 0.86);
  color: #b14d4d;
  line-height: 1.6;
}

.student-code-preview__content {
  margin: 0;
  padding: 18px;
  border-radius: 20px;
  background: #101827;
  color: #f3f7ff;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  overflow: auto;
}

.student-summary-card :deep(.workspace-empty),
.student-current-card :deep(.workspace-empty) {
  min-height: 116px;
}

.student-summary-card :deep(.workspace-empty h3),
.student-current-card :deep(.workspace-empty h3) {
  margin-bottom: 6px;
  font-size: 17px;
}

.student-summary-card :deep(.workspace-empty p),
.student-current-card :deep(.workspace-empty p) {
  line-height: 1.5;
}

@media (max-width: 1280px) {
  .student-assignment-detail__meta-grid,
  .student-current-card__grid,
  .student-assignment-detail__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .student-assignment-detail__hero,
  .student-assignment-detail__submit-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .student-assignment-detail__hero-head {
    align-items: flex-start;
  }

  .student-assignment-detail__hero-side {
    justify-items: start;
  }

  .student-assignment-detail__hero-copy h1 {
    font-size: 36px;
  }
}
</style>

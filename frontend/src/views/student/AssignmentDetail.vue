<template>
  <div class="workspace-page student-assignment-detail-page">
    <WorkspaceShellSection>
      <template #tools>
        <AppBackButton label="返回我的作业" @click="goBack" />
      </template>
    </WorkspaceShellSection>

    <WorkspacePanel soft>
      <div v-if="loading" class="workspace-empty">
        <el-skeleton :rows="6" animated />
      </div>

      <WorkspaceEmpty
        v-else-if="!assignment.id"
        title="未找到作业"
        description="当前作业不存在，或者你暂时没有访问权限。"
      />

      <template v-else>
        <div class="student-assignment-detail__hero">
          <div class="student-assignment-detail__hero-main">
            <div class="student-assignment-detail__eyebrow">{{ className || '当前班级' }} · {{ assignment.language }}</div>
            <h1>{{ assignment.title }}</h1>
            <p>{{ assignment.description || '当前作业暂无详细说明，请按要求完成并及时提交。' }}</p>
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
            <strong>{{ assignment.allowResubmit ? '允许重复提交' : '仅允许提交一次' }}</strong>
          </article>
        </div>

        <div class="student-assignment-detail__submit-bar">
          <div>
            <h3>准备好后就可以去提交</h3>
            <p>先确认作业要求，再进入独立的提交页上传文件或填写多份文本代码。</p>
          </div>
          <el-button type="primary" round :disabled="!assignment.canSubmit" @click="goSubmit">
            {{ assignment.canSubmit ? '提交作业' : '当前不可提交' }}
          </el-button>
        </div>
      </template>
    </WorkspacePanel>

    <div class="student-assignment-detail__grid">
      <WorkspacePanel title="个人查重摘要" subtitle="教师完成查重后，这里会展示与你相关的结果摘要。">
        <div v-if="summaryLoading" class="workspace-empty">
          <el-skeleton :rows="4" animated />
        </div>

        <div v-else class="student-summary-card">
          <template v-if="plagiarismSummary.generated">
            <div class="student-summary-card__score">{{ plagiarismSummary.highestScore }}%</div>
            <div class="student-summary-card__status">{{ plagiarismSummary.status || '已生成摘要' }}</div>
            <p>{{ plagiarismSummary.message || '教师已完成当前作业查重，你可以先查看摘要结果。' }}</p>
            <div v-if="plagiarismSummary.teacherNote" class="student-summary-card__note">
              教师备注：{{ plagiarismSummary.teacherNote }}
            </div>
          </template>
          <WorkspaceEmpty
            v-else
            title="暂无查重摘要"
            :description="plagiarismSummary.message || '教师尚未完成当前作业查重任务。'"
          />
        </div>
      </WorkspacePanel>

      <WorkspacePanel title="提交历史" subtitle="这里会保留你的提交版本，方便回看。">
        <div v-if="historyLoading" class="workspace-empty">
          <el-skeleton :rows="5" animated />
        </div>
        <WorkspaceEmpty
          v-else-if="submissionHistory.length === 0"
          title="暂无提交记录"
          description="你还没有提交过当前作业。"
        />
        <div v-else class="student-history-list">
          <article v-for="item in submissionHistory" :key="item.id" class="student-history-card">
            <div class="student-history-card__header">
              <div>
                <h4>第 {{ item.version }} 次提交</h4>
                <p>{{ item.submitTimeLabel }}</p>
              </div>
              <div class="student-history-card__badges">
                <span class="workspace-badge-soft" :class="item.isLatest ? 'workspace-badge-soft--green' : ''">
                  {{ item.isLatest ? '当前版本' : '历史版本' }}
                </span>
                <span class="workspace-badge-soft" :class="item.isValid ? 'workspace-badge-soft--blue' : 'workspace-badge-soft--danger'">
                  {{ item.isValid ? '有效' : '无效' }}
                </span>
              </div>
            </div>

            <div class="student-history-card__meta">
              <span>文件解析 {{ item.parseOkFiles }}/{{ item.totalFiles }}</span>
              <span>{{ item.isLate ? '迟交' : '按时提交' }}</span>
              <span>截止：{{ item.deadlineAtLabel }}</span>
            </div>
          </article>
        </div>
      </WorkspacePanel>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchStudentAssignmentDetail,
  fetchStudentPlagiarismSummary,
  fetchStudentSubmissionHistory
} from '../../api/student'
import AppBackButton from '../../components/AppBackButton.vue'
import WorkspaceEmpty from '../../components/workspace/WorkspaceEmpty.vue'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import WorkspaceShellSection from '../../components/workspace/WorkspaceShellSection.vue'

const route = useRoute()
const router = useRouter()

const assignment = ref({})
const plagiarismSummary = ref({
  generated: false,
  message: '',
  highestScore: 0,
  status: '',
  teacherNote: ''
})
const submissionHistory = ref([])
const loading = ref(false)
const summaryLoading = ref(false)
const historyLoading = ref(false)

const assignmentId = computed(() => route.params.assignmentId)
const classId = computed(() => route.query.classId || '')
const className = computed(() => route.query.className || '')

async function loadAssignment() {
  loading.value = true
  try {
    assignment.value = await fetchStudentAssignmentDetail(assignmentId.value, {
      classId: classId.value,
      className: className.value
    })
  } finally {
    loading.value = false
  }
}

async function loadSubmissionHistory() {
  historyLoading.value = true
  try {
    submissionHistory.value = await fetchStudentSubmissionHistory(assignmentId.value)
  } finally {
    historyLoading.value = false
  }
}

async function loadPlagiarismSummary() {
  summaryLoading.value = true
  try {
    plagiarismSummary.value = await fetchStudentPlagiarismSummary(assignmentId.value)
  } catch (error) {
    plagiarismSummary.value = {
      generated: false,
      message: typeof error === 'string' ? error : '教师尚未完成当前作业查重任务。',
      highestScore: 0,
      status: '',
      teacherNote: ''
    }
  } finally {
    summaryLoading.value = false
  }
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
    loadSubmissionHistory(),
    loadPlagiarismSummary()
  ])
})
</script>

<style scoped>
.student-assignment-detail-page {
  gap: 20px;
}

.student-assignment-detail__hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 28px;
}

.student-assignment-detail__hero-main {
  display: grid;
  gap: 14px;
  max-width: 880px;
}

.student-assignment-detail__eyebrow {
  color: #6f62d8;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.student-assignment-detail__hero-main h1 {
  margin: 0;
  font-size: 46px;
  line-height: 1.06;
}

.student-assignment-detail__hero-main p {
  margin: 0;
  color: var(--text-body);
  font-size: 18px;
  line-height: 1.8;
}

.student-assignment-detail__hero-side {
  display: grid;
  justify-items: end;
}

.student-assignment-detail__meta-grid {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.student-assignment-detail__meta-card {
  display: grid;
  gap: 10px;
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(109, 128, 166, 0.12);
}

.student-assignment-detail__meta-card span {
  color: var(--text-muted);
}

.student-assignment-detail__meta-card strong {
  font-size: 28px;
  line-height: 1.2;
}

.student-assignment-detail__submit-bar {
  margin-top: 22px;
  padding: 20px 22px;
  border-radius: 24px;
  border: 1px solid rgba(111, 98, 216, 0.12);
  background: linear-gradient(135deg, rgba(111, 98, 216, 0.06), rgba(255, 255, 255, 0.86));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.student-assignment-detail__submit-bar h3,
.student-assignment-detail__submit-bar p {
  margin: 0;
}

.student-assignment-detail__submit-bar h3 {
  margin-bottom: 6px;
  font-size: 20px;
}

.student-assignment-detail__submit-bar p {
  color: var(--text-body);
  line-height: 1.7;
}

.student-assignment-detail__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.85fr);
  gap: 20px;
}

.student-summary-card {
  min-height: 100%;
  display: grid;
  align-content: start;
  gap: 14px;
}

.student-summary-card__score {
  font-size: 56px;
  font-weight: 800;
  line-height: 1;
  color: var(--text-title);
}

.student-summary-card__status {
  color: #6f62d8;
  font-weight: 700;
}

.student-summary-card p,
.student-summary-card__note {
  margin: 0;
  color: var(--text-body);
  line-height: 1.75;
}

.student-summary-card__note {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(111, 98, 216, 0.08);
}

.student-history-list {
  display: grid;
  gap: 12px;
}

.student-history-card {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(109, 128, 166, 0.14);
  background: rgba(255, 255, 255, 0.78);
  display: grid;
  gap: 12px;
}

.student-history-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.student-history-card__header h4,
.student-history-card__header p {
  margin: 0;
}

.student-history-card__header h4 {
  margin-bottom: 6px;
  font-size: 17px;
}

.student-history-card__header p,
.student-history-card__meta {
  color: var(--text-muted);
}

.student-history-card__badges {
  display: flex;
  gap: 8px;
}

.student-history-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
}

@media (max-width: 1280px) {
  .student-assignment-detail__meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .student-assignment-detail__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .student-assignment-detail__hero {
    flex-direction: column;
  }

  .student-assignment-detail__submit-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .student-assignment-detail__hero-main h1 {
    font-size: 36px;
  }
}

@media (max-width: 640px) {
  .student-assignment-detail__meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>

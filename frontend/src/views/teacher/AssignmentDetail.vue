<template>
  <div v-if="assignment" class="assignment-detail-page">
    <section class="assignment-detail-shell">
      <div class="assignment-detail-shell__header">
        <div>
          <AppBackButton label="返回作业总览" @click="goBack" />
          <h1>{{ assignment.title }}</h1>
          <p>{{ assignment.classNamesText }} · {{ assignment.startTime }} - {{ assignment.endTime }}</p>
        </div>
        <div class="assignment-detail-shell__actions">
          <span class="assignment-detail-shell__status">{{ assignment.statusLabel }}</span>
          <el-button
            v-if="assignment.status === 'archived'"
            round
            @click="handleRestoreAssignment"
          >
            恢复作业
          </el-button>
          <el-button
            type="primary"
            round
            :loading="jobLoading"
            :disabled="!canCreatePlagiarismJob"
            @click="handleCreatePlagiarismJob"
          >
            发起查重
          </el-button>
        </div>
      </div>

      <div class="assignment-detail-tabs">
        <button
          v-for="item in detailTabs"
          :key="item.value"
          type="button"
          class="assignment-detail-tabs__item"
          :class="{ active: currentTab === item.value }"
          @click="currentTab = item.value"
        >
          {{ item.label }}
        </button>
      </div>

      <div v-if="currentTab === 'basic'" class="assignment-detail-panel">
        <div class="assignment-detail-grid">
          <div class="assignment-detail-info-card"><span>编程语言</span><strong>{{ assignment.language }}</strong></div>
          <div class="assignment-detail-info-card"><span>班级数量</span><strong>{{ assignment.classes.length }}</strong></div>
          <div class="assignment-detail-info-card"><span>最大文件数</span><strong>{{ assignment.maxFiles }}</strong></div>
          <div class="assignment-detail-info-card"><span>开始时间</span><strong>{{ assignment.startTime }}</strong></div>
          <div class="assignment-detail-info-card"><span>截止时间</span><strong>{{ assignment.endTime }}</strong></div>
          <div class="assignment-detail-info-card"><span>作业状态</span><strong>{{ assignment.statusLabel }}</strong></div>
          <div class="assignment-detail-info-card"><span>允许重复提交</span><strong>{{ assignment.allowResubmit ? '是' : '否' }}</strong></div>
          <div class="assignment-detail-info-card"><span>允许迟交</span><strong>{{ assignment.allowLateSubmit ? '是' : '否' }}</strong></div>
          <div class="assignment-detail-info-card"><span>资料附件</span><strong>{{ assignment.materials.length }}</strong></div>
        </div>

        <div class="assignment-detail-two-col">
          <article class="assignment-detail-card">
            <div class="assignment-detail-card__head"><h3>班级范围</h3></div>
            <el-table :data="assignment.classes" height="100%" empty-text="暂无班级">
              <el-table-column prop="className" label="班级" min-width="150" />
              <el-table-column prop="studentCount" label="应交人数" width="100" />
              <el-table-column prop="submittedStudentCount" label="已交人数" width="100" />
            </el-table>
          </article>

          <article class="assignment-detail-card">
            <div class="assignment-detail-card__head"><h3>作业说明</h3></div>
            <p class="assignment-detail-card__text">{{ assignment.description || '暂无作业说明。' }}</p>
          </article>
        </div>
      </div>

      <div v-else-if="currentTab === 'submissions'" class="assignment-detail-panel">
        <div class="assignment-detail-summary-row">
          <div class="assignment-detail-summary-card"><span>应交人数</span><strong>{{ stats?.studentCount || 0 }}</strong></div>
          <div class="assignment-detail-summary-card"><span>已交人数</span><strong>{{ stats?.submittedStudentCount || 0 }}</strong></div>
          <div class="assignment-detail-summary-card"><span>未交人数</span><strong>{{ stats?.unsubmittedStudentCount || 0 }}</strong></div>
          <div class="assignment-detail-summary-card"><span>迟交人数</span><strong>{{ stats?.lateSubmissionCount || 0 }}</strong></div>
        </div>

        <article class="assignment-detail-card assignment-detail-card--flex">
          <div class="assignment-detail-card__head"><h3>当前提交列表</h3><span>系统只保留每位学生最后一次提交</span></div>
          <el-table :data="submissions" height="100%" empty-text="暂无提交记录">
            <el-table-column prop="studentNumber" label="学生ID" min-width="140" />
            <el-table-column prop="fileCount" label="文件数" width="90" />
            <el-table-column prop="parseOkFiles" label="解析成功数" width="120" />
            <el-table-column label="迟交" width="90" align="center"><template #default="scope">{{ scope.row.isLate ? '是' : '否' }}</template></el-table-column>
            <el-table-column label="最新提交时间" min-width="160"><template #default="scope">{{ scope.row.lastSubmittedAt || '后端暂未返回' }}</template></el-table-column>
          </el-table>
        </article>
      </div>

      <div v-else-if="currentTab === 'files'" class="assignment-detail-panel">
        <div class="assignment-detail-two-col">
          <article class="assignment-detail-card assignment-detail-card--flex">
            <div class="assignment-detail-card__head">
              <h3>作业资料</h3>
              <el-upload :auto-upload="false" :show-file-list="false" multiple accept="*" @change="handleMaterialSelect">
                <el-button type="primary">上传资料</el-button>
              </el-upload>
            </div>
            <el-table :data="materials" height="100%" empty-text="暂无资料附件">
              <el-table-column prop="originalName" label="文件名" min-width="220" show-overflow-tooltip />
              <el-table-column label="大小" width="110"><template #default="scope">{{ formatBytes(scope.row.sizeBytes) }}</template></el-table-column>
              <el-table-column prop="contentType" label="类型" min-width="150" show-overflow-tooltip />
              <el-table-column label="操作" width="150" align="center">
                <template #default="scope">
                  <el-button link type="primary" @click="downloadMaterial(scope.row)">下载</el-button>
                  <el-button link type="danger" @click="removeMaterial(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </article>

          <article class="assignment-detail-card">
            <div class="assignment-detail-card__head"><h3>提交文件情况</h3></div>
            <div class="assignment-placeholder-list">
              <div>
                <strong>文件明细接口</strong>
                <p>系统已经切换成单提交模型，这里展示的是当前提交聚合情况，不再区分历史版本。</p>
              </div>
              <div>
                <strong>当前可见数据</strong>
                <p>已从提交概览中统计总提交数 {{ submissions.length }}，其中解析成功文件总数 {{ totalParseOkFiles }}。</p>
              </div>
            </div>
          </article>
        </div>
      </div>

      <div v-else class="assignment-detail-panel">
        <div class="assignment-detail-two-col">
          <article class="assignment-detail-card assignment-detail-card--flex">
            <div class="assignment-detail-card__head"><h3>Job 历史</h3><span>{{ jobs.length }} 个</span></div>
            <el-table :data="jobs" height="100%" empty-text="暂无查重任务">
              <el-table-column prop="id" label="Job ID" min-width="150" />
              <el-table-column prop="status" label="状态" width="110" />
              <el-table-column label="进度" width="120"><template #default="scope">{{ scope.row.progressDone }}/{{ scope.row.progressTotal }}</template></el-table-column>
              <el-table-column label="执行模式" width="110"><template #default="scope">{{ scope.row.executionMode }}</template></el-table-column>
              <el-table-column label="阈值" width="90"><template #default="scope">{{ scope.row.threshold }}</template></el-table-column>
              <el-table-column label="TopK" width="90"><template #default="scope">{{ scope.row.topK }}</template></el-table-column>
              <el-table-column label="命中结果" width="100"><template #default="scope">{{ scope.row.thresholdMatchedPairs }}</template></el-table-column>
              <el-table-column label="操作" width="110" align="center">
                <template #default="scope"><el-button link type="primary" @click="selectJob(scope.row.id)">查看报告</el-button></template>
              </el-table-column>
            </el-table>
          </article>

          <article class="assignment-detail-card">
            <div class="assignment-detail-card__head"><h3>报告摘要</h3><span>{{ pairs.length }} 对</span></div>
            <p class="assignment-meta-text">{{ reportMessage || '暂无可用报告。' }}</p>
            <div class="assignment-report-summary">
              <div><span>结果范围</span><strong>{{ reportFilters.minScore <= 0 ? '全部结果' : `>= ${reportFilters.minScore}` }}</strong></div>
              <div><span>每人保留</span><strong>{{ reportFilters.perStudentTopK <= 0 ? '不限' : reportFilters.perStudentTopK }}</strong></div>
              <div><span>不可比对</span><strong>{{ incomparableSubmissions.length }}</strong></div>
            </div>
            <div v-if="reportStats" class="assignment-report-summary assignment-report-summary--metrics">
              <div><span>可比对提交</span><strong>{{ reportStats.comparableSubmissionCount }}</strong></div>
              <div><span>理论 Pair</span><strong>{{ reportStats.comparablePairCount }}</strong></div>
              <div><span>大小跳过</span><strong>{{ reportStats.sizeSkippedPairs }}</strong></div>
              <div><span>粗桶跳过</span><strong>{{ reportStats.bucketSkippedPairs }}</strong></div>
              <div><span>完整计算</span><strong>{{ reportStats.fullCalculatedPairs }}</strong></div>
              <div><span>命中阈值</span><strong>{{ reportStats.thresholdMatchedPairs }}</strong></div>
              <div><span>执行模式</span><strong>{{ reportStats.executionMode }}</strong></div>
              <div><span>复用来源</span><strong>{{ reportStats.reusedFromJobId || '-' }}</strong></div>
            </div>
          </article>
        </div>

        <article class="assignment-detail-card assignment-detail-card--flex">
          <div class="assignment-detail-card__head"><h3>相似对</h3><span>点击进入相似对详情</span></div>
          <el-table :data="pairs" height="100%" empty-text="暂无相似对">
            <el-table-column prop="studentA" label="学生 A" min-width="120" />
            <el-table-column prop="studentB" label="学生 B" min-width="120" />
            <el-table-column prop="score" label="相似度" width="100" />
            <el-table-column prop="status" label="处理状态" width="140" />
            <el-table-column prop="teacherNote" label="教师备注" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button link type="primary" @click="goToPairDetail(scope.row)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </article>
      </div>
    </section>
  </div>

  <div v-else class="assignment-detail-empty">
    <el-empty description="未找到该作业">
      <AppBackButton label="返回作业总览" @click="goBack" />
    </el-empty>
  </div>
</template>

<script setup>
import AppBackButton from '../../components/AppBackButton.vue'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createTeacherPlagiarismJob,
  restoreTeacherAssignment,
  deleteTeacherAssignmentMaterial,
  fetchTeacherAssignmentDetail,
  fetchTeacherAssignmentPlagiarism,
  fetchTeacherAssignmentStats,
  fetchTeacherAssignmentSubmissions,
  fetchTeacherPlagiarismReport,
  getTeacherAssignmentMaterialDownloadUrl,
  uploadTeacherAssignmentMaterials
} from '../../api/teacherAssignments'
import { formatBytes } from './assignmentMappers'
import {
  TEACHER_PLAGIARISM_MONITOR_FINISHED_EVENT,
  useTeacherPlagiarismMonitor
} from '../../composables/useTeacherPlagiarismMonitor'

const route = useRoute()
const router = useRouter()
const { registerTeacherPlagiarismJobs, trackTeacherPlagiarismJob } = useTeacherPlagiarismMonitor()

const detailTabs = [
  { value: 'basic', label: '基本信息' },
  { value: 'submissions', label: '提交情况' },
  { value: 'files', label: '文件情况' },
  { value: 'plagiarism', label: '查重记录' }
]

const currentTab = ref('basic')
const assignment = ref(null)
const stats = ref(null)
const submissions = ref([])
const materials = ref([])
const jobs = ref([])
const pairs = ref([])
const incomparableSubmissions = ref([])
const reportMessage = ref('')
const reportStats = ref(null)
const jobLoading = ref(false)
const activeJobId = ref('')
const reportFilters = ref({ minScore: 0, perStudentTopK: 0 })

const totalParseOkFiles = computed(() => submissions.value.reduce((sum, item) => sum + Number(item.parseOkFiles || 0), 0))
const canCreatePlagiarismJob = computed(() => assignment.value?.status === 'ended')

onMounted(() => {
  window.addEventListener(TEACHER_PLAGIARISM_MONITOR_FINISHED_EVENT, handleMonitorFinished)
  loadPage()
})

onBeforeUnmount(() => {
  window.removeEventListener(TEACHER_PLAGIARISM_MONITOR_FINISHED_EVENT, handleMonitorFinished)
})

async function loadPage() {
  const assignmentId = route.params.assignmentId
  const [detail, statsResult, submissionResult, plagiarismResult] = await Promise.all([
    fetchTeacherAssignmentDetail(assignmentId),
    fetchTeacherAssignmentStats(assignmentId),
    fetchTeacherAssignmentSubmissions(assignmentId),
    fetchTeacherAssignmentPlagiarism(assignmentId, reportFilters.value)
  ])

  assignment.value = detail
  stats.value = statsResult
  submissions.value = submissionResult || []
  materials.value = detail?.materials || []
  jobs.value = plagiarismResult?.jobs || []
  activeJobId.value = plagiarismResult?.activeJobId || ''
  pairs.value = plagiarismResult?.report?.pairs || []
  incomparableSubmissions.value = plagiarismResult?.report?.incomparableSubmissions || []
  reportMessage.value = plagiarismResult?.report?.message || ''
  reportStats.value = plagiarismResult?.report?.jobStats || null
  registerTeacherPlagiarismJobs({
    assignmentId,
    assignmentTitle: detail?.title || `作业 ${assignmentId}`,
    jobs: jobs.value
  })
}

async function handleMaterialSelect(uploadFile) {
  const raw = uploadFile?.raw
  if (!raw || !assignment.value?.id) return
  await uploadTeacherAssignmentMaterials(assignment.value.id, [raw])
  ElMessage.success('资料已上传')
  await loadPage()
}

function downloadMaterial(material) {
  window.open(getTeacherAssignmentMaterialDownloadUrl(assignment.value.id, material.id), '_blank')
}

async function removeMaterial(material) {
  await ElMessageBox.confirm(`确认删除资料 ${material.originalName} 吗？`, '删除确认', { type: 'warning' })
  await deleteTeacherAssignmentMaterial(assignment.value.id, material.id)
  ElMessage.success('资料已删除')
  await loadPage()
}

async function handleCreatePlagiarismJob() {
  if (!assignment.value?.id) return
  if (!canCreatePlagiarismJob.value) {
    ElMessage.warning('只有已结束且未归档的作业可以发起查重')
    return
  }
  jobLoading.value = true
  try {
    const job = await createTeacherPlagiarismJob(assignment.value.id, {
      thresholdScore: 80,
      topKPerStudent: 0,
      plagiarismMode: 'FAST'
    })
    trackTeacherPlagiarismJob({
      assignmentId: assignment.value.id,
      assignmentTitle: assignment.value.title,
      job
    })
    activeJobId.value = job.id
    ElMessage.success(`已创建查重任务 #${job.id}`)
    currentTab.value = 'plagiarism'
    await loadPage()
  } finally {
    jobLoading.value = false
  }
}

async function handleRestoreAssignment() {
  if (!assignment.value?.id) return
  await ElMessageBox.confirm(
    `确认恢复作业“${assignment.value.title}”吗？恢复后这份作业会重新回到默认作业列表中。`,
    '恢复作业',
    {
      type: 'warning',
      confirmButtonText: '恢复作业',
      cancelButtonText: '取消'
    }
  )

  await restoreTeacherAssignment(assignment.value.id)
  ElMessage.success('作业已恢复')
  await loadPage()
}

async function selectJob(jobId) {
  activeJobId.value = jobId
  const report = await fetchTeacherPlagiarismReport(jobId, reportFilters.value)
  pairs.value = report.pairs || []
  incomparableSubmissions.value = report.incomparableSubmissions || []
  reportMessage.value = report.message || ''
  reportStats.value = report.jobStats || null
}

function handleMonitorFinished(event) {
  const detail = event?.detail || {}
  if (String(detail.assignmentId) !== String(route.params.assignmentId)) return
  if (currentTab.value === 'plagiarism') {
    loadPage()
  }
}

function goToPairDetail(row) {
  router.push({ path: `/teacher/similarity-pairs/${row.pairId}`, query: { assignmentId: assignment.value.id, jobId: activeJobId.value || '' } })
}

function goBack() {
  router.push({ path: '/teacher/assignments', query: { tab: 'overview' } })
}
</script>

<style scoped>
.assignment-detail-page {
  height: 100%;
}

.assignment-detail-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 24px 26px;
  border-radius: 32px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 255, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
  overflow: hidden;
}

.assignment-detail-shell__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.assignment-detail-shell__header h1 {
  margin: 8px 0 6px;
  font-size: 32px;
  line-height: 1.06;
}

.assignment-detail-shell__header p,
.assignment-meta-text,
.assignment-detail-card__text {
  margin: 0;
  color: #6f7f96;
}

.assignment-detail-shell__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.assignment-detail-shell__status {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(18, 25, 38, 0.08);
  color: #53627a;
}

.assignment-detail-tabs {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(18, 25, 38, 0.06);
}

.assignment-detail-tabs__item {
  border: 0;
  padding: 10px 18px;
  border-radius: 999px;
  background: transparent;
  color: #68798f;
  cursor: pointer;
}

.assignment-detail-tabs__item.active {
  background: #121926;
  color: #fff;
}

.assignment-detail-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 0;
  flex: 1;
}

.assignment-detail-grid,
.assignment-detail-summary-row,
.assignment-detail-two-col,
.assignment-report-summary {
  display: grid;
  gap: 14px;
}

.assignment-detail-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.assignment-detail-summary-row {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.assignment-detail-two-col {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-height: 0;
  flex: 1;
}

.assignment-report-summary {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.assignment-report-summary--metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.assignment-detail-info-card,
.assignment-detail-summary-card,
.assignment-detail-card {
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(127, 142, 163, 0.14);
  box-shadow: 0 16px 38px rgba(160, 176, 199, 0.08);
}

.assignment-detail-info-card span,
.assignment-detail-summary-card span,
.assignment-report-summary span {
  color: #7f8ea3;
}

.assignment-detail-info-card strong,
.assignment-detail-summary-card strong,
.assignment-report-summary strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
}

.assignment-detail-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.assignment-detail-card--flex {
  min-height: 0;
  flex: 1;
}

.assignment-detail-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.assignment-detail-card__head h3 {
  margin: 0;
}

.assignment-placeholder-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.assignment-placeholder-list div {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(18, 25, 38, 0.04);
}

.assignment-placeholder-list strong {
  display: block;
  margin-bottom: 8px;
}

.assignment-placeholder-list p {
  margin: 0;
  color: #6f7f96;
  line-height: 1.7;
}

.assignment-detail-empty {
  display: grid;
  place-items: center;
  height: 100%;
}

@media (max-width: 1280px) {
  .assignment-detail-shell__header,
  .assignment-detail-shell__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .assignment-detail-grid,
  .assignment-detail-summary-row,
  .assignment-detail-two-col,
  .assignment-report-summary {
    grid-template-columns: 1fr;
  }
}
</style>

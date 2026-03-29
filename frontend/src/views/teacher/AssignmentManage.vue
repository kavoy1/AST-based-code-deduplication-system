<template>
  <div class="teacher-assignment-page">
    <section v-if="currentTab === 'settings'" class="assignment-surface assignment-surface--settings">
      <div class="assignment-section-head">
        <div>
          <h2>{{ editingAssignmentId ? '编辑作业' : '发布作业' }}</h2>
          <p>{{ editingAssignmentId ? '修改现有作业配置，保存后立即生效。' : '创建新作业并发布到一个或多个班级。' }}</p>
        </div>
        <el-button round @click="goToOverview">返回作业总览</el-button>
      </div>

      <div class="assignment-settings-layout">
        <el-form label-position="top" :model="form" class="assignment-card assignment-card--form">
          <div class="assignment-form-grid assignment-form-grid--wide">
            <el-form-item label="作业标题">
              <el-input v-model="form.title" maxlength="80" show-word-limit placeholder="请输入作业标题" />
            </el-form-item>
            <el-form-item label="编程语言">
              <el-select v-model="form.language" placeholder="选择语言">
                <el-option v-for="item in languageOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </div>

          <el-form-item label="发布班级">
            <el-select v-model="form.classIds" multiple collapse-tags collapse-tags-tooltip placeholder="选择一个或多个班级">
              <el-option
                v-for="item in classList"
                :key="item.id"
                :label="`${item.name} · ${item.studentCount}人`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <div class="assignment-form-grid">
            <el-form-item label="开始时间">
              <el-date-picker v-model="form.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择开始时间" />
            </el-form-item>
            <el-form-item label="截止时间">
              <el-date-picker v-model="form.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择截止时间" />
            </el-form-item>
          </div>

          <el-form-item label="作业说明">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="7"
              maxlength="800"
              show-word-limit
              placeholder="请输入题目要求、提交说明和评分重点"
            />
          </el-form-item>

          <div class="assignment-form-grid assignment-form-grid--three">
            <el-form-item label="允许重复提交"><el-switch v-model="form.allowResubmit" /></el-form-item>
            <el-form-item label="允许迟交"><el-switch v-model="form.allowLateSubmit" /></el-form-item>
            <el-form-item label="单次最大文件数"><el-input-number v-model="form.maxFiles" :min="1" :max="50" /></el-form-item>
          </div>
        </el-form>

        <aside class="assignment-card assignment-side-panel">
          <div class="assignment-side-panel__item">
            <span>已选班级</span>
            <strong>{{ selectedClasses.length }}</strong>
          </div>
          <div class="assignment-side-panel__item">
            <span>预计应交人数</span>
            <strong>{{ selectedStudentCount }}</strong>
          </div>
          <div class="assignment-side-panel__block">
            <h3>查重默认参数</h3>
            <div><span>阈值</span><strong>80</strong></div>
            <div><span>TopK</span><strong>10</strong></div>
          </div>
        </aside>
      </div>

      <div class="assignment-actions">
        <el-button @click="resetForm">重置</el-button>
        <el-button type="primary" round :loading="submitLoading" @click="submitAssignment">
          {{ editingAssignmentId ? '保存修改' : '发布作业' }}
        </el-button>
      </div>
    </section>

    <section v-else-if="currentTab === 'submissions'" class="assignment-surface assignment-surface--submissions">
      <div class="assignment-section-head assignment-section-head--compact">
        <div>
          <h2>提交与批改</h2>
          <p>按作业查看提交进度，点击学生提交进入详情。</p>
        </div>
        <div class="assignment-tools assignment-tools--tight">
          <el-select v-model="submissionAssignmentId" class="assignment-select" placeholder="选择作业" @change="loadSubmissionTab">
            <el-option v-for="item in allAssignments" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
          <el-button @click="loadSubmissionTab">刷新</el-button>
        </div>
      </div>

      <div class="assignment-summary-grid assignment-summary-grid--five" v-if="submissionSummary">
        <div class="assignment-summary-card"><span>应交人数</span><strong>{{ submissionSummary.studentCount }}</strong></div>
        <div class="assignment-summary-card"><span>已交人数</span><strong>{{ submissionSummary.submittedStudentCount }}</strong></div>
        <div class="assignment-summary-card"><span>未交人数</span><strong>{{ submissionSummary.unsubmittedStudentCount }}</strong></div>
        <div class="assignment-summary-card"><span>迟交人数</span><strong>{{ submissionSummary.lateSubmissionCount }}</strong></div>
        <div class="assignment-summary-card"><span>不可比对</span><strong>{{ incomparableSubmissionCount }}</strong></div>
      </div>

      <div class="assignment-card assignment-card--table">
        <el-table :data="submissionRows" height="100%" empty-text="暂无提交数据">
          <el-table-column prop="studentNumber" label="学生ID" min-width="140" />
          <el-table-column prop="version" label="版本数" width="100" />
          <el-table-column prop="fileCount" label="文件数" width="90" />
          <el-table-column prop="parseOkFiles" label="解析成功数" width="120" />
          <el-table-column label="迟交" width="90" align="center">
            <template #default="scope">{{ scope.row.isLate ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="可比对" width="100" align="center">
            <template #default="scope">{{ scope.row.parseOkFiles > 0 ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="最新提交时间" min-width="170">
            <template #default="scope">{{ scope.row.lastSubmittedAt || '后端暂未返回' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="scope">
              <el-button link type="primary" @click="goToSubmissionDetail(scope.row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <section v-else-if="currentTab === 'plagiarism'" class="assignment-surface assignment-surface--plagiarism">
      <div class="assignment-section-head assignment-section-head--compact">
        <div>
          <h2>查重与结果</h2>
          <p>查看 Job、报告摘要和相似对处理结果。</p>
        </div>
        <div class="assignment-tools assignment-tools--tight">
          <el-select v-model="plagiarismAssignmentId" class="assignment-select" placeholder="选择作业" @change="loadPlagiarismTab">
            <el-option v-for="item in allAssignments" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
          <el-input-number v-model="reportFilters.minScore" :min="0" :max="100" class="assignment-number" />
          <el-input-number v-model="reportFilters.perStudentTopK" :min="1" :max="50" class="assignment-number" />
          <el-button :disabled="!activeJobId" @click="loadActiveReport">刷新报告</el-button>
          <el-button :disabled="!activeJobId" @click="handleExportReport">导出 CSV</el-button>
          <el-button type="primary" round :disabled="!plagiarismAssignmentId" :loading="jobLoading" @click="handleCreatePlagiarismJob">发起查重</el-button>
        </div>
      </div>

      <div class="assignment-two-column">
        <div class="assignment-card assignment-card--table assignment-card--fixed">
          <div class="assignment-card__head">
            <h3>Job 列表</h3>
            <span>{{ plagiarismJobs.length }} 个</span>
          </div>
          <el-table :data="plagiarismJobs" height="100%" empty-text="暂无查重任务">
            <el-table-column prop="id" label="Job ID" min-width="150" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column label="进度" width="120">
              <template #default="scope">{{ scope.row.progressDone }}/{{ scope.row.progressTotal }}</template>
            </el-table-column>
            <el-table-column label="执行模式" width="110">
              <template #default="scope">{{ scope.row.executionMode }}</template>
            </el-table-column>
            <el-table-column label="阈值" width="90">
              <template #default="scope">{{ scope.row.threshold }}</template>
            </el-table-column>
            <el-table-column label="TopK" width="90">
              <template #default="scope">{{ scope.row.topK }}</template>
            </el-table-column>
            <el-table-column label="命中结果" width="100">
              <template #default="scope">{{ scope.row.thresholdMatchedPairs }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center">
              <template #default="scope">
                <el-button link type="primary" @click="selectJob(scope.row.id)">查看报告</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="assignment-card assignment-card--report">
          <div class="assignment-card__head">
            <h3>查重报告</h3>
            <span>{{ plagiarismPairs.length }} 对相似对</span>
          </div>
          <p class="assignment-muted">{{ reportMessage || '当前作业还没有可用报告。' }}</p>
          <div class="assignment-report-summary">
            <div><span>最低分数</span><strong>{{ reportFilters.minScore }}</strong></div>
            <div><span>TopK</span><strong>{{ reportFilters.perStudentTopK }}</strong></div>
            <div><span>不可比对</span><strong>{{ incomparableRows.length }}</strong></div>
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
        </div>
      </div>

      <div class="assignment-card assignment-card--table assignment-card--flex">
        <div class="assignment-card__head">
          <h3>相似对列表</h3>
          <span>点击进入相似对详情</span>
        </div>
        <el-table :data="plagiarismPairs" height="100%" empty-text="暂无相似对">
          <el-table-column prop="studentA" label="学生 A" min-width="120" />
          <el-table-column prop="studentB" label="学生 B" min-width="120" />
          <el-table-column prop="score" label="相似度" width="100" />
          <el-table-column prop="status" label="处理状态" width="140" />
          <el-table-column prop="teacherNote" label="教师备注" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="120" align="center">
            <template #default="scope">
              <el-button link type="primary" @click="goToPairDetail(scope.row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="assignment-card assignment-card--table assignment-card--short">
        <div class="assignment-card__head">
          <h3>不可比对提交</h3>
          <span>{{ incomparableRows.length }} 条</span>
        </div>
        <el-table :data="incomparableRows" height="100%" empty-text="暂无不可比对提交">
          <el-table-column prop="studentId" label="学生ID" min-width="120" />
          <el-table-column prop="classId" label="班级ID" width="100" />
          <el-table-column prop="version" label="版本" width="80" />
          <el-table-column prop="reason" label="原因" min-width="180" />
        </el-table>
      </div>
    </section>

    <section v-else class="assignment-surface assignment-surface--overview">
      <div class="assignment-section-head assignment-section-head--compact">
        <div>
          <h2>作业总览</h2>
          <p>按班级、状态和标题快速找到作业并进入详情。</p>
        </div>
        <el-button type="primary" round @click="goToSettings">创建作业</el-button>
      </div>

      <div class="assignment-tools assignment-tools--overview">
        <el-select v-model="classFilter" class="assignment-select" clearable placeholder="全部班级" @change="loadAssignments">
          <el-option v-for="item in classList" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="statusFilter" class="assignment-select" placeholder="全部状态" @change="loadAssignments">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="keyword" class="assignment-search" clearable placeholder="搜索作业标题" @change="loadAssignments" />
      </div>

      <div class="assignment-summary-grid">
        <div class="assignment-summary-card"><span>全部作业</span><strong>{{ overviewStats.total }}</strong></div>
        <div class="assignment-summary-card"><span>进行中</span><strong>{{ overviewStats.active }}</strong></div>
        <div class="assignment-summary-card"><span>已结束</span><strong>{{ overviewStats.ended }}</strong></div>
      </div>

      <div class="assignment-card assignment-card--table assignment-card--flex">
        <el-table :data="pagedOverviewRecords" height="100%" empty-text="暂无符合条件的作业">
          <el-table-column label="作业标题" min-width="220" show-overflow-tooltip>
            <template #default="scope">
              <button type="button" class="assignment-link-button" @click="goToDetail(scope.row.id)">{{ scope.row.title }}</button>
            </template>
          </el-table-column>
          <el-table-column prop="classNamesText" label="所属班级" min-width="140" />
          <el-table-column prop="endTime" label="截止时间" min-width="170" />
          <el-table-column prop="studentCount" label="应交人数" width="100" />
          <el-table-column prop="submittedCount" label="已交人数" width="100" />
          <el-table-column label="提交率" width="110">
            <template #default="scope">{{ scope.row.progress }}%</template>
          </el-table-column>
          <el-table-column prop="statusLabel" label="作业状态" width="110" />
          <el-table-column label="操作" width="320" align="center">
            <template #default="scope">
              <el-button link type="primary" @click="goToDetail(scope.row.id)">查看详情</el-button>
              <el-button link @click="editAssignment(scope.row.id)">编辑</el-button>
              <el-button link @click="goToSubmissions(scope.row.id)">查看提交</el-button>
              <el-button link @click="goToPlagiarism(scope.row.id)">查看查重</el-button>
              <el-button v-if="scope.row.status === 'ended'" link type="warning" @click="openReopenDialog(scope.row)">重新开启</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="assignment-pagination">
        <span>共 {{ filteredAssignments.length }} 份作业</span>
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="filteredAssignments.length"
          @current-change="currentPage = $event"
        />
      </div>
    </section>

    <el-dialog v-model="reopenDialogVisible" title="重新开启作业" width="460px">
      <el-form label-position="top" :model="reopenForm">
        <el-form-item label="开始时间">
          <el-date-picker v-model="reopenForm.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择开始时间" />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="reopenForm.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择截止时间" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="reopenForm.reason" type="textarea" :rows="4" placeholder="请输入重开原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reopenDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reopenLoading" @click="submitReopen">确认重开</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../api/request'
import {
  createTeacherAssignment,
  createTeacherPlagiarismJob,
  exportTeacherPlagiarismReport,
  fetchTeacherAssignments,
  fetchTeacherAssignmentDetail,
  fetchTeacherAssignmentPlagiarism,
  fetchTeacherAssignmentStats,
  fetchTeacherAssignmentSubmissions,
  fetchTeacherPlagiarismReport,
  reopenTeacherAssignment,
  updateTeacherAssignment
} from '../../api/teacherAssignments'
import { formatDateTimeForInput, normalizeClasses } from './assignmentMappers'
import {
  TEACHER_PLAGIARISM_MONITOR_FINISHED_EVENT,
  useTeacherPlagiarismMonitor
} from '../../composables/useTeacherPlagiarismMonitor'

const route = useRoute()
const router = useRouter()
const { registerTeacherPlagiarismJobs, trackTeacherPlagiarismJob } = useTeacherPlagiarismMonitor()

const languageOptions = ['JAVA', 'PYTHON', 'C', 'C++', 'JAVASCRIPT', 'TYPESCRIPT', 'GO']
const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'PUBLISHED', label: '进行中' },
  { value: 'CLOSED', label: '已结束' }
]

const classList = ref([])
const allAssignments = ref([])
const submissionRows = ref([])
const submissionSummary = ref(null)
const plagiarismJobs = ref([])
const plagiarismPairs = ref([])
const incomparableRows = ref([])
const reportMessage = ref('')
const reportStats = ref(null)

const currentPage = ref(1)
const pageSize = 8
const keyword = ref('')
const statusFilter = ref('')
const classFilter = ref('')
const submitLoading = ref(false)
const jobLoading = ref(false)
const reopenLoading = ref(false)
const editingAssignmentId = ref('')
const submissionAssignmentId = ref('')
const plagiarismAssignmentId = ref('')
const activeJobId = ref('')
const reopenDialogVisible = ref(false)
const reopeningAssignmentId = ref('')

const form = reactive({
  title: '',
  classIds: [],
  startAt: '',
  endAt: '',
  description: '',
  language: 'JAVA',
  allowResubmit: true,
  allowLateSubmit: false,
  maxFiles: 20
})

const reportFilters = reactive({
  minScore: 80,
  perStudentTopK: 10
})

const reopenForm = reactive({
  startAt: '',
  endAt: '',
  reason: ''
})

const currentTab = computed(() => {
  const raw = String(route.query.tab || 'overview')
  if (raw === 'list') return 'overview'
  if (raw === 'create') return 'settings'
  return ['overview', 'settings', 'submissions', 'plagiarism'].includes(raw) ? raw : 'overview'
})

const selectedClasses = computed(() => classList.value.filter((item) => form.classIds.includes(item.id)))
const selectedStudentCount = computed(() => selectedClasses.value.reduce((sum, item) => sum + Number(item.studentCount || 0), 0))
const incomparableSubmissionCount = computed(() => submissionRows.value.filter((item) => item.parseOkFiles === 0).length)

const filteredAssignments = computed(() => {
  let list = [...allAssignments.value]
  if (classFilter.value) {
    list = list.filter((item) => Array.isArray(item.classIds) ? item.classIds.includes(Number(classFilter.value)) : true)
  }
  const statusWeight = { active: 0, ended: 1, draft: 2 }
  return list.sort((a, b) => {
    const weightA = statusWeight[a.status] ?? 9
    const weightB = statusWeight[b.status] ?? 9
    if (weightA !== weightB) return weightA - weightB
    return new Date(b.startAt || 0).getTime() - new Date(a.startAt || 0).getTime()
  })
})

const pagedOverviewRecords = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredAssignments.value.slice(start, start + pageSize)
})

const overviewStats = computed(() => ({
  total: filteredAssignments.value.length,
  active: filteredAssignments.value.filter((item) => item.status === 'active').length,
  ended: filteredAssignments.value.filter((item) => item.status === 'ended').length
}))

watch([keyword, statusFilter], async () => {
  currentPage.value = 1
  await loadAssignments()
})

watch(
  () => route.fullPath,
  async () => {
    if (currentTab.value === 'settings') {
      const assignmentId = typeof route.query.assignmentId === 'string' ? route.query.assignmentId : ''
      if (assignmentId) {
        await fillEditForm(assignmentId)
      } else {
        resetForm()
      }
    }
    if (currentTab.value === 'submissions') await ensureSubmissionContext()
    if (currentTab.value === 'plagiarism') await ensurePlagiarismContext()
  }
)

onMounted(async () => {
  window.addEventListener(TEACHER_PLAGIARISM_MONITOR_FINISHED_EVENT, handleMonitorFinished)
  await fetchClasses()
  await loadAssignments()
  if (currentTab.value === 'settings') {
    const assignmentId = typeof route.query.assignmentId === 'string' ? route.query.assignmentId : ''
    if (assignmentId) await fillEditForm(assignmentId)
  }
  if (currentTab.value === 'submissions') await ensureSubmissionContext()
  if (currentTab.value === 'plagiarism') await ensurePlagiarismContext()
})

onBeforeUnmount(() => {
  window.removeEventListener(TEACHER_PLAGIARISM_MONITOR_FINISHED_EVENT, handleMonitorFinished)
})

async function fetchClasses() {
  const result = await request.get('/teacher/classes', { params: { page: 1, limit: 100 } })
  classList.value = normalizeClasses(result)
}

async function loadAssignments() {
  const result = await fetchTeacherAssignments({
    keyword: keyword.value || undefined,
    status: statusFilter.value || undefined,
    page: 1,
    size: 100
  })
  allAssignments.value = result.records || []
}

function goToOverview() {
  router.push({ path: '/teacher/assignments', query: { tab: 'overview' } })
}

function goToSettings() {
  resetForm()
  router.push({ path: '/teacher/assignments', query: { tab: 'settings' } })
}

function goToSubmissions(assignmentId) {
  router.push({ path: '/teacher/assignments', query: { tab: 'submissions', assignmentId } })
}

function goToPlagiarism(assignmentId) {
  router.push({ path: '/teacher/assignments', query: { tab: 'plagiarism', assignmentId } })
}

function goToDetail(assignmentId) {
  router.push(`/teacher/assignments/${assignmentId}`)
}

function goToSubmissionDetail(row) {
  router.push({ path: `/teacher/submissions/${row.submissionId}`, query: { assignmentId: submissionAssignmentId.value } })
}

function goToPairDetail(row) {
  router.push({ path: `/teacher/similarity-pairs/${row.pairId}`, query: { assignmentId: plagiarismAssignmentId.value, jobId: activeJobId.value || '' } })
}

function resetForm() {
  editingAssignmentId.value = ''
  Object.assign(form, {
    title: '',
    classIds: [],
    startAt: '',
    endAt: '',
    description: '',
    language: 'JAVA',
    allowResubmit: true,
    allowLateSubmit: false,
    maxFiles: 20
  })
}

async function fillEditForm(assignmentId) {
  const detail = await fetchTeacherAssignmentDetail(assignmentId)
  editingAssignmentId.value = String(assignmentId)
  Object.assign(form, {
    title: detail.title,
    classIds: [...detail.classIds],
    startAt: formatDateTimeForInput(detail.startAt),
    endAt: formatDateTimeForInput(detail.endAt),
    description: detail.description || '',
    language: detail.language || 'JAVA',
    allowResubmit: Boolean(detail.allowResubmit),
    allowLateSubmit: Boolean(detail.allowLateSubmit),
    maxFiles: Number(detail.maxFiles || 20)
  })
}

async function editAssignment(assignmentId) {
  await fillEditForm(assignmentId)
  router.push({ path: '/teacher/assignments', query: { tab: 'settings', assignmentId } })
}

async function submitAssignment() {
  if (!form.title.trim()) return ElMessage.warning('请填写作业标题')
  if (!form.classIds.length) return ElMessage.warning('请至少选择一个班级')
  if (!form.startAt || !form.endAt) return ElMessage.warning('请选择开始和截止时间')
  if (new Date(form.endAt).getTime() <= new Date(form.startAt).getTime()) return ElMessage.warning('截止时间必须晚于开始时间')

  submitLoading.value = true
  try {
    if (editingAssignmentId.value) {
      await updateTeacherAssignment(editingAssignmentId.value, form)
      ElMessage.success('作业已更新')
    } else {
      await createTeacherAssignment(form)
      ElMessage.success('作业已创建')
    }
    await loadAssignments()
    resetForm()
    goToOverview()
  } finally {
    submitLoading.value = false
  }
}

function openReopenDialog(row) {
  reopeningAssignmentId.value = row.id
  reopenForm.startAt = row.startAt || ''
  reopenForm.endAt = row.endAt || ''
  reopenForm.reason = ''
  reopenDialogVisible.value = true
}

async function submitReopen() {
  if (!reopeningAssignmentId.value) return
  if (!reopenForm.startAt || !reopenForm.endAt) return ElMessage.warning('请填写新的开始和截止时间')
  reopenLoading.value = true
  try {
    await reopenTeacherAssignment(reopeningAssignmentId.value, reopenForm)
    ElMessage.success('作业已重新开启')
    reopenDialogVisible.value = false
    await loadAssignments()
  } finally {
    reopenLoading.value = false
  }
}

async function ensureSubmissionContext() {
  const assignmentId = typeof route.query.assignmentId === 'string' ? route.query.assignmentId : ''
  if (assignmentId) submissionAssignmentId.value = assignmentId
  if (!submissionAssignmentId.value && allAssignments.value[0]) submissionAssignmentId.value = allAssignments.value[0].id
  if (submissionAssignmentId.value) await loadSubmissionTab()
}

async function ensurePlagiarismContext() {
  const assignmentId = typeof route.query.assignmentId === 'string' ? route.query.assignmentId : ''
  if (assignmentId) plagiarismAssignmentId.value = assignmentId
  if (!plagiarismAssignmentId.value && allAssignments.value[0]) plagiarismAssignmentId.value = allAssignments.value[0].id
  if (plagiarismAssignmentId.value) await loadPlagiarismTab()
}

async function loadSubmissionTab() {
  if (!submissionAssignmentId.value) return
  submissionRows.value = await fetchTeacherAssignmentSubmissions(submissionAssignmentId.value)
  submissionSummary.value = await fetchTeacherAssignmentStats(submissionAssignmentId.value)
}

async function loadPlagiarismTab() {
  if (!plagiarismAssignmentId.value) return
  const result = await fetchTeacherAssignmentPlagiarism(plagiarismAssignmentId.value, reportFilters)
  plagiarismJobs.value = result.jobs || []
  activeJobId.value = result.activeJobId || ''
  plagiarismPairs.value = result.report?.pairs || []
  incomparableRows.value = result.report?.incomparableSubmissions || []
  reportMessage.value = result.report?.message || ''
  reportStats.value = result.report?.jobStats || null
  registerTeacherPlagiarismJobs({
    assignmentId: plagiarismAssignmentId.value,
    assignmentTitle: getAssignmentTitle(plagiarismAssignmentId.value),
    jobs: plagiarismJobs.value
  })
}

async function loadActiveReport() {
  if (!activeJobId.value) return
  const report = await fetchTeacherPlagiarismReport(activeJobId.value, reportFilters)
  plagiarismPairs.value = report.pairs || []
  incomparableRows.value = report.incomparableSubmissions || []
  reportMessage.value = report.message || ''
  reportStats.value = report.jobStats || null
}

async function selectJob(jobId) {
  activeJobId.value = jobId
  await loadActiveReport()
}

async function handleCreatePlagiarismJob() {
  if (!plagiarismAssignmentId.value) return
  jobLoading.value = true
  try {
    const job = await createTeacherPlagiarismJob(plagiarismAssignmentId.value, reportFilters)
    trackTeacherPlagiarismJob({
      assignmentId: plagiarismAssignmentId.value,
      assignmentTitle: getAssignmentTitle(plagiarismAssignmentId.value),
      job
    })
    activeJobId.value = job.id
    ElMessage.success(`已创建查重任务 #${job.id}`)
    await loadPlagiarismTab()
  } finally {
    jobLoading.value = false
  }
}

function getAssignmentTitle(assignmentId) {
  return allAssignments.value.find((item) => String(item.id) === String(assignmentId))?.title || `作业 ${assignmentId}`
}

function handleMonitorFinished(event) {
  const detail = event?.detail || {}
  if (String(detail.assignmentId) !== String(plagiarismAssignmentId.value)) return
  loadPlagiarismTab()
}

async function handleExportReport() {
  if (!activeJobId.value) return
  const blob = await exportTeacherPlagiarismReport(activeJobId.value, reportFilters)
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `plagiarism-report-${activeJobId.value}.csv`
  link.click()
  window.URL.revokeObjectURL(url)
}
</script>

<style scoped>
.teacher-assignment-page {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.assignment-surface {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 32px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 255, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
  overflow: hidden;
}

.assignment-surface--overview {
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto;
}

.assignment-surface--submissions {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
}

.assignment-surface--plagiarism {
  display: grid;
  grid-template-rows: auto minmax(220px, 0.9fr) minmax(0, 1fr) minmax(180px, 0.52fr);
}

.assignment-surface--settings {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.assignment-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.assignment-section-head--compact {
  align-items: flex-start;
}

.assignment-section-head h2 {
  margin: 0;
  font-size: 24px;
  line-height: 1.1;
}

.assignment-section-head p,
.assignment-muted {
  margin: 6px 0 0;
  color: #6f7f96;
  font-size: 13px;
}

.assignment-tools {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.assignment-tools--overview {
  justify-content: flex-start;
}

.assignment-tools--tight {
  justify-content: flex-end;
}

.assignment-select {
  width: 220px;
}

.assignment-search {
  width: 280px;
}

.assignment-number {
  width: 120px;
}

.assignment-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.assignment-summary-grid--five {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.assignment-summary-card {
  padding: 12px 16px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(127, 142, 163, 0.14);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.assignment-summary-card span,
.assignment-side-panel__item span,
.assignment-side-panel__block span,
.assignment-report-summary span {
  color: #7f8ea3;
}

.assignment-summary-card strong,
.assignment-side-panel__item strong,
.assignment-report-summary strong {
  font-size: 18px;
}

.assignment-card {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(127, 142, 163, 0.14);
  box-shadow: 0 16px 38px rgba(160, 176, 199, 0.08);
}

.assignment-card--table {
  padding: 16px 18px;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.assignment-card--flex {
  min-height: 0;
  flex: 1;
}

.assignment-card--short {
  min-height: 0;
}

.assignment-card--fixed {
  min-height: 0;
}

.assignment-card--report {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  overflow: auto;
}

.assignment-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  flex-shrink: 0;
}

.assignment-card__head h3,
.assignment-side-panel__block h3 {
  margin: 0;
}

.assignment-link-button {
  padding: 0;
  border: none;
  background: transparent;
  color: #2d5cff;
  font-weight: 600;
  cursor: pointer;
}

.assignment-pagination,
.assignment-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.assignment-actions {
  justify-content: flex-end;
}

.assignment-settings-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) 300px;
  gap: 16px;
  min-height: 0;
  flex: 1;
  overflow: hidden;
}

.assignment-card--form {
  padding: 18px;
  overflow: auto;
}

.assignment-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.assignment-form-grid--wide {
  grid-template-columns: minmax(0, 1.3fr) 220px;
}

.assignment-form-grid--three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.assignment-side-panel {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
}

.assignment-side-panel__item,
.assignment-side-panel__block {
  padding: 16px;
  border-radius: 22px;
  background: rgba(18, 25, 38, 0.04);
}

.assignment-side-panel__block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.assignment-side-panel__block > div,
.assignment-report-summary > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.assignment-two-column {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 16px;
  min-height: 0;
  overflow: hidden;
}

.assignment-report-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.assignment-report-summary--metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.assignment-card :deep(.el-table) {
  flex: 1;
  min-height: 0;
}

.assignment-card :deep(.el-table__inner-wrapper),
.assignment-card :deep(.el-table__body-wrapper) {
  min-height: 0;
}

@media (max-width: 1280px) {
  .assignment-section-head,
  .assignment-tools,
  .assignment-pagination,
  .assignment-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .assignment-summary-grid,
  .assignment-summary-grid--five,
  .assignment-settings-layout,
  .assignment-form-grid,
  .assignment-form-grid--wide,
  .assignment-form-grid--three,
  .assignment-two-column,
  .assignment-report-summary {
    grid-template-columns: 1fr;
  }

  .assignment-surface,
  .assignment-surface--overview,
  .assignment-surface--submissions,
  .assignment-surface--plagiarism,
  .assignment-surface--settings {
    display: flex;
  }

  .assignment-select,
  .assignment-search,
  .assignment-number {
    width: 100%;
  }
}
</style>

<template>
  <section class="assignment-submissions-page">
    <header class="assignment-submissions-page__hero">
      <div>
        <p class="assignment-submissions-page__eyebrow">Review Flow</p>
        <h1>提交与批改</h1>
        <p>{{ assignment?.title || '当前作业' }}：先看谁已提交、谁需要提醒、谁的提交无法参与比较。</p>
      </div>
      <div class="assignment-submissions-page__hero-actions">
        <el-select v-model="selectedAssignmentId" class="assignment-submissions-page__select" @change="handleAssignmentChange">
          <el-option v-for="item in assignmentOptions" :key="item.id" :label="item.title" :value="String(item.id)" />
        </el-select>
        <el-button round @click="goToSettings">查看设置</el-button>
        <el-button type="primary" round @click="goToPlagiarismResults">查看结果</el-button>
      </div>
    </header>

    <section class="assignment-submissions-page__stats">
      <article v-for="item in summaryCards" :key="item.label" class="assignment-submissions-page__stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </section>

    <section class="assignment-submissions-page__content">
      <div class="assignment-submissions-page__content-head">
        <div>
          <h2>学生提交列表</h2>
          <p>用老师能直接看懂的状态词来展示，不把数据库字段暴露在首屏。</p>
        </div>
      </div>

      <div class="assignment-submissions-page__table-shell">
        <el-table v-loading="loading" :data="submissions" height="100%" empty-text="当前作业还没有提交记录">
          <el-table-column label="学生" min-width="220">
            <template #default="scope">
              <div class="assignment-submissions-page__student-cell">
                <strong>{{ scope.row.studentName }}</strong>
                <span>ID {{ scope.row.studentNumber }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="当前状态" min-width="280">
            <template #default="scope">
              <div class="assignment-submissions-page__tags">
                <el-tag effect="plain" :type="scope.row.parseOkFiles > 0 ? 'success' : 'danger'">
                  {{ scope.row.parseOkFiles > 0 ? '可参与比较' : '无法参与比较' }}
                </el-tag>
                <el-tag effect="plain" :type="scope.row.isLate ? 'warning' : 'info'">
                  {{ scope.row.isLate ? '迟交' : '按时提交' }}
                </el-tag>
                <el-tag effect="plain">
                  {{ scope.row.fileCount }} 个文件 / {{ scope.row.parseOkFiles }} 个可用
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="version" label="版本数" width="100" />
          <el-table-column label="最后提交时间" min-width="190">
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
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchTeacherAssignments,
  fetchTeacherAssignmentDetail,
  fetchTeacherAssignmentStats,
  fetchTeacherAssignmentSubmissions
} from '../../api/teacherAssignments'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const assignment = ref(null)
const submissions = ref([])
const stats = ref(null)
const assignmentOptions = ref([])
const selectedAssignmentId = ref(String(route.params.assignmentId || ''))

const incomparableCount = computed(() => submissions.value.filter((item) => item.parseOkFiles === 0).length)
const summaryCards = computed(() => [
  { label: '应交人数', value: stats.value?.studentCount || 0, hint: '班级范围内应提交学生数' },
  { label: '已交人数', value: stats.value?.submittedStudentCount || 0, hint: '至少提交过一次的学生' },
  { label: '未交人数', value: stats.value?.unsubmittedStudentCount || 0, hint: '建议优先提醒' },
  { label: '迟交人数', value: stats.value?.lateSubmissionCount || 0, hint: '需要单独关注' },
  { label: '无法比较', value: incomparableCount.value, hint: '解析失败或没有可用代码' }
])

onMounted(async () => {
  await loadAssignmentOptions()
  await loadPage()
})

async function loadAssignmentOptions() {
  const result = await fetchTeacherAssignments({ page: 1, size: 100 })
  assignmentOptions.value = result.records || []
}

async function loadPage() {
  loading.value = true
  try {
    const assignmentId = await resolveAssignmentId()
    if (!assignmentId) {
      assignment.value = null
      stats.value = null
      submissions.value = []
      return
    }
    const [detail, statsResult, submissionsResult] = await Promise.all([
      fetchTeacherAssignmentDetail(assignmentId),
      fetchTeacherAssignmentStats(assignmentId),
      fetchTeacherAssignmentSubmissions(assignmentId)
    ])
    assignment.value = detail
    stats.value = statsResult
    submissions.value = submissionsResult || []
    selectedAssignmentId.value = String(assignmentId || '')
  } finally {
    loading.value = false
  }
}

async function resolveAssignmentId() {
  const routeAssignmentId = String(route.params.assignmentId || '').trim()
  if (routeAssignmentId) return routeAssignmentId
  if (!assignmentOptions.value.length) return ''
  const fallbackId = String(selectedAssignmentId.value || assignmentOptions.value[0]?.id || '')
  if (!fallbackId) return ''
  if (route.path === '/teacher/assignments/submissions') {
    await router.replace(`/teacher/assignments/${fallbackId}/submissions`)
  }
  return fallbackId
}

function handleAssignmentChange(value) {
  router.push(`/teacher/assignments/${value}/submissions`)
}

function goToSettings() {
  if (!assignment.value?.id) return
  router.push(`/teacher/assignments/${assignment.value.id}/settings`)
}

function goToPlagiarismResults() {
  if (!assignment.value?.id) return
  router.push(`/teacher/assignments/${assignment.value.id}/plagiarism/results`)
}

function goToSubmissionDetail(row) {
  router.push({ path: `/teacher/submissions/${row.submissionId}`, query: { assignmentId: assignment.value?.id } })
}
</script>

<style scoped>
.assignment-submissions-page {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 18px;
  padding: 26px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top left, rgba(84, 122, 255, 0.08), transparent 25%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(246, 249, 255, 0.93));
  border: 1px solid rgba(207, 217, 235, 0.6);
  overflow: hidden;
}

.assignment-submissions-page__hero,
.assignment-submissions-page__content-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.assignment-submissions-page__eyebrow {
  margin: 0 0 10px;
  color: #6484ff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.assignment-submissions-page__hero h1,
.assignment-submissions-page__content-head h2 {
  margin: 0;
  color: #101626;
}

.assignment-submissions-page__hero h1 {
  font-size: 40px;
}

.assignment-submissions-page__hero p,
.assignment-submissions-page__content-head p {
  margin: 10px 0 0;
  color: #70819a;
  line-height: 1.6;
}

.assignment-submissions-page__hero-actions {
  display: flex;
  gap: 10px;
}

.assignment-submissions-page__select {
  width: 240px;
}

.assignment-submissions-page__stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.assignment-submissions-page__stat-card {
  display: grid;
  gap: 6px;
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(207, 217, 235, 0.64);
}

.assignment-submissions-page__stat-card span,
.assignment-submissions-page__student-cell span {
  color: #7b8aa2;
}

.assignment-submissions-page__stat-card strong {
  font-size: 30px;
  color: #101626;
}

.assignment-submissions-page__stat-card small {
  color: #90a0b7;
}

.assignment-submissions-page__content {
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 14px;
  padding: 18px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(207, 217, 235, 0.55);
}

.assignment-submissions-page__table-shell {
  min-height: 0;
}

.assignment-submissions-page__student-cell,
.assignment-submissions-page__tags {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.assignment-submissions-page__student-cell strong {
  color: #101626;
}

.assignment-submissions-page__tags {
  flex-direction: row;
  flex-wrap: wrap;
}

@media (max-width: 1280px) {
  .assignment-submissions-page__hero,
  .assignment-submissions-page__stats {
    grid-template-columns: 1fr;
  }
}
</style>

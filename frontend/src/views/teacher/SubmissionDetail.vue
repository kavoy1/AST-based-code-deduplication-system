<template>
  <div v-if="submission" class="submission-detail-page">
    <section class="submission-detail-shell">
      <div class="submission-detail-shell__header">
        <div>
          <el-button text @click="goBack">返回提交与批改</el-button>
          <h1>提交详情</h1>
          <p>学生ID {{ submission.studentNumber }} · 提交ID {{ submission.submissionId }}</p>
        </div>
      </div>

      <div class="submission-detail-tabs">
        <button
          v-for="item in tabs"
          :key="item.value"
          type="button"
          class="submission-detail-tabs__item"
          :class="{ active: currentTab === item.value }"
          @click="currentTab = item.value"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="submission-detail-summary-grid">
        <div class="submission-detail-card"><span>版本号</span><strong>{{ submission.version }}</strong></div>
        <div class="submission-detail-card"><span>文件数</span><strong>{{ submission.fileCount }}</strong></div>
        <div class="submission-detail-card"><span>解析成功数</span><strong>{{ submission.parseOkFiles }}</strong></div>
        <div class="submission-detail-card"><span>提交状态</span><strong>{{ submission.isLate ? '迟交' : '正常' }}</strong></div>
      </div>

      <div v-if="currentTab === 'history'" class="submission-detail-panel">
        <article class="submission-detail-card submission-detail-card--flex">
          <div class="submission-detail-card__head"><h3>版本历史</h3><span>{{ historyRows.length }} 个版本</span></div>
          <el-table :data="historyRows" height="100%" empty-text="暂无版本历史">
            <el-table-column prop="version" label="版本" width="90" />
            <el-table-column label="文件数" width="100"><template #default="scope">{{ scope.row.fileCount }}</template></el-table-column>
            <el-table-column label="解析成功数" width="120"><template #default="scope">{{ scope.row.parseOkFiles }}</template></el-table-column>
            <el-table-column label="提交时间" min-width="170"><template #default="scope">{{ scope.row.lastSubmittedAt || '后端暂未返回' }}</template></el-table-column>
            <el-table-column label="迟交" width="90" align="center"><template #default="scope">{{ scope.row.isLate ? '是' : '否' }}</template></el-table-column>
            <el-table-column label="最新版本" width="100" align="center"><template #default="scope">{{ scope.row.submissionId === submission.submissionId ? '是' : '否' }}</template></el-table-column>
          </el-table>
        </article>
      </div>

      <div v-else class="submission-detail-panel">
        <article class="submission-detail-card submission-detail-card--flex">
          <div class="submission-detail-card__head"><h3>文件明细</h3></div>
          <div class="submission-detail-placeholder">
            <strong>前端骨架已预留</strong>
            <p>后端当前未提供提交文件明细接口。本页先保留文件明细区域，待接口补齐后直接接入文件名、大小、SHA256、解析状态和错误原因。</p>
            <ul>
              <li>当前版本文件数：{{ submission.fileCount }}</li>
              <li>解析成功文件数：{{ submission.parseOkFiles }}</li>
              <li>可比对状态：{{ submission.parseOkFiles > 0 ? '可比对' : '不可比对' }}</li>
            </ul>
          </div>
        </article>
      </div>
    </section>
  </div>

  <div v-else class="submission-detail-empty">
    <el-empty description="未找到该提交">
      <el-button type="primary" round @click="goBack">返回提交与批改</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchTeacherAssignmentSubmissions } from '../../api/teacherAssignments'

const route = useRoute()
const router = useRouter()

const tabs = [
  { value: 'history', label: '版本历史' },
  { value: 'files', label: '文件明细' }
]

const currentTab = ref('history')
const submission = ref(null)
const historyRows = ref([])

const assignmentId = computed(() => String(route.query.assignmentId || ''))

onMounted(loadPage)

async function loadPage() {
  if (!assignmentId.value) return
  const rows = await fetchTeacherAssignmentSubmissions(assignmentId.value)
  const currentSubmissionId = Number(route.params.submissionId)
  const current = rows.find((item) => Number(item.submissionId) === currentSubmissionId) || null
  submission.value = current
  if (!current) return
  historyRows.value = rows
    .filter((item) => Number(item.studentId) === Number(current.studentId))
    .sort((a, b) => Number(b.version || 0) - Number(a.version || 0))
}

function goBack() {
  router.push({ path: '/teacher/assignments', query: { tab: 'submissions', assignmentId: assignmentId.value || undefined } })
}
</script>

<style scoped>
.submission-detail-page {
  height: 100%;
}

.submission-detail-shell {
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

.submission-detail-shell__header h1 {
  margin: 8px 0 6px;
  font-size: 30px;
}

.submission-detail-shell__header p,
.submission-detail-placeholder p,
.submission-detail-placeholder li {
  margin: 0;
  color: #6f7f96;
}

.submission-detail-tabs {
  display: inline-flex;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(18, 25, 38, 0.06);
}

.submission-detail-tabs__item {
  border: 0;
  padding: 10px 18px;
  border-radius: 999px;
  background: transparent;
  color: #68798f;
  cursor: pointer;
}

.submission-detail-tabs__item.active {
  background: #121926;
  color: #fff;
}

.submission-detail-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.submission-detail-panel {
  min-height: 0;
  flex: 1;
  display: flex;
}

.submission-detail-card {
  width: 100%;
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(127, 142, 163, 0.14);
  box-shadow: 0 16px 38px rgba(160, 176, 199, 0.08);
}

.submission-detail-card--flex {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.submission-detail-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.submission-detail-card__head h3 {
  margin: 0;
}

.submission-detail-card span {
  color: #7f8ea3;
}

.submission-detail-card strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
}

.submission-detail-placeholder {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(18, 25, 38, 0.04);
}

.submission-detail-placeholder strong {
  font-size: 16px;
}

.submission-detail-placeholder ul {
  margin: 0;
  padding-left: 18px;
}

.submission-detail-empty {
  display: grid;
  place-items: center;
  height: 100%;
}

@media (max-width: 1280px) {
  .submission-detail-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>

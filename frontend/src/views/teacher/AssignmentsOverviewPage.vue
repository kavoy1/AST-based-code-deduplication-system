<template>
  <section class="assignment-overview-page">
    <header class="assignment-overview-page__hero">
      <div>
        <p class="assignment-overview-page__eyebrow">Assignment Center</p>
        <h1>作业总览</h1>
        <p>把老师真正要处理的作业放在首屏。当前页固定展示 4 张卡片，更多内容翻页查看。</p>
      </div>
      <div class="assignment-overview-page__hero-actions">
        <el-input
          v-model="keyword"
          class="assignment-overview-page__search"
          placeholder="搜索作业标题"
          clearable
        />
        <el-select v-model="statusFilter" class="assignment-overview-page__select">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="classFilter" class="assignment-overview-page__select">
          <el-option label="全部班级" value="" />
          <el-option v-for="item in classList" :key="item.id" :label="item.name" :value="String(item.id)" />
        </el-select>
        <el-button type="primary" round @click="goToCreate">发布作业</el-button>
      </div>
    </header>

    <section class="assignment-overview-page__stats">
      <article v-for="item in summaryCards" :key="item.label" class="assignment-overview-page__stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </section>

    <section class="assignment-overview-page__content">
      <div class="assignment-overview-page__content-head">
        <div>
          <h2>本学期作业</h2>
          <p>每页最多展示 {{ pageSize }} 份作业，让页面保持在一个屏幕里。</p>
        </div>
        <span class="assignment-overview-page__page-meta">第 {{ currentPage }} / {{ pageCount }} 页</span>
      </div>

      <div v-loading="loading" class="assignment-overview-page__grid">
        <AssignmentOverviewCard
          v-for="assignment in pagedAssignments"
          :key="assignment.id"
          :assignment="assignment"
          @settings="goToSettings"
          @submissions="goToSubmissions"
          @launch="goToPlagiarismLaunch"
          @results="goToPlagiarismResults"
        />

        <el-empty
          v-if="!loading && !filteredAssignments.length"
          class="assignment-overview-page__empty"
          description="当前筛选条件下没有作业"
        >
          <el-button type="primary" round @click="goToCreate">新建第一份作业</el-button>
        </el-empty>
      </div>

      <footer v-if="filteredAssignments.length > pageSize" class="assignment-overview-page__footer">
        <el-pagination
          v-model:current-page="currentPage"
          background
          layout="prev, pager, next"
          :page-size="pageSize"
          :total="filteredAssignments.length"
        />
      </footer>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../api/request'
import { fetchTeacherAssignments } from '../../api/teacherAssignments'
import { normalizeClasses } from './assignmentMappers'
import AssignmentOverviewCard from './components/AssignmentOverviewCard.vue'

const router = useRouter()

const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const classFilter = ref('')
const currentPage = ref(1)
const classList = ref([])
const assignments = ref([])
const pageSize = 4

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'active', label: '进行中' },
  { value: 'ended', label: '已结束' },
  { value: 'draft', label: '草稿' }
]

const filteredAssignments = computed(() => {
  const keywordText = keyword.value.trim().toLowerCase()
  const selectedClassId = classFilter.value ? Number(classFilter.value) : null

  return [...assignments.value]
    .filter((item) => {
      if (statusFilter.value && item.status !== statusFilter.value) return false
      if (selectedClassId && Array.isArray(item.classIds) && item.classIds.length && !item.classIds.includes(selectedClassId)) {
        return false
      }
      if (keywordText) {
        const text = `${item.title} ${item.classNamesText} ${(item.classNames || []).join(' ')}`.toLowerCase()
        if (!text.includes(keywordText)) return false
      }
      return true
    })
    .sort((left, right) => {
      const weight = { active: 0, ended: 1, draft: 2 }
      const leftWeight = weight[left.status] ?? 9
      const rightWeight = weight[right.status] ?? 9
      if (leftWeight !== rightWeight) return leftWeight - rightWeight
      return new Date(right.endAt || 0).getTime() - new Date(left.endAt || 0).getTime()
    })
})

const pageCount = computed(() => Math.max(1, Math.ceil(filteredAssignments.value.length / pageSize)))

const pagedAssignments = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredAssignments.value.slice(start, start + pageSize)
})

const summaryCards = computed(() => {
  const total = filteredAssignments.value.length
  const active = filteredAssignments.value.filter((item) => item.status === 'active').length
  const ended = filteredAssignments.value.filter((item) => item.status === 'ended').length
  const needFollowUp = filteredAssignments.value.filter((item) => item.unsubmittedCount > 0 || item.lateSubmissionCount > 0).length
  return [
    { label: '全部作业', value: total, hint: '当前筛选结果' },
    { label: '进行中', value: active, hint: '学生仍可提交' },
    { label: '已结束', value: ended, hint: '适合进入查重' },
    { label: '需要跟进', value: needFollowUp, hint: '建议优先看提交与批改' }
  ]
})

watch([keyword, statusFilter, classFilter], () => {
  currentPage.value = 1
})

watch(pageCount, (value) => {
  if (currentPage.value > value) currentPage.value = value
})

onMounted(loadPage)

async function loadPage() {
  loading.value = true
  try {
    const [classResult, assignmentResult] = await Promise.all([
      request.get('/teacher/classes', { params: { page: 1, limit: 100 } }),
      fetchTeacherAssignments({ page: 1, size: 100 })
    ])
    classList.value = normalizeClasses(classResult)
    assignments.value = assignmentResult.records || []
  } finally {
    loading.value = false
  }
}

function goToCreate() {
  router.push('/teacher/assignments/create')
}

function goToSettings(assignment) {
  router.push(`/teacher/assignments/${assignment.id}/settings`)
}

function goToSubmissions(assignment) {
  router.push(`/teacher/assignments/${assignment.id}/submissions`)
}

function goToPlagiarismLaunch(assignment) {
  router.push(`/teacher/assignments/${assignment.id}/plagiarism/run`)
}

function goToPlagiarismResults(assignment) {
  router.push(`/teacher/assignments/${assignment.id}/plagiarism/results`)
}
</script>

<style scoped>
.assignment-overview-page {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 14px;
  padding: 20px 22px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top left, rgba(111, 91, 255, 0.08), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(246, 249, 255, 0.93));
  border: 1px solid rgba(207, 217, 235, 0.6);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
  overflow: hidden;
}

.assignment-overview-page__hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: start;
}

.assignment-overview-page__eyebrow {
  margin: 0 0 8px;
  color: #8a78ff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.assignment-overview-page__hero h1,
.assignment-overview-page__content-head h2 {
  margin: 0;
  color: #101626;
}

.assignment-overview-page__hero h1 {
  font-size: 34px;
  line-height: 1;
}

.assignment-overview-page__hero p,
.assignment-overview-page__content-head p {
  margin: 8px 0 0;
  color: #71829b;
  line-height: 1.5;
}

.assignment-overview-page__hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(207, 217, 235, 0.7);
  box-shadow: 0 12px 30px rgba(191, 203, 227, 0.12);
}

.assignment-overview-page__search {
  width: 220px;
}

.assignment-overview-page__select {
  width: 138px;
}

.assignment-overview-page__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.assignment-overview-page__stat-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(207, 217, 235, 0.64);
}

.assignment-overview-page__stat-card span {
  color: #7d8ca4;
  font-size: 13px;
}

.assignment-overview-page__stat-card strong {
  font-size: 28px;
  color: #101626;
}

.assignment-overview-page__stat-card small {
  color: #90a0b7;
}

.assignment-overview-page__content {
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 12px;
  padding: 14px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(207, 217, 235, 0.55);
}

.assignment-overview-page__content-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.assignment-overview-page__content-head h2 {
  font-size: 20px;
}

.assignment-overview-page__page-meta {
  color: #7d8ca4;
  font-size: 13px;
  white-space: nowrap;
}

.assignment-overview-page__grid {
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-content: start;
  overflow: hidden;
}

.assignment-overview-page__empty {
  grid-column: 1 / -1;
  padding: 22px 0;
}

.assignment-overview-page__footer {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1320px) {
  .assignment-overview-page__hero,
  .assignment-overview-page__stats,
  .assignment-overview-page__grid {
    grid-template-columns: 1fr;
  }

  .assignment-overview-page__content-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

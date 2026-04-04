<template>
  <section class="assignment-overview-page">
    <header class="assignment-overview-page__toolbar">
      <div class="assignment-overview-page__filters">
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
      </div>

      <div class="assignment-overview-page__actions">
        <el-button type="primary" round @click="goToCreate">发布作业</el-button>
      </div>
    </header>

    <section class="assignment-overview-page__content">
      <div class="assignment-overview-page__content-head">
        <span class="assignment-overview-page__result-count">共 {{ filteredAssignments.length }} 份作业</span>
        <span v-if="filteredAssignments.length > pageSize" class="assignment-overview-page__page-meta">第 {{ currentPage }} / {{ pageCount }} 页</span>
      </div>

      <div v-loading="loading" class="assignment-overview-page__grid">
        <AssignmentOverviewCard
          v-for="assignment in pagedAssignments"
          :key="assignment.id"
          :assignment="assignment"
          @settings="goToSettings"
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
import { isOverviewLaunchDisabled } from './assignmentOverviewCardHelpers.js'
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

function goToPlagiarismLaunch(assignment) {
  if (isOverviewLaunchDisabled(assignment)) {
    return
  }
  router.push(`/teacher/assignments/${assignment.id}/plagiarism/run`)
}

function goToPlagiarismResults(assignment) {
  if (!assignment?.hasPlagiarismJob) {
    return
  }
  router.push(`/teacher/assignments/${assignment.id}/plagiarism/results`)
}
</script>

<style scoped>
.assignment-overview-page {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 14px;
  padding: 18px 20px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top left, rgba(111, 91, 255, 0.08), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(246, 249, 255, 0.93));
  border: 1px solid rgba(207, 217, 235, 0.6);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
  overflow: hidden;
}

.assignment-overview-page__toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.assignment-overview-page__filters,
.assignment-overview-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(207, 217, 235, 0.7);
  box-shadow: 0 12px 30px rgba(191, 203, 227, 0.12);
}

.assignment-overview-page__filters {
  min-width: 0;
}

.assignment-overview-page__actions {
  justify-content: flex-end;
}

.assignment-overview-page__search {
  width: 240px;
}

.assignment-overview-page__select {
  width: 140px;
}

.assignment-overview-page__content {
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 10px;
}

.assignment-overview-page__content-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 4px;
}

.assignment-overview-page__result-count,
.assignment-overview-page__page-meta {
  color: #71829b;
  font-size: 13px;
  font-weight: 600;
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
  .assignment-overview-page__toolbar,
  .assignment-overview-page__grid {
    grid-template-columns: 1fr;
  }

  .assignment-overview-page__actions {
    justify-content: flex-start;
  }
}
</style>

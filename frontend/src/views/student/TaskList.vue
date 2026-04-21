<template>
  <div class="workspace-page">
    <WorkspacePanel title="我的作业">
      <template #extra>
        <div v-if="approvedClasses.length" class="student-task-header-tools">
          <el-select
            v-model="selectedClassId"
            class="student-task-class-filter"
            placeholder="筛选班级"
            @change="handleClassChange"
          >
            <el-option label="全部班级" value="all" />
            <el-option
              v-for="item in approvedClasses"
              :key="item.id"
              :label="item.className"
              :value="String(item.id)"
            />
          </el-select>
          <el-button round @click="reloadAssignments">刷新</el-button>
        </div>
      </template>

      <div v-if="classesLoading" class="workspace-empty">
        <LoadingSpinner label="正在加载班级列表..." />
      </div>

      <WorkspaceEmpty
        v-else-if="approvedClasses.length === 0"
        title="还没有可查看的班级"
        description="先加入班级，审核通过后就能查看对应作业。"
        action-text="去加入班级"
        @action="router.push('/student/classes')"
      />

      <template v-else>
        <div class="workspace-toolbar student-task-toolbar">
          <div class="workspace-segmented">
            <button
              v-for="item in statusTabs"
              :key="item.value"
              :class="{ active: statusFilter === item.value }"
              @click="statusFilter = item.value"
            >
              {{ item.label }}
            </button>
          </div>

          <div class="student-task-search-wrap">
            <input
              v-model="searchQuery"
              type="text"
              class="student-task-search-input"
              placeholder="搜索作业标题"
              aria-label="搜索作业标题"
            />
          </div>
        </div>

        <WorkspaceEmpty
          v-if="assignmentsLoading"
          title="正在加载作业"
          description="正在同步当前班级的作业列表。"
        />

        <WorkspaceEmpty
          v-else-if="filteredAssignments.length === 0"
          title="当前没有符合条件的作业"
          description="可以调整筛选条件，或者切换到其他班级看看。"
        />

        <div v-else class="student-assignment-grid">
          <article v-for="item in pagedAssignments" :key="item.id" class="student-assignment-card">
            <div class="student-assignment-card__main">
              <div class="student-assignment-card__header">
                <div>
                  <div class="student-assignment-card__eyebrow">{{ item.className || '全部班级' }} · {{ item.language }}</div>
                  <h3>{{ item.title }}</h3>
                </div>
                <div class="student-assignment-card__status">
                  <el-tag :type="item.statusTagType" round>{{ item.statusText }}</el-tag>
                  <span v-if="item.countdownLabel" class="student-assignment-card__countdown">
                    {{ item.countdownLabel }}
                  </span>
                </div>
              </div>

              <div class="student-assignment-card__timeline">
                <div class="student-assignment-card__timebox">
                  <span>开始时间</span>
                  <strong>{{ item.startAtLabel }}</strong>
                </div>
                <div class="student-assignment-card__timebox">
                  <span>截止时间</span>
                  <strong>{{ item.endAtLabel }}</strong>
                </div>
              </div>
            </div>

            <div class="student-assignment-card__aside">
              <div class="student-assignment-card__flags">
                <span class="workspace-pill">最多 {{ item.maxFiles }} 个文件</span>
                <span class="workspace-pill">{{ item.allowResubmit ? '允许重复提交' : '仅可提交一次' }}</span>
              </div>

              <div class="student-assignment-card__actions">
                <el-button type="primary" @click="openAssignment(item)">查看作业</el-button>
                <el-button v-if="item.canSubmit" plain @click="openSubmit(item)">去提交</el-button>
              </div>
            </div>
          </article>
        </div>

        <div v-if="filteredAssignments.length" class="student-task-pagination">
          <div class="student-task-pagination__summary">
            <span class="student-task-pagination__total">共 {{ filteredAssignments.length }} 个作业</span>
            <span class="student-task-pagination__meta">
              第 {{ currentPage }} / {{ paginationMeta.totalPages }} 页 · 显示 {{ paginationMeta.from }}-{{ paginationMeta.to }} 条
            </span>
          </div>
          <div class="student-task-pagination__controls">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              layout="sizes, prev, pager, next"
              :page-sizes="[2, 4, 6]"
              :total="filteredAssignments.length"
            />
          </div>
        </div>
      </template>
    </WorkspacePanel>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchStudentAssignmentsByClass, fetchStudentClasses } from '../../api/student'
import LoadingSpinner from '../../components/LoadingSpinner.vue'
import WorkspaceEmpty from '../../components/workspace/WorkspaceEmpty.vue'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import { buildCountdownLabel, buildPaginationMeta, filterAssignmentsByClass, paginateAssignments } from './taskListHelpers'

const route = useRoute()
const router = useRouter()

const studentClasses = ref([])
const selectedClassId = ref('all')
const searchQuery = ref('')
const statusFilter = ref('all')
const assignments = ref([])
const classesLoading = ref(false)
const assignmentsLoading = ref(false)
const nowTick = ref(Date.now())
const currentPage = ref(1)
const pageSize = ref(2)

let timer = null

const statusTabs = [
  { label: '全部', value: 'all' },
  { label: '进行中', value: 'active' },
  { label: '未开始', value: 'pending' },
  { label: '已结束', value: 'ended' }
]

const approvedClasses = computed(() => studentClasses.value.filter((item) => item.status === 1))

const filteredAssignments = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase()
  const classScopedAssignments = filterAssignmentsByClass(assignments.value, selectedClassId.value)

  return classScopedAssignments
    .filter((item) => {
      const matchStatus = statusFilter.value === 'all' || item.status === statusFilter.value
      const matchKeyword = !keyword || item.title.toLowerCase().includes(keyword)
      return matchStatus && matchKeyword
    })
    .map((item) => ({
      ...item,
      countdownLabel: item.status === 'active' ? buildCountdownLabel(item.endAt, nowTick.value) : ''
    }))
})

const pagedAssignments = computed(() => paginateAssignments(filteredAssignments.value, currentPage.value, pageSize.value).items)
const paginationMeta = computed(() => buildPaginationMeta(filteredAssignments.value.length, currentPage.value, pageSize.value))

async function fetchClassesAndResolveSelection() {
  classesLoading.value = true
  try {
    studentClasses.value = await fetchStudentClasses()
    const queryClassId = typeof route.query.classId === 'string' ? route.query.classId : ''
    const matchedClass = approvedClasses.value.find((item) => String(item.id) === String(queryClassId))
    selectedClassId.value = matchedClass ? String(matchedClass.id) : 'all'
  } finally {
    classesLoading.value = false
  }
}

async function reloadAssignments() {
  if (!approvedClasses.value.length) {
    assignments.value = []
    return
  }

  assignmentsLoading.value = true
  try {
    const list = await Promise.all(
      approvedClasses.value.map(async (item) => {
        const items = await fetchStudentAssignmentsByClass(item.id, {
          classId: item.id,
          className: item.className
        })
        return items
      })
    )

    assignments.value = list.flat().sort((left, right) => {
      const statusWeight = { active: 0, pending: 1, ended: 2 }
      const statusGap = statusWeight[left.status] - statusWeight[right.status]
      if (statusGap !== 0) return statusGap
      return new Date(right.startAt || right.endAt || 0).getTime() - new Date(left.startAt || left.endAt || 0).getTime()
    })
  } finally {
    assignmentsLoading.value = false
  }
}

function handleClassChange(value) {
  currentPage.value = 1
  if (value === 'all') {
    router.replace({ path: '/student/tasks' })
    return
  }

  const selected = approvedClasses.value.find((item) => String(item.id) === String(value))
  router.replace({
    path: '/student/tasks',
    query: selected
      ? { classId: selected.id, className: selected.className }
      : {}
  })
}

function openAssignment(item) {
  router.push({
    path: `/student/assignments/${item.id}`,
    query: {
      classId: item.classId,
      className: item.className
    }
  })
}

function openSubmit(item) {
  openAssignment(item)
}

watch(
  () => route.query.classId,
  (newValue) => {
    selectedClassId.value = typeof newValue === 'string' && newValue ? String(newValue) : 'all'
    currentPage.value = 1
  }
)

watch([statusFilter, searchQuery, pageSize], () => {
  currentPage.value = 1
})

onMounted(async () => {
  await fetchClassesAndResolveSelection()
  await reloadAssignments()
  timer = window.setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.workspace-page {
  display: flex;
  height: 100%;
  min-height: 100%;
  overflow: hidden;
}

.workspace-page :deep(.workspace-panel) {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
}

.workspace-page :deep(.workspace-panel__body) {
  display: flex;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.workspace-page :deep(.workspace-panel) {
  overflow: hidden;
}

.student-task-header-tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.student-task-class-filter {
  width: 224px;
}

.student-task-header-tools :deep(.el-select__wrapper) {
  min-height: 48px;
  padding: 0 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(242, 244, 255, 0.96));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.7),
    0 14px 28px rgba(122, 133, 255, 0.12);
  border: 1px solid rgba(120, 133, 255, 0.12);
  transition: box-shadow 180ms ease, transform 180ms ease, border-color 180ms ease;
}

.student-task-header-tools :deep(.el-select__wrapper:hover),
.student-task-header-tools :deep(.el-select__wrapper.is-focused) {
  border-color: rgba(111, 133, 255, 0.26);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.76),
    0 18px 34px rgba(122, 133, 255, 0.16);
}

.student-task-header-tools :deep(.el-select__selected-item) {
  font-weight: 600;
  color: #5463d8;
}

.student-task-header-tools :deep(.el-select__caret) {
  color: #7c87d9;
}

.student-task-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  margin-bottom: 14px;
  align-items: center;
  gap: 14px;
}

.student-task-search-wrap {
  justify-self: end;
  flex-shrink: 0;
}

.student-task-search-input {
  width: 220px;
  height: 45px;
  padding: 12px;
  border-radius: 12px;
  border: 1.5px solid lightgrey;
  outline: none;
  background: rgba(255, 255, 255, 0.96);
  color: var(--text-strong);
  transition: all 0.3s cubic-bezier(0.19, 1, 0.22, 1);
  box-shadow: 0 0 20px -18px rgba(15, 23, 42, 0.72);
}

.student-task-search-input::placeholder {
  color: #9aa6bb;
}

.student-task-search-input:hover {
  border: 2px solid lightgrey;
  box-shadow: 0 0 20px -17px rgba(15, 23, 42, 0.76);
}

.student-task-search-input:active {
  transform: scale(0.98);
}

.student-task-search-input:focus {
  border: 2px solid grey;
}

.student-assignment-grid {
  display: grid;
  gap: 14px;
  min-height: 0;
  flex: 1;
  overflow: auto;
  padding-right: 4px;
}

.student-assignment-card {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 18px 20px;
  border-radius: 26px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(247, 245, 255, 0.92));
  border: 1px solid rgba(91, 91, 214, 0.08);
  box-shadow: 0 18px 36px rgba(135, 94, 255, 0.08);
}

.student-assignment-card__main {
  flex: 1;
  min-width: 0;
}

.student-assignment-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.student-assignment-card__eyebrow {
  margin-bottom: 8px;
  color: #7b6fc8;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.student-assignment-card__header h3 {
  margin: 0;
  font-size: 24px;
  line-height: 1.1;
}

.student-assignment-card__status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.student-assignment-card__countdown {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(227, 53, 78, 0.12);
  border: 1px solid rgba(227, 53, 78, 0.18);
  color: #d92d20;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.015em;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.4);
}

.student-assignment-card__timeline {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.student-assignment-card__timebox {
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(29, 35, 43, 0.06);
}

.student-assignment-card__timebox span {
  display: block;
  margin-bottom: 8px;
  color: var(--text-soft);
  font-size: 12px;
}

.student-assignment-card__timebox strong {
  font-size: 16px;
}

.student-assignment-card__aside {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 240px;
  min-width: 240px;
}

.student-assignment-card__flags {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.student-assignment-card__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
}

.student-assignment-card__actions .el-button {
  width: 100%;
}

.student-assignment-card__actions .el-button + .el-button {
  display: none;
}

.student-task-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-top: 16px;
  padding: 18px 20px;
  border-radius: 24px;
  border: 1px solid rgba(109, 124, 255, 0.1);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(245, 247, 255, 0.94));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.68),
    0 12px 26px rgba(124, 135, 217, 0.08);
  flex-shrink: 0;
}

.student-task-pagination__summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.student-task-pagination__total {
  color: var(--text-strong);
  font-size: 15px;
  font-weight: 700;
}

.student-task-pagination__meta {
  color: var(--text-soft);
  font-size: 13px;
}

.student-task-pagination__controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
}

.student-task-pagination__controls :deep(.el-pagination) {
  --el-pagination-button-width: 40px;
  --el-pagination-button-height: 40px;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.student-task-pagination__controls :deep(.btn-prev),
.student-task-pagination__controls :deep(.btn-next),
.student-task-pagination__controls :deep(.el-pager li) {
  border-radius: 14px;
  border: 1px solid rgba(109, 124, 255, 0.12);
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-body);
  box-shadow: 0 8px 18px rgba(124, 135, 217, 0.08);
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}

.student-task-pagination__controls :deep(.btn-prev:hover),
.student-task-pagination__controls :deep(.btn-next:hover),
.student-task-pagination__controls :deep(.el-pager li:hover) {
  transform: translateY(-1px);
  border-color: rgba(109, 124, 255, 0.22);
  box-shadow: 0 12px 22px rgba(124, 135, 217, 0.12);
}

.student-task-pagination__controls :deep(.el-pager li.is-active) {
  border-color: transparent;
  background: linear-gradient(135deg, #6f77ff, #8458ff);
  color: #fff;
  box-shadow: 0 14px 28px rgba(116, 108, 255, 0.24);
}

.student-task-pagination__controls :deep(.el-pagination__sizes) {
  margin-right: 4px;
}

.student-task-pagination__controls :deep(.el-select__wrapper) {
  min-height: 42px;
  min-width: 102px;
  padding: 0 14px;
  border-radius: 16px;
  border: 1px solid rgba(109, 124, 255, 0.12);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 22px rgba(124, 135, 217, 0.08);
}

@media (max-width: 1100px) {
  .student-task-header-tools {
    width: 100%;
  }
}

@media (max-width: 980px) {
  .student-assignment-card {
    flex-direction: column;
  }

  .student-assignment-card__aside {
    width: 100%;
    min-width: 0;
  }

  .student-assignment-card__timeline {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .student-task-header-tools,
  .student-task-toolbar {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }

  .student-task-class-filter,
  .student-task-search-input {
    width: 100%;
  }

  .student-task-pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .student-task-pagination__controls {
    justify-content: flex-start;
  }
}
</style>


<template>
  <div class="workspace-page">
    <WorkspaceShellSection>
      <template #tools>
        <el-select v-model="selectedClassId" class="task-class-select" placeholder="选择班级" @change="handleClassChange">
          <el-option
            v-for="item in approvedClasses"
            :key="item.id"
            :label="item.className"
            :value="item.id"
          />
        </el-select>
        <el-button @click="reloadAssignments">刷新</el-button>
      </template>
    </WorkspaceShellSection>

    <WorkspacePanel title="我的作业" :subtitle="panelSubtitle">
      <div v-if="classesLoading" class="workspace-empty">
        <el-skeleton :rows="4" animated />
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
            <button v-for="item in statusTabs" :key="item.value" :class="{ active: statusFilter === item.value }" @click="statusFilter = item.value">
              {{ item.label }}
            </button>
          </div>
          <el-input v-model="searchQuery" clearable placeholder="搜索作业标题" class="student-task-search" />
        </div>

        <WorkspaceEmpty
          v-if="assignmentsLoading"
          title="正在加载作业"
          description="稍等一下，我们正在同步当前班级的作业列表。"
        />

        <WorkspaceEmpty
          v-else-if="filteredAssignments.length === 0"
          title="当前没有符合条件的作业"
          description="可以切换筛选条件，或者换一个班级看看。"
        />

        <div v-else class="student-assignment-grid">
          <article v-for="item in filteredAssignments" :key="item.id" class="student-assignment-card">
            <div class="student-assignment-card__header">
              <div>
                <h3>{{ item.title }}</h3>
                <p>{{ activeClassName }} · {{ item.language }}</p>
              </div>
              <el-tag :type="item.statusTagType" round>{{ item.statusText }}</el-tag>
            </div>

            <div class="student-assignment-card__meta">
              <span>开始时间：{{ item.startAtLabel }}</span>
              <span>截止时间：{{ item.endAtLabel }}</span>
            </div>

            <p class="student-assignment-card__description">
              {{ item.description || '暂无作业说明，点击详情查看完整信息。' }}
            </p>

            <div class="student-assignment-card__footer">
              <div class="student-assignment-card__flags">
                <span class="workspace-pill">最多 {{ item.maxFiles }} 个文件</span>
                <span class="workspace-pill">{{ item.allowResubmit ? '允许重交' : '仅可提交一次' }}</span>
              </div>
              <el-button type="primary" @click="openAssignment(item)">查看详情</el-button>
            </div>
          </article>
        </div>
      </template>
    </WorkspacePanel>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchStudentAssignmentsByClass,
  fetchStudentClasses
} from '../../api/student'
import WorkspaceEmpty from '../../components/workspace/WorkspaceEmpty.vue'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import WorkspaceShellSection from '../../components/workspace/WorkspaceShellSection.vue'

const route = useRoute()
const router = useRouter()

const studentClasses = ref([])
const selectedClassId = ref('')
const searchQuery = ref('')
const statusFilter = ref('all')
const assignments = ref([])
const classesLoading = ref(false)
const assignmentsLoading = ref(false)

const statusTabs = [
  { label: '全部', value: 'all' },
  { label: '进行中', value: 'active' },
  { label: '未开始', value: 'pending' },
  { label: '已结束', value: 'ended' }
]

const approvedClasses = computed(() => studentClasses.value.filter((item) => item.status === 1))
const activeClass = computed(() => studentClasses.value.find((item) => String(item.id) === String(selectedClassId.value)) || null)
const activeClassName = computed(() => activeClass.value?.className || route.query.className || '当前班级')
const panelSubtitle = computed(() => activeClass.value ? `${activeClass.value.className} 的作业列表已按 API 实时加载。` : '先选择班级，再查看当前班级下的作业。')

const filteredAssignments = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase()
  return assignments.value.filter((item) => {
    const matchStatus = statusFilter.value === 'all' || item.status === statusFilter.value
    const matchKeyword = !keyword || item.title.toLowerCase().includes(keyword)
    return matchStatus && matchKeyword
  })
})

async function fetchClassesAndResolveSelection() {
  classesLoading.value = true
  try {
    studentClasses.value = await fetchStudentClasses()
    const queryClassId = typeof route.query.classId === 'string' ? route.query.classId : ''
    const matchedClass = approvedClasses.value.find((item) => String(item.id) === String(queryClassId))
    const fallbackClass = approvedClasses.value[0] || null

    if (matchedClass) {
      selectedClassId.value = String(matchedClass.id)
    } else if (fallbackClass) {
      selectedClassId.value = String(fallbackClass.id)
      router.replace({
        path: '/student/tasks',
        query: {
          classId: fallbackClass.id,
          className: fallbackClass.className
        }
      })
    } else {
      selectedClassId.value = ''
    }
  } finally {
    classesLoading.value = false
  }
}

async function reloadAssignments() {
  if (!selectedClassId.value) {
    assignments.value = []
    return
  }
  assignmentsLoading.value = true
  try {
    assignments.value = await fetchStudentAssignmentsByClass(selectedClassId.value, {
      classId: selectedClassId.value,
      className: activeClassName.value
    })
  } finally {
    assignmentsLoading.value = false
  }
}

function handleClassChange(value) {
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
      classId: selectedClassId.value,
      className: activeClassName.value
    }
  })
}

watch(() => route.query.classId, async (newValue, oldValue) => {
  if (newValue === oldValue && assignments.value.length) return
  if (newValue) {
    selectedClassId.value = String(newValue)
  }
  await reloadAssignments()
})

onMounted(async () => {
  await fetchClassesAndResolveSelection()
  await reloadAssignments()
})
</script>

<style scoped>
.student-task-toolbar {
  margin-bottom: 18px;
}

.task-class-select {
  min-width: 240px;
}

.student-task-search :deep(.el-input) {
  width: 280px;
}

.student-assignment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 18px;
}

.student-assignment-card {
  padding: 20px;
  border-radius: 28px;
  background: rgba(248, 250, 251, 0.9);
  border: 1px solid rgba(29, 35, 43, 0.06);
}

.student-assignment-card__header,
.student-assignment-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.student-assignment-card__header h3,
.student-assignment-card__header p,
.student-assignment-card__description {
  margin: 0;
}

.student-assignment-card__header h3 {
  font-size: 20px;
  margin-bottom: 8px;
}

.student-assignment-card__header p,
.student-assignment-card__meta,
.student-assignment-card__description {
  color: var(--text-body);
}

.student-assignment-card__meta {
  display: grid;
  gap: 8px;
  margin-top: 16px;
  margin-bottom: 14px;
  font-size: 13px;
}

.student-assignment-card__description {
  min-height: 44px;
  line-height: 1.7;
}

.student-assignment-card__footer {
  margin-top: 18px;
  align-items: center;
}

.student-assignment-card__flags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>

<template>
  <div class="workspace-page teacher-page teacher-teaching-page">
    <WorkspaceShellSection :eyebrow="pageEyebrow" :title="pageTitle" :description="pageDescription">
      <template #tools>
        <el-button
          v-if="currentTab === 'students' && selectedClassId"
          class="teacher-back-button"
          round
          @click="backToClassList"
        >
          <el-icon><ArrowLeft /></el-icon>
          <span>返回班级列表</span>
        </el-button>
        <el-button v-if="currentTab === 'applications'" @click="refreshApplications">刷新申请</el-button>
        <el-button v-if="currentTab === 'invites'" @click="fetchClasses">刷新邀请码</el-button>
        <el-button v-if="currentTab === 'classes'" type="primary" @click="dialogVisible = true">新建班级</el-button>
      </template>
    </WorkspaceShellSection>

    <WorkspacePanel class="teacher-teaching-panel" :title="panelTitle" :subtitle="panelSubtitle">
      <template v-if="currentTab === 'classes'">
        <div class="teacher-class-list" v-loading="classLoading">
          <div v-if="!classList.length" class="workspace-empty">暂无班级，先创建第一个班级。</div>
          <div v-else class="teacher-class-list__grid">
            <article v-for="item in classList" :key="item.id" class="teacher-class-list__card" @click="openClassStudents(item)">
              <div class="teacher-class-list__head">
                <div>
                  <p class="teacher-class-list__course">{{ item.courseName || '未设置课程' }}</p>
                  <h3>{{ item.className }}</h3>
                </div>
                <span class="workspace-badge-soft workspace-badge-soft--blue">{{ item.studentCount || 0 }} 人</span>
              </div>
              <div class="teacher-class-list__meta">
                <span>邀请码</span>
                <strong>{{ item.inviteCode || '-' }}</strong>
              </div>
              <div class="teacher-class-list__meta">
                <span>创建时间</span>
                <strong>{{ formatDate(item.createTime) }}</strong>
              </div>
              <div class="teacher-class-list__actions" @click.stop>
                <el-button @click="copyCode(item.inviteCode)">复制邀请码</el-button>
                <el-button type="danger" @click="handleDeleteClass(item)">删除</el-button>
              </div>
            </article>
          </div>
        </div>
        <div class="teacher-teaching-pagination">
          <div class="workspace-muted">点击班级卡片进入班级内学生视图。</div>
          <el-pagination
            :current-page="classPage"
            :page-size="classPageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            :total="classTotal"
            @size-change="handleClassSizeChange"
            @current-change="handleClassPageChange"
          />
        </div>
      </template>

      <template v-else-if="currentTab === 'students'">
        <div class="teacher-students-view">
          <div class="teacher-students-view__toolbar">
            <div v-if="selectedClassId" class="teacher-students-view__context">
              <span class="teacher-students-view__context-label">{{ selectedClassInfo.className || '班级学生' }}</span>
              <strong>{{ studentTotal }} 名学生</strong>
            </div>
            <div class="teacher-students-view__filters" v-if="!selectedClassId">
              <el-input v-model="searchKeyword" clearable placeholder="搜索学号、姓名或用户名" @clear="handleStudentSearch" @keyup.enter="handleStudentSearch" />
              <el-select v-model="selectedCollege" clearable placeholder="学院" @change="handleCollegeChange">
                <el-option v-for="item in collegeOptions" :key="item" :label="item" :value="item" />
              </el-select>
              <el-select v-model="selectedClass" clearable placeholder="班级" @change="handleStudentSearch">
                <el-option v-for="item in classOptions" :key="item" :label="item" :value="item" />
              </el-select>
              <el-button @click="handleStudentReset">重置</el-button>
              <el-button type="primary" @click="handleStudentSearch">查询</el-button>
            </div>
          </div>

          <div class="teacher-students-view__table" v-loading="studentLoading">
            <el-table :data="pagedStudents" height="100%" empty-text="暂无学生数据" @row-click="openStudentDialog">
              <el-table-column prop="student_number" label="学号" min-width="140" />
              <el-table-column prop="username" label="用户名" min-width="140" show-overflow-tooltip />
              <el-table-column label="姓名" min-width="120" show-overflow-tooltip>
                <template #default="scope">{{ scope.row.nickname || '-' }}</template>
              </el-table-column>
              <el-table-column prop="college" label="学院" min-width="140" show-overflow-tooltip />
              <el-table-column :prop="selectedClassId ? 'admin_class' : 'class_name'" :label="selectedClassId ? '行政班' : '班级'" min-width="140" show-overflow-tooltip />
              <el-table-column prop="email" label="邮箱" min-width="220" show-overflow-tooltip />
              <el-table-column label="操作" width="140" align="center">
                <template #default="scope">
                  <div class="teacher-students-view__actions">
                    <el-button class="teacher-students-view__action teacher-students-view__action--view" round @click.stop="openStudentDialog(scope.row)">查看</el-button>
                    <el-button v-if="selectedClassId" class="teacher-students-view__action teacher-students-view__action--danger" round @click.stop="handleKickStudent(scope.row)">移出</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="teacher-teaching-pagination">
            <div class="workspace-muted">共 {{ studentTotal }} 名学生</div>
            <el-pagination
              :current-page="studentPage"
              :page-size="studentPageSize"
              :page-sizes="[10, 20, 30]"
              layout="sizes, prev, pager, next"
              :total="studentTotal"
              @current-change="handleStudentPageChange"
              @size-change="handleStudentSizeChange"
            />
          </div>
        </div>
      </template>

      <template v-else-if="currentTab === 'applications'">
        <div class="teacher-applications-view" v-loading="applicationLoading">
          <div class="teacher-applications-view__summary">
            <span class="workspace-pill">已解决 {{ resolvedApplicationCount }}</span>
            <span class="workspace-pill">未解决 {{ unresolvedApplicationCount }}</span>
            <span class="workspace-pill">总计 {{ sortedApplications.length }}</span>
          </div>
          <div class="teacher-applications-view__filters">
            <div class="teacher-applications-view__segmented">
              <button
                v-for="item in applicationStatusOptions"
                :key="item.value"
                type="button"
                class="teacher-applications-view__segment"
                :class="{ active: applicationStatusFilter === item.value }"
                @click="applicationStatusFilter = item.value"
              >
                {{ item.label }}
              </button>
            </div>
            <el-input
              v-model="applicationKeyword"
              clearable
              placeholder="搜索学号、姓名或申请班级"
              class="teacher-applications-view__search"
            />
            <el-select v-model="applicationClassFilter" clearable placeholder="申请班级" class="teacher-applications-view__class-filter">
              <el-option v-for="item in applicationClassOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </div>
          <el-table :data="filteredApplications" height="100%" empty-text="暂无入班申请">
            <el-table-column prop="studentNumber" label="学号" width="130" />
            <el-table-column label="姓名" width="120">
              <template #default="scope">{{ scope.row.nickname || scope.row.username || '-' }}</template>
            </el-table-column>
            <el-table-column prop="school" label="学校" width="140" show-overflow-tooltip />
            <el-table-column prop="college" label="学院" width="140" show-overflow-tooltip />
            <el-table-column prop="adminClass" label="行政班" width="140" show-overflow-tooltip />
            <el-table-column prop="className" label="申请班级" min-width="140" show-overflow-tooltip />
            <el-table-column label="申请时间" width="180">
              <template #default="scope">{{ formatDate(scope.row.applyTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110" align="center">
              <template #default="scope">
                <span class="workspace-badge-soft" :class="applicationStatusClass(scope.row)">{{ applicationStatusText(scope.row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" align="center">
              <template #default="scope">
                <template v-if="applicationResolved(scope.row)">
                  <span class="teacher-applications-view__resolved">已处理</span>
                </template>
                <template v-else>
                  <el-button type="primary" size="small" @click="handleApprove(scope.row)">通过</el-button>
                  <el-button type="danger" size="small" @click="handleReject(scope.row)">拒绝</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <template v-else-if="currentTab === 'invites'">
        <div class="teacher-invites-view" v-loading="classLoading">
          <div v-if="!classList.length" class="workspace-empty">暂无班级邀请码可展示。</div>
          <div v-else class="teacher-invites-view__grid">
            <article v-for="item in classList" :key="`invite-${item.id}`" class="teacher-invites-view__card">
              <div>
                <p class="teacher-invites-view__course">{{ item.courseName || '未设置课程' }}</p>
                <h3>{{ item.className }}</h3>
              </div>
              <div class="teacher-invites-view__info">
                <span>邀请码</span>
                <strong>{{ item.inviteCode || '-' }}</strong>
              </div>
              <div class="teacher-invites-view__info">
                <span>当前人数</span>
                <strong>{{ item.studentCount || 0 }} 人</strong>
              </div>
              <el-button type="primary" plain @click="copyCode(item.inviteCode)">快捷复制</el-button>
            </article>
          </div>
        </div>
      </template>
    </WorkspacePanel>

    <el-dialog v-model="dialogVisible" title="新建班级" width="420px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="班级名称" prop="className"><el-input v-model="form.className" placeholder="请输入班级名称" /></el-form-item>
        <el-form-item label="课程名称" prop="courseName"><el-input v-model="form.courseName" placeholder="请输入课程名称" @keyup.enter="handleCreate" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="studentDialogVisible" title="学生信息" width="520px">
      <div class="teacher-student-dialog" v-if="activeStudent">
        <div class="teacher-student-dialog__row"><span>学号</span><strong>{{ activeStudent.student_number || activeStudent.studentNumber || '-' }}</strong></div>
        <div class="teacher-student-dialog__row"><span>用户名</span><strong>{{ activeStudent.username || '-' }}</strong></div>
        <div class="teacher-student-dialog__row"><span>姓名</span><strong>{{ activeStudent.nickname || '-' }}</strong></div>
        <div class="teacher-student-dialog__row"><span>学院</span><strong>{{ activeStudent.college || '-' }}</strong></div>
        <div class="teacher-student-dialog__row"><span>班级</span><strong>{{ activeStudent.class_name || activeStudent.admin_class || '-' }}</strong></div>
        <div class="teacher-student-dialog__row"><span>邮箱</span><strong>{{ activeStudent.email || '-' }}</strong></div>
        <div class="teacher-student-dialog__row"><span>学校</span><strong>{{ activeStudent.school || '-' }}</strong></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import request from '../../api/request'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import useClipboard from 'vue-clipboard3'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import WorkspaceShellSection from '../../components/workspace/WorkspaceShellSection.vue'

const route = useRoute()
const router = useRouter()
const { toClipboard } = useClipboard()

const classList = ref([])
const classLoading = ref(false)
const classPage = ref(1)
const classPageSize = ref(10)
const classTotal = ref(0)

const studentLoading = ref(false)
const studentList = ref([])
const studentPage = ref(1)
const studentPageSize = ref(10)
const searchKeyword = ref('')
const selectedCollege = ref('')
const selectedClass = ref('')
const collegeOptions = ref([])
const classOptions = ref([])
const selectedClassInfo = ref({})

const dialogVisible = ref(false)
const createLoading = ref(false)
const formRef = ref(null)
const form = reactive({ className: '', courseName: '' })
const rules = {
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }]
}

const applicationLoading = ref(false)
const applicationList = ref([])
const applicationKeyword = ref('')
const applicationStatusFilter = ref('all')
const applicationClassFilter = ref('')
const studentDialogVisible = ref(false)
const activeStudent = ref(null)

const currentTab = computed(() => {
  const tab = typeof route.query.tab === 'string' ? route.query.tab : 'classes'
  return ['classes', 'students', 'applications', 'invites'].includes(tab) ? tab : 'classes'
})

const selectedClassId = computed(() => {
  if (currentTab.value !== 'students') return ''
  return typeof route.query.classId === 'string' ? route.query.classId : ''
})

const panelTitle = computed(() => {
  if (currentTab.value === 'students') return selectedClassId.value ? '' : '学生列表'
  if (currentTab.value === 'applications') return '入班申请'
  if (currentTab.value === 'invites') return '邀请码'
  return '班级列表'
})

const panelSubtitle = computed(() => {
  if (currentTab.value === 'students') return selectedClassId.value ? '' : '分页查看学生名单，支持条件查询'
  if (currentTab.value === 'applications') return '保留完整申请信息，并按状态和时间排序'
  if (currentTab.value === 'invites') return '查看每个班级的邀请码、人数和快捷复制入口'
  return '简约展示班级卡片，点击卡片进入班级内学生'
})

const pageTitle = computed(() => {
  if (currentTab.value === 'students') return selectedClassId.value ? `${selectedClassInfo.value.className || '班级'} 学生` : '学生列表'
  if (currentTab.value === 'applications') return '入班申请'
  if (currentTab.value === 'invites') return '邀请码'
  return '班级列表'
})

const pageDescription = computed(() => {
  if (currentTab.value === 'students') return selectedClassId.value ? '' : '支持按条件查询'
  if (currentTab.value === 'applications') return ''
  if (currentTab.value === 'invites') return ''
  return ''
})

const pageEyebrow = computed(() => {
  if (currentTab.value === 'students' && selectedClassId.value) return ''
  return '教学管理'
})

const studentTotal = computed(() => studentList.value.length)
const pagedStudents = computed(() => {
  const start = (studentPage.value - 1) * studentPageSize.value
  return studentList.value.slice(start, start + studentPageSize.value)
})

const applicationStatusOptions = [
  { value: 'all', label: '全部' },
  { value: 'resolved', label: '已解决' },
  { value: 'pending', label: '未解决' }
]

const sortedApplications = computed(() => {
  const list = Array.isArray(applicationList.value) ? [...applicationList.value] : []
  return list.sort((left, right) => {
    const statusDelta = Number(applicationResolved(right)) - Number(applicationResolved(left))
    if (statusDelta !== 0) return statusDelta
    return new Date(right.applyTime || 0).getTime() - new Date(left.applyTime || 0).getTime()
  })
})
const applicationClassOptions = computed(() => Array.from(new Set(sortedApplications.value.map((item) => item.className).filter(Boolean))))
const filteredApplications = computed(() => {
  const keyword = applicationKeyword.value.trim().toLowerCase()
  return sortedApplications.value.filter((item) => {
    const matchesStatus = applicationStatusFilter.value === 'all'
      || (applicationStatusFilter.value === 'resolved' && applicationResolved(item))
      || (applicationStatusFilter.value === 'pending' && !applicationResolved(item))
    const matchesClass = !applicationClassFilter.value || item.className === applicationClassFilter.value
    const source = [item.studentNumber, item.nickname, item.username, item.className].filter(Boolean).join(' ').toLowerCase()
    const matchesKeyword = !keyword || source.includes(keyword)
    return matchesStatus && matchesClass && matchesKeyword
  })
})
const resolvedApplicationCount = computed(() => sortedApplications.value.filter(applicationResolved).length)
const unresolvedApplicationCount = computed(() => sortedApplications.value.length - resolvedApplicationCount.value)

function syncTeachingTab(tab, extra = {}) {
  router.push({ path: '/teacher/classes', query: { tab, ...extra } })
}

function openClassStudents(item) {
  syncTeachingTab('students', { classId: String(item.id) })
}

function backToClassList() {
  syncTeachingTab('classes')
}

function openStudentDialog(student) {
  activeStudent.value = student
  studentDialogVisible.value = true
}

function applicationStatusText(row) {
  const raw = String(row?.status || row?.applyStatus || row?.reviewStatus || row?.result || '').toLowerCase()
  if (['approved', 'passed', 'success', 'done', 'resolved', 'accepted', '已通过', '已拒绝', '已解决', 'rejected', 'refused'].some((item) => raw.includes(item.toLowerCase()))) {
    return '已解决'
  }
  return '未解决'
}

function applicationResolved(row) {
  return applicationStatusText(row) === '已解决'
}

function applicationStatusClass(row) {
  return applicationResolved(row) ? 'workspace-badge-soft--green' : 'workspace-badge-soft--amber'
}

async function fetchClasses() {
  classLoading.value = true
  try {
    const res = await request.get('/teacher/classes', { params: { page: classPage.value, limit: classPageSize.value } })
    classList.value = res.records || []
    classTotal.value = res.total || 0
  } finally {
    classLoading.value = false
  }
}

async function fetchSelectedClassInfo() {
  if (!selectedClassId.value) {
    selectedClassInfo.value = {}
    return
  }
  selectedClassInfo.value = (await request.get(`/teacher/classes/${selectedClassId.value}`)) || {}
}

async function fetchClassStudents() {
  if (!selectedClassId.value) {
    studentList.value = []
    return
  }
  studentLoading.value = true
  try {
    studentList.value = (await request.get(`/teacher/classes/${selectedClassId.value}/students`)) || []
  } finally {
    studentLoading.value = false
  }
}

async function fetchColleges() {
  collegeOptions.value = (await request.get('/student/colleges')) || []
}

async function fetchClassOptions(college = '') {
  classOptions.value = (await request.get('/student/class-options', { params: { college } })) || []
}

async function fetchAllStudents() {
  studentLoading.value = true
  try {
    const res = await request.get('/student/list', {
      params: {
        keyword: searchKeyword.value,
        college: selectedCollege.value,
        className: selectedClass.value
      }
    })
    studentList.value = Array.isArray(res) ? res : []
  } finally {
    studentLoading.value = false
  }
}

async function handleStudentSearch() {
  studentPage.value = 1
  await fetchAllStudents()
}

async function handleStudentReset() {
  searchKeyword.value = ''
  selectedCollege.value = ''
  selectedClass.value = ''
  studentPage.value = 1
  await fetchClassOptions('')
  await fetchAllStudents()
}

async function handleCollegeChange() {
  selectedClass.value = ''
  await fetchClassOptions(selectedCollege.value)
  await handleStudentSearch()
}

async function fetchApplications() {
  applicationLoading.value = true
  try {
    applicationList.value = (await request.get('/teacher/classes/applications')) || []
  } finally {
    applicationLoading.value = false
  }
}

async function refreshApplications() {
  await fetchApplications()
}

async function handleApprove(row) {
  await request.post(`/teacher/classes/applications/${row.id}/approve`)
  ElMessage.success('已通过')
  await Promise.all([fetchApplications(), fetchClasses()])
}

async function handleReject(row) {
  await request.post(`/teacher/classes/applications/${row.id}/reject`)
  ElMessage.success('已拒绝')
  await fetchApplications()
}

function handleClassSizeChange(value) {
  classPageSize.value = value
  fetchClasses()
}

function handleClassPageChange(value) {
  classPage.value = value
  fetchClasses()
}

function handleStudentSizeChange(value) {
  studentPageSize.value = value
  studentPage.value = 1
}

function handleStudentPageChange(value) {
  studentPage.value = value
}

function resetForm() {
  formRef.value?.resetFields()
}

async function handleCreate() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    createLoading.value = true
    try {
      const res = await request.post('/teacher/classes/create', { className: form.className, courseName: form.courseName })
      dialogVisible.value = false
      ElNotification({ title: '班级创建成功', message: `邀请码：${res.inviteCode || '未知'}`, type: 'success', duration: 0 })
      await fetchClasses()
    } finally {
      createLoading.value = false
    }
  })
}

function resolveStudentId(student) {
  return student?.id || student?.user_id || student?.userId
}

async function handleKickStudent(student) {
  if (!selectedClassId.value) return
  await ElMessageBox.confirm(`确认将 ${student.nickname || student.username || '该学生'} 移出当前班级？`, '确认踢出', { type: 'warning' })
  await request.delete(`/teacher/classes/${selectedClassId.value}/students/${resolveStudentId(student)}`)
  ElMessage.success('已移出班级')
  await Promise.all([fetchClassStudents(), fetchClasses(), fetchSelectedClassInfo()])
}

function handleDeleteClass(row) {
  ElMessageBox.confirm(`确定删除班级“${row.className}”？此操作不可恢复。`, '警告', { type: 'warning' })
    .then(async () => {
      await request.delete(`/teacher/classes/${row.id}`)
      ElMessage.success('班级已删除')
      await fetchClasses()
    })
    .catch(() => {})
}

async function copyCode(code) {
  if (!code) {
    ElMessage.warning('暂无邀请码')
    return
  }
  try {
    await toClipboard(code)
    ElMessage.success('邀请码已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString() : '-'
}

watch(
  () => [currentTab.value, selectedClassId.value],
  async ([tab]) => {
    if (tab === 'classes' || tab === 'invites') {
      await fetchClasses()
      return
    }

    if (tab === 'applications') {
      await fetchApplications()
      return
    }

    if (tab === 'students') {
      studentPage.value = 1
      if (selectedClassId.value) {
        await Promise.all([fetchSelectedClassInfo(), fetchClassStudents()])
      } else {
        selectedClassInfo.value = {}
        await Promise.all([fetchColleges(), fetchClassOptions(selectedCollege.value), fetchAllStudents()])
      }
    }
  },
  { immediate: true }
)

onMounted(() => {
  fetchClasses()
})
</script>

<style scoped>
.teacher-teaching-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 100%;
}

.teacher-teaching-panel :deep(.workspace-panel__body) {
  display: flex;
  min-height: 620px;
  flex-direction: column;
}

.teacher-class-list,
.teacher-students-view,
.teacher-applications-view,
.teacher-invites-view {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
}

.teacher-class-list__grid,
.teacher-invites-view__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.teacher-class-list__card,
.teacher-invites-view__card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  border-radius: 22px;
  border: 1px solid rgba(29, 35, 43, 0.05);
  background: linear-gradient(180deg, rgba(250, 252, 253, 0.98), rgba(245, 248, 249, 0.94));
}

.teacher-class-list__card {
  cursor: pointer;
  transition: transform var(--transition-soft), box-shadow var(--transition-soft);
}

.teacher-class-list__card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(121, 137, 147, 0.08);
}

.teacher-class-list__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.teacher-class-list__course,
.teacher-invites-view__course {
  margin: 0 0 6px;
  color: var(--text-soft);
  font-size: 12px;
}

.teacher-class-list__head h3,
.teacher-invites-view__card h3 {
  margin: 0;
  font-size: 22px;
  color: var(--text-strong);
}

.teacher-class-list__meta,
.teacher-invites-view__info,
.teacher-student-dialog__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.teacher-class-list__actions {
  display: flex;
  gap: 10px;
  margin-top: auto;
}

.teacher-students-view__toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.teacher-back-button {
  gap: 8px;
  padding-inline: 16px;
  border: 1px solid rgba(212, 220, 232, 0.95);
  background: rgba(255, 255, 255, 0.94);
  color: var(--text-main);
  box-shadow: 0 10px 24px rgba(29, 44, 79, 0.08);
}

.teacher-back-button :deep(.el-icon) {
  font-size: 14px;
}

.teacher-students-view__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

.teacher-students-view__filters :deep(.el-input),
.teacher-students-view__filters :deep(.el-select) {
  width: 220px;
}

.teacher-students-view__context {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 40px;
}

.teacher-students-view__context-label {
  color: var(--text-main);
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.teacher-students-view__context strong {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(91, 115, 255, 0.1);
  color: #3557dc;
  font-size: 13px;
  font-weight: 700;
}

.teacher-students-view__table,
.teacher-applications-view :deep(.el-table) {
  flex: 1;
  min-height: 0;
}

.teacher-students-view__actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.teacher-students-view__action {
  min-width: 58px;
  margin: 0;
  border: 1px solid rgba(214, 223, 235, 0.95);
  background: rgba(255, 255, 255, 0.95);
  color: var(--text-main);
}

.teacher-students-view__action--view {
  border-color: rgba(169, 182, 214, 0.95);
}

.teacher-students-view__action--danger {
  border-color: rgba(246, 193, 193, 0.95);
  color: #c04e4e;
}

.teacher-teaching-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
}

.teacher-applications-view__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.teacher-applications-view__filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.teacher-applications-view__segmented {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(18, 25, 38, 0.05);
}

.teacher-applications-view__segment {
  border: 0;
  background: transparent;
  color: var(--text-soft);
  padding: 9px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition: all var(--transition-soft);
}

.teacher-applications-view__segment.active {
  background: #121926;
  color: #fff;
}

.teacher-applications-view__search,
.teacher-applications-view__class-filter {
  width: 240px;
}

.teacher-applications-view__resolved {
  color: var(--text-soft);
  font-size: 13px;
}

.teacher-student-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

@media (max-width: 1120px) {
  .teacher-class-list__grid,
  .teacher-invites-view__grid {
    grid-template-columns: 1fr;
  }

  .teacher-students-view__toolbar,
  .teacher-teaching-pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .teacher-students-view__filters {
    justify-content: flex-start;
  }

  .teacher-students-view__filters :deep(.el-input),
  .teacher-students-view__filters :deep(.el-select) {
    width: 100%;
  }
}
</style>





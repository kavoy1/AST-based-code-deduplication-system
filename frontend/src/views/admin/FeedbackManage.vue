<template>
  <div class="workspace-page">
    <WorkspaceShellSection eyebrow="admin feedback" title="问题反馈" description="按状态处理工单、更新回复与解决状态，修复此前统计接口值被错误渲染的问题。" />

    <div class="workspace-kpi-grid">
      <div class="workspace-kpi"><div class="workspace-kpi__label">今日新增</div><div class="workspace-kpi__value">{{ todayCount }}</div><div class="workspace-kpi__meta">今天提交的反馈数</div></div>
      <div class="workspace-kpi"><div class="workspace-kpi__label">总工单</div><div class="workspace-kpi__value">{{ total }}</div><div class="workspace-kpi__meta">按当前筛选统计</div></div>
      <div class="workspace-kpi"><div class="workspace-kpi__label">当前筛选</div><div class="workspace-kpi__value">{{ currentStatusLabel }}</div><div class="workspace-kpi__meta">支持待处理 / 处理中 / 已解决</div></div>
    </div>

    <div class="workspace-grid workspace-grid--two">
      <WorkspacePanel title="反馈列表" subtitle="工单工作区视图">
        <div class="workspace-toolbar">
          <div class="workspace-segmented">
            <button v-for="tab in tabs" :key="tab.value" :class="{ active: statusFilter === tab.value }" @click="handleFilterChange(tab.value)">{{ tab.label }}</button>
          </div>
          <div class="workspace-toolbar__group feedback-toolbar-right">
            <el-input v-model="searchQuery" clearable placeholder="搜索标题或内容" @keyup.enter="fetchFeedbacks" @clear="fetchFeedbacks" />
            <el-button @click="fetchFeedbacks">刷新</el-button>
          </div>
        </div>
        <div class="workspace-table-wrap">
          <el-table :data="feedbacks" v-loading="loading" @row-click="viewDetail">
            <el-table-column prop="id" label="编号" width="90"><template #default="scope">#{{ scope.row.id }}</template></el-table-column>
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="content" label="摘要" min-width="220" show-overflow-tooltip />
            <el-table-column label="提交人" min-width="150"><template #default="scope">{{ scope.row.submitterName }} / {{ formatRole(scope.row.submitterRole) }}</template></el-table-column>
            <el-table-column prop="createTime" label="提交时间" min-width="160"><template #default="scope">{{ formatDate(scope.row.createTime) }}</template></el-table-column>
            <el-table-column prop="status" label="状态" width="100"><template #default="scope"><span class="workspace-badge-soft" :class="statusClass(scope.row.status)">{{ formatStatus(scope.row.status) }}</span></template></el-table-column>
          </el-table>
        </div>
        <div class="workspace-toolbar feedback-pagination">
          <div class="workspace-muted">当前页 {{ currentPage }} · 每页 {{ pageSize }} 条</div>
          <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" :total="total" @size-change="fetchFeedbacks" @current-change="fetchFeedbacks" />
        </div>
      </WorkspacePanel>

      <WorkspacePanel title="反馈详情" subtitle="在右侧直接处理工单" soft>
        <WorkspaceEmpty v-if="!currentFeedback.id" title="请选择一条反馈" description="点击左侧列表后，在这里调整状态和编辑回复。" />
        <div v-else class="workspace-list">
          <div class="workspace-list-item"><div><p class="workspace-list-item__title">反馈标题</p><p class="workspace-list-item__meta">{{ currentFeedback.title }}</p></div><span class="workspace-badge-soft" :class="statusClass(currentFeedback.status)">{{ formatStatus(currentFeedback.status) }}</span></div>
          <div class="workspace-list-item"><div><p class="workspace-list-item__title">提交人</p><p class="workspace-list-item__meta">{{ currentFeedback.submitterName }} / {{ currentFeedback.submitterUid || '未知 UID' }}</p></div></div>
          <div class="feedback-detail-block">{{ currentFeedback.content }}</div>
          <el-select v-model="currentFeedback.status" @change="handleStatusChange" placeholder="状态">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
          </el-select>
          <el-input v-model="replyContent" type="textarea" :rows="6" placeholder="输入管理员回复或处理说明" />
          <div class="workspace-toolbar__group">
            <el-button v-if="currentFeedback.status !== 'RESOLVED'" @click="handleResolveInDetail">标记为已解决</el-button>
            <el-button type="primary" @click="saveReply">保存回复</el-button>
          </div>
        </div>
      </WorkspacePanel>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'
import WorkspaceEmpty from '../../components/workspace/WorkspaceEmpty.vue'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import WorkspaceShellSection from '../../components/workspace/WorkspaceShellSection.vue'

const feedbacks = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusFilter = ref('')
const searchQuery = ref('')
const todayCount = ref(0)
const currentFeedback = ref({})
const replyContent = ref('')

const tabs = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已解决', value: 'RESOLVED' }
]

const currentStatusLabel = computed(() => tabs.find((item) => item.value === statusFilter.value)?.label || '全部')

const fetchFeedbacks = async () => {
  loading.value = true
  try {
    const res = await request.get('/feedback/list', {
      params: {
        page: currentPage.value,
        size: pageSize.value,
        status: statusFilter.value,
        keyword: searchQuery.value
      }
    })
    feedbacks.value = res.records || []
    total.value = Number(res.total || 0)
    todayCount.value = Number(await request.get('/feedback/stats/today'))
    if (!currentFeedback.value.id && feedbacks.value.length) viewDetail(feedbacks.value[0])
  } catch {
    ElMessage.error('获取反馈列表失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = (row) => {
  currentFeedback.value = { ...row }
  replyContent.value = row.reply || ''
}

const handleFilterChange = (value) => {
  statusFilter.value = value
  currentPage.value = 1
  fetchFeedbacks()
}

const handleStatusChange = async (value) => {
  if (!currentFeedback.value.id) return
  await request.post(`/feedback/status/${currentFeedback.value.id}`, null, { params: { status: value } })
  ElMessage.success('反馈状态已更新')
  fetchFeedbacks()
}

const handleResolveInDetail = async () => {
  currentFeedback.value.status = 'RESOLVED'
  await handleStatusChange('RESOLVED')
}

const saveReply = async () => {
  if (!currentFeedback.value.id) return
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  await request.post(`/feedback/reply/${currentFeedback.value.id}`, { reply: replyContent.value })
  ElMessage.success('回复已保存')
  fetchFeedbacks()
}

const formatDate = (value) => value ? new Date(value).toLocaleString() : '-'
const formatRole = (role) => ({ ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[role] || role)
const formatStatus = (status) => ({ PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决' }[status] || status)
const statusClass = (status) => ({ PENDING: 'workspace-badge-soft--amber', PROCESSING: 'workspace-badge-soft--blue', RESOLVED: 'workspace-badge-soft--green' }[status] || '')

onMounted(fetchFeedbacks)
</script>

<style scoped>
.feedback-toolbar-right :deep(.el-input) {
  width: 260px;
}

.feedback-pagination {
  margin-top: 18px;
}

.feedback-detail-block {
  padding: 18px;
  border-radius: 22px;
  background: rgba(248, 250, 251, 0.86);
  color: var(--text-body);
  line-height: 1.8;
}
</style>

<template>
  <div class="workspace-page">
    <WorkspaceShellSection eyebrow="admin users" title="用户管理" description="保留筛选、分页、排序与状态操作，但页面组织改为工作台式列表视图。">
      <template #tools>
        <el-button @click="resetFilters">重置筛选</el-button>
        <el-button type="primary" @click="fetchUsers">查询</el-button>
      </template>
    </WorkspaceShellSection>

    <div class="workspace-grid workspace-grid--two">
      <WorkspacePanel title="用户列表" subtitle="按角色、状态和关键字筛选">
        <div class="workspace-toolbar">
          <div class="workspace-toolbar__group user-filters">
            <el-input v-model="filters.keyword" clearable placeholder="搜索用户名 / 姓名 / 邮箱 / UID" @keyup.enter="handleFilterSearch" @clear="handleFilterSearch" />
            <el-select v-model="filters.role" clearable placeholder="角色" @change="handleFilterSearch">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="教师" value="TEACHER" />
              <el-option label="学生" value="STUDENT" />
            </el-select>
            <el-select v-model="filters.status" clearable placeholder="状态" @change="handleFilterSearch">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </div>
          <div class="workspace-pill">共 {{ pagination.total }} 人</div>
        </div>

        <div class="workspace-table-wrap">
          <el-table :data="users" v-loading="loading" :default-sort="defaultSort" @sort-change="handleSortChange" @row-click="selectUser">
            <el-table-column prop="id" label="ID" width="80" sortable="custom" />
            <el-table-column prop="uid" label="UID" width="140" />
            <el-table-column prop="username" label="用户名" min-width="120" sortable="custom" />
            <el-table-column prop="nickname" label="姓名" min-width="110" />
            <el-table-column prop="email" label="邮箱" min-width="180" />
            <el-table-column prop="role" label="角色" width="100" sortable="custom">
              <template #default="scope"><el-tag :type="getRoleType(scope.row.role)">{{ formatRole(scope.row.role) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="createTime" label="注册时间" min-width="160" sortable="custom">
              <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90" sortable="custom">
              <template #default="scope">
                <LockSwitch v-model="scope.row.status" :active-value="1" :inactive-value="0" @change="(val) => handleStatusChange(scope.row, val)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="scope"><el-button type="danger" size="small" @click.stop="handleDelete(scope.row)">删除</el-button></template>
            </el-table-column>
          </el-table>
        </div>

        <div class="workspace-toolbar user-pagination">
          <div class="workspace-muted">支持分页、排序和筛选参数回填到 URL。</div>
          <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next" :total="pagination.total" @size-change="handlePageSizeChange" @current-change="handlePageChange" />
        </div>
      </WorkspacePanel>

      <WorkspacePanel title="选中用户" subtitle="工作区右侧上下文面板" soft>
        <WorkspaceEmpty v-if="!selectedUser.id" title="尚未选中用户" description="点击左侧任意行，在这里查看用户上下文信息。" />
        <div v-else class="workspace-list">
          <div class="workspace-list-item"><div><p class="workspace-list-item__title">姓名</p><p class="workspace-list-item__meta">{{ selectedUser.nickname || '未填写' }}</p></div><span class="workspace-pill">{{ formatRole(selectedUser.role) }}</span></div>
          <div class="workspace-list-item"><div><p class="workspace-list-item__title">账号</p><p class="workspace-list-item__meta">{{ selectedUser.username }}</p></div><strong>{{ selectedUser.uid || '-' }}</strong></div>
          <div class="workspace-list-item"><div><p class="workspace-list-item__title">联系方式</p><p class="workspace-list-item__meta">{{ selectedUser.email || '未填写邮箱' }}</p></div><span class="workspace-badge-soft" :class="selectedUser.status === 1 ? 'workspace-badge-soft--green' : 'workspace-badge-soft--danger'">{{ selectedUser.status === 1 ? '启用中' : '已禁用' }}</span></div>
          <div class="workspace-list-item"><div><p class="workspace-list-item__title">注册时间</p><p class="workspace-list-item__meta">{{ formatDate(selectedUser.createTime) }}</p></div></div>
        </div>
      </WorkspacePanel>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import WorkspaceEmpty from '../../components/workspace/WorkspaceEmpty.vue'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import WorkspaceShellSection from '../../components/workspace/WorkspaceShellSection.vue'

const route = useRoute()
const router = useRouter()
const users = ref([])
const loading = ref(false)
const selectedUser = ref({})

const filters = reactive({ keyword: '', role: '', status: null })
const pagination = reactive({ page: 1, size: 10, total: 0 })
const sort = reactive({ by: '', order: '' })

const defaultSort = computed(() => sort.by && sort.order ? ({ prop: sort.by, order: sort.order === 'desc' ? 'descending' : 'ascending' }) : {})

const fetchUsers = async () => {
  loading.value = true
  try {
    const response = await request.get('/admin/users', {
      params: {
        page: pagination.page,
        size: pagination.size,
        keyword: filters.keyword || undefined,
        role: filters.role || undefined,
        status: filters.status === null ? undefined : filters.status,
        sortBy: sort.by || undefined,
        sortOrder: sort.order || undefined
      }
    })
    users.value = response.records || []
    pagination.total = response.total || 0
    if (!selectedUser.value.id && users.value.length) selectedUser.value = users.value[0]
  } finally {
    loading.value = false
  }
}

const selectUser = (row) => { selectedUser.value = row }
const handleFilterSearch = () => { pagination.page = 1; syncQuery(); fetchUsers() }
const resetFilters = () => { filters.keyword = ''; filters.role = ''; filters.status = null; pagination.page = 1; sort.by = ''; sort.order = ''; syncQuery(); fetchUsers() }
const handlePageChange = (page) => { pagination.page = page; syncQuery(); fetchUsers() }
const handlePageSizeChange = (size) => { pagination.size = size; pagination.page = 1; syncQuery(); fetchUsers() }
const handleSortChange = ({ prop, order }) => { sort.by = order ? prop : ''; sort.order = order ? (order === 'descending' ? 'desc' : 'asc') : ''; pagination.page = 1; syncQuery(); fetchUsers() }

const syncQuery = () => {
  router.replace({
    path: route.path,
    query: {
      page: String(pagination.page),
      size: String(pagination.size),
      keyword: filters.keyword || undefined,
      role: filters.role || undefined,
      status: filters.status === null ? undefined : String(filters.status),
      sortBy: sort.by || undefined,
      sortOrder: sort.order || undefined
    }
  })
}

const initFromQuery = () => {
  const query = route.query
  pagination.page = Number(query.page || 1) || 1
  pagination.size = Number(query.size || 10) || 10
  filters.keyword = typeof query.keyword === 'string' ? query.keyword : ''
  filters.role = typeof query.role === 'string' ? query.role : ''
  filters.status = query.status === '0' || query.status === '1' ? Number(query.status) : null
  sort.by = typeof query.sortBy === 'string' ? query.sortBy : ''
  sort.order = typeof query.sortOrder === 'string' ? query.sortOrder : ''
}

const handleStatusChange = async (userRow, status) => {
  try {
    await request.put('/admin/users/status', null, { params: { id: userRow.id, status } })
    ElMessage.success('用户状态已更新')
  } catch {
    userRow.status = status === 1 ? 0 : 1
  }
}

const handleDelete = (userRow) => {
  ElMessageBox.confirm('确定删除该用户？此操作不可恢复。', '警告', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(async () => {
      await request.delete(`/admin/users/${userRow.id}`)
      ElMessage.success('用户已删除')
      if (users.value.length === 1 && pagination.page > 1) pagination.page -= 1
      fetchUsers()
    })
    .catch(() => {})
}

const getRoleType = (role) => ({ ADMIN: 'danger', TEACHER: 'warning', STUDENT: 'success' }[role] || 'info')
const formatRole = (role) => ({ ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[role] || role)
const formatDate = (value) => value ? new Date(value).toLocaleString() : '-'

onMounted(() => { initFromQuery(); fetchUsers() })
</script>

<style scoped>
.user-filters {
  flex: 1;
  min-width: 0;
}

.user-filters :deep(.el-input),
.user-filters :deep(.el-select) {
  width: 220px;
}

.user-pagination {
  margin-top: 18px;
}
</style>

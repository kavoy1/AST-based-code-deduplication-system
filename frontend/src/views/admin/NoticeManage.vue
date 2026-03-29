<template>
  <div class="workspace-page">
    <WorkspaceShellSection eyebrow="admin notice" title="通知公告" description="以内容工作区承载公告发布、预览与删除，不再沿用传统管理页模板。">
      <template #tools>
        <el-button type="primary" @click="dialogVisible = true">发布公告</el-button>
      </template>
    </WorkspaceShellSection>

    <div class="workspace-grid workspace-grid--two">
      <WorkspacePanel title="公告列表" subtitle="查看已发布公告并进行管理">
        <div class="workspace-table-wrap">
          <el-table :data="notices" v-loading="loading" @row-click="handleView">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="content" label="内容摘要" min-width="260" show-overflow-tooltip />
            <el-table-column prop="authorId" label="发布人 ID" width="100" />
            <el-table-column prop="createTime" label="发布时间" min-width="160">
              <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="scope"><el-button type="danger" size="small" @click.stop="handleDelete(scope.row.id)">删除</el-button></template>
            </el-table-column>
          </el-table>
        </div>
      </WorkspacePanel>

      <WorkspacePanel title="内容预览" subtitle="右侧预览当前选中的公告" soft>
        <WorkspaceEmpty v-if="!currentNotice" title="请选择一条公告" description="点击左侧表格中的公告，即可在这里查看完整内容。" />
        <div v-else class="notice-preview">
          <div class="workspace-badge-soft workspace-badge-soft--blue">{{ formatDate(currentNotice.createTime) }}</div>
          <h3>{{ currentNotice.title }}</h3>
          <div class="notice-preview__body">{{ currentNotice.content }}</div>
        </div>
      </WorkspacePanel>
    </div>

    <el-dialog v-model="dialogVisible" title="发布新公告" width="540px">
      <el-form :model="form" label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="请输入公告标题" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import request from '../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import WorkspaceEmpty from '../../components/workspace/WorkspaceEmpty.vue'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import WorkspaceShellSection from '../../components/workspace/WorkspaceShellSection.vue'

const notices = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentNotice = ref(null)
const form = reactive({ title: '', content: '' })

const fetchNotices = async () => {
  loading.value = true
  try {
    notices.value = await request.get('/admin/notices')
    if (!currentNotice.value && notices.value?.length) currentNotice.value = notices.value[0]
  } finally {
    loading.value = false
  }
}

const handlePublish = async () => {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  await request.post('/admin/notices', form)
  ElMessage.success('公告已发布')
  dialogVisible.value = false
  form.title = ''
  form.content = ''
  fetchNotices()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除这条公告？', '提示', { type: 'warning' })
  await request.delete(`/admin/notices/${id}`)
  ElMessage.success('公告已删除')
  if (currentNotice.value?.id === id) currentNotice.value = null
  fetchNotices()
}

const handleView = (notice) => { currentNotice.value = notice }
const formatDate = (value) => value ? new Date(value).toLocaleString() : '-'

onMounted(fetchNotices)
</script>

<style scoped>
.notice-preview h3 {
  margin: 16px 0 12px;
  font-size: 24px;
}

.notice-preview__body {
  padding: 18px;
  border-radius: 22px;
  background: rgba(248, 250, 251, 0.86);
  color: var(--text-body);
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>

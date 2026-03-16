<template>
    <div class="notice-manage">
        <el-card>
            <template #header>
                <div class="card-header">
                    <span>通知管理</span>
                    <el-button type="primary" @click="dialogVisible = true">发布通知</el-button>
                </div>
            </template>
            
            <el-table :data="notices" style="width: 100%" v-loading="loading">
                <el-table-column prop="id" label="ID" width="100" />
                <el-table-column label="标题" width="200">
                    <template #default="scope">
                        <div class="truncate-text">
                            <el-link type="primary" @click="handleView(scope.row)">{{ scope.row.title }}</el-link>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="内容">
                    <template #default="scope">
                        <div class="truncate-text">{{ scope.row.content }}</div>
                    </template>
                </el-table-column>
                <el-table-column prop="authorId" label="发布人ID" width="100" />
                <el-table-column prop="createTime" label="发布时间" width="180">
                    <template #default="scope">
                        {{ formatDate(scope.row.createTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="120">
                    <template #default="scope">
                        <el-popconfirm title="确定删除这条通知吗？" @confirm="handleDelete(scope.row.id)">
                            <template #reference>
                                <el-button type="danger" size="small">删除</el-button>
                            </template>
                        </el-popconfirm>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 发布通知对话框 -->
        <el-dialog v-model="dialogVisible" title="发布新通知" width="500px">
            <el-form :model="form" label-width="80px">
                <el-form-item label="标题">
                    <el-input v-model="form.title" placeholder="请输入通知标题" />
                </el-form-item>
                <el-form-item label="内容">
                    <el-input v-model="form.content" type="textarea" rows="4" placeholder="请输入通知内容" />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="handlePublish">发布</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 查看通知详情对话框 -->
        <el-dialog v-model="viewDialogVisible" title="通知详情" width="500px">
            <div v-if="currentNotice">
                <h3>{{ currentNotice.title }}</h3>
                <div style="color: #909399; font-size: 12px; margin-bottom: 20px;">
                    发布时间：{{ formatDate(currentNotice.createTime) }}
                </div>
                <div style="white-space: pre-wrap; line-height: 1.6;">
                    {{ currentNotice.content }}
                </div>
            </div>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="viewDialogVisible = false">关闭</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'

const notices = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const currentNotice = ref(null)

const form = reactive({
    title: '',
    content: ''
})

const fetchNotices = async () => {
    loading.value = true
    try {
        const res = await request.get('/admin/notices')
        notices.value = res
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

const handlePublish = async () => {
    if (!form.title || !form.content) {
        ElMessage.warning('请填写标题和内容')
        return
    }
    try {
        await request.post('/admin/notices', form)
        ElMessage.success('发布成功')
        dialogVisible.value = false
        form.title = ''
        form.content = ''
        fetchNotices()
    } catch (e) {
        console.error(e)
    }
}

const handleDelete = async (id) => {
    try {
        await request.delete(`/admin/notices/${id}`)
        ElMessage.success('删除成功')
        fetchNotices()
    } catch (e) {
        console.error(e)
    }
}

const handleView = (notice) => {
    currentNotice.value = notice
    viewDialogVisible.value = true
}

const formatDate = (timeStr) => {
    if (!timeStr) return ''
    return new Date(timeStr).toLocaleString()
}

onMounted(() => {
    fetchNotices()
})
</script>

<style scoped>
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.truncate-text {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
</style>

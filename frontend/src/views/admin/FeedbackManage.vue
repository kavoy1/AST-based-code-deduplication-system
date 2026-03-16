<template>
    <div class="feedback-manage">
        <div class="page-header">
            <h2 class="page-title">问题反馈管理</h2>
        </div>

        <div class="toolbar">
            <div class="status-tabs">
                <div 
                    class="tab-item" 
                    :class="{ active: statusFilter === '' }"
                    @click="handleFilterChange('')"
                >
                    全部
                </div>
                <div 
                    class="tab-item" 
                    :class="{ active: statusFilter === 'PENDING' }"
                    @click="handleFilterChange('PENDING')"
                >
                    待处理
                </div>
                <div 
                    class="tab-item" 
                    :class="{ active: statusFilter === 'PROCESSING' }"
                    @click="handleFilterChange('PROCESSING')"
                >
                    处理中
                </div>
                <div 
                    class="tab-item" 
                    :class="{ active: statusFilter === 'RESOLVED' }"
                    @click="handleFilterChange('RESOLVED')"
                >
                    已解决
                </div>
            </div>
            
            <div class="search-box">
                <el-icon class="search-icon" @click="fetchFeedbacks"><Search /></el-icon>
                <input 
                    v-model="searchQuery" 
                    placeholder="搜索反馈标题或内容..." 
                    class="search-input"
                    @keyup.enter="fetchFeedbacks"
                />
            </div>
        </div>

        <div class="table-container">
            <el-table 
                :data="feedbacks" 
                style="width: 100%" 
                v-loading="loading"
                :header-cell-style="{ background: '#1e293b', color: '#94a3b8', borderBottom: '1px solid #334155' }"
                :cell-style="{ background: '#151e32', color: '#f8fafc', borderBottom: '1px solid #1e293b' }"
            >
                <el-table-column prop="id" label="ID" width="120">
                    <template #default="scope">
                        <span class="id-text">#FB-{{ String(scope.row.id).padStart(5, '0') }}</span>
                    </template>
                </el-table-column>
                
                <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
                    <template #default="scope">
                        <span class="title-text" @click="viewDetail(scope.row)">{{ scope.row.title }}</span>
                    </template>
                </el-table-column>
                
                <el-table-column prop="content" label="内容摘要" min-width="300" show-overflow-tooltip>
                    <template #default="scope">
                        <span class="content-preview">{{ scope.row.content }}</span>
                    </template>
                </el-table-column>
                
                <el-table-column label="提交人" width="200">
                    <template #default="scope">
                        <div class="submitter-info">
                            <el-avatar :size="24" class="submitter-avatar">{{ scope.row.submitterName?.charAt(0) }}</el-avatar>
                            <div class="submitter-detail">
                                <span class="submitter-name">{{ scope.row.submitterName }}</span>
                                <span class="submitter-role">({{ formatRole(scope.row.submitterRole) }})</span>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                
                <el-table-column prop="createTime" label="提交时间" width="180">
                    <template #default="scope">
                        <div class="time-column">
                            <div class="date">{{ formatDate(scope.row.createTime) }}</div>
                            <div class="time">{{ formatTimeOnly(scope.row.createTime) }}</div>
                        </div>
                    </template>
                </el-table-column>
                
                <el-table-column prop="status" label="状态" width="120" align="center">
                    <template #default="scope">
                        <div class="status-badge" :class="getStatusClass(scope.row.status)">
                            {{ formatStatus(scope.row.status) }}
                        </div>
                    </template>
                </el-table-column>
                
                <el-table-column label="操作" width="100" align="center" fixed="right">
                    <template #default="scope">
                        <el-button 
                            type="primary" 
                            link
                            @click="viewDetail(scope.row)"
                        >
                            详情
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-container">
                <span class="page-info">显示 {{ (currentPage - 1) * pageSize + 1 }} 到 {{ Math.min(currentPage * pageSize, total) }} / 共 {{ total }} 条记录</span>
                <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="pageSize"
                    :page-sizes="[10, 20, 50]"
                    layout="prev, pager, next"
                    :total="total"
                    @size-change="fetchFeedbacks"
                    @current-change="fetchFeedbacks"
                />
            </div>
        </div>

        <!-- 底部统计卡片 -->
        <div class="stats-row">
            <div class="stat-card">
                <div class="stat-header">
                    <span class="stat-title">今日新增反馈</span>
                    <el-icon class="stat-icon-refresh" @click="fetchFeedbacks" style="cursor: pointer"><Refresh /></el-icon>
                </div>
                <div class="stat-body">
                    <span class="stat-value">{{ todayCount }}</span>
                </div>
            </div>
        </div>

        <!-- 反馈详情弹窗 -->
        <el-dialog 
            v-model="detailVisible" 
            :show-close="false"
            width="600px"
            class="feedback-detail-dialog"
            align-center
        >
            <template #header="{ close, titleId, titleClass }">
                <div class="dialog-header">
                    <div class="dialog-title-area">
                        <el-icon class="dialog-icon"><ChatDotSquare /></el-icon>
                        <div class="dialog-title-content">
                            <h3 class="dialog-main-title">反馈详情</h3>
                            <span class="dialog-sub-title">反馈编号: #FB-{{ currentFeedback.id }}</span>
                        </div>
                    </div>
                    <el-icon class="close-icon" @click="close"><Close /></el-icon>
                </div>
            </template>

            <div class="dialog-content">
                <h2 class="detail-title">{{ currentFeedback.title }}</h2>
                
                <div class="detail-meta">
                    <el-select 
                        v-model="currentFeedback.status" 
                        class="status-select"
                        size="small"
                        @change="handleStatusChange"
                    >
                        <el-option label="待处理" value="PENDING" />
                        <el-option label="处理中" value="PROCESSING" />
                        <el-option label="已解决" value="RESOLVED" />
                    </el-select>
                    <span class="meta-time">提交于 {{ currentFeedback.createTime?.replace('T', ' ').substring(0, 16) }}</span>
                </div>

                <div class="user-profile-card">
                    <el-avatar :size="48" class="profile-avatar">{{ currentFeedback.submitterName?.charAt(0) }}</el-avatar>
                    <div class="profile-info">
                        <div class="profile-name">{{ currentFeedback.submitterName }}</div>
                        <div class="profile-role">{{ formatRole(currentFeedback.submitterRole) }} · UID: {{ currentFeedback.submitterUid || '未知' }}</div>
                    </div>
                </div>

                <div class="section-title">反馈描述</div>
                <div class="detail-desc-box">
                    {{ currentFeedback.content }}
                </div>

                <div class="reply-section">
                    <div class="reply-header">
                        <div class="reply-title">
                            <el-icon><Edit /></el-icon> 管理员回复
                        </div>
                        <span class="reply-time" v-if="currentFeedback.updateTime">最后编辑于 {{ formatTimeDiff(currentFeedback.updateTime) }}前</span>
                    </div>
                    <el-input
                        v-model="replyContent"
                        type="textarea"
                        :rows="4"
                        placeholder="请输入回复内容或处理意见..."
                        class="reply-input"
                    />
                </div>
            </div>

            <template #footer>
                <div class="dialog-footer">
                    <el-button class="cancel-btn" @click="detailVisible = false">
                        <el-icon><Close /></el-icon> 关闭
                    </el-button>
                    <div class="footer-actions">
                        <el-button 
                            v-if="currentFeedback.status !== 'RESOLVED'"
                            class="resolve-btn"
                            @click="handleResolveInDetail"
                        >
                            <el-icon><Check /></el-icon> 标记为已解决
                        </el-button>
                        <el-button type="primary" class="save-btn" @click="saveReply">
                            <el-icon><Files /></el-icon> 保存回复
                        </el-button>
                    </div>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
    Search, Bell, Download, Refresh, Warning, Timer, 
    ChatDotSquare, Close, Edit, Check, Files 
} from '@element-plus/icons-vue'

const feedbacks = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusFilter = ref('')
const searchQuery = ref('')
const todayCount = ref(0)
const detailVisible = ref(false)
const currentFeedback = ref({})
const replyContent = ref('')

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
        feedbacks.value = res.records
        total.value = res.total
        
        // 获取今日新增数
        const statsRes = await request.get('/feedback/stats/today')
        todayCount.value = statsRes
    } catch (e) {
        console.error(e)
        ElMessage.error('获取反馈列表失败')
    } finally {
        loading.value = false
    }
}

const handleFilterChange = (status) => {
    statusFilter.value = status
    currentPage.value = 1
    fetchFeedbacks()
}

const viewDetail = (row) => {
    currentFeedback.value = row
    replyContent.value = row.reply || '' 
    detailVisible.value = true
}

const handleStatusChange = async (val) => {
    try {
        await request.post(`/feedback/status/${currentFeedback.value.id}`, null, {
            params: { status: val }
        })
        ElMessage.success('状态更新成功')
        fetchFeedbacks()
    } catch (e) {
        ElMessage.error('状态更新失败')
        // 回滚状态显示
        fetchFeedbacks() 
    }
}

const handleResolveInDetail = async () => {
    currentFeedback.value.status = 'RESOLVED'
    handleStatusChange('RESOLVED')
}

const saveReply = async () => {
    if (!replyContent.value.trim()) {
        ElMessage.warning('请输入回复内容')
        return
    }
    try {
        await request.post(`/feedback/reply/${currentFeedback.value.id}`, {
            reply: replyContent.value
        })
        ElMessage.success('回复已保存')
        // 更新列表状态（因为回复可能改变状态）
        fetchFeedbacks()
        // 更新当前详情视图的状态（如果后端变更为处理中）
        if (currentFeedback.value.status === 'PENDING') {
             currentFeedback.value.status = 'PROCESSING'
        }
        detailVisible.value = false
    } catch (e) {
        console.error(e)
        ElMessage.error('保存回复失败')
    }
}

// 格式化辅助函数
const formatStatus = (status) => {
    const map = {
        'PENDING': '待处理',
        'PROCESSING': '处理中',
        'RESOLVED': '已解决'
    }
    return map[status] || status
}

const getStatusClass = (status) => {
    const map = {
        'PENDING': 'status-pending',
        'PROCESSING': 'status-processing',
        'RESOLVED': 'status-resolved'
    }
    return map[status] || ''
}

const formatRole = (role) => {
    const map = {
        'ADMIN': '管理员',
        'TEACHER': '教师',
        'STUDENT': '学生'
    }
    return map[role] || role
}

const formatDate = (timeStr) => {
    if (!timeStr) return ''
    return timeStr.substring(0, 10)
}

const formatTimeOnly = (timeStr) => {
    if (!timeStr) return ''
    return timeStr.substring(11, 16)
}

const formatTimeDiff = (timeStr) => {
    if (!timeStr) return ''
    const now = new Date()
    const diff = now - new Date(timeStr)
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)
    
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟`
    if (hours < 24) return `${hours}小时`
    return `${days}天`
}

onMounted(() => {
    fetchFeedbacks()
})
</script>

<style scoped>
.feedback-manage {
    padding: 24px;
    background-color: var(--bg-color-dark);
    min-height: 100vh;
    color: #f8fafc;
}

/* Page Header */
.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
}

.page-title {
    font-size: 20px;
    font-weight: 600;
    margin: 0;
}

.header-actions {
    display: flex;
    align-items: center;
    gap: 16px;
}

.action-icon {
    font-size: 20px;
    color: #94a3b8;
    cursor: pointer;
}

.export-btn {
    background-color: #3b82f6;
    border: none;
    padding: 8px 16px;
    border-radius: 6px;
}

/* Toolbar */
.toolbar {
    display: flex;
    justify-content: space-between;
    margin-bottom: 20px;
}

.status-tabs {
    display: flex;
    background-color: #1e293b;
    padding: 4px;
    border-radius: 8px;
}

.tab-item {
    padding: 6px 24px;
    color: #94a3b8;
    font-size: 14px;
    cursor: pointer;
    border-radius: 6px;
    transition: all 0.2s;
}

.tab-item.active {
    background-color: #3b82f6;
    color: white;
    font-weight: 500;
}

.search-box {
    position: relative;
    width: 400px;
}

.search-input {
    width: 100%;
    height: 36px;
    background-color: #1e293b;
    border: 1px solid #475569;
    border-radius: 8px;
    padding: 0 16px 0 40px;
    color: white;
    outline: none;
    transition: all 0.3s;
}

.search-input:focus {
    border-color: #3b82f6;
    box-shadow: 0 0 0 1px #3b82f6;
}

.search-icon {
    position: absolute;
    left: 12px;
    top: 50%;
    transform: translateY(-50%);
    color: #64748b;
    cursor: pointer;
}
.search-icon:hover {
    color: #3b82f6;
}

/* Table Styling */
.table-container {
    background-color: #151e32;
    border-radius: 12px;
    border: 1px solid #1e293b;
    overflow: hidden;
    margin-bottom: 24px;
}

.id-text {
    color: #64748b;
    font-family: monospace;
}

.title-text {
    color: #f8fafc;
    font-weight: 600;
    cursor: pointer;
}
.title-text:hover {
    color: #3b82f6;
}

.content-preview {
    color: #94a3b8;
}

.submitter-info {
    display: flex;
    align-items: center;
    gap: 10px;
}

.submitter-avatar {
    background-color: #334155;
    color: #cbd5e1;
    font-size: 12px;
}

.submitter-detail {
    display: flex;
    flex-direction: column;
}

.submitter-name {
    font-size: 13px;
    color: #f8fafc;
}

.submitter-role {
    font-size: 12px;
    color: #64748b;
}

.time-column {
    display: flex;
    flex-direction: column;
}

.date {
    color: #f8fafc;
    font-size: 13px;
}

.time {
    color: #64748b;
    font-size: 12px;
}

/* Status Badges */
.status-badge {
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;
    display: inline-block;
}

.status-pending {
    background-color: rgba(245, 158, 11, 0.1);
    color: #f59e0b;
}

.status-processing {
    background-color: rgba(59, 130, 246, 0.1);
    color: #3b82f6;
}

.status-resolved {
    background-color: rgba(16, 185, 129, 0.1);
    color: #10b981;
}

/* Pagination */
.pagination-container {
    padding: 16px 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid #1e293b;
}

.page-info {
    color: #64748b;
    font-size: 13px;
}

:deep(.el-pagination button) {
    background-color: #1e293b !important;
    color: #94a3b8 !important;
    border-radius: 4px;
}

:deep(.el-pager li) {
    background-color: #1e293b !important;
    color: #94a3b8 !important;
    border-radius: 4px;
}

:deep(.el-pager li.is-active) {
    background-color: #3b82f6 !important;
    color: white !important;
}

/* Stats Cards */
.stats-row {
    display: flex;
    gap: 20px;
}

.stat-card {
    min-width: 240px;
    background-color: #151e32;
    border: 1px solid #1e293b;
    border-radius: 12px;
    padding: 20px;
}

.stat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.stat-title {
    color: #94a3b8;
    font-size: 13px;
}

.stat-icon-refresh { color: #3b82f6; background: rgba(59, 130, 246, 0.1); padding: 4px; border-radius: 4px; }
.stat-icon-warning { color: #ef4444; background: rgba(239, 68, 68, 0.1); padding: 4px; border-radius: 4px; }
.stat-icon-timer { color: #10b981; background: rgba(16, 185, 129, 0.1); padding: 4px; border-radius: 4px; }

.stat-body {
    display: flex;
    align-items: baseline;
    gap: 8px;
}

.stat-value {
    font-size: 32px;
    font-weight: 700;
    color: #f8fafc;
}

.stat-trend {
    font-size: 13px;
}
.stat-trend.positive { color: #10b981; }

.stat-sub {
    color: #64748b;
    font-size: 13px;
}

/* Dialog Styles */
.dialog-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 20px;
    border-bottom: 1px solid #334155;
}

.dialog-title-area {
    display: flex;
    gap: 12px;
}

.dialog-icon {
    width: 40px;
    height: 40px;
    background-color: #3b82f6;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 20px;
}

.dialog-title-content {
    display: flex;
    flex-direction: column;
}

.dialog-main-title {
    margin: 0;
    font-size: 16px;
    color: #f8fafc;
}

.dialog-sub-title {
    font-size: 12px;
    color: #64748b;
}

.close-icon {
    font-size: 20px;
    color: #94a3b8;
    cursor: pointer;
}

.dialog-content {
    padding: 20px 0;
}

.detail-title {
    font-size: 24px;
    color: #f8fafc;
    margin: 0 0 16px 0;
}

.detail-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 24px;
}

.status-tag {
    padding: 2px 10px;
    border-radius: 4px;
    font-size: 12px;
}

.meta-time {
    color: #64748b;
    font-size: 13px;
}

.status-select {
    width: 120px;
}
:deep(.status-select .el-input__wrapper) {
    background-color: #1e293b;
    box-shadow: 0 0 0 1px #334155 inset;
}
:deep(.status-select .el-input__inner) {
    color: #f8fafc;
}

.user-profile-card {
    background-color: #1e293b;
    border-radius: 8px;
    padding: 16px;
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 24px;
}

.profile-avatar {
    border: 2px solid #3b82f6;
}

.profile-name {
    color: #f8fafc;
    font-weight: 600;
    font-size: 16px;
}

.profile-role {
    color: #94a3b8;
    font-size: 13px;
}

.section-title {
    color: #64748b;
    font-size: 13px;
    margin-bottom: 8px;
}

.detail-desc-box {
    color: #cbd5e1;
    line-height: 1.6;
    margin-bottom: 32px;
    white-space: pre-wrap;
}

.reply-section {
    border-top: 1px solid #334155;
    padding-top: 24px;
}

.reply-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
}

.reply-title {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #94a3b8;
    font-size: 13px;
}

.reply-time {
    color: #64748b;
    font-size: 12px;
}

.reply-input :deep(.el-textarea__inner) {
    background-color: #1e293b;
    border: 1px solid #334155;
    color: #f8fafc;
    box-shadow: none;
}

.dialog-footer {
    display: flex;
    justify-content: space-between;
    padding-top: 20px;
    border-top: 1px solid #334155;
}

.cancel-btn {
    background: transparent;
    border: none;
    color: #94a3b8;
}
.cancel-btn:hover {
    color: #f8fafc;
}

.footer-actions {
    display: flex;
    gap: 12px;
}

.resolve-btn {
    background-color: rgba(59, 130, 246, 0.1);
    border: 1px solid #3b82f6;
    color: #3b82f6;
}

.save-btn {
    background-color: #3b82f6;
}

:deep(.feedback-detail-dialog) {
    background-color: #151e32;
    border-radius: 16px;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

:deep(.feedback-detail-dialog .el-dialog__header) {
    display: none; /* 使用自定义 header */
}

:deep(.feedback-detail-dialog .el-dialog__body) {
    padding: 32px;
}

:deep(.feedback-detail-dialog .el-dialog__footer) {
    padding: 0 32px 32px 32px;
}
</style>

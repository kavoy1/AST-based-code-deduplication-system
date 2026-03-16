<template>
    <div class="notifications-page">
        <div class="page-header">
            <div class="header-content">
                <h2 class="page-title">通知中心</h2>
                <p class="page-desc">管理您的反馈回复与系统消息，及时掌握查重动态。</p>
            </div>
            <div class="header-actions">
                <el-button type="primary" class="mark-read-btn" @click="markAllRead">
                    <el-icon><Check /></el-icon> 全部标记为已读
                </el-button>
                <el-button class="clear-btn" @click="clearRead">
                    <el-icon><Delete /></el-icon> 清除已读
                </el-button>
            </div>
        </div>

        <div class="content-wrapper">
            <div class="tabs-header">
                <div 
                    class="tab-item" 
                    :class="{ active: activeTab === 'all' }"
                    @click="activeTab = 'all'"
                >
                    全部
                </div>
                <div 
                    class="tab-item" 
                    :class="{ active: activeTab === 'unread' }"
                    @click="activeTab = 'unread'"
                >
                    未读 <span class="badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
                </div>
                <div 
                    class="tab-item" 
                    :class="{ active: activeTab === 'reply' }"
                    @click="activeTab = 'reply'"
                >
                    反馈回复
                </div>
                <div 
                    class="tab-item" 
                    :class="{ active: activeTab === 'system' }"
                    @click="activeTab = 'system'"
                >
                    系统通知
                </div>
            </div>

            <div class="notification-list" v-loading="loading">
                <div v-if="filteredList.length === 0" class="empty-state">
                    <el-empty description="暂无通知" />
                </div>
                <div 
                    v-else 
                    v-for="item in currentPageList" 
                    :key="item.id" 
                    class="notification-item"
                    :class="{ unread: !item.isRead }"
                >
                    <div class="item-icon" :class="item.type">
                        <el-icon>
                            <component :is="getIcon(item.type)" />
                        </el-icon>
                    </div>
                    <div class="item-content">
                        <div class="item-header">
                            <span class="item-title" @click.stop="viewDetail(item)">{{ item.title }}</span>
                            <span class="item-time">{{ formatTimeDiff(item.time) }}</span>
                        </div>
                        <div class="item-desc">{{ item.content }}</div>
                        <div class="item-actions" v-if="item.linkText">
                            <span class="action-link" @click.stop="viewDetail(item)">{{ item.linkText }}</span>
                        </div>
                    </div>
                    <div class="item-status" v-if="!item.isRead">
                        <div class="unread-dot"></div>
                    </div>
                </div>
            </div>

            <div class="pagination-bar" v-if="filteredList.length > 0">
                <span class="page-info">显示 {{ (currentPage - 1) * pageSize + 1 }} 到 {{ Math.min(currentPage * pageSize, filteredList.length) }} 条，共 {{ filteredList.length }} 条通知</span>
                <el-pagination
                    v-model:current-page="currentPage"
                    v-model:page-size="pageSize"
                    layout="prev, pager, next"
                    :total="filteredList.length"
                    class="pagination"
                />
            </div>
        </div>
    </div>

    <!-- 通知详情弹窗 -->
    <el-dialog
        v-model="detailVisible"
        :title="currentDetail.title"
        width="500px"
        class="notification-detail-dialog"
    >
        <div class="detail-content">
            <div class="detail-meta">
                <el-tag size="small" :type="currentDetail.type === 'reply' ? 'success' : 'info'">
                    {{ currentDetail.type === 'reply' ? '反馈回复' : '系统通知' }}
                </el-tag>
                <span class="detail-time">{{ currentDetail.time?.replace('T', ' ') }}</span>
            </div>
            <div class="detail-body">
                {{ currentDetail.content }}
            </div>
        </div>
        <template #footer>
            <span class="dialog-footer">
                <el-button type="primary" @click="detailVisible = false">关闭</el-button>
            </span>
        </template>
    </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Check, Delete, ChatDotSquare, Bell, Warning, CircleCheck, InfoFilled } from '@element-plus/icons-vue'
import request from '../api/request'
import { ElMessage } from 'element-plus'

const activeTab = ref('all')
const loading = ref(false)
const rawList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)

const user = JSON.parse(localStorage.getItem('user') || '{}')

// 模拟本地存储已读状态 (区分用户)
const storageKey = user.id ? `readNotificationIds_${user.id}` : 'readNotificationIds'
const readIds = ref(JSON.parse(localStorage.getItem(storageKey) || '[]'))

const fetchData = async () => {
    loading.value = true
    try {
        const list = []
        
        // 1. 获取系统通知
        const notices = await request.get('/notice/list')
        if (notices) {
            notices.forEach(n => {
                list.push({
                    id: `notice-${n.id}`,
                    type: 'system',
                    title: n.title,
                    content: n.content,
                    time: n.createTime,
                    isRead: readIds.value.includes(`notice-${n.id}`),
                    expanded: false
                })
            })
        }

        // 2. 获取反馈回复
        if (user.role !== 'ADMIN') {
            const feedbacks = await request.get('/feedback/my', { params: { userId: user.id } })
            if (feedbacks) {
                feedbacks.forEach(f => {
                    // 只显示有回复的或已解决的
                    if (f.reply || f.status === 'RESOLVED') {
                        list.push({
                            id: `feedback-${f.id}`,
                            type: 'reply',
                            title: f.reply ? '管理员已回复您的反馈' : '反馈状态更新',
                            content: f.reply ? `关于 "${f.title}" 的反馈，管理员回复：${f.reply}` : `您的反馈 "${f.title}" 已被标记为已解决。`,
                            time: f.updateTime || f.createTime,
                            isRead: readIds.value.includes(`feedback-${f.id}`),
                            linkText: '查看详情',
                            raw: f,
                            expanded: false
                        })
                    }
                })
            }
        }

        // 按时间倒序
        list.sort((a, b) => new Date(b.time) - new Date(a.time))
        rawList.value = list
    } catch (e) {
        console.error(e)
        ElMessage.error('获取通知失败')
    } finally {
        loading.value = false
    }
}

const filteredList = computed(() => {
    if (activeTab.value === 'all') return rawList.value
    if (activeTab.value === 'unread') return rawList.value.filter(i => !i.isRead)
    if (activeTab.value === 'reply') return rawList.value.filter(i => i.type === 'reply')
    if (activeTab.value === 'system') return rawList.value.filter(i => i.type === 'system')
    return rawList.value
})

const currentPageList = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    return filteredList.value.slice(start, end)
})

const unreadCount = computed(() => {
    return rawList.value.filter(i => !i.isRead).length
})

const getIcon = (type) => {
    const map = {
        'reply': ChatDotSquare,
        'system': Bell,
        'warning': Warning,
        'success': CircleCheck
    }
    return map[type] || InfoFilled
}

const formatTimeDiff = (timeStr) => {
    if (!timeStr) return ''
    const now = new Date()
    const target = new Date(timeStr)
    const diff = now - target
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)
    
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    if (hours < 24) return `${hours}小时前`
    if (days === 1) return `昨天 ${target.getHours().toString().padStart(2, '0')}:${target.getMinutes().toString().padStart(2, '0')}`
    return timeStr.substring(0, 10)
}

const detailVisible = ref(false)
const currentDetail = ref({})

const viewDetail = (item) => {
    currentDetail.value = item
    detailVisible.value = true
    
    if (!item.isRead) {
        item.isRead = true
        readIds.value.push(item.id)
        localStorage.setItem(storageKey, JSON.stringify(readIds.value))
        window.dispatchEvent(new Event('notification-update'))
    }
}

const markAllRead = () => {
    rawList.value.forEach(item => {
        if (!item.isRead) {
            item.isRead = true
            readIds.value.push(item.id)
        }
    })
    localStorage.setItem(storageKey, JSON.stringify(readIds.value))
    ElMessage.success('已全部标记为已读')
    window.dispatchEvent(new Event('notification-update'))
}

const clearRead = () => {
    // 这里的清除已读是清除列表中的已读项吗？通常是。
    // 但由于我们没有后端删除接口，这里只能是前端隐藏或者不做物理删除
    // 假设是“清除已读记录”，即从列表中移除已读项
    // 或者仅仅是把“已读”状态清除？不，应该是移除已读消息
    // 这里简单实现为：在当前视图中隐藏已读项（如果后端不支持删除）
    // 但通常“清除已读”意味着删除。这里为了安全，暂不实现物理删除，只做提示
    ElMessage.info('暂不支持物理删除通知')
}

onMounted(() => {
    fetchData()
})
</script>

<style scoped>
.notifications-page {
    padding: 24px 40px;
    max-width: 1200px;
    margin: 0 auto;
    color: #f8fafc;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 32px;
}

.page-title {
    font-size: 28px;
    font-weight: 700;
    margin: 0 0 8px 0;
    color: #f8fafc;
}

.page-desc {
    color: #94a3b8;
    margin: 0;
    font-size: 14px;
}

.header-actions {
    display: flex;
    gap: 12px;
}

.mark-read-btn {
    background-color: #2563eb;
    border: none;
    padding: 8px 20px;
}

.clear-btn {
    background-color: transparent;
    border: 1px solid #334155;
    color: #cbd5e1;
}
.clear-btn:hover {
    background-color: rgba(255, 255, 255, 0.05);
    color: #f8fafc;
}

.content-wrapper {
    background-color: #151e32; /* var(--card-bg) */
    border-radius: 12px;
    border: 1px solid #1e293b; /* var(--border-color) */
    overflow: hidden;
    min-height: 600px;
    display: flex;
    flex-direction: column;
}

.tabs-header {
    display: flex;
    border-bottom: 1px solid #1e293b;
    padding: 0 24px;
}

.tab-item {
    padding: 16px 24px;
    color: #94a3b8;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    border-bottom: 2px solid transparent;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    gap: 6px;
}

.tab-item:hover {
    color: #cbd5e1;
}

.tab-item.active {
    color: #3b82f6;
    border-bottom-color: #3b82f6;
}

.badge {
    background-color: #ef4444;
    color: white;
    font-size: 10px;
    padding: 0 6px;
    border-radius: 10px;
    height: 16px;
    line-height: 16px;
}

.notification-list {
    flex: 1;
}

.notification-item {
    display: flex;
    padding: 24px;
    border-bottom: 1px solid #1e293b;
    cursor: pointer;
    transition: background-color 0.2s;
    position: relative;
}

.notification-item:hover {
    background-color: rgba(30, 41, 59, 0.5);
}

.notification-item:last-child {
    border-bottom: none;
}

.item-icon {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20px;
    flex-shrink: 0;
}

.item-icon.reply { background-color: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.item-icon.system { background-color: rgba(100, 116, 139, 0.1); color: #94a3b8; }
.item-icon.warning { background-color: rgba(245, 158, 11, 0.1); color: #f59e0b; }
.item-icon.success { background-color: rgba(16, 185, 129, 0.1); color: #10b981; }

.item-icon .el-icon {
    font-size: 24px;
}

.item-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.item-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 6px;
}

.item-title {
    font-size: 16px;
    font-weight: 600;
    color: #f8fafc;
    margin-right: 12px;
    word-break: break-all;
    cursor: pointer;
    transition: color 0.2s;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.item-title:hover {
    color: #3b82f6;
    text-decoration: underline;
}

.item-time {
    font-size: 13px;
    color: #64748b;
    white-space: nowrap;
    flex-shrink: 0;
}

.item-desc {
    color: #94a3b8;
    font-size: 14px;
    line-height: 1.5;
    margin-bottom: 8px;
    word-break: break-all;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.detail-content {
    padding: 10px 0;
}

.detail-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e2e8f0;
}

.detail-time {
    color: #94a3b8;
    font-size: 13px;
}

.detail-body {
    font-size: 15px;
    line-height: 1.6;
    color: #334155;
    white-space: pre-wrap;
}

/* Dark mode support for dialog */
:deep(.notification-detail-dialog) {
    background-color: #1e293b;
    border: 1px solid #334155;
}

:deep(.notification-detail-dialog .el-dialog__title) {
    color: #f8fafc;
}

:deep(.notification-detail-dialog .detail-body) {
    color: #cbd5e1;
}

:deep(.notification-detail-dialog .el-dialog__headerbtn .el-dialog__close) {
    color: #94a3b8;
}

:deep(.notification-detail-dialog .el-dialog__headerbtn:hover .el-dialog__close) {
    color: #3b82f6;
}

.action-link {
    color: #3b82f6;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
}
.action-link:hover {
    text-decoration: underline;
}

.item-status {
    display: flex;
    align-items: center;
    margin-left: 16px;
}

.unread-dot {
    width: 8px;
    height: 8px;
    background-color: #3b82f6;
    border-radius: 50%;
}

.notification-item.unread .item-title {
    color: #f8fafc;
}

/* Pagination */
.pagination-bar {
    padding: 16px 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid #1e293b;
    background-color: #151e32;
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
</style>
<template>
  <div class="workspace-page notifications-page">
    <WorkspacePanel compact class="notifications-panel">
      <div class="notifications-toolbar">
        <div class="workspace-segmented notifications-toolbar__tabs">
          <button v-for="tab in tabs" :key="tab.value" :class="{ active: activeTab === tab.value }" @click="activeTab = tab.value">{{ tab.label }}</button>
        </div>
        <div class="notifications-toolbar__actions">
          <span class="workspace-pill">未读 {{ unreadCount }}</span>
          <el-button @click="markAllRead">全部标记已读</el-button>
          <el-button type="primary" @click="clearRead">清理本地已读</el-button>
        </div>
      </div>

      <div class="notifications-stream" v-loading="loading">
        <WorkspaceEmpty v-if="filteredList.length === 0" title="没有新的通知" description="当前筛选条件下没有可展示消息。" />
        <article
          v-for="item in filteredList"
          :key="item.id"
          class="notifications-stream__item"
          :class="{ unread: !item.isRead }"
          @click="viewDetail(item)"
        >
          <div class="notifications-stream__meta">
            <div class="workspace-badge-soft" :class="badgeClass(item.type)">{{ typeLabel(item.type) }}</div>
            <div class="notifications-stream__meta-right">
              <span>{{ formatTimeDiff(item.time) }}</span>
              <span class="notifications-stream__state">{{ item.isRead ? '已读' : '未读' }}</span>
            </div>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.content }}</p>
          <div class="notifications-stream__time">{{ formatDateTime(item.time) }}</div>
        </article>
      </div>
    </WorkspacePanel>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../api/request'
import { ElMessage } from 'element-plus'
import WorkspaceEmpty from '../components/workspace/WorkspaceEmpty.vue'
import WorkspacePanel from '../components/workspace/WorkspacePanel.vue'

const user = JSON.parse(localStorage.getItem('user') || '{}')
const storageKey = user.id ? `readNotificationIds_${user.id}` : 'readNotificationIds'
const readIds = ref(JSON.parse(localStorage.getItem(storageKey) || '[]'))
const activeTab = ref('all')
const loading = ref(false)
const rawList = ref([])

const tabs = [
  { label: '全部', value: 'all' },
  { label: '未读', value: 'unread' },
  { label: '反馈回复', value: 'reply' },
  { label: '私人通知', value: 'private' },
  { label: '系统公告', value: 'system' }
]

const persistRead = () => {
  localStorage.setItem(storageKey, JSON.stringify(readIds.value))
  window.dispatchEvent(new Event('notification-update'))
}

const buildNoticeItem = (notice) => {
  const isPrivate = Boolean(notice.receiverId)
  return {
    id: `notice-${notice.id}`,
    noticeId: notice.id,
    receiverId: notice.receiverId,
    type: isPrivate ? 'private' : 'system',
    title: notice.title,
    content: notice.content,
    time: notice.createTime,
    isRead: isPrivate ? Number(notice.isRead) === 1 : readIds.value.includes(`notice-${notice.id}`)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const list = []
    const notices = await request.get('/notice/list')
    if (Array.isArray(notices)) {
      notices.forEach((notice) => list.push(buildNoticeItem(notice)))
    }
    if (user.role !== 'ADMIN') {
      const feedbacks = await request.get('/feedback/my', { params: { userId: user.id } })
      if (Array.isArray(feedbacks)) {
        feedbacks.forEach((feedback) => {
          if (feedback.reply || feedback.status === 'RESOLVED') {
            list.push({
              id: `feedback-${feedback.id}`,
              feedbackId: feedback.id,
              type: 'reply',
              title: feedback.reply ? `反馈“${feedback.title}”收到回复` : `反馈“${feedback.title}”状态已更新`,
              content: feedback.reply || `当前状态：${feedback.status}`,
              time: feedback.updateTime || feedback.createTime,
              isRead: readIds.value.includes(`feedback-${feedback.id}`)
            })
          }
        })
      }
    }
    rawList.value = list.sort((a, b) => new Date(b.time) - new Date(a.time))
  } catch {
    ElMessage.error('获取通知失败')
  } finally {
    loading.value = false
  }
}

const filteredList = computed(() => {
  if (activeTab.value === 'unread') return rawList.value.filter((item) => !item.isRead)
  if (activeTab.value === 'reply') return rawList.value.filter((item) => item.type === 'reply')
  if (activeTab.value === 'private') return rawList.value.filter((item) => item.type === 'private')
  if (activeTab.value === 'system') return rawList.value.filter((item) => item.type === 'system')
  return rawList.value
})

const unreadCount = computed(() => rawList.value.filter((item) => !item.isRead).length)

const markItemRead = async (item) => {
  if (item.isRead) return
  if (item.noticeId && item.receiverId) {
    await request.post(`/notice/read/${item.noticeId}`)
    item.isRead = true
    window.dispatchEvent(new Event('notification-update'))
    return
  }
  item.isRead = true
  if (!readIds.value.includes(item.id)) readIds.value.push(item.id)
  persistRead()
}

const viewDetail = async (item) => {
  if (!item.isRead) {
    try {
      await markItemRead(item)
    } catch {
      ElMessage.error('标记已读失败')
    }
  }
}

const markAllRead = async () => {
  try {
    const pendingRemote = rawList.value.filter((item) => !item.isRead && item.noticeId && item.receiverId)
    if (pendingRemote.length) {
      await Promise.all(pendingRemote.map((item) => request.post(`/notice/read/${item.noticeId}`)))
      pendingRemote.forEach((item) => { item.isRead = true })
      window.dispatchEvent(new Event('notification-update'))
    }
    rawList.value.forEach((item) => {
      if (!item.noticeId || !item.receiverId) {
        item.isRead = true
        if (!readIds.value.includes(item.id)) readIds.value.push(item.id)
      }
    })
    persistRead()
    ElMessage.success('已全部标记已读')
  } catch {
    ElMessage.error('批量标记已读失败')
  }
}

const clearRead = async () => {
  readIds.value = []
  rawList.value.forEach((item) => {
    if (!item.noticeId || !item.receiverId) item.isRead = false
  })
  persistRead()
  ElMessage.success('本地已读记录已清理')
  await fetchData()
}

const typeLabel = (type) => ({ reply: '反馈回复', private: '私人通知', system: '系统公告' }[type] || '通知')
const badgeClass = (type) => ({ reply: 'workspace-badge-soft--green', private: 'workspace-badge-soft--amber', system: 'workspace-badge-soft--blue' }[type] || '')
const formatDateTime = (time) => time ? new Date(time).toLocaleString() : ''
const formatTimeDiff = (time) => {
  if (!time) return ''
  const diff = Date.now() - new Date(time).getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < hour) return `${Math.max(1, Math.floor(diff / minute))} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < day * 2) return '昨天'
  return new Date(time).toLocaleDateString()
}

onMounted(fetchData)
</script>

<style scoped>
.notifications-page {
  height: 100%;
  min-height: calc(100vh - 96px);
}

.notifications-panel {
  height: 100%;
}

.notifications-panel :deep(.workspace-panel) {
  height: 100%;
}

.notifications-panel :deep(.workspace-panel__body) {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
}

.notifications-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.notifications-toolbar__tabs {
  flex-wrap: wrap;
}

.notifications-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.notifications-stream {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
  flex: 1;
  min-height: 0;
}

.notifications-stream__item {
  border: 1px solid rgba(29, 35, 43, 0.05);
  background: rgba(248, 250, 251, 0.84);
  border-radius: 22px;
  padding: 20px 22px;
  cursor: pointer;
  transition: transform var(--transition-soft), box-shadow var(--transition-soft), border-color var(--transition-soft);
}

.notifications-stream__item:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(122, 136, 146, 0.10);
}

.notifications-stream__item.unread {
  background: rgba(255, 255, 255, 0.98);
  border-color: rgba(82, 110, 255, 0.18);
  box-shadow: 0 14px 28px rgba(122, 136, 146, 0.12);
}

.notifications-stream__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.notifications-stream__meta-right {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-soft);
  font-size: 13px;
}

.notifications-stream__state {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(18, 25, 38, 0.06);
}

.notifications-stream__item h3 {
  margin: 0 0 10px;
  font-size: 20px;
}

.notifications-stream__item p {
  margin: 0;
  color: var(--text-body);
  line-height: 1.7;
}

.notifications-stream__time {
  margin-top: 14px;
  color: var(--text-soft);
  font-size: 13px;
}
</style>

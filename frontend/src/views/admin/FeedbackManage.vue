<template>
  <div class="workspace-page feedback-page">
    <header class="feedback-page__head">
      <div>
        <p class="feedback-page__eyebrow">管理员工作台</p>
        <h1>问题反馈</h1>
        <p class="feedback-page__description">按状态查看反馈工单，在右侧连续完成阅读、回复与结案。</p>
      </div>
      <div class="feedback-page__tools">
        <el-input
          v-model="searchQuery"
          clearable
          placeholder="搜索标题或内容"
          @keyup.enter="fetchFeedbacks"
          @clear="fetchFeedbacks"
        />
        <el-button @click="fetchFeedbacks">刷新</el-button>
      </div>
    </header>

    <div class="feedback-workbench">
      <section class="feedback-inbox">
        <div class="feedback-inbox__tabs">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            type="button"
            class="feedback-tab"
            :class="{ 'feedback-tab--active': statusFilter === tab.value }"
            @click="handleFilterChange(tab.value)"
          >
            <span>{{ tab.label }}</span>
            <strong>{{ tab.value ? countByStatus(tab.value) : total }}</strong>
          </button>
        </div>

        <div class="feedback-inbox__meta">
          <span>当前页 {{ currentPage }}</span>
          <span>共 {{ total }} 条</span>
        </div>

        <div v-loading="loading" class="feedback-inbox__list">
          <WorkspaceEmpty
            v-if="!feedbacks.length"
            title="暂无反馈"
            description="调整筛选条件后再试，或等待用户提交新的问题反馈。"
          />
          <button
            v-for="item in feedbacks"
            :key="item.id"
            type="button"
            class="feedback-ticket"
            :class="{ 'feedback-ticket--active': currentFeedback.id === item.id }"
            @click="viewDetail(item)"
          >
            <div class="feedback-ticket__top">
              <span class="feedback-ticket__id">#PG-{{ item.id }}</span>
              <span class="feedback-ticket__time">{{ formatRelativeTime(item.createTime) }}</span>
            </div>
            <h3>{{ item.title }}</h3>
            <p class="feedback-ticket__summary">{{ item.content }}</p>
            <div class="feedback-ticket__bottom">
              <span class="feedback-ticket__author">{{ item.submitterName || '未知提交人' }}</span>
              <span class="workspace-badge-soft" :class="statusClass(item.status)">{{ formatStatus(item.status) }}</span>
            </div>
          </button>
        </div>

        <div class="feedback-inbox__pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            :total="total"
            @size-change="fetchFeedbacks"
            @current-change="fetchFeedbacks"
          />
        </div>
      </section>

      <section class="feedback-thread">
        <WorkspaceEmpty
          v-if="!currentFeedback.id"
          title="请选择一条反馈"
          description="点击左侧工单后，在这里查看详情和回复内容。"
        />

        <template v-else>
          <div class="feedback-thread__header">
            <div class="feedback-thread__user">
              <div class="feedback-avatar">{{ buildAvatarText(currentFeedback.submitterName) }}</div>
              <div class="feedback-thread__user-meta">
                <div class="feedback-thread__name-row">
                  <h2>{{ currentFeedback.submitterName || '未知提交人' }}</h2>
                  <span class="workspace-badge-soft" :class="statusClass(currentFeedback.status)">
                    {{ formatStatus(currentFeedback.status) }}
                  </span>
                </div>
                <p>
                  {{ formatRole(currentFeedback.submitterRole) }}
                  <span v-if="currentFeedback.submitterUid"> · UID {{ currentFeedback.submitterUid }}</span>
                </p>
                <small>{{ formatDate(currentFeedback.createTime) }}</small>
              </div>
            </div>
          </div>

          <article class="feedback-thread__content">
            <div class="feedback-thread__title-row">
              <h3>{{ currentFeedback.title }}</h3>
              <span>{{ formatDate(currentFeedback.createTime) }}</span>
            </div>
            <p>{{ currentFeedback.content }}</p>
          </article>

          <div class="feedback-thread__helper">
            <button
              v-for="template in quickReplyTemplates"
              :key="template.label"
              type="button"
              class="feedback-helper-chip"
              @click="appendQuickReply(template.content)"
            >
              {{ template.label }}
            </button>
          </div>

          <div class="feedback-thread__composer">
            <div class="feedback-thread__composer-head">
              <div>
                <span class="feedback-thread__label">回复内容</span>
                <strong>发给 {{ currentFeedback.submitterName || '该用户' }}</strong>
              </div>
              <el-select v-model="currentFeedback.status" @change="handleStatusChange" placeholder="状态">
                <el-option label="待处理" value="PENDING" />
                <el-option label="处理中" value="PROCESSING" />
                <el-option label="已解决" value="RESOLVED" />
              </el-select>
            </div>
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="8"
              resize="none"
              :placeholder="`回复 ${currentFeedback.submitterName || '用户'}，说明处理结果或下一步安排`"
            />
          </div>

          <footer class="feedback-thread__footer">
            <div class="feedback-thread__footer-note">
              <span>最近更新</span>
              <strong>{{ formatDate(currentFeedback.updateTime || currentFeedback.createTime) }}</strong>
            </div>
            <div class="feedback-thread__actions">
              <el-button @click="saveReply">保存回复</el-button>
              <el-button
                v-if="currentFeedback.status !== 'RESOLVED'"
                type="primary"
                @click="handleResolveInDetail"
              >
                标记为已解决
              </el-button>
            </div>
          </footer>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'
import WorkspaceEmpty from '../../components/workspace/WorkspaceEmpty.vue'

const feedbacks = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusFilter = ref('')
const searchQuery = ref('')
const currentFeedback = ref({})
const replyContent = ref('')

const tabs = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已解决', value: 'RESOLVED' }
]

const quickReplyTemplates = [
  { label: '已收到，正在排查', content: '您好，我们已经收到这条反馈，当前正在排查中，确认结果后会第一时间回复您。' },
  { label: '请补充截图/复现步骤', content: '您好，为了更快定位问题，请补充报错截图、操作步骤以及出现问题的大致时间。' }
]

const fetchFeedbacks = async () => {
  loading.value = true
  try {
    const previousId = currentFeedback.value.id
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

    const nextSelected = feedbacks.value.find((item) => item.id === previousId) || feedbacks.value[0]
    if (nextSelected) {
      viewDetail(nextSelected)
    } else {
      currentFeedback.value = {}
      replyContent.value = ''
    }
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

const appendQuickReply = (content) => {
  replyContent.value = replyContent.value.trim() ? `${replyContent.value}\n\n${content}` : content
}

const buildAvatarText = (name) => {
  if (!name) return '匿'
  return String(name).trim().slice(0, 1).toUpperCase()
}

const countByStatus = (status) => feedbacks.value.filter((item) => item.status === status).length

const formatDate = (value) => value ? new Date(value).toLocaleString() : '-'

const formatRelativeTime = (value) => {
  if (!value) return '-'
  const diff = Date.now() - new Date(value).getTime()
  const minutes = Math.max(1, Math.floor(diff / 60000))
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  return `${days} 天前`
}

const formatRole = (role) => ({ ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[role] || role)
const formatStatus = (status) => ({ PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决' }[status] || status)
const statusClass = (status) => ({
  PENDING: 'workspace-badge-soft--amber',
  PROCESSING: 'workspace-badge-soft--blue',
  RESOLVED: 'workspace-badge-soft--green'
}[status] || '')

onMounted(fetchFeedbacks)
</script>

<style scoped>
.feedback-page {
  gap: 24px;
}

.feedback-page__head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
}

.feedback-page__eyebrow {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: rgba(88, 109, 162, 0.72);
  text-transform: uppercase;
}

.feedback-page__head h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.05;
  color: #13233f;
}

.feedback-page__description {
  margin: 10px 0 0;
  max-width: 520px;
  color: rgba(73, 90, 126, 0.78);
  line-height: 1.7;
}

.feedback-page__tools {
  display: flex;
  align-items: center;
  gap: 12px;
}

.feedback-page__tools :deep(.el-input) {
  width: 300px;
}

.feedback-workbench {
  display: grid;
  grid-template-columns: minmax(320px, 390px) minmax(0, 1fr);
  gap: 20px;
  min-height: 720px;
}

.feedback-inbox,
.feedback-thread {
  border: 1px solid rgba(194, 206, 238, 0.82);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(246, 249, 255, 0.9)),
    rgba(255, 255, 255, 0.88);
  border-radius: 30px;
  box-shadow: 0 22px 56px rgba(78, 95, 143, 0.08);
}

.feedback-inbox {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.feedback-inbox__tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 18px;
  gap: 10px;
  border-bottom: 1px solid rgba(220, 227, 244, 0.9);
}

.feedback-tab {
  border: 0;
  border-radius: 18px;
  background: rgba(241, 244, 253, 0.9);
  color: rgba(83, 98, 137, 0.88);
  padding: 14px 12px;
  display: grid;
  gap: 4px;
  justify-items: center;
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease, color 0.18s ease;
}

.feedback-tab strong {
  font-size: 18px;
  color: #1c2e4b;
}

.feedback-tab--active {
  background: #17233d;
  color: rgba(237, 242, 255, 0.84);
  transform: translateY(-1px);
}

.feedback-tab--active strong {
  color: #fff;
}

.feedback-inbox__meta {
  padding: 0 22px 16px;
  display: flex;
  justify-content: space-between;
  color: rgba(91, 106, 141, 0.72);
  font-size: 13px;
}

.feedback-inbox__list {
  flex: 1;
  padding: 0 14px 14px;
  overflow: auto;
}

.feedback-ticket {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(228, 234, 248, 0.95);
  background: #fff;
  border-radius: 22px;
  padding: 16px 18px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.feedback-ticket:hover {
  transform: translateY(-1px);
  border-color: rgba(162, 178, 225, 0.9);
  box-shadow: 0 14px 28px rgba(97, 115, 162, 0.08);
}

.feedback-ticket--active {
  border-color: rgba(130, 153, 227, 0.92);
  box-shadow: inset 3px 0 0 #6d85df, 0 16px 32px rgba(91, 114, 186, 0.12);
  background: linear-gradient(180deg, rgba(246, 249, 255, 0.98), rgba(255, 255, 255, 1));
}

.feedback-ticket__top,
.feedback-ticket__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.feedback-ticket__id {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(227, 234, 254, 0.9);
  color: #5871be;
  font-size: 12px;
  font-weight: 700;
}

.feedback-ticket__time,
.feedback-ticket__author {
  color: rgba(95, 108, 140, 0.76);
  font-size: 13px;
}

.feedback-ticket h3 {
  margin: 14px 0 10px;
  font-size: 22px;
  line-height: 1.35;
  color: #192a45;
}

.feedback-ticket__summary {
  margin: 0 0 14px;
  color: rgba(65, 80, 115, 0.78);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
  -webkit-line-clamp: 2;
}

.feedback-inbox__pagination {
  padding: 8px 18px 18px;
  border-top: 1px solid rgba(220, 227, 244, 0.9);
}

.feedback-thread {
  display: flex;
  flex-direction: column;
  padding: 24px;
  gap: 20px;
}

.feedback-thread__header,
.feedback-thread__content,
.feedback-thread__composer,
.feedback-thread__footer {
  border: 1px solid rgba(226, 232, 246, 0.92);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
}

.feedback-thread__header {
  padding: 20px 22px;
}

.feedback-thread__user {
  display: flex;
  align-items: center;
  gap: 16px;
}

.feedback-avatar {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: linear-gradient(180deg, #d8e4ff, #bfd2ff);
  color: #22355c;
  font-size: 24px;
  font-weight: 800;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.feedback-thread__user-meta {
  min-width: 0;
}

.feedback-thread__name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.feedback-thread__name-row h2 {
  margin: 0;
  font-size: 30px;
  color: #1b2943;
}

.feedback-thread__user-meta p,
.feedback-thread__user-meta small {
  display: block;
  margin-top: 6px;
  color: rgba(95, 110, 142, 0.8);
}

.feedback-thread__content {
  padding: 26px 28px;
}

.feedback-thread__title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.feedback-thread__title-row h3 {
  margin: 0;
  font-size: 34px;
  line-height: 1.22;
  color: #162540;
}

.feedback-thread__title-row span {
  color: rgba(112, 127, 163, 0.76);
  white-space: nowrap;
  font-size: 14px;
}

.feedback-thread__content p {
  margin: 0;
  white-space: pre-line;
  color: #334261;
  line-height: 1.95;
  font-size: 18px;
}

.feedback-thread__helper {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.feedback-helper-chip {
  border: 1px solid rgba(213, 222, 245, 0.96);
  background: rgba(248, 250, 255, 0.95);
  color: #4d63a0;
  border-radius: 14px;
  padding: 10px 14px;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease, background 0.18s ease;
}

.feedback-helper-chip:hover {
  border-color: rgba(130, 153, 227, 0.94);
  color: #31487f;
  background: rgba(235, 241, 255, 0.96);
}

.feedback-thread__composer {
  padding: 20px;
}

.feedback-thread__composer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
}

.feedback-thread__composer-head :deep(.el-select) {
  width: 160px;
}

.feedback-thread__label {
  display: block;
  margin-bottom: 6px;
  color: rgba(92, 108, 144, 0.76);
  font-size: 13px;
}

.feedback-thread__composer strong {
  color: #1f2e4d;
  font-size: 18px;
}

.feedback-thread__composer :deep(.el-textarea__inner) {
  border-radius: 18px;
  min-height: 220px !important;
  padding: 18px 20px;
  line-height: 1.8;
}

.feedback-thread__footer {
  padding: 18px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.feedback-thread__footer-note {
  display: grid;
  gap: 6px;
  color: rgba(97, 111, 145, 0.78);
}

.feedback-thread__footer-note strong {
  color: #233457;
  font-size: 16px;
}

.feedback-thread__actions {
  display: flex;
  gap: 12px;
}

@media (max-width: 1280px) {
  .feedback-workbench {
    grid-template-columns: 340px minmax(0, 1fr);
  }

  .feedback-thread__title-row h3 {
    font-size: 28px;
  }
}

@media (max-width: 1080px) {
  .feedback-page__head {
    flex-direction: column;
    align-items: stretch;
  }

  .feedback-page__tools {
    justify-content: space-between;
  }

  .feedback-page__tools :deep(.el-input) {
    width: 100%;
  }

  .feedback-workbench {
    grid-template-columns: 1fr;
  }

  .feedback-thread__title-row,
  .feedback-thread__composer-head,
  .feedback-thread__footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

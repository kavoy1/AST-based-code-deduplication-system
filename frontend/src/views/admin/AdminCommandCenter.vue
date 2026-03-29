<template>
  <div class="workspace-page">
    <WorkspaceShellSection eyebrow="admin operations" title="系统治理工作台" description="用更轻的监控画布承接用户、反馈、公告与配置治理，不再堆叠传统深色仪表盘。">
      <template #tools>
        <WorkspaceStatPill label="系统状态" :value="stats.status || '未知'" tone="green" />
        <el-button @click="go('/admin/notices')">发布公告</el-button>
        <el-button type="primary" @click="go('/admin/feedbacks')">处理反馈</el-button>
      </template>
    </WorkspaceShellSection>

    <div class="workspace-kpi-grid">
      <div class="workspace-kpi">
        <div class="workspace-kpi__label">用户总量</div>
        <div class="workspace-kpi__value">{{ stats.userCount }}</div>
        <div class="workspace-kpi__meta">学生 {{ stats.studentCount }} · 教师 {{ stats.teacherCount }} · 管理员 {{ stats.adminCount }}</div>
      </div>
      <div class="workspace-kpi">
        <div class="workspace-kpi__label">反馈总数</div>
        <div class="workspace-kpi__value">{{ feedbackSummary.total }}</div>
        <div class="workspace-kpi__meta">待处理 {{ feedbackSummary.pending }} · 已解决 {{ feedbackSummary.resolved }}</div>
      </div>
      <div class="workspace-kpi">
        <div class="workspace-kpi__label">公告数量</div>
        <div class="workspace-kpi__value">{{ stats.noticeCount }}</div>
        <div class="workspace-kpi__meta">本周访问 {{ weeklyVisitsTotal }}</div>
      </div>
    </div>

    <div class="workspace-grid workspace-grid--three">
      <WorkspacePanel title="治理导航" subtitle="三类核心治理路径" soft>
        <div class="workspace-list">
          <button type="button" class="workspace-list-item admin-nav-item" @click="go('/admin/users')">
            <div>
              <p class="workspace-list-item__title">用户治理</p>
              <p class="workspace-list-item__meta">查看账号状态、角色分布与分页数据。</p>
            </div>
            <strong>{{ stats.userCount }}</strong>
          </button>
          <button type="button" class="workspace-list-item admin-nav-item" @click="go('/admin/feedbacks')">
            <div>
              <p class="workspace-list-item__title">反馈流转</p>
              <p class="workspace-list-item__meta">待处理工单 {{ feedbackSummary.pending }} 条。</p>
            </div>
            <strong>{{ feedbackSummary.pending }}</strong>
          </button>
          <button type="button" class="workspace-list-item admin-nav-item" @click="go('/admin/settings')">
            <div>
              <p class="workspace-list-item__title">参数策略</p>
              <p class="workspace-list-item__meta">阈值、上传限制、存储与 AI 配置。</p>
            </div>
            <strong>即时</strong>
          </button>
        </div>
      </WorkspacePanel>

      <WorkspacePanel title="系统画布" subtitle="参考图风格的主画布，用任务块表达当前治理焦点">
        <div class="admin-canvas">
          <div class="admin-canvas__axis">
            <span>08:00</span>
            <span>10:00</span>
            <span>12:00</span>
            <span>14:00</span>
            <span>16:00</span>
          </div>
          <div class="admin-canvas__lanes">
            <div class="admin-canvas__lane">
              <div class="admin-canvas__title">系统</div>
              <div class="admin-canvas__block tone-blue" style="top: 44px; height: 92px;">
                <strong>服务健康巡检</strong>
                <span>{{ stats.status || '运行中' }}</span>
                <small>API / DB / 存储</small>
              </div>
            </div>
            <div class="admin-canvas__lane">
              <div class="admin-canvas__title">用户</div>
              <div class="admin-canvas__block tone-purple" style="top: 162px; height: 86px;">
                <strong>角色分布核查</strong>
                <span>学生 {{ stats.studentCount }} · 教师 {{ stats.teacherCount }}</span>
                <small>管理员 {{ stats.adminCount }}</small>
              </div>
            </div>
            <div class="admin-canvas__lane">
              <div class="admin-canvas__title">反馈</div>
              <div class="admin-canvas__block tone-green" style="top: 118px; height: 96px;">
                <strong>待处理反馈</strong>
                <span>{{ feedbackSummary.pending }} 条等待处理</span>
                <small>已解决 {{ feedbackSummary.resolved }}</small>
              </div>
            </div>
          </div>
        </div>
      </WorkspacePanel>

      <WorkspacePanel title="最新动态" subtitle="最近 3 条系统事件" soft>
        <div class="workspace-list">
          <div class="workspace-list-item" v-for="event in events" :key="event.id">
            <div>
              <p class="workspace-list-item__title">{{ event.text }}</p>
              <p class="workspace-list-item__meta">{{ event.time }}</p>
            </div>
            <span class="workspace-badge-soft" :class="event.tone">{{ event.tag }}</span>
          </div>
        </div>
      </WorkspacePanel>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../api/request'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import WorkspaceShellSection from '../../components/workspace/WorkspaceShellSection.vue'
import WorkspaceStatPill from '../../components/workspace/WorkspaceStatPill.vue'

const router = useRouter()
const stats = ref({ userCount: 0, studentCount: 0, teacherCount: 0, adminCount: 0, noticeCount: 0, status: '运行中', weeklyVisits: [] })
const feedbackSummary = ref({ total: 0, resolved: 0, pending: 0 })

const go = (path) => router.push(path)

const fetchAdminStats = async () => {
  const data = await request.get('/admin/stats')
  stats.value = { ...stats.value, ...data }
}

const fetchFeedbackSummary = async () => {
  const page = await request.get('/feedback/list', { params: { page: 1, size: 200 } })
  const records = Array.isArray(page?.records) ? page.records : []
  const resolved = records.filter((item) => String(item.status || '').toUpperCase() === 'RESOLVED').length
  feedbackSummary.value = { total: records.length, resolved, pending: records.length - resolved }
}

const weeklyVisitsTotal = computed(() => (stats.value.weeklyVisits || []).reduce((sum, item) => sum + Number(item || 0), 0))
const events = computed(() => [
  { id: 1, text: `系统状态：${stats.value.status || '未知'}`, time: '刚刚', tag: '在线', tone: 'workspace-badge-soft--green' },
  { id: 2, text: `当前待处理反馈 ${feedbackSummary.value.pending} 条`, time: '5 分钟前', tag: '工单', tone: 'workspace-badge-soft--amber' },
  { id: 3, text: `当前公告总数 ${stats.value.noticeCount} 条`, time: '10 分钟前', tag: '公告', tone: 'workspace-badge-soft--blue' }
])

onMounted(async () => {
  await Promise.all([fetchAdminStats(), fetchFeedbackSummary()])
})
</script>

<style scoped>
.admin-nav-item {
  width: 100%;
  border: none;
  cursor: pointer;
  text-align: left;
}

.admin-canvas {
  position: relative;
  min-height: 380px;
  padding: 12px 0 0;
}

.admin-canvas__axis {
  display: flex;
  justify-content: space-between;
  padding: 0 8px 18px;
  color: var(--text-soft);
  font-size: 12px;
}

.admin-canvas__lanes {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  min-height: 300px;
}

.admin-canvas__lane {
  position: relative;
  border-left: 1px solid rgba(29, 35, 43, 0.06);
  padding-left: 18px;
}

.admin-canvas__title {
  color: var(--text-soft);
  font-size: 12px;
  margin-bottom: 12px;
}

.admin-canvas__block {
  position: absolute;
  left: 18px;
  right: 8px;
  padding: 16px;
  border-radius: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.admin-canvas__block strong {
  font-size: 16px;
}

.admin-canvas__block span,
.admin-canvas__block small {
  color: var(--text-body);
}

.tone-blue { background: rgba(167, 203, 255, 0.34); }
.tone-purple { background: rgba(217, 205, 252, 0.38); }
.tone-green { background: rgba(205, 238, 219, 0.6); }

@media (max-width: 980px) {
  .admin-canvas__lanes {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .admin-canvas__lane {
    min-height: 140px;
  }
}
</style>

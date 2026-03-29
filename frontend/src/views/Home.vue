<template>
  <div v-if="isTeacher" class="teacher-home">
    <section class="teacher-home__hero">
      <div class="teacher-home__headline">
        <p class="teacher-home__eyebrow">{{ greetingPeriod }}</p>
        <h1>{{ greetingHeadline }}</h1>
        <p class="teacher-home__summary">{{ greetingDescription }}</p>
      </div>

      <div class="teacher-home__hero-actions">
        <el-button type="primary" size="large" @click="router.push('/teacher/classes')">进入班级管理</el-button>
        <el-button size="large" plain @click="router.push('/teacher/assignments')">进入作业管理</el-button>
      </div>

      <div class="teacher-home__deadline-strip">
        <div class="teacher-home__deadline-chip">
          <span>今日截止</span>
          <strong>{{ deadlineStats.today }}</strong>
        </div>
        <div class="teacher-home__deadline-chip">
          <span>3 天内</span>
          <strong>{{ deadlineStats.threeDays }}</strong>
        </div>
        <div class="teacher-home__deadline-chip">
          <span>7 天内</span>
          <strong>{{ deadlineStats.sevenDays }}</strong>
        </div>
      </div>
    </section>

    <section class="teacher-home__stats">
      <article class="teacher-home__stat-card">
        <span>班级总数</span>
        <strong>{{ metrics.classCount }}</strong>
      </article>
      <article class="teacher-home__stat-card">
        <span>学生总数</span>
        <strong>{{ metrics.studentCount }}</strong>
      </article>
      <article class="teacher-home__stat-card">
        <span>作业总数</span>
        <strong>{{ metrics.homeworkCount }}</strong>
      </article>
      <article class="teacher-home__stat-card teacher-home__stat-card--accent">
        <span>重点班级</span>
        <strong>{{ dominantClass.name }}</strong>
        <small>{{ dominantClass.value }} 人</small>
      </article>
    </section>

    <section class="teacher-home__body">
      <article class="teacher-home__panel">
        <div class="teacher-home__panel-header">
          <div>
            <p>班级学生分布</p>
            <h2>学生数前 5 班级</h2>
          </div>
          <span>{{ topClassChartData.length }} 个班级</span>
        </div>

        <div v-if="topClassChartData.length" class="teacher-home__chart-wrap">
          <v-chart class="teacher-home__chart" :option="classBarOption" autoresize />
        </div>
        <div v-else class="teacher-home__empty">当前还没有可展示的班级学生分布。</div>
      </article>

      <article class="teacher-home__panel teacher-home__panel--deadlines">
        <div class="teacher-home__panel-header">
          <div>
            <p>最近截止</p>
            <h2>作业时间安排</h2>
          </div>
          <span>{{ recentDeadlines.length }} 项</span>
        </div>

        <div v-if="recentDeadlines.length" class="teacher-home__deadline-list">
          <div v-for="item in recentDeadlines" :key="item.id" class="teacher-home__deadline-item">
            <div class="teacher-home__deadline-copy">
              <strong>{{ item.title }}</strong>
              <p>{{ item.className }}</p>
            </div>
            <div class="teacher-home__deadline-time">
              <span>{{ deadlineDistance(item.deadline) }}</span>
              <small>{{ formatDeadline(item.deadline) }}</small>
            </div>
          </div>
        </div>
        <div v-else class="teacher-home__empty">当前没有即将截止的作业。</div>
      </article>
    </section>
  </div>

  <div v-else class="workspace-page">
    <WorkspaceShellSection :eyebrow="roleEyebrow" :title="heroTitle" :description="heroDescription">
      <template #tools>
        <WorkspaceStatPill label="当前身份" :value="roleLabel" tone="blue" />
      </template>
    </WorkspaceShellSection>

    <div class="workspace-grid workspace-grid--two">
      <WorkspacePanel title="入口概览" subtitle="按当前角色进入主要工作区">
        <div class="workspace-list">
          <button
            v-for="item in actions"
            :key="item.path"
            type="button"
            class="workspace-home-action"
            @click="router.push(item.path)"
          >
            <div>
              <p class="workspace-list-item__title">{{ item.title }}</p>
              <p class="workspace-list-item__meta">{{ item.desc }}</p>
            </div>
          </button>
        </div>
      </WorkspacePanel>

      <WorkspacePanel title="页面说明" subtitle="首页保留入口概览，不承载复杂业务操作" soft>
        <div class="workspace-list">
          <div v-for="tip in tips" :key="tip.title" class="workspace-list-item">
            <div>
              <p class="workspace-list-item__title">{{ tip.title }}</p>
              <p class="workspace-list-item__meta">{{ tip.desc }}</p>
            </div>
          </div>
        </div>
      </WorkspacePanel>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import request from '../api/request'
import WorkspacePanel from '../components/workspace/WorkspacePanel.vue'
import WorkspaceShellSection from '../components/workspace/WorkspaceShellSection.vue'
import WorkspaceStatPill from '../components/workspace/WorkspaceStatPill.vue'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent])

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')

const metrics = reactive({
  classCount: 0,
  studentCount: 0,
  homeworkCount: 0,
  chartNames: [],
  chartValues: [],
  deadlineStats: { today: 0, threeDays: 0, sevenDays: 0 },
  recentDeadlines: []
})

const roleLabelMap = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生'
}

const roleLabel = computed(() => roleLabelMap[user.role] || '访客')
const roleEyebrow = computed(() => `${roleLabel.value} workspace`)
const isTeacher = computed(() => user.role === 'TEACHER')
const teacherName = computed(() => user.nickname || user.username || '老师')

const heroTitle = computed(() => {
  if (user.role === 'ADMIN') return '系统治理工作台'
  if (user.role === 'STUDENT') return '个人学习工作台'
  return '统一工作台'
})

const heroDescription = computed(() => {
  if (user.role === 'ADMIN') return '这里承接用户治理、公告发布、反馈流转和系统配置。'
  if (user.role === 'STUDENT') return '这里承接作业、班级和通知，突出当前任务与个人学习节奏。'
  return '当前页面使用统一工作台语言承载业务内容。'
})

const actions = computed(() => {
  if (user.role === 'ADMIN') {
    return [
      { path: '/admin/command-center', title: '进入系统监控', desc: '查看系统运行情况与关键统计。' },
      { path: '/admin/users', title: '进入用户管理', desc: '筛选并分页管理系统用户。' }
    ]
  }

  return [
    { path: '/student/tasks', title: '查看我的作业', desc: '聚焦当前进行中的任务与提交。' },
    { path: '/student/classes', title: '查看我的班级', desc: '查看班级状态与加入情况。' }
  ]
})

const tips = [
  { title: '保留主工作区', desc: '首页只保留必要入口，不再承载复杂业务。' },
  { title: '减少信息密度', desc: '避免首页卡片堆叠，把重点留给具体模块。' }
]

const currentHour = computed(() => new Date().getHours())

const greetingPeriod = computed(() => {
  if (currentHour.value < 12) return '早上好'
  if (currentHour.value < 18) return '下午好'
  return '晚上好'
})

const greetingHeadline = computed(() => `${greetingPeriod.value}，${teacherName.value}`)
const greetingDescription = computed(
  () => `今天负责 ${metrics.classCount} 个班级、${metrics.studentCount} 名学生，当前共有 ${metrics.homeworkCount} 项作业需要关注。`
)

const deadlineStats = computed(() => ({
  today: Number(metrics.deadlineStats?.today || 0),
  threeDays: Number(metrics.deadlineStats?.threeDays || 0),
  sevenDays: Number(metrics.deadlineStats?.sevenDays || 0)
}))

const recentDeadlines = computed(() => (Array.isArray(metrics.recentDeadlines) ? metrics.recentDeadlines : []))

const topClassChartData = computed(() => {
  const names = Array.isArray(metrics.chartNames) ? metrics.chartNames : []
  const values = Array.isArray(metrics.chartValues) ? metrics.chartValues : []

  return names
    .map((name, index) => ({ name, value: Number(values[index] || 0) }))
    .sort((left, right) => right.value - left.value)
    .slice(0, 5)
})

const dominantClass = computed(() => topClassChartData.value[0] || { name: '暂无班级数据', value: 0 })

const classBarOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 12, right: 12, top: 12, bottom: 12, containLabel: true },
  xAxis: {
    type: 'category',
    data: topClassChartData.value.map((item) => item.name),
    axisLabel: { color: '#7b8796', interval: 0, rotate: 14, fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#7b8796', fontSize: 11 },
    splitLine: { lineStyle: { color: 'rgba(123, 135, 150, 0.12)' } }
  },
  series: [
    {
      type: 'bar',
      barWidth: 22,
      data: topClassChartData.value.map((item) => item.value),
      itemStyle: {
        borderRadius: [8, 8, 0, 0],
        color: '#4e7cff'
      }
    }
  ]
}))

function formatDeadline(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

function deadlineDistance(value) {
  if (!value) return '未设置时间'
  const target = new Date(value).getTime()
  if (Number.isNaN(target)) return '时间无效'

  const diff = target - Date.now()
  if (diff <= 0) return '已到期'

  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)
  if (days > 0) return `${days} 天后截止`
  if (hours > 0) return `${hours} 小时后截止`

  const minutes = Math.max(1, Math.floor(diff / (1000 * 60)))
  return `${minutes} 分钟后截止`
}

async function fetchTeacherOverview() {
  if (!isTeacher.value) return

  try {
    const data = await request.get('/teacher/stats')
    metrics.classCount = Number(data?.classCount || 0)
    metrics.studentCount = Number(data?.studentCount || 0)
    metrics.homeworkCount = Number(data?.homeworkCount || 0)
    metrics.chartNames = Array.isArray(data?.chartNames) ? data.chartNames : []
    metrics.chartValues = Array.isArray(data?.chartValues) ? data.chartValues : []
    metrics.deadlineStats = {
      today: Number(data?.deadlineStats?.today || 0),
      threeDays: Number(data?.deadlineStats?.threeDays || 0),
      sevenDays: Number(data?.deadlineStats?.sevenDays || 0)
    }
    metrics.recentDeadlines = Array.isArray(data?.recentDeadlines) ? data.recentDeadlines : []
  } catch {
    metrics.classCount = 0
    metrics.studentCount = 0
    metrics.homeworkCount = 0
    metrics.chartNames = []
    metrics.chartValues = []
    metrics.deadlineStats = { today: 0, threeDays: 0, sevenDays: 0 }
    metrics.recentDeadlines = []
  }
}

onMounted(fetchTeacherOverview)
</script>

<style scoped>
.workspace-home-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 16px 18px;
  border: 1px solid rgba(29, 35, 43, 0.05);
  border-radius: 20px;
  background: rgba(248, 250, 251, 0.84);
  color: var(--text-strong);
  text-align: left;
  cursor: pointer;
}

.teacher-home {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 12px;
  min-height: calc(100vh - 64px);
  max-height: calc(100vh - 64px);
  overflow: hidden;
}

.teacher-home__hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto 320px;
  align-items: end;
  gap: 16px;
  padding: 18px 22px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(91, 139, 255, 0.18), transparent 28%),
    radial-gradient(circle at bottom right, rgba(95, 211, 188, 0.16), transparent 26%),
    linear-gradient(135deg, #171b22 0%, #262d38 100%);
  color: #fff;
  box-shadow: 0 22px 46px rgba(37, 45, 56, 0.14);
}

.teacher-home__eyebrow {
  margin: 0;
  color: rgba(255, 255, 255, 0.62);
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.teacher-home__headline h1 {
  margin: 4px 0 0;
  font-size: clamp(28px, 3vw, 42px);
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.teacher-home__summary {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.78);
  line-height: 1.45;
  font-size: 14px;
}

.teacher-home__hero-actions {
  display: flex;
  gap: 10px;
  align-self: center;
}

.teacher-home__hero-actions :deep(.el-button) {
  min-width: 132px;
  height: 42px;
  border-radius: 14px;
}

.teacher-home__deadline-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.teacher-home__deadline-chip {
  display: flex;
  min-height: 88px;
  flex-direction: column;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}

.teacher-home__deadline-chip span,
.teacher-home__stat-card span,
.teacher-home__panel-header p,
.teacher-home__deadline-item p {
  color: var(--text-soft);
}

.teacher-home__deadline-chip strong {
  font-size: 28px;
  line-height: 1;
}

.teacher-home__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.teacher-home__stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(29, 35, 43, 0.05);
  box-shadow: 0 14px 28px rgba(168, 177, 188, 0.08);
}

.teacher-home__stat-card strong {
  font-size: 28px;
  line-height: 1;
}

.teacher-home__stat-card small {
  color: var(--text-body);
  font-size: 12px;
}

.teacher-home__stat-card--accent {
  background: linear-gradient(180deg, rgba(78, 124, 255, 0.1), rgba(255, 255, 255, 0.9));
}

.teacher-home__body {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) 360px;
  gap: 12px;
  min-height: 0;
  overflow: hidden;
}

.teacher-home__panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(29, 35, 43, 0.05);
  box-shadow: 0 14px 28px rgba(168, 177, 188, 0.08);
  overflow: hidden;
}

.teacher-home__panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.teacher-home__panel-header p,
.teacher-home__panel-header h2 {
  margin: 0;
}

.teacher-home__panel-header h2 {
  margin-top: 4px;
  font-size: 20px;
  color: var(--text-strong);
}

.teacher-home__panel-header span {
  min-width: fit-content;
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(78, 124, 255, 0.08);
  color: #4e7cff;
  font-size: 12px;
  font-weight: 600;
}

.teacher-home__chart-wrap {
  flex: 1;
  min-height: 0;
  margin-top: 10px;
}

.teacher-home__chart {
  width: 100%;
  height: 100%;
  min-height: 240px;
}

.teacher-home__deadline-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
  overflow: auto;
}

.teacher-home__deadline-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(248, 250, 251, 0.88);
}

.teacher-home__deadline-copy {
  min-width: 0;
}

.teacher-home__deadline-copy strong {
  display: block;
  color: var(--text-strong);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.teacher-home__deadline-copy p {
  margin: 4px 0 0;
}

.teacher-home__deadline-time {
  display: flex;
  min-width: 100px;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

.teacher-home__deadline-time span {
  color: #ff7d4d;
  font-weight: 600;
}

.teacher-home__deadline-time small {
  color: var(--text-soft);
}

.teacher-home__empty {
  display: grid;
  place-items: center;
  flex: 1;
  min-height: 180px;
  margin-top: 10px;
  border-radius: 20px;
  background: rgba(248, 250, 251, 0.72);
  color: var(--text-soft);
}

@media (max-width: 1280px) {
  .teacher-home {
    max-height: none;
    overflow: visible;
  }

  .teacher-home__hero,
  .teacher-home__body {
    grid-template-columns: 1fr;
  }

  .teacher-home__hero-actions {
    align-self: flex-start;
  }

  .teacher-home__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .teacher-home__stats,
  .teacher-home__deadline-strip {
    grid-template-columns: 1fr;
  }

  .teacher-home__hero-actions {
    width: 100%;
    flex-direction: column;
  }

  .teacher-home__deadline-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .teacher-home__deadline-time {
    align-items: flex-start;
  }
}
</style>

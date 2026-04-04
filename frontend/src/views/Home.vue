<template>
  <div v-if="isTeacher" class="teacher-home">
    <section class="teacher-home__header">
      <div>
        <p class="teacher-home__eyebrow">{{ todayLabel }}</p>
        <h1>{{ greetingText }}，{{ teacherName }}</h1>
      </div>

      <div class="teacher-home__header-actions">
        <button type="button" class="teacher-home__quick-button" @click="router.push('/teacher/classes')">
          班级管理
        </button>
        <button type="button" class="teacher-home__quick-button teacher-home__quick-button--primary" @click="router.push('/teacher/assignments')">
          作业管理
        </button>
      </div>
    </section>

    <section class="teacher-home__metrics">
      <article
        v-for="item in metricCards"
        :key="item.label"
        class="teacher-home__metric-card"
        :class="{ 'teacher-home__metric-card--accent': item.accent }"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="teacher-home__dashboard">
      <article class="teacher-home__panel teacher-home__panel--main">
        <header class="teacher-home__panel-header">
          <div>
            <p>主图表</p>
            <h2>班级活跃 / 提交情况</h2>
          </div>
          <span>{{ activityChartData.length }} 个班级</span>
        </header>

        <div v-if="activityChartData.length" class="teacher-home__chart-wrap teacher-home__chart-wrap--main">
          <v-chart class="teacher-home__chart" :option="activityChartOption" autoresize />
        </div>
        <div v-else class="teacher-home__empty">暂无班级活跃数据</div>
      </article>

      <div class="teacher-home__side">
        <article class="teacher-home__panel teacher-home__panel--compact">
          <header class="teacher-home__panel-header">
            <div>
              <p>趋势</p>
              <h2>作业截止趋势</h2>
            </div>
          </header>

          <div class="teacher-home__deadline-stats">
            <div class="teacher-home__deadline-chip">
              <span>今天</span>
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

          <div class="teacher-home__chart-wrap teacher-home__chart-wrap--trend">
            <v-chart class="teacher-home__chart" :option="deadlineTrendOption" autoresize />
          </div>
        </article>

        <article class="teacher-home__panel teacher-home__panel--compact">
          <header class="teacher-home__panel-header">
            <div>
              <p>分布</p>
              <h2>查重结果分布</h2>
            </div>
          </header>

          <div class="teacher-home__plagiarism-layout">
            <div class="teacher-home__plagiarism-chart-card">
              <div class="teacher-home__chart-wrap teacher-home__chart-wrap--donut">
                <v-chart class="teacher-home__chart" :option="plagiarismOption" autoresize />
              </div>
              <div class="teacher-home__plagiarism-total">
                <strong>{{ plagiarismTotal }}</strong>
                <span>总任务数</span>
              </div>
            </div>

            <div class="teacher-home__legend">
              <div
                v-for="item in plagiarismLegend"
                :key="item.label"
                class="teacher-home__legend-item"
              >
                <span class="teacher-home__legend-swatch" :style="{ background: item.color }"></span>
                <div>
                  <strong>{{ item.value }}</strong>
                  <small>{{ item.label }}</small>
                </div>
              </div>
            </div>
          </div>
        </article>
      </div>
    </section>
  </div>

  <div v-else class="workspace-page">
    <WorkspaceShellSection :eyebrow="roleEyebrow" :title="heroTitle" :description="heroDescription">
      <template #tools>
        <WorkspaceStatPill label="当前身份" :value="roleLabel" tone="blue" />
      </template>
    </WorkspaceShellSection>

    <div class="workspace-grid workspace-grid--two">
      <WorkspacePanel title="常用入口" subtitle="按当前角色进入主要工作区">
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

      <WorkspacePanel title="使用建议" subtitle="首页只保留轻量入口和概览信息" soft>
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
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import request from '../api/request'
import WorkspacePanel from '../components/workspace/WorkspacePanel.vue'
import WorkspaceShellSection from '../components/workspace/WorkspaceShellSection.vue'
import WorkspaceStatPill from '../components/workspace/WorkspaceStatPill.vue'

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')

const metrics = reactive({
  classCount: 0,
  studentCount: 0,
  homeworkCount: 0,
  activeHomeworkCount: 0,
  warningCount: 0,
  chartNames: [],
  chartValues: [],
  deadlineStats: { today: 0, threeDays: 0, sevenDays: 0 },
  recentDeadlines: [],
  activityChart: [],
  deadlineTrend: [],
  plagiarismDistribution: { pending: 0, confirmed: 0, falsePositive: 0 }
})

const roleLabelMap = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生'
}

const roleLabel = computed(() => roleLabelMap[user.role] || '访客')
const roleEyebrow = computed(() => `${roleLabel.value} 工作台`)
const isTeacher = computed(() => user.role === 'TEACHER')
const teacherName = computed(() => user.nickname || user.username || '老师')

const heroTitle = computed(() => {
  if (user.role === 'ADMIN') return '系统治理工作台'
  if (user.role === 'STUDENT') return '个人学习工作台'
  return '统一工作台'
})

const heroDescription = computed(() => {
  if (user.role === 'ADMIN') return '用于管理用户、公告、反馈与系统配置。'
  if (user.role === 'STUDENT') return '用于查看作业、班级状态与个人通知。'
  return '用于进入当前角色的主要工作区。'
})

const actions = computed(() => {
  if (user.role === 'ADMIN') {
    return [
      { path: '/admin/command-center', title: '系统监控', desc: '查看系统运行状态与关键统计。' },
      { path: '/admin/users', title: '用户管理', desc: '筛选并管理系统用户。' }
    ]
  }

  return [
    { path: '/student/tasks', title: '我的作业', desc: '查看当前进行中的学习任务。' },
    { path: '/student/classes', title: '我的班级', desc: '查看班级状态与加入情况。' }
  ]
})

const tips = [
  { title: '首页轻量', desc: '首页只负责概览与跳转，不堆叠复杂操作。' },
  { title: '进入模块处理', desc: '具体业务在对应模块内完成，降低首页干扰。' }
]

const today = new Date()

const greetingText = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const todayLabel = computed(() => {
  const date = new Date()
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${date.getMonth() + 1} 月 ${date.getDate()} 日 · ${weekdays[date.getDay()]}`
})

const deadlineStats = computed(() => ({
  today: Number(metrics.deadlineStats?.today || 0),
  threeDays: Number(metrics.deadlineStats?.threeDays || 0),
  sevenDays: Number(metrics.deadlineStats?.sevenDays || 0)
}))

const activityChartData = computed(() => {
  if (Array.isArray(metrics.activityChart) && metrics.activityChart.length) {
    return metrics.activityChart
      .map((item) => ({
        className: item.className || '未命名班级',
        studentCount: Number(item.studentCount || 0),
        submittedCount: Number(item.submittedCount || 0),
        pendingCount: Number(item.pendingCount || 0)
      }))
      .sort((left, right) => right.studentCount - left.studentCount)
      .slice(0, 6)
  }

  const names = Array.isArray(metrics.chartNames) ? metrics.chartNames : []
  const values = Array.isArray(metrics.chartValues) ? metrics.chartValues : []

  return names
    .map((name, index) => {
      const studentCount = Number(values[index] || 0)
      return {
        className: name,
        studentCount,
        submittedCount: 0,
        pendingCount: studentCount
      }
    })
    .sort((left, right) => right.studentCount - left.studentCount)
    .slice(0, 6)
})

const deadlineTrendData = computed(() => {
  if (Array.isArray(metrics.deadlineTrend) && metrics.deadlineTrend.length) {
    return metrics.deadlineTrend.map((item) => ({
      label: item.label || '',
      count: Number(item.count || 0)
    }))
  }

  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(today)
    date.setDate(today.getDate() + index)
    return {
      label: `${date.getMonth() + 1}/${date.getDate()}`,
      count: 0
    }
  })
})

const plagiarismLegend = computed(() => {
  const distribution = metrics.plagiarismDistribution || {}
  return [
    { label: '待确认', value: Number(distribution.pending || 0), color: '#7c87ff' },
    { label: '高风险', value: Number(distribution.confirmed || 0), color: '#ff8b7b' },
    { label: '已排除', value: Number(distribution.falsePositive || 0), color: '#72d5b4' }
  ]
})

const plagiarismTotal = computed(() => plagiarismLegend.value.reduce((sum, item) => sum + item.value, 0))

const metricCards = computed(() => [
  { label: '班级总数', value: metrics.classCount },
  { label: '学生总数', value: metrics.studentCount },
  { label: '进行中作业', value: metrics.activeHomeworkCount || metrics.homeworkCount },
  { label: '待关注数', value: metrics.warningCount, accent: true }
])

const activityChartOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: {
    bottom: 0,
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#7f8a9a', fontSize: 12 }
  },
  grid: { left: 8, right: 12, top: 24, bottom: 40, containLabel: true },
  xAxis: {
    type: 'category',
    data: activityChartData.value.map((item) => item.className),
    axisTick: { show: false },
    axisLine: { lineStyle: { color: 'rgba(165, 177, 194, 0.22)' } },
    axisLabel: { color: '#7f8a9a', fontSize: 11, interval: 0 }
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#95a0af', fontSize: 11 },
    splitLine: { lineStyle: { color: 'rgba(165, 177, 194, 0.18)' } }
  },
  series: [
    {
      name: '已提交',
      type: 'bar',
      stack: 'submission',
      barWidth: 24,
      itemStyle: { borderRadius: [10, 10, 0, 0], color: '#6f85ff' },
      data: activityChartData.value.map((item) => item.submittedCount)
    },
    {
      name: '未提交',
      type: 'bar',
      stack: 'submission',
      barWidth: 24,
      itemStyle: { borderRadius: [10, 10, 0, 0], color: '#dbe3ff' },
      data: activityChartData.value.map((item) => item.pendingCount)
    }
  ]
}))

const deadlineTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 0, right: 6, top: 16, bottom: 0, containLabel: true },
  xAxis: {
    type: 'category',
    data: deadlineTrendData.value.map((item) => item.label),
    boundaryGap: false,
    axisTick: { show: false },
    axisLine: { lineStyle: { color: 'rgba(165, 177, 194, 0.18)' } },
    axisLabel: { color: '#96a1b0', fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#96a1b0', fontSize: 11 },
    splitLine: { lineStyle: { color: 'rgba(165, 177, 194, 0.16)' } }
  },
  series: [
    {
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 3, color: '#7c87ff' },
      itemStyle: { color: '#7c87ff', borderColor: '#ffffff', borderWidth: 2 },
      areaStyle: { color: 'rgba(124, 135, 255, 0.16)' },
      data: deadlineTrendData.value.map((item) => item.count)
    }
  ]
}))

const plagiarismOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { show: false },
  series: [
    {
      type: 'pie',
      radius: ['58%', '78%'],
      center: ['50%', '50%'],
      label: { show: false },
      labelLine: { show: false },
      itemStyle: { borderColor: '#ffffff', borderWidth: 4 },
      data: plagiarismLegend.value.map((item) => ({
        name: item.label,
        value: item.value,
        itemStyle: { color: item.color }
      }))
    }
  ]
}))

async function fetchTeacherOverview() {
  if (!isTeacher.value) return

  try {
    const data = await request.get('/teacher/stats')
    metrics.classCount = Number(data?.classCount || 0)
    metrics.studentCount = Number(data?.studentCount || 0)
    metrics.homeworkCount = Number(data?.homeworkCount || 0)
    metrics.activeHomeworkCount = Number(data?.activeHomeworkCount || 0)
    metrics.warningCount = Number(data?.warningCount || 0)
    metrics.chartNames = Array.isArray(data?.chartNames) ? data.chartNames : []
    metrics.chartValues = Array.isArray(data?.chartValues) ? data.chartValues : []
    metrics.deadlineStats = {
      today: Number(data?.deadlineStats?.today || 0),
      threeDays: Number(data?.deadlineStats?.threeDays || 0),
      sevenDays: Number(data?.deadlineStats?.sevenDays || 0)
    }
    metrics.recentDeadlines = Array.isArray(data?.recentDeadlines) ? data.recentDeadlines : []
    metrics.activityChart = Array.isArray(data?.activityChart) ? data.activityChart : []
    metrics.deadlineTrend = Array.isArray(data?.deadlineTrend) ? data.deadlineTrend : []
    metrics.plagiarismDistribution = data?.plagiarismDistribution || { pending: 0, confirmed: 0, falsePositive: 0 }
  } catch {
    metrics.classCount = 0
    metrics.studentCount = 0
    metrics.homeworkCount = 0
    metrics.activeHomeworkCount = 0
    metrics.warningCount = 0
    metrics.chartNames = []
    metrics.chartValues = []
    metrics.deadlineStats = { today: 0, threeDays: 0, sevenDays: 0 }
    metrics.recentDeadlines = []
    metrics.activityChart = []
    metrics.deadlineTrend = []
    metrics.plagiarismDistribution = { pending: 0, confirmed: 0, falsePositive: 0 }
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
  gap: 16px;
  height: calc(100vh - 72px);
  min-height: calc(100vh - 72px);
  max-height: calc(100vh - 72px);
  padding: 2px 0 0;
  overflow: hidden;
}

.teacher-home__header,
.teacher-home__metric-card,
.teacher-home__panel {
  border: 1px solid rgba(34, 44, 60, 0.05);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(250, 252, 255, 0.88));
  box-shadow: 0 20px 40px rgba(191, 201, 214, 0.14);
}

.teacher-home__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 24px;
  border-radius: 28px;
}

.teacher-home__eyebrow {
  margin: 0 0 6px;
  color: #8d97a6;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.teacher-home__header h1 {
  margin: 0;
  color: #1f2c3b;
  font-size: clamp(24px, 2.6vw, 34px);
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.teacher-home__header-actions {
  display: flex;
  gap: 12px;
}

.teacher-home__quick-button {
  min-width: 118px;
  height: 40px;
  padding: 0 18px;
  border: 1px solid rgba(111, 133, 255, 0.14);
  border-radius: 999px;
  background: rgba(245, 248, 255, 0.92);
  color: #5d6b7f;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.teacher-home__quick-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(130, 146, 255, 0.16);
  border-color: rgba(111, 133, 255, 0.28);
}

.teacher-home__quick-button--primary {
  background: linear-gradient(135deg, #6f85ff, #8ea0ff);
  color: #ffffff;
  border-color: transparent;
}

.teacher-home__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.teacher-home__metric-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 15px 18px;
  border-radius: 22px;
}

.teacher-home__metric-card span {
  color: #8a95a5;
  font-size: 13px;
}

.teacher-home__metric-card strong {
  color: #1f2c3b;
  font-size: clamp(24px, 2.2vw, 30px);
  line-height: 1;
}

.teacher-home__metric-card--accent {
  background: linear-gradient(135deg, rgba(111, 133, 255, 0.16), rgba(255, 255, 255, 0.94));
}

.teacher-home__dashboard {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(360px, 0.92fr);
  gap: 16px;
  min-height: 0;
  overflow: hidden;
}

.teacher-home__side {
  display: grid;
  grid-template-rows: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  min-height: 0;
}

.teacher-home__panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  padding: 18px 20px;
  border-radius: 26px;
  overflow: hidden;
}

.teacher-home__panel--main {
  min-height: 0;
  height: 100%;
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

.teacher-home__panel-header p {
  color: #97a1af;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.teacher-home__panel-header h2 {
  margin-top: 6px;
  color: #223042;
  font-size: 18px;
  line-height: 1.1;
}

.teacher-home__panel-header span {
  min-width: fit-content;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(111, 133, 255, 0.08);
  color: #6f85ff;
  font-size: 12px;
  font-weight: 600;
}

.teacher-home__chart-wrap {
  flex: 1;
  min-height: 0;
}

.teacher-home__chart-wrap--main {
  margin-top: 12px;
}

.teacher-home__chart-wrap--trend {
  height: 150px;
  margin-top: 10px;
}

.teacher-home__chart-wrap--donut {
  width: 168px;
  height: 168px;
}

.teacher-home__chart {
  width: 100%;
  height: 100%;
}

.teacher-home__deadline-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.teacher-home__deadline-chip {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(243, 246, 255, 0.95);
  border: 1px solid rgba(111, 133, 255, 0.08);
}

.teacher-home__deadline-chip span {
  color: #8c97a7;
  font-size: 12px;
}

.teacher-home__deadline-chip strong {
  color: #243245;
  font-size: 20px;
  line-height: 1;
}

.teacher-home__plagiarism-layout {
  display: grid;
  grid-template-columns: 1fr;
  align-items: start;
  gap: 14px;
  flex: 1;
  min-height: 0;
}

.teacher-home__plagiarism-chart-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 12px 16px;
  border-radius: 22px;
  background: rgba(246, 248, 252, 0.88);
}

.teacher-home__plagiarism-total {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.teacher-home__plagiarism-total strong {
  color: #223042;
  font-size: 34px;
  line-height: 1;
}

.teacher-home__plagiarism-total span {
  color: #96a1b0;
  font-size: 12px;
}

.teacher-home__legend {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.teacher-home__legend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(246, 248, 252, 0.9);
}

.teacher-home__legend-swatch {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  flex-shrink: 0;
}

.teacher-home__legend-item strong {
  display: block;
  color: #223042;
  font-size: 18px;
  line-height: 1;
}

.teacher-home__legend-item small {
  display: block;
  margin-top: 4px;
  color: #96a1b0;
  font-size: 12px;
}

.teacher-home__empty {
  display: grid;
  place-items: center;
  flex: 1;
  min-height: 120px;
  margin-top: 12px;
  border-radius: 22px;
  background: rgba(247, 249, 252, 0.92);
  color: #9aa4b1;
}

@media (max-width: 1360px) {
  .teacher-home__dashboard {
    grid-template-columns: 1fr;
  }

  .teacher-home__side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-rows: none;
  }

  .teacher-home {
    height: auto;
    min-height: calc(100vh - 72px);
    max-height: none;
    overflow: visible;
  }
}

@media (max-width: 980px) {
  .teacher-home__header,
  .teacher-home__metrics,
  .teacher-home__side,
  .teacher-home__plagiarism-layout,
  .teacher-home__deadline-stats,
  .teacher-home__legend {
    grid-template-columns: 1fr;
  }

  .teacher-home__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .teacher-home__header-actions {
    width: 100%;
  }

  .teacher-home__quick-button {
    flex: 1;
  }

  .teacher-home__panel--main {
    min-height: 360px;
  }

  .teacher-home__plagiarism-chart-card {
    flex-direction: column;
    align-items: center;
  }
}

@media (max-width: 640px) {
  .teacher-home {
    min-height: auto;
    height: auto;
  }

  .teacher-home__header-actions {
    flex-direction: column;
  }
}
</style>

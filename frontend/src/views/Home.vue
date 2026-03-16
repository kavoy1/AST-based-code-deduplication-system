<template>
    <div class="home-container">
        <!-- 欢迎语区域 -->
        <div class="welcome-section" v-if="user.role">
            <h2 class="welcome-title">欢迎回来，{{ user.nickname || user.username }}</h2>
            <p class="welcome-subtitle">今天是 {{ new Date().toLocaleDateString() }}，祝你度过充实的一天。</p>
        </div>

        <!-- 学生端界面 -->
        <template v-if="user.role === 'STUDENT'">
            <!-- 顶部统计卡片 -->
            <el-row :gutter="20" class="stat-row">
                <el-col :span="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon task-icon">
                                <el-icon><List /></el-icon>
                            </div>
                            <div class="stat-info">
                                <el-statistic :value="studentStats.pendingTasks" title="待办作业" />
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon finished-icon">
                                <el-icon><Check /></el-icon>
                            </div>
                            <div class="stat-info">
                                <el-statistic :value="studentStats.submittedTasks" title="已交作业" />
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon similarity-icon">
                                <el-icon><PieChart /></el-icon>
                            </div>
                            <div class="stat-info">
                                <el-statistic :value="studentStats.avgSimilarity" title="平均相似度" suffix="%" :precision="1" />
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon class-icon">
                                <el-icon><School /></el-icon>
                            </div>
                            <div class="stat-info">
                                <el-statistic :value="studentStats.classCount" title="加入班级" />
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <el-row :gutter="20" class="main-content-row">
                <!-- 中间主体区 -->
                <el-col :span="16">
                    <!-- 查重预警卡片 -->
                    <el-card class="warning-card-student" shadow="hover" v-if="studentStats.lastTaskSimilarity > 50">
                        <div class="warning-content">
                            <el-icon class="warning-icon-large"><WarningFilled /></el-icon>
                            <div class="warning-text">
                                <h3>查重预警</h3>
                                <p>您最近提交的《{{ studentStats.lastTaskName }}》相似度高达 {{ studentStats.lastTaskSimilarity }}%，请注意学术诚信！</p>
                            </div>
                        </div>
                    </el-card>
                    
                    <!-- 待办作业列表 -->
                    <el-card class="todo-list-card" shadow="hover">
                        <template #header>
                            <div class="card-header">
                                <div class="card-title">
                                    <el-icon><Calendar /></el-icon> 待办作业
                                </div>
                                <el-tag type="danger" effect="plain" round v-if="todoTasks.length > 0">
                                    {{ todoTasks.length }} 项待处理
                                </el-tag>
                            </div>
                        </template>
                        <div class="task-flow">
                            <el-card 
                                v-for="task in todoTasks" 
                                :key="task.id" 
                                class="task-item-card" 
                                shadow="hover" 
                                @click="goToUpload(task.id)"
                            >
                                <div class="task-content">
                                    <div class="task-main">
                                        <h4 class="task-title">{{ task.name }}</h4>
                                        <div class="task-meta">
                                            <el-tag size="small" type="info">{{ task.className }}</el-tag>
                                            <span class="deadline-text">截止: {{ task.deadline }}</span>
                                        </div>
                                    </div>
                                    <div class="task-timer">
                                        <div class="timer-label">剩余时间</div>
                                        <el-countdown 
                                            :value="task.deadlineValue" 
                                            format="DD 天 HH:mm:ss"
                                            value-style="font-size: 14px; color: #f56c6c; font-weight: bold"
                                        />
                                    </div>
                                    <div class="task-action">
                                        <el-button type="primary" circle icon="Upload" />
                                    </div>
                                </div>
                            </el-card>
                            <el-empty v-if="todoTasks.length === 0" description="暂无待办作业，太棒了！" />
                        </div>
                    </el-card>
                </el-col>

                <!-- 右侧侧边栏：最近公告 -->
                <el-col :span="8">
                    <el-card class="notice-card" shadow="hover">
                        <template #header>
                            <div class="card-header">
                                <span><el-icon><Bell /></el-icon> 最近公告</span>
                                <el-button link type="primary">更多</el-button>
                            </div>
                        </template>
                        <div class="notice-list">
                            <div v-for="(item, index) in notices" :key="index" class="notice-item">
                                <div class="notice-title">{{ item.title }}</div>
                                <div class="notice-time">{{ item.time }}</div>
                            </div>
                            <el-empty v-if="notices.length === 0" description="暂无公告" />
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </template>

        <!-- 教师端界面 -->
        <template v-else-if="user.role === 'TEACHER'">
            <!-- 顶部统计卡片 -->
            <el-row :gutter="20" class="stat-row">
                <el-col :span="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon class-icon">
                                <el-icon><School /></el-icon>
                            </div>
                            <div class="stat-info">
                                <div class="stat-value">{{ teacherStats.classCount }}</div>
                                <div class="stat-label">管理班级</div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon student-icon">
                                <el-icon><User /></el-icon>
                            </div>
                            <div class="stat-info">
                                <div class="stat-value">{{ teacherStats.studentCount }}</div>
                                <div class="stat-label">学生总数</div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon task-icon">
                                <el-icon><Document /></el-icon>
                            </div>
                            <div class="stat-info">
                                <div class="stat-value">24</div>
                                <div class="stat-label">发布作业</div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card warning-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon warning-icon">
                                <el-icon><Warning /></el-icon>
                            </div>
                            <div class="stat-info">
                                <div class="stat-value">5</div>
                                <div class="stat-label">高危预警</div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <!-- 中间内容区 -->
            <el-row :gutter="20" class="chart-row">
                <!-- 左侧：最近提交动态 -->
                <el-col :span="8">
                    <el-card class="chart-card" header="最新提交动态">
                        <el-timeline>
                            <el-timeline-item
                                v-for="(activity, index) in activities"
                                :key="index"
                                :type="activity.type"
                                :color="activity.color"
                                :timestamp="activity.timestamp"
                            >
                                {{ activity.content }}
                            </el-timeline-item>
                        </el-timeline>
                    </el-card>
                </el-col>

                <!-- 中间：高危预警名单 (查重特色) -->
                <el-col :span="8">
                    <el-card class="chart-card warning-list-card">
                        <template #header>
                            <div class="card-header-warning">
                                <span>🚨 抄袭预警名单 (Top 5)</span>
                                <el-tag type="danger" size="small">实时</el-tag>
                            </div>
                        </template>
                        <el-table :data="warningList" style="width: 100%" :show-header="false" size="small">
                            <el-table-column width="60">
                                <template #default="scope">
                                    <div class="rank-badge" :class="'rank-' + (scope.$index + 1)">{{ scope.$index + 1 }}</div>
                                </template>
                            </el-table-column>
                            <el-table-column prop="student" label="学生" />
                            <el-table-column prop="task" label="作业" show-overflow-tooltip />
                            <el-table-column prop="similarity" label="相似度" align="right">
                                <template #default="scope">
                                    <span class="similarity-text">{{ scope.row.similarity }}%</span>
                                </template>
                            </el-table-column>
                        </el-table>
                    </el-card>
                </el-col>

                <!-- 右侧：班级人数分布 -->
                <el-col :span="8">
                    <el-card class="chart-card" header="班级人数概览">
                        <v-chart class="chart" :option="chartOption" />
                    </el-card>
                </el-col>
            </el-row>
        </template>

        <!-- 管理员端界面 (新增) -->
        <template v-else>
            <!-- 管理员统计卡片 -->
            <el-row :gutter="20" class="stat-row">
                <el-col :span="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon admin-user-icon">
                                <el-icon><UserFilled /></el-icon>
                            </div>
                            <div class="stat-info">
                                <div class="stat-value">{{ stats.userCount }}</div>
                                <div class="stat-label">总用户数</div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon admin-notice-icon">
                                <el-icon><BellFilled /></el-icon>
                            </div>
                            <div class="stat-info">
                                <div class="stat-value">{{ stats.noticeCount }}</div>
                                <div class="stat-label">系统通知</div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon admin-status-icon">
                                <el-icon><Monitor /></el-icon>
                            </div>
                            <div class="stat-info">
                                <div class="stat-value" style="color: #67c23a">{{ stats.status }}</div>
                                <div class="stat-label">系统状态</div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
                <el-col :span="6">
                    <el-card class="stat-card" shadow="hover">
                        <div class="stat-content">
                            <div class="stat-icon admin-log-icon">
                                <el-icon><DataLine /></el-icon>
                            </div>
                            <div class="stat-info">
                                <div class="stat-value">{{ stats.resource }}</div>
                                <div class="stat-label">资源占用</div>
                            </div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>

            <el-row :gutter="20" class="chart-row">
                <!-- 用户分布饼图 -->
                <el-col :span="12">
                    <el-card class="chart-card" header="用户角色分布">
                        <v-chart class="chart" :option="adminPieOption" />
                    </el-card>
                </el-col>
                
                <!-- 系统访问趋势 -->
                <el-col :span="12">
                    <el-card class="chart-card" header="本周系统访问量">
                        <v-chart class="chart" :option="adminLineOption" />
                    </el-card>
                </el-col>
            </el-row>
        </template>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../api/request'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart as EBarChart, PieChart as EPieChart, LineChart as ELineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { ElMessageBox } from 'element-plus'

use([CanvasRenderer, EBarChart, EPieChart, ELineChart, GridComponent, TooltipComponent, LegendComponent])

const router = useRouter()
let user = {}
try {
    const userStr = localStorage.getItem('user')
    user = userStr ? JSON.parse(userStr) : {}
    if (!user) user = {}
} catch (e) {
    console.error('解析用户信息失败', e)
    user = {}
}

const notice = JSON.parse(localStorage.getItem('latestNotice') || 'null')

const stats = ref({
    userCount: 0,
    noticeCount: 0,
    status: '运行正常',
    resource: '32% / 16GB'
})

const teacherStats = ref({
    classCount: 0,
    studentCount: 0,
    homeworkCount: 0,
    warningCount: 0
})

// 学生端数据
const studentStats = ref({
    pendingTasks: 3,
    submittedTasks: 12,
    avgSimilarity: 24.5,
    classCount: 5,
    lastTaskName: 'Java Web期末大作业',
    lastTaskSimilarity: 58.2 // > 50 to show warning
})

const todoTasks = ref([
    { 
        id: 1, 
        name: '算法设计与分析 - 动态规划作业', 
        className: '算法设计2201班', 
        deadline: '2026-03-01 23:59',
        deadlineValue: new Date('2026-03-01 23:59').getTime()
    },
    { 
        id: 2, 
        name: '操作系统 - 进程调度实验', 
        className: '操作系统2202班', 
        deadline: '2026-02-28 12:00',
        deadlineValue: new Date('2026-02-28 12:00').getTime()
    },
    { 
        id: 3, 
        name: '计算机网络 - Socket编程', 
        className: '网络工程2201班', 
        deadline: '2026-03-05 18:00',
        deadlineValue: new Date('2026-03-05 18:00').getTime()
    }
])

const notices = ref([
    { title: '关于严禁代码抄袭的通知', time: '2023-06-01 10:00' },
    { title: '期末作业提交截止时间变更', time: '2023-05-28 14:30' },
    { title: '系统维护公告', time: '2023-05-20 09:00' }
])

const goToSubmit = () => {
    ElMessageBox.alert('功能开发中，请稍后...', '提示')
}

const goToUpload = (taskId) => {
    // 实际应跳转到 /student/task/upload/:id
    ElMessageBox.confirm(`准备上传作业 ID: ${taskId}，是否继续？`, '提示', {
        confirmButtonText: '前往上传',
        cancelButtonText: '取消',
        type: 'info'
    }).then(() => {
        // router.push(`/student/task/${taskId}/upload`)
        ElMessage.success('跳转到上传页面')
    })
}

const fetchAdminStats = async () => {
    try {
        const res = await request.get('/admin/stats')
        // 合并数据
        stats.value = { ...stats.value, ...res }
        
        // 更新饼图数据
        adminPieOption.value.series[0].data = [
            { value: res.studentCount || 0, name: '学生', itemStyle: { color: '#667eea' } },
            { value: res.teacherCount || 0, name: '教师', itemStyle: { color: '#764ba2' } },
            { value: res.adminCount || 0, name: '管理员', itemStyle: { color: '#f56c6c' } }
        ]
        
        // 更新折线图数据
        if (res.weeklyVisits && Array.isArray(res.weeklyVisits)) {
            adminLineOption.value.series[0].data = res.weeklyVisits
        }
    } catch (e) {
        console.error('获取统计数据失败', e)
    }
}

const fetchTeacherStats = async () => {
    try {
        const res = await request.get('/teacher/stats')
        teacherStats.value = res
        
        // 更新班级人数概览图表
        if (res.chartNames && res.chartValues) {
            chartOption.value.xAxis.data = res.chartNames
            chartOption.value.series[0].data = res.chartValues
        }
    } catch (e) {
        console.error('获取教师统计数据失败', e)
    }
}

// 教师端数据 (保持不变)
const activities = [
    { content: '张三 提交了《Java基础作业》', timestamp: '10分钟前', type: 'primary' },
    { content: '李四 提交了《数据结构实验一》', timestamp: '30分钟前', color: '#0bbd87' },
    { content: '王五 提交了《算法分析》', timestamp: '1小时前', type: 'info' },
    { content: '系统检测到2份高度相似作业', timestamp: '2小时前', color: '#f56c6c' },
]

const warningList = [
    { student: '赵六', task: 'C语言期末大作业', similarity: 98 },
    { student: '钱七', task: 'Python爬虫实验', similarity: 92 },
    { student: '孙八', task: 'Java Web项目', similarity: 88 },
    { student: '周九', task: '数据结构链表', similarity: 85 },
    { student: '吴十', task: '操作系统实验', similarity: 82 },
]

const chartOption = ref({
    tooltip: { trigger: 'axis' },
    grid: { top: '10%', bottom: '10%', left: '15%', right: '5%' },
    xAxis: { type: 'category', data: ['软工1班', '软工2班', '计科1班', '计科2班'] },
    yAxis: { type: 'value' },
    series: [
        {
            data: [45, 42, 48, 46],
            type: 'bar',
            itemStyle: { color: '#667eea', borderRadius: [5, 5, 0, 0] },
            barWidth: '40%'
        }
    ]
})

// 管理员端图表配置
const adminPieOption = ref({
    tooltip: { trigger: 'item' },
    legend: { bottom: '5%', left: 'center' },
    series: [
        {
            name: '用户分布',
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: {
                borderRadius: 10,
                borderColor: '#fff',
                borderWidth: 2
            },
            label: { show: false, position: 'center' },
            emphasis: {
                label: { show: true, fontSize: '20', fontWeight: 'bold' }
            },
            labelLine: { show: false },
            data: [
                { value: 1048, name: '学生', itemStyle: { color: '#667eea' } },
                { value: 150, name: '教师', itemStyle: { color: '#764ba2' } },
                { value: 50, name: '管理员', itemStyle: { color: '#f56c6c' } }
            ]
        }
    ]
})

const adminLineOption = ref({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
    yAxis: { type: 'value' },
    series: [
        {
            data: [150, 230, 224, 218, 135, 147, 260],
            type: 'line',
            smooth: true,
            itemStyle: { color: '#667eea' },
            areaStyle: {
                color: {
                    type: 'linear',
                    x: 0, y: 0, x2: 0, y2: 1,
                    colorStops: [
                        { offset: 0, color: 'rgba(102, 126, 234, 0.5)' },
                        { offset: 1, color: 'rgba(102, 126, 234, 0.0)' }
                    ]
                }
            }
        }
    ]
})

onMounted(() => {
    // 检查是否需要显示通知（仅在登录后显示一次，且非管理员）
    const shouldShow = sessionStorage.getItem('showNotice') === 'true'
    
    // 从 localStorage 获取已读的通知 ID
    const readNoticeIds = JSON.parse(localStorage.getItem('readNoticeIds') || '[]')
    
    if (shouldShow && notice && user.role !== 'ADMIN') {
        // 如果该通知未读过，才显示
        // 假设 notice 对象有 id 字段，如果没有可以用 title + time 作为唯一标识
        const noticeId = notice.id || (notice.title + notice.time)
        
        if (!readNoticeIds.includes(noticeId)) {
            // 只有当接收者ID为null（全局）或者等于当前用户ID时，才弹窗
            // 注意：notice对象是从localStorage 'latestNotice' 读取的，这个逻辑其实应该在Login的时候处理
            // LoginController.java 中返回 latestNotice 时应该只返回该用户可见的
            // 如果后端 LoginController 返回的是全局最新通知，这里无法判断 receiverId
            // 建议：仅展示全局通知（receiverId == null）作为弹窗，私有通知走通知中心
            
            // 简单处理：假设登录接口返回的 notice 是针对所有人的全局公告
            // 如果包含 receiverId 且不匹配，就不弹（前端做个防御性判断，虽然 login 接口可能已经过滤了）
            if (!notice.receiverId || notice.receiverId == user.id) {
                ElMessageBox.alert(notice.content, notice.title || '最新通知', {
                    confirmButtonText: '我知道了',
                    callback: () => {
                        sessionStorage.removeItem('showNotice')
                        // 标记为已读
                        readNoticeIds.push(noticeId)
                        localStorage.setItem('readNoticeIds', JSON.stringify(readNoticeIds))
                    }
                })
            }
        } else {
            // 如果已读，直接移除本次会话的显示标记
            sessionStorage.removeItem('showNotice')
        }
    }
    
    // 如果是管理员，获取统计数据
    if (user.role === 'ADMIN') {
        fetchAdminStats()
    } else if (user.role === 'TEACHER') {
        fetchTeacherStats()
    }
})
</script>

<style scoped>
.home-container {
    padding-bottom: 0;
    display: flex;
    flex-direction: column;
    height: calc(100vh - 130px); /* 100vh - header(70px) - padding(60px) */
}

/* 统计卡片 */
.stat-row {
    margin-bottom: 30px;
    flex-shrink: 0;
}
.stat-card {
    border: none;
    border-radius: 12px;
    transition: all 0.4s ease;
    box-shadow: 0 4px 12px rgba(0,0,0,0.03);
    background-color: var(--card-bg);
}
.stat-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 12px 24px rgba(0,0,0,0.08);
}
.stat-content {
    display: flex;
    align-items: center;
    padding: 10px;
}
.stat-icon {
    width: 64px;
    height: 64px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32px;
    margin-right: 20px;
    color: white;
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
/* 教师端图标色 - 调整为更高级的渐变 */
.class-icon { background: linear-gradient(135deg, #5c6bc0 0%, #3949ab 100%); }
.student-icon { background: linear-gradient(135deg, #26a69a 0%, #00897b 100%); }
.task-icon { background: linear-gradient(135deg, #ab47bc 0%, #8e24aa 100%); }
.warning-icon { background: linear-gradient(135deg, #ef5350 0%, #c62828 100%); color: #fff; }
.finished-icon { background: linear-gradient(135deg, #42a5f5 0%, #1e88e5 100%); }
.similarity-icon { background: linear-gradient(135deg, #ec407a 0%, #d81b60 100%); }

.warning-card-student {
    margin-bottom: 30px;
    border: none;
    border-radius: 12px;
    background-color: rgba(239, 83, 80, 0.1); /* Translucent red for dark mode compatibility */
    box-shadow: 0 4px 12px rgba(239, 83, 80, 0.1);
}
.warning-content {
    display: flex;
    align-items: center;
    color: #ef5350;
    padding: 10px;
}
.warning-icon-large {
    font-size: 48px;
    margin-right: 24px;
    color: #ef5350;
}
.warning-text h3 {
    margin: 0 0 8px 0;
    font-size: 20px;
    font-weight: 600;
}
.warning-text p {
    margin: 0;
    font-size: 15px;
    opacity: 0.9;
}
.submit-btn {
    width: 100%;
    height: 100px;
    font-size: 24px;
    border-radius: 12px;
    box-shadow: 0 8px 20px rgba(63, 81, 181, 0.2);
    transition: all 0.3s;
}
.submit-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 24px rgba(63, 81, 181, 0.3);
}

.notice-card, .quick-access-card, .todo-list-card, .chart-card {
    height: 100%;
    border: none;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.03);
    transition: box-shadow 0.3s;
    background-color: var(--card-bg);
}
.notice-card:hover, .quick-access-card:hover, .todo-list-card:hover, .chart-card:hover {
    box-shadow: 0 8px 24px rgba(0,0,0,0.06);
}

.notice-item {
    padding: 16px 0;
    border-bottom: 1px solid var(--border-color);
    display: flex;
    justify-content: space-between;
    transition: background-color 0.2s;
}
.notice-item:hover {
    background-color: rgba(255, 255, 255, 0.05);
    padding-left: 8px;
    padding-right: 8px;
    border-radius: 6px;
}
.notice-item:last-child {
    border-bottom: none;
}
.notice-title {
    font-size: 15px;
    color: var(--text-main);
    flex: 1;
    margin-right: 16px;
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
.notice-time {
    font-size: 13px;
    color: var(--text-muted);
}
.main-content-row {
    flex: 1;
    min-height: 0;
}
.main-content-row :deep(.el-col) {
    display: flex;
    flex-direction: column;
}

.task-flow {
    display: flex;
    flex-direction: column;
    gap: 20px;
    height: 100%;
    overflow-y: auto;
    padding: 4px; /* 防止阴影被裁切 */
}
.task-item-card {
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
    border: none;
    border-left: 4px solid #5c6bc0;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
    background-color: var(--card-bg);
}
.task-item-card:hover {
    transform: translateX(8px);
    box-shadow: 0 8px 20px rgba(92, 107, 192, 0.15);
}
.task-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
}
.task-title {
    margin: 0 0 10px 0;
    font-size: 18px;
    font-weight: 600;
    color: var(--text-main);
}
.task-meta {
    display: flex;
    align-items: center;
    gap: 12px;
}
.deadline-text {
    font-size: 13px;
    color: var(--text-muted);
    background-color: rgba(255, 255, 255, 0.05);
    padding: 2px 8px;
    border-radius: 4px;
}
.task-timer {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    margin: 0 24px;
    min-width: 140px;
}
.timer-label {
    font-size: 13px;
    color: var(--text-muted);
    margin-bottom: 6px;
    font-weight: 500;
}
.card-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-weight: 600;
    font-size: 16px;
    color: var(--text-main);
}
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
}
.todo-list-card {
    height: 100%;
    min-height: 400px;
}
.text-ellipsis {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

/* 管理员端图标色 */
.admin-user-icon { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.admin-notice-icon { background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 99%, #fecfef 100%); }
.admin-status-icon { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
.admin-log-icon { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }

.stat-value { font-size: 24px; font-weight: bold; color: var(--text-main); }
.stat-label { font-size: 14px; color: var(--text-muted); }

/* 图表区 */
.chart-row {
    margin-bottom: 20px;
    flex: 1; /* 让图表行占据剩余空间 */
    min-height: 0;
}
/* 让 row 内部的 col 也能撑满高度 */
.chart-row :deep(.el-col) {
    height: 100%;
}
.chart-card {
    height: 100%; /* 铺满 col */
    display: flex;
    flex-direction: column;
}
.chart {
    flex: 1; /* 图表自动填充卡片内容区 */
    width: 100%;
}

/* 预警列表 */
.card-header-warning {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #f56c6c;
    font-weight: bold;
}
.rank-badge {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);
    color: var(--text-muted);
    text-align: center;
    line-height: 20px;
    font-size: 12px;
    font-weight: bold;
}
.rank-1 { background: #f56c6c; color: white; }
.rank-2 { background: #e6a23c; color: white; }
.rank-3 { background: #409eff; color: white; }
.similarity-text {
    color: #f56c6c;
    font-weight: bold;
}

/* 快捷入口 */
.quick-actions {
    display: flex;
    gap: 20px;
}

/* 欢迎语区域 */
.welcome-section {
    margin-bottom: 30px;
    padding-left: 10px;
}
.welcome-title {
    font-size: 24px;
    font-weight: 700;
    color: var(--text-main);
    margin: 0 0 8px 0;
}
.welcome-subtitle {
    font-size: 14px;
    color: var(--text-muted);
    margin: 0;
}
</style>
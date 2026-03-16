<template>
    <div class="task-list-container">
        <el-card class="box-card">
            <template #header>
                <div class="card-header">
                    <span class="title">我的题目集</span>
                    <el-input
                        v-model="searchQuery"
                        placeholder="搜索题目集..."
                        class="search-input"
                        prefix-icon="Search"
                        clearable
                    />
                </div>
            </template>
            
            <el-tabs v-model="activeTab" class="task-tabs" @tab-click="handleTabClick">
                <el-tab-pane label="活跃题目集" name="active">
                    <div v-if="activeTasks.length > 0" class="task-grid">
                        <el-card 
                            v-for="task in activeTasks" 
                            :key="task.id" 
                            class="task-card active-card" 
                            shadow="hover"
                            @click="viewTask(task.id)"
                        >
                            <div class="task-status-badge">进行中</div>
                            <div class="task-info">
                                <h3 class="task-name">{{ task.name }}</h3>
                                <p class="task-class"><el-icon><School /></el-icon> {{ task.className }}</p>
                                <p class="task-teacher"><el-icon><User /></el-icon> {{ task.teacherName }}</p>
                                <div class="task-deadline">
                                    <el-icon><Timer /></el-icon>
                                    <span>截止: {{ task.deadline }}</span>
                                </div>
                                <div class="task-countdown">
                                    <span class="label">剩余时间:</span>
                                    <el-countdown 
                                        :value="task.deadlineValue" 
                                        format="DD 天 HH:mm:ss"
                                        value-style="font-size: 14px; color: #f56c6c; font-weight: bold"
                                    />
                                </div>
                            </div>
                            <div class="card-footer">
                                <el-button type="primary" round size="small" @click.stop="viewTask(task.id)">去完成</el-button>
                            </div>
                        </el-card>
                    </div>
                    <el-empty v-else description="暂无活跃题目集" />
                </el-tab-pane>

                <el-tab-pane label="所有题目集" name="all">
                    <div v-if="filteredAllTasks.length > 0" class="task-list-view">
                        <el-table :data="filteredAllTasks" style="width: 100%" stripe>
                            <el-table-column prop="name" label="题目集名称" min-width="200" />
                            <el-table-column prop="className" label="所属班级" width="160" />
                            <el-table-column prop="teacherName" label="发布教师" width="120" />
                            <el-table-column prop="status" label="状态" width="100">
                                <template #default="scope">
                                    <el-tag :type="getStatusType(scope.row.status)">
                                        {{ getStatusText(scope.row.status) }}
                                    </el-tag>
                                </template>
                            </el-table-column>
                            <el-table-column prop="deadline" label="截止时间" width="180" />
                            <el-table-column prop="score" label="成绩" width="100" align="center">
                                <template #default="scope">
                                    <span v-if="scope.row.score !== null" :class="getScoreClass(scope.row.score)">
                                        {{ scope.row.score }}
                                    </span>
                                    <span v-else>-</span>
                                </template>
                            </el-table-column>
                            <el-table-column label="操作" width="120" align="center">
                                <template #default="scope">
                                    <el-button 
                                        :type="scope.row.status === 'active' ? 'primary' : 'default'" 
                                        size="small" 
                                        @click="viewTask(scope.row.id)"
                                    >
                                        {{ scope.row.status === 'active' ? '去提交' : '查看' }}
                                    </el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                    <el-empty v-else description="暂无题目集" />
                </el-tab-pane>
            </el-tabs>
        </el-card>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const activeTab = ref('active')
const searchQuery = ref('')

// 模拟数据 - 实际应从后端获取
const allTasks = ref([
    {
        id: 1,
        name: '算法设计与分析 - 动态规划作业',
        className: '算法设计2201班',
        teacherName: '张老师',
        deadline: '2026-03-01 23:59',
        deadlineValue: new Date('2026-03-01 23:59').getTime(),
        status: 'active', // active, expired, submitted
        score: null
    },
    {
        id: 2,
        name: '操作系统 - 进程调度实验',
        className: '操作系统2202班',
        teacherName: '李教授',
        deadline: '2026-02-28 12:00',
        deadlineValue: new Date('2026-02-28 12:00').getTime(),
        status: 'active',
        score: null
    },
    {
        id: 3,
        name: '计算机网络 - Socket编程',
        className: '网络工程2201班',
        teacherName: '王讲师',
        deadline: '2026-03-05 18:00',
        deadlineValue: new Date('2026-03-05 18:00').getTime(),
        status: 'active',
        score: null
    },
    {
        id: 4,
        name: 'Java基础 - 面向对象编程',
        className: 'Java程序设计2201班',
        teacherName: '赵老师',
        deadline: '2025-12-31 23:59',
        deadlineValue: new Date('2025-12-31 23:59').getTime(),
        status: 'expired',
        score: 85
    },
    {
        id: 5,
        name: '数据结构 - 链表操作',
        className: '数据结构2203班',
        teacherName: '钱教授',
        deadline: '2025-11-15 12:00',
        deadlineValue: new Date('2025-11-15 12:00').getTime(),
        status: 'submitted',
        score: 92
    }
])

// 计算属性：活跃任务
const activeTasks = computed(() => {
    return allTasks.value.filter(task => 
        task.status === 'active' && 
        task.name.toLowerCase().includes(searchQuery.value.toLowerCase())
    )
})

// 计算属性：所有任务（带搜索）
const filteredAllTasks = computed(() => {
    return allTasks.value.filter(task => 
        task.name.toLowerCase().includes(searchQuery.value.toLowerCase())
    )
})

const handleTabClick = (tab) => {
    // 可以在这里处理标签切换逻辑，例如重新加载数据
}

const viewTask = (taskId) => {
    // 实际跳转逻辑
    // router.push(`/student/task/${taskId}`)
    ElMessage.success(`跳转到题目集详情 ID: ${taskId}`)
}

const getStatusType = (status) => {
    const map = {
        'active': 'success',
        'expired': 'danger',
        'submitted': 'info'
    }
    return map[status] || 'info'
}

const getStatusText = (status) => {
    const map = {
        'active': '进行中',
        'expired': '已截止',
        'submitted': '已提交'
    }
    return map[status] || '未知'
}

const getScoreClass = (score) => {
    if (score >= 90) return 'score-excellent'
    if (score >= 80) return 'score-good'
    if (score >= 60) return 'score-pass'
    return 'score-fail'
}
</script>

<style scoped>
.task-list-container {
    padding: 0;
    min-height: 100%;
}

.box-card {
    border-radius: 8px;
    min-height: 80vh;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.title {
    font-size: 18px;
    font-weight: bold;
    color: var(--text-main);
}

.search-input {
    width: 250px;
}

.task-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 20px;
    padding: 10px 0;
}

.task-card {
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s;
    position: relative;
    overflow: hidden;
    border: 1px solid var(--border-color);
    background-color: var(--card-bg);
}

.active-card {
    border-top: 4px solid #67C23A;
}

.task-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.task-status-badge {
    position: absolute;
    top: 10px;
    right: 10px;
    background-color: rgba(103, 194, 58, 0.1);
    color: #67C23A;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
}

.task-info {
    padding: 10px;
}

.task-name {
    margin: 0 0 10px 0;
    font-size: 16px;
    color: var(--text-main);
    line-height: 1.4;
    height: 44px; /* 两行高度 */
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.task-class, .task-teacher {
    color: var(--text-muted);
    font-size: 13px;
    margin-bottom: 8px;
    display: flex;
    align-items: center;
    gap: 5px;
}

.task-deadline {
    color: #E6A23C;
    font-size: 13px;
    margin-bottom: 8px;
    display: flex;
    align-items: center;
    gap: 5px;
}

.task-countdown {
    display: flex;
    align-items: center;
    gap: 5px;
    margin-top: 10px;
    background-color: #fef0f0;
    padding: 8px;
    border-radius: 6px;
}

.task-countdown .label {
    font-size: 12px;
    color: #f56c6c;
}

.card-footer {
    margin-top: 15px;
    text-align: right;
    border-top: 1px solid #f0f2f5;
    padding-top: 10px;
}

.score-excellent { color: #67C23A; font-weight: bold; }
.score-good { color: #409EFF; font-weight: bold; }
.score-pass { color: #E6A23C; font-weight: bold; }
.score-fail { color: #F56C6C; font-weight: bold; }

.task-list-view {
    margin-top: 10px;
}
</style>
<template>
  <div class="common-layout">
    <el-container class="main-container">
      <el-header class="top-header">
        <div class="header-left">
            <div class="logo-area">
                <div class="logo-icon-box">
                    <el-icon><share /></el-icon>
                </div>
                <span class="logo-text">Java AST 查重</span>
            </div>
            
            <el-menu 
                :default-active="activeTopMenu" 
                mode="horizontal"
                class="el-menu-horizontal-demo"
                :ellipsis="false"
                @select="handleTopMenuSelect"
            >
                <el-menu-item index="/home">仪表盘</el-menu-item>
                
                <template v-if="user.role === 'TEACHER'">
                    <el-menu-item index="/teacher/classes">班级管理</el-menu-item>
                    <el-menu-item index="/teacher/students">学生管理</el-menu-item>
                    <el-menu-item index="/teacher/repository">作业管理</el-menu-item>
                    <el-menu-item index="/teacher/reports">报告</el-menu-item>
                </template>
                
                <template v-if="user.role === 'STUDENT'">
                    <el-menu-item index="/student/tasks">我的作业</el-menu-item>
                    <el-menu-item index="/student/classes">我的班级</el-menu-item>
                </template>

                <template v-if="user.role === 'ADMIN'">
                    <el-menu-item index="/admin/users">系统管理</el-menu-item>
                </template>

                <el-menu-item index="/settings">设置</el-menu-item>
            </el-menu>
        </div>

        <div class="header-center">
            <div class="search-box">
                <el-icon class="search-icon"><Search /></el-icon>
                <input v-model="searchQuery" placeholder="搜索学生或代码..." class="search-input" />
            </div>
        </div>

        <div class="header-right">
            <div class="notification-badge" @click="$router.push('/notifications')">
                <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="item">
                    <button class="button">
                        <svg class="bell" viewBox="0 0 448 512"><path d="M224 512c35.32 0 63.97-28.65 63.97-64H160.03c0 35.35 28.65 64 63.97 64zm215.39-149.71c-19.32-20.76-55.47-51.99-55.47-154.29 0-77.7-54.48-139.9-127.94-155.16V32c0-17.67-14.32-32-31.98-32s-31.98 14.33-31.98 32v20.84C118.56 68.1 64.08 130.3 64.08 208c0 102.3-36.15 133.53-55.47 154.29-6 6.45-8.66 14.16-8.61 21.71.11 16.4 12.98 32 32.1 32h383.8c19.12 0 32-15.6 32.1-32 .05-7.55-2.61-15.27-8.61-21.71z"></path></svg>
                    </button>
                </el-badge>
            </div>

            <div class="help-btn" @click="openFeedback" v-if="user.role !== 'ADMIN'">
                <el-icon><ChatLineRound /></el-icon> 问题反馈
            </div>
            <el-dropdown trigger="click">
                <div class="user-info">
                    <el-avatar :size="32" class="user-avatar" :src="user.avatar">{{ user.nickname?.charAt(0).toUpperCase() || 'U' }}</el-avatar>
                </div>
                <template #dropdown>
                    <el-dropdown-menu class="user-dropdown">
                        <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
                        <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
      </el-header>
      
      <el-container class="content-container">
        <el-aside width="240px" class="aside-menu">
            <div class="breadcrumb-area">
                <el-breadcrumb separator-icon="ArrowRight">
                    <el-breadcrumb-item>控制台</el-breadcrumb-item>
                    <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
                </el-breadcrumb>
            </div>

            <!-- 根据顶部菜单和角色动态显示左侧菜单 -->
            <el-menu 
                router 
                :default-active="$route.path" 
                class="el-menu-vertical-demo"
                :key="activeTopMenu"
            >
                <div class="menu-group-title">{{ currentMenuTitle }}</div>
                
                <!-- 仪表盘子菜单 -->
                <template v-if="activeTopMenu === '/home' || activeTopMenu === '/notifications'">
                    <el-menu-item index="/home">
                        <el-icon><Odometer /></el-icon>
                        <span>概览</span>
                    </el-menu-item>
                    <el-menu-item index="/notifications">
                        <el-icon><Bell /></el-icon>
                        <span>通知中心</span>
                    </el-menu-item>
                </template>

                <!-- 教师端菜单 -->
                <template v-if="user.role === 'TEACHER'">
                    <template v-if="activeTopMenu === '/teacher/classes'">
                        <el-menu-item index="/teacher/classes">
                            <el-icon><School /></el-icon>
                            <span>班级列表</span>
                        </el-menu-item>
                    </template>
                    <template v-if="activeTopMenu === '/teacher/students'">
                        <el-menu-item index="/teacher/students">
                            <el-icon><User /></el-icon>
                            <span>学生列表</span>
                        </el-menu-item>
                    </template>
                    <template v-if="activeTopMenu === '/teacher/repository'">
                        <el-menu-item index="/teacher/repository">
                            <el-icon><Folder /></el-icon>
                            <span>代码库</span>
                        </el-menu-item>
                        <!-- 可以在这里加一些假的子菜单来填充 -->
                        <el-menu-item index="/teacher/repository?type=recent">
                            <el-icon><Clock /></el-icon>
                            <span>最近提交</span>
                        </el-menu-item>
                    </template>
                    <template v-if="activeTopMenu === '/teacher/reports'">
                        <el-menu-item index="/teacher/reports">
                            <el-icon><DataAnalysis /></el-icon>
                            <span>查重报告</span>
                        </el-menu-item>
                    </template>
                </template>

                <!-- 学生端菜单 -->
                <template v-if="user.role === 'STUDENT'">
                    <template v-if="activeTopMenu === '/student/tasks'">
                        <el-menu-item index="/student/tasks">
                            <el-icon><List /></el-icon>
                            <span>作业列表</span>
                        </el-menu-item>
                        <el-menu-item index="/student/tasks?filter=todo">
                            <el-icon><Timer /></el-icon>
                            <span>待办事项</span>
                        </el-menu-item>
                    </template>
                    <template v-if="activeTopMenu === '/student/classes'">
                        <el-menu-item index="/student/classes">
                            <el-icon><School /></el-icon>
                            <span>我的班级</span>
                        </el-menu-item>
                    </template>
                </template>

                <!-- 管理员菜单 -->
                <template v-if="user.role === 'ADMIN' && activeTopMenu === '/admin/users'">
                    <el-menu-item index="/admin/users">
                        <el-icon><User /></el-icon>
                        <span>用户管理</span>
                    </el-menu-item>
                    <el-menu-item index="/admin/notices">
                        <el-icon><Bell /></el-icon>
                        <span>通知公告</span>
                    </el-menu-item>
                    <el-menu-item index="/admin/feedbacks">
                        <el-icon><ChatLineRound /></el-icon>
                        <span>问题反馈</span>
                    </el-menu-item>
                </template>

                <!-- 设置菜单 -->
                <template v-if="activeTopMenu === '/settings'">
                    <el-menu-item index="/settings">
                        <el-icon><Setting /></el-icon>
                        <span>系统设置</span>
                    </el-menu-item>
                    <el-menu-item index="/profile">
                        <el-icon><User /></el-icon>
                        <span>个人资料</span>
                    </el-menu-item>
                </template>
            </el-menu>
        </el-aside>
        
        <el-main class="main-content">
            <router-view v-slot="{ Component }">
                <component :is="Component" :key="$route.fullPath" />
            </router-view>
        </el-main>
      </el-container>
    </el-container>

    <!-- 问题反馈对话框 -->
    <el-dialog
        v-model="feedbackVisible"
        title="问题反馈"
        width="500px"
        :close-on-click-modal="false"
    >
        <el-form :model="feedbackForm" label-width="80px">
            <el-form-item label="标题">
                <el-input v-model="feedbackForm.title" placeholder="请输入问题简述" />
            </el-form-item>
            <el-form-item label="详细内容">
                <el-input
                    v-model="feedbackForm.content"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入详细问题描述（不少于10字）"
                />
            </el-form-item>
        </el-form>
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="feedbackVisible = false">取消</el-button>
                <el-button type="primary" @click="submitFeedback" :loading="submitting">
                    提交反馈
                </el-button>
            </span>
        </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Odometer, User, Bell, List, Folder, DataAnalysis, Setting, Share, QuestionFilled, ArrowRight, Search, ChatLineRound, School, Clock, Timer } from '@element-plus/icons-vue'
import request from '../api/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const searchQuery = ref('')

const unreadCount = ref(0)

const fetchUnreadCount = async () => {
    try {
        if (!user.id) return
        
        // 获取本地存储的已读ID (区分用户)
        const storageKey = `readNotificationIds_${user.id}`
        const readIds = JSON.parse(localStorage.getItem(storageKey) || '[]')
        
        let count = 0
        // 1. 获取系统通知
        const notices = await request.get('/notice/list')
        if (notices) {
            notices.forEach(n => {
                if (!readIds.includes(`notice-${n.id}`)) {
                    count++
                }
            })
        }

        // 2. 获取反馈回复
        if (user.role !== 'ADMIN') {
            const feedbacks = await request.get('/feedback/my', { params: { userId: user.id } })
            if (feedbacks) {
                feedbacks.forEach(f => {
                    if ((f.reply || f.status === 'RESOLVED') && !readIds.includes(`feedback-${f.id}`)) {
                        count++
                    }
                })
            }
        }
        
        unreadCount.value = count
    } catch (e) {
        console.error('获取未读数失败', e)
    }
}

onMounted(() => {
    fetchUnreadCount()
    // 监听通知更新事件
    window.addEventListener('notification-update', fetchUnreadCount)
})

// 反馈相关
const feedbackVisible = ref(false)
const submitting = ref(false)
const feedbackForm = reactive({
    title: '',
    content: ''
})

const openFeedback = () => {
    feedbackForm.title = ''
    feedbackForm.content = ''
    feedbackVisible.value = true
}

const submitFeedback = async () => {
    if (!feedbackForm.title) {
        ElMessage.warning('请输入标题')
        return
    }
    if (!feedbackForm.content || feedbackForm.content.length < 10) {
        ElMessage.warning('反馈内容不能少于10个字')
        return
    }
    
    submitting.value = true
    try {
        await request.post('/feedback/submit', {
            userId: user.id,
            title: feedbackForm.title,
            content: feedbackForm.content
        })
        ElMessage.success('反馈提交成功，感谢您的建议！')
        feedbackVisible.value = false
    } catch (e) {
        console.error(e)
        ElMessage.error(e.message || '提交失败')
    } finally {
        submitting.value = false
    }
}

const handleTopMenuSelect = (index) => {
    router.push(index)
}

// 计算当前激活的顶部菜单
const activeTopMenu = computed(() => {
    const path = route.path
    if (path.startsWith('/home')) return '/home'
    if (path.startsWith('/notifications')) return '/notifications'
    if (path.startsWith('/teacher/classes')) return '/teacher/classes'
    if (path.startsWith('/teacher/students')) return '/teacher/students'
    if (path.startsWith('/teacher/repository')) return '/teacher/repository'
    if (path.startsWith('/teacher/reports')) return '/teacher/reports'
    if (path.startsWith('/student/tasks')) return '/student/tasks'
    if (path.startsWith('/student/classes')) return '/student/classes'
    if (path.startsWith('/admin')) return '/admin/users'
    if (path.startsWith('/settings') || path.startsWith('/profile')) return '/settings'
    return '/home'
})

// 计算左侧菜单标题
const currentMenuTitle = computed(() => {
    const map = {
        '/home': '仪表盘',
        '/notifications': '通知中心',
        '/teacher/classes': '班级管理',
        '/teacher/students': '学生管理',
        '/teacher/repository': '作业管理',
        '/teacher/reports': '报告中心',
        '/student/tasks': '我的作业',
        '/student/classes': '我的班级',
        '/admin/users': '系统管理',
        '/settings': '设置'
    }
    return map[activeTopMenu.value] || '主菜单'
})

const currentRouteName = computed(() => {
    const map = {
        '/home': '仪表盘',
        '/notifications': '通知中心',
        '/admin/users': '用户管理',
        '/admin/notices': '通知管理',
        '/admin/feedbacks': '问题反馈',
        '/teacher/classes': '班级列表',
        '/teacher/students': '学生列表',
        '/teacher/repository': '代码库',
        '/teacher/reports': '报告中心',
        '/student/tasks': '作业列表',
        '/student/classes': '我的班级',
        '/profile': '个人资料',
        '/settings': '系统设置'
    }
    return map[route.path] || route.meta.title || '当前页面'
})

const logout = () => {
    localStorage.removeItem('user')
    router.push('/login')
}
</script>

<style scoped>
.common-layout {
    height: 100vh;
    width: 100vw;
    background-color: var(--bg-color-dark);
    overflow: hidden;
    color: var(--text-main);
}

.main-container {
    height: 100%;
    flex-direction: column;
}

/* Header Styling */
.top-header {
    background-color: var(--header-bg);
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    border-bottom: 1px solid var(--border-color);
    z-index: 20;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 40px;
}

.logo-area {
    display: flex;
    align-items: center;
    gap: 12px;
}

.logo-icon-box {
    width: 32px;
    height: 32px;
    background: #2563eb;
    border-radius: 6px;
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
}

.logo-text {
    font-size: 16px;
    font-weight: 700;
    color: white;
    letter-spacing: 0.5px;
    white-space: nowrap;
}

/* Horizontal Menu */
.el-menu-horizontal-demo {
    background-color: transparent !important;
    border-bottom: none !important;
    height: 64px;
}

:deep(.el-menu--horizontal .el-menu-item) {
    background-color: transparent !important;
    color: var(--text-muted) !important;
    font-weight: 500;
    font-size: 14px;
    height: 64px;
    border-bottom: 2px solid transparent !important;
}

:deep(.el-menu--horizontal .el-menu-item:hover) {
    color: white !important;
}

:deep(.el-menu--horizontal .el-menu-item.is-active) {
    color: #3b82f6 !important;
    border-bottom: 2px solid #3b82f6 !important;
    font-weight: 600;
}

/* Search Box */
.header-center {
    flex: 1;
    display: flex;
    justify-content: center;
    padding: 0 40px;
}

.search-box {
    position: relative;
    width: 100%;
    max-width: 400px;
}

.search-input {
    width: 100%;
    height: 36px;
    background-color: #1e293b;
    border: 1px solid #334155;
    border-radius: 8px;
    padding: 0 16px 0 40px;
    color: white;
    font-size: 13px;
    transition: all 0.2s;
    outline: none;
}

.search-input:focus {
    border-color: #3b82f6;
    background-color: #0f172a;
}

.search-icon {
    position: absolute;
    left: 12px;
    top: 50%;
    transform: translateY(-50%);
    color: #64748b;
    font-size: 16px;
}

/* Header Right */
.header-right {
    display: flex;
    align-items: center;
    gap: 20px;
}

.header-icon {
    font-size: 20px;
    color: var(--text-muted);
    cursor: pointer;
    transition: color 0.2s;
}
.header-icon:hover {
    color: white;
}

/* Custom Bell Button */
.button {
  width: 40px;
  height: 40px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgb(44, 44, 44);
  border-radius: 50%;
  cursor: pointer;
  transition-duration: .3s;
  box-shadow: 2px 2px 10px rgba(0, 0, 0, 0.13);
  border: none;
}

.bell {
  width: 16px;
}

.bell path {
  fill: white;
}

.button:hover {
  background-color: rgb(56, 56, 56);
}

.button:hover .bell {
  animation: bellRing 0.9s both;
}

/* bell ringing animation keyframes*/
@keyframes bellRing {
  0%,
  100% {
    transform-origin: top;
  }

  15% {
    transform: rotateZ(10deg);
  }

  30% {
    transform: rotateZ(-10deg);
  }

  45% {
    transform: rotateZ(5deg);
  }

  60% {
    transform: rotateZ(-5deg);
  }

  75% {
    transform: rotateZ(2deg);
  }
}

.button:active {
  transform: scale(0.8);
}

.help-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    color: var(--text-muted);
    font-size: 13px;
    cursor: pointer;
    transition: color 0.2s;
}
.help-btn:hover {
    color: white;
}

.user-info {
    cursor: pointer;
}

.user-avatar {
    background: #3b82f6;
    color: white;
    font-weight: 600;
    border: 2px solid #1e293b;
}

/* Sidebar & Content */
.content-container {
    flex: 1;
    display: flex;
    flex-direction: row;
    overflow: hidden;
}

.aside-menu {
    background-color: var(--sidebar-bg);
    border-right: 1px solid var(--border-color);
    display: flex;
    flex-direction: column;
    z-index: 10;
    padding-top: 20px;
}

.breadcrumb-area {
    padding: 0 20px 20px 20px;
}

.breadcrumb-area :deep(.el-breadcrumb__inner) {
    color: var(--text-muted);
    font-weight: 400;
}

.breadcrumb-area :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
    color: white;
    font-weight: 600;
}

.menu-group-title {
    padding: 12px 20px 8px 20px;
    font-size: 12px;
    color: var(--text-muted);
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

.el-menu-vertical-demo {
    border-right: none;
    padding: 0 12px;
    background-color: transparent !important;
}

:deep(.el-menu-vertical-demo .el-menu-item) {
    border-radius: 6px;
    margin-bottom: 2px;
    height: 40px;
    line-height: 40px;
    font-weight: 500;
    color: var(--text-muted);
}

:deep(.el-menu-vertical-demo .el-menu-item.is-active) {
    background-color: rgba(59, 130, 246, 0.1);
    color: #3b82f6;
}

:deep(.el-menu-vertical-demo .el-menu-item:hover) {
    background-color: rgba(255, 255, 255, 0.05);
    color: white;
}

.main-content {
    padding: 24px;
    overflow-y: auto;
    background-color: var(--bg-color-dark);
}

/* Transitions */
.fade-slide-up-enter-active,
.fade-slide-up-leave-active {
    transition: opacity 0.3s ease, transform 0.3s ease;
}
.fade-slide-up-enter-from {
    opacity: 0;
    transform: translateY(10px);
}
.fade-slide-up-leave-to {
    opacity: 0;
    transform: translateY(-10px);
}
</style>

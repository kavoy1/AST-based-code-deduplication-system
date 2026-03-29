<template>
  <div class="workspace-app-root" :class="appRoleClass">
    <template v-if="isTeacher">
      <div class="teacher-shell-frame" :class="{ 'teacher-shell-frame--with-panel': showTeacherSecondary && teacherPanelMounted }">
        <aside class="teacher-rail">
          <el-tooltip content="工作台入口" placement="right" :show-after="180">
            <button type="button" class="teacher-rail__brand" @click="navigateTo('/home')">
              <span class="teacher-rail__brand-mark">J</span>
              <span class="teacher-rail__brand-mark teacher-rail__brand-mark--sub">AST</span>
            </button>
          </el-tooltip>

          <div class="teacher-rail__group teacher-rail__group--primary">
            <div class="teacher-rail__active-indicator" :style="teacherRailTopActiveStyle"></div>
            <el-tooltip v-for="item in teacherRailPrimaryTop" :key="item.path" :content="item.label" placement="right" :show-after="180">
              <button
                type="button"
                class="teacher-rail__item"
                :class="{ active: isActive(item) }"
                @click="handleTeacherRailClick(item)"
              >
                <el-icon><component :is="item.icon" /></el-icon>
              </button>
            </el-tooltip>
          </div>

          <div class="teacher-rail__group teacher-rail__group--bottom">
            <el-tooltip v-for="item in teacherRailPrimaryBottom" :key="item.label" :content="item.label" placement="right" :show-after="180">
              <button
                type="button"
                class="teacher-rail__item"
                :class="{ active: item.path && isActive(item) }"
                @click="item.action ? item.action() : navigateTo(item.path)"
              >
                <el-badge :value="item.path === '/notifications' ? unreadCount : 0" :hidden="item.path !== '/notifications' || !unreadCount">
                  <el-icon><component :is="item.icon" /></el-icon>
                </el-badge>
              </button>
            </el-tooltip>
          </div>
        </aside>

          <aside v-if="showTeacherSecondary && teacherPanelMounted" class="teacher-hub-panel" :class="{ 'is-visible': teacherPanelShown }">
            <div class="teacher-hub-panel__header">
              <h2>{{ teacherPageMeta.title }}</h2>
            </div>

            <div class="teacher-hub-panel__nav">
              <button
                v-for="item in teacherPageMeta.secondaryNav"
                :key="item.label"
                type="button"
                class="teacher-hub-panel__nav-item"
                :class="{ active: item.active }"
                @click="item.path ? navigateTo(item.path) : undefined"
              >
                <span>{{ item.label }}</span>
                <small v-if="item.badge">{{ item.badge }}</small>
              </button>
            </div>

            <div class="teacher-hub-panel__section" v-for="group in teacherPageMeta.sections" :key="group.title">
              <div class="teacher-hub-panel__section-title">{{ group.title }}</div>
              <div class="teacher-hub-panel__stack">
                <component
                  :is="item.path ? 'button' : 'div'"
                  v-for="item in group.items"
                  :key="`${group.title}-${item.label}`"
                  type="button"
                  class="teacher-hub-panel__link"
                  :class="{ active: !!item.highlight }"
                  @click="item.path ? navigateTo(item.path) : undefined"
                >
                  <span class="teacher-hub-panel__link-main">
                    <el-icon><component :is="item.icon" /></el-icon>
                    <span>
                      <strong>{{ item.label }}</strong>
                    </span>
                  </span>
                  <small v-if="item.badge">{{ item.badge }}</small>
                </component>
              </div>
            </div>

          </aside>

        <section class="teacher-stage-shell">
          <main class="teacher-stage-main">
            <router-view v-slot="{ Component }">
              <component :is="Component" />
            </router-view>
          </main>
        </section>
      </div>
      <TeacherPlagiarismMonitorPanel />
    </template>

    <template v-else>
      <div class="workspace-app-frame">
        <div class="workspace-app-glow workspace-app-glow--left"></div>
        <div class="workspace-app-glow workspace-app-glow--right"></div>

        <div class="workspace-app-shell">
          <aside class="workspace-sidebar">
            <div class="workspace-sidebar__brand">
              <div class="workspace-sidebar__logo">
                <el-icon><Connection /></el-icon>
              </div>
              <div>
                <div class="workspace-sidebar__name">Java AST</div>
                <div class="workspace-sidebar__tag">{{ roleLabel }} 工作台</div>
              </div>
            </div>

            <div class="workspace-sidebar__group">
              <div class="workspace-sidebar__caption">Main menu</div>
              <button
                v-for="item in currentNav"
                :key="item.path"
                type="button"
                class="workspace-sidebar__item"
                :class="{ active: isActive(item) }"
                @click="handleTeacherRailClick(item)"
              >
                <span class="workspace-sidebar__icon"><el-icon><component :is="item.icon" /></el-icon></span>
                <span>{{ item.label }}</span>
                <small v-if="item.badge">{{ item.badge }}</small>
              </button>
            </div>

            <div class="workspace-sidebar__group workspace-sidebar__group--secondary">
              <div class="workspace-sidebar__caption">System</div>
              <button type="button" class="workspace-sidebar__item" :class="{ active: route.path === '/notifications' }" @click="navigateTo('/notifications')">
                <span class="workspace-sidebar__icon"><el-icon><Bell /></el-icon></span>
                <span>通知中心</span>
                <small v-if="unreadCount">{{ unreadCount }}</small>
              </button>
              <button type="button" class="workspace-sidebar__item" :class="{ active: route.path === '/profile' }" @click="navigateTo('/profile')">
                <span class="workspace-sidebar__icon"><el-icon><User /></el-icon></span>
                <span>个人资料</span>
              </button>
              <button v-if="user.role !== 'ADMIN'" type="button" class="workspace-sidebar__item" @click="openFeedback">
                <span class="workspace-sidebar__icon"><el-icon><ChatLineRound /></el-icon></span>
                <span>问题反馈</span>
              </button>
            </div>

            <div class="workspace-sidebar__account">
              <el-avatar :size="42" :src="user.avatar">{{ avatarText }}</el-avatar>
              <div>
                <div class="workspace-sidebar__user">{{ user.nickname || user.username || '用户' }}</div>
                <div class="workspace-sidebar__role">{{ roleLabel }}</div>
              </div>
            </div>
          </aside>

          <section class="workspace-main-shell">
            <header class="workspace-topbar">
              <div class="workspace-topbar__left">
                <div class="workspace-topbar__breadcrumb">{{ roleLabel }} / {{ currentSection }}</div>
              </div>

              <div class="workspace-topbar__actions">
                <span class="workspace-pill workspace-topbar__status-pill">
                  <span class="workspace-status-dot"></span>
                  系统在线
                </span>
                <button type="button" class="workspace-icon-button" @click="navigateTo('/notifications')">
                  <el-badge :value="unreadCount" :hidden="!unreadCount">
                    <el-icon><Bell /></el-icon>
                  </el-badge>
                </button>
                <el-dropdown trigger="click">
                  <button type="button" class="workspace-user-button">
                    <el-avatar :size="36" :src="user.avatar">{{ avatarText }}</el-avatar>
                    <div>
                      <strong>{{ user.nickname || user.username || '用户' }}</strong>
                      <span>{{ roleLabel }}</span>
                    </div>
                    <el-icon><ArrowDown /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="navigateTo('/profile')">个人资料</el-dropdown-item>
                      <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </header>

            <main class="workspace-stage-shell">
              <router-view v-slot="{ Component }">
                <component :is="Component" :key="route.fullPath" />
              </router-view>
            </main>
          </section>
        </div>
      </div>
    </template>

    <el-dialog v-model="feedbackVisible" title="提交问题反馈" width="520px" :close-on-click-modal="false">
      <el-form :model="feedbackForm" label-position="top">
        <el-form-item label="反馈标题">
          <el-input v-model="feedbackForm.title" placeholder="请描述问题标题" />
        </el-form-item>
        <el-form-item label="详细说明">
          <el-input v-model="feedbackForm.content" type="textarea" :rows="5" placeholder="请说明问题现象、出现路径和复现条件" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitFeedback">提交反馈</el-button>
      </template>
    </el-dialog>

    <div v-if="logoutConfirmVisible" class="logout-modal" @click.self="closeLogoutConfirm">
      <div class="logout-card" role="dialog" aria-modal="true" aria-labelledby="logout-title">
        <div class="logout-card__header">
          <div class="logout-card__image">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M12 9V13" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
              <path d="M12 17.01L12.01 16.9989" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" />
              <path d="M10.29 3.85999L1.82001 18C1.64539 18.3024 1.55085 18.6442 1.54555 18.9926C1.54025 19.341 1.62437 19.6855 1.78971 19.993C1.95506 20.3005 2.19606 20.5609 2.48997 20.749C2.78388 20.9372 3.12096 21.0468 3.47001 21.067H20.53C20.8791 21.0468 21.2161 20.9372 21.51 20.749C21.8039 20.5609 22.0449 20.3005 22.2103 19.993C22.3757 19.6855 22.4598 19.341 22.4545 18.9926C22.4492 18.6442 22.3546 18.3024 22.18 18L13.71 3.85999C13.5168 3.56796 13.2544 3.32813 12.9461 3.16194C12.6378 2.99575 12.2932 2.9082 11.943 2.90723C11.5928 2.90626 11.2477 2.99189 10.9384 3.15637C10.6292 3.32085 10.3654 3.55922 10.17 3.84999L10.29 3.85999Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
            </svg>
          </div>
          <div class="logout-card__content">
            <h3 id="logout-title" class="logout-card__title">退出登录</h3>
            <p class="logout-card__message">确认退出当前账号吗？退出后需要重新登录才能继续使用。</p>
          </div>
        </div>
        <div class="logout-card__actions">
          <button type="button" class="logout-card__action logout-card__action--danger" @click="confirmLogout">退出登录</button>
          <button type="button" class="logout-card__action logout-card__action--cancel" @click="closeLogoutConfirm">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Bell,
  ChatLineRound,
  Connection,
  DataAnalysis,
  Files,
  HomeFilled,
  List,
  Memo,
  School,
  Tickets,
  Setting,
  SwitchButton,
  User
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import TeacherPlagiarismMonitorPanel from '../components/TeacherPlagiarismMonitorPanel.vue'

const router = useRouter()
const route = useRoute()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const unreadCount = ref(0)
const feedbackVisible = ref(false)
const logoutConfirmVisible = ref(false)
const submitting = ref(false)
const feedbackForm = reactive({ title: '', content: '' })

const navMap = {
  ADMIN: [
    { path: '/admin/command-center', label: '系统监控', icon: DataAnalysis },
    { path: '/admin/users', label: '用户管理', icon: User },
    { path: '/admin/notices', label: '通知公告', icon: Memo },
    { path: '/admin/feedbacks', label: '问题反馈', icon: ChatLineRound },
    { path: '/admin/settings', label: '系统设置', icon: Setting }
  ],
  TEACHER: [
    { path: '/home', label: '教师首页', icon: HomeFilled },
    { path: '/teacher/classes', label: '教学管理', icon: School },
    { path: '/teacher/assignments', label: '作业管理', icon: Tickets }
  ],
  STUDENT: [
    { path: '/student/tasks', label: '我的作业', icon: Files },
    { path: '/student/classes', label: '我的班级', icon: School },
    { path: '/home', label: '学习工作台', icon: List }
  ]
}

const roleLabelMap = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生'
}

const currentNav = computed(() => navMap[user.role] || [{ path: '/home', label: '工作台', icon: HomeFilled }])
const roleLabel = computed(() => roleLabelMap[user.role] || '访客')
const avatarText = computed(() => (user.nickname || user.username || 'U').slice(0, 1).toUpperCase())
const isActive = (item) => {
  if (!item?.path) return false
  const itemPath = item.path
  if (itemPath === '/teacher/classes') {
    return route.path.startsWith('/teacher/classes') || route.path.startsWith('/teacher/students')
  }
  if (itemPath === '/teacher/assignments') {
    return route.path.startsWith('/teacher/assignments') || route.path.startsWith('/teacher/submissions') || route.path.startsWith('/teacher/similarity-pairs')
  }
  if (itemPath === '/student/tasks') {
    return route.path.startsWith('/student/tasks') || route.path.startsWith('/student/assignments')
  }
  return Boolean(itemPath) && (route.path === itemPath || route.path.startsWith(`${itemPath}/`))
}
const currentSection = computed(() => currentNav.value.find((item) => isActive(item))?.label || route.meta.title || '工作台')
const isTeacher = computed(() => user.role === 'TEACHER')
const appRoleClass = computed(() => `role-${(user.role || 'guest').toLowerCase()}`)

const TEACHER_PANEL_ANIMATION_MS = 180
const teacherPanelMounted = ref(false)
const teacherPanelShown = ref(false)
const teacherPanelClosing = ref(false)
let teacherPanelTimer = null

function clearTeacherPanelTimer() {
  if (teacherPanelTimer) {
    clearTimeout(teacherPanelTimer)
    teacherPanelTimer = null
  }
}

function handleTeacherRailClick(item) {
  if (!item) return
  if (item.action) {
    item.action()
    return
  }
  navigateTo(item.path)
}

function goTeacherHome() {
  navigateTo('/home')
}

function goTeachingManage() {
  router.push({ path: '/teacher/classes', query: { tab: 'classes' } })
}

function goAssignmentManage() {
  navigateTo({ path: '/teacher/assignments', query: { tab: 'overview' } })
}

function navigateTo(target) {
  if (!target) return
  const normalizedTarget = isTeacher.value && target === '/teacher/classes' ? { path: '/teacher/classes', query: { tab: 'classes' } } : target
  const resolved = router.resolve(normalizedTarget)
  if (route.fullPath === resolved.fullPath) {
    window.location.reload()
    return
  }

  const leavingTeacherPanel = isTeacher.value && teacherSection.value !== 'home' && resolved.path === '/home'
  if (leavingTeacherPanel) {
    clearTeacherPanelTimer()
    teacherPanelClosing.value = true
    teacherPanelShown.value = false
    teacherPanelTimer = setTimeout(() => {
      teacherPanelMounted.value = false
      teacherPanelClosing.value = false
      router.push(normalizedTarget)
      clearTeacherPanelTimer()
    }, TEACHER_PANEL_ANIMATION_MS)
    return
  }

  router.push(normalizedTarget)
}

const teacherRailPrimaryTop = computed(() => [
  { path: '/home', icon: HomeFilled, label: '首页', action: goTeacherHome },
  { path: '/teacher/classes', icon: School, label: '教学管理', action: goTeachingManage },
  { path: '/teacher/assignments', icon: Tickets, label: '作业管理', action: goAssignmentManage }
])

const teacherRailPrimaryBottom = computed(() => [
  { path: '/notifications', icon: Bell, label: '通知中心' },
  { path: '/profile', icon: Setting, label: '系统 / 个人设置' },
  { icon: SwitchButton, label: '退出登录', action: logout }
])

const RAIL_ITEM_SIZE = 56
const RAIL_GROUP_GAP = 10

function createRailIndicatorStyle(items) {
  const index = items.findIndex((item) => item.path && isActive(item))
  if (index < 0) {
    return { opacity: '0', transform: 'translateY(0) scale(0.92)' }
  }
  const offset = index * (RAIL_ITEM_SIZE + RAIL_GROUP_GAP)
  return {
    opacity: '1',
    transform: `translateY(${offset}px) scale(1)`
  }
}

const teacherRailTopActiveStyle = computed(() => createRailIndicatorStyle(teacherRailPrimaryTop.value))

const teacherSection = computed(() => {
  if (route.name === 'Home') return 'home'
  if (route.path.startsWith('/teacher/classes/')) return 'teaching'
  if (route.path.startsWith('/teacher/classes')) return 'teaching'
  if (route.path.startsWith('/teacher/students')) return 'teaching'
  if (route.path.startsWith('/teacher/assignments')) return 'assignments'
  if (route.path.startsWith('/teacher/submissions')) return 'assignments'
  if (route.path.startsWith('/teacher/similarity-pairs')) return 'assignments'
  if (route.path.startsWith('/notifications')) return 'notifications'
  if (route.path.startsWith('/profile')) return 'profile'
  return 'default'
})

const showTeacherSecondary = computed(() => ['teaching', 'assignments'].includes(teacherSection.value))

watch(
  teacherSection,
  async (section) => {
    if (!showTeacherSecondary.value) {
      if (!teacherPanelClosing.value) {
        teacherPanelMounted.value = false
        teacherPanelShown.value = false
      }
      return
    }

    clearTeacherPanelTimer()
    teacherPanelMounted.value = true
    await nextTick()
    requestAnimationFrame(() => {
      teacherPanelShown.value = true
    })
  },
  { immediate: true }
)

const teacherPageMeta = computed(() => {
  if (teacherSection.value === 'teaching') {
    const queryTab = typeof route.query.tab === 'string' ? route.query.tab : ''
    const activeTeachingTab = route.path.startsWith('/teacher/students') || route.path.startsWith('/teacher/classes/')
      ? 'students'
      : ['classes', 'students', 'applications', 'invites'].includes(queryTab)
        ? queryTab
        : 'classes'
    const activeClassId = typeof route.query.classId === 'string' ? route.query.classId : ''
    const hasClassContext = route.path.startsWith('/teacher/classes/') || Boolean(activeClassId)

    return {
      title: '教学管理',
      description: '班级、学生、入班申请与邀请码统一在这里切换。',
      secondaryNav: [
        { label: '班级列表', path: { path: '/teacher/classes', query: { tab: 'classes' } }, active: activeTeachingTab === 'classes', badge: '管理' },
        { label: '学生列表', path: { path: '/teacher/classes', query: { tab: 'students', ...(hasClassContext && activeClassId ? { classId: activeClassId } : {}) } }, active: activeTeachingTab === 'students', badge: hasClassContext ? '成员' : '名单' },
        { label: '入班申请', path: { path: '/teacher/classes', query: { tab: 'applications' } }, active: activeTeachingTab === 'applications', badge: '审批' },
        { label: '邀请码', path: { path: '/teacher/classes', query: { tab: 'invites' } }, active: activeTeachingTab === 'invites', badge: '分享' }
      ],
      sections: [
        {
          title: '辅助入口',
          items: [
            { icon: HomeFilled, label: '返回首页', desc: '回到教师首页。', path: '/home' }
          ]
        }
      ]
    }
  }

  if (teacherSection.value === 'assignments') {
    const rawAssignmentTab = String(route.query.tab || 'overview')
    const activeAssignmentTab = route.path.startsWith('/teacher/submissions')
      ? 'submissions'
      : route.path.startsWith('/teacher/similarity-pairs')
        ? 'plagiarism'
        : rawAssignmentTab === 'list'
          ? 'overview'
          : rawAssignmentTab === 'create'
            ? 'settings'
            : ['overview', 'settings', 'submissions', 'plagiarism'].includes(rawAssignmentTab)
              ? rawAssignmentTab
              : 'overview'
    return {
      title: '作业管理',
      description: '作业发布、提交进度与查重结果统一在这里处理。',
      secondaryNav: [
        { label: '作业总览', path: { path: '/teacher/assignments', query: { tab: 'overview' } }, active: activeAssignmentTab === 'overview', badge: '入口' },
        { label: '发布与设置', path: { path: '/teacher/assignments', query: { tab: 'settings' } }, active: activeAssignmentTab === 'settings', badge: '配置' },
        { label: '提交与批改', path: { path: '/teacher/assignments', query: { tab: 'submissions' } }, active: activeAssignmentTab === 'submissions', badge: '过程' },
        { label: '查重与结果', path: { path: '/teacher/assignments', query: { tab: 'plagiarism' } }, active: activeAssignmentTab === 'plagiarism', badge: '结果' }
      ],
      sections: [
        {
          title: '辅助入口',
          items: [
            { icon: School, label: '教学管理', desc: '切回班级、学生、申请与邀请码处理。', path: '/teacher/classes' },
            { icon: HomeFilled, label: '返回首页', desc: '回到教师首页。', path: '/home' }
          ]
        }
      ]
    }
  }

  if (teacherSection.value === 'notifications') {
    return {
      title: '通知中心',
      description: '这里集中查看通知、公告和反馈消息。',
      secondaryNav: [],
      sections: []
    }
  }

  if (teacherSection.value === 'profile') {
    return {
      title: '系统 / 个人设置',
      description: '这里维护个人信息、密码与偏好设置。',
      secondaryNav: [
        { label: '个人资料', active: true },
        { label: '密码安全', active: false, badge: '安全' },
        { label: '偏好设置', active: false, badge: '偏好' }
      ],
      sections: []
    }
  }

  return {
    title: '首页',
    description: '首页不显示二级面板，只保留右侧内容展示。',
    secondaryNav: [],
    sections: []
  }
})

async function fetchUnreadCount() {
  try {
    if (!user.id) {
      unreadCount.value = 0
      return
    }
    const storageKey = `readNotificationIds_${user.id}`
    const readIds = JSON.parse(localStorage.getItem(storageKey) || '[]')
    let count = 0
    const notices = await request.get('/notice/list')
    if (Array.isArray(notices)) {
      notices.forEach((notice) => {
        const isPrivate = Boolean(notice.receiverId)
        const isRead = isPrivate ? Number(notice.isRead) === 1 : readIds.includes(`notice-${notice.id}`)
        if (!isRead) count += 1
      })
    }
    if (user.role !== 'ADMIN') {
      const feedbacks = await request.get('/feedback/my', { params: { userId: user.id } })
      if (Array.isArray(feedbacks)) {
        feedbacks.forEach((feedback) => {
          if ((feedback.reply || feedback.status === 'RESOLVED') && !readIds.includes(`feedback-${feedback.id}`)) {
            count += 1
          }
        })
      }
    }
    unreadCount.value = count
  } catch (error) {
    unreadCount.value = 0
    console.error('获取未读通知失败', error)
  }
}

function openFeedback() {
  feedbackForm.title = ''
  feedbackForm.content = ''
  feedbackVisible.value = true
}

async function submitFeedback() {
  if (!feedbackForm.title.trim() || !feedbackForm.content.trim()) {
    ElMessage.warning('请完整填写反馈标题和内容')
    return
  }
  submitting.value = true
  try {
    await request.post('/feedback/submit', {
      userId: user.id,
      title: feedbackForm.title.trim(),
      content: feedbackForm.content.trim()
    })
    feedbackVisible.value = false
    ElMessage.success('反馈已提交')
  } finally {
    submitting.value = false
  }
}

function logout() {
  logoutConfirmVisible.value = true
}

function closeLogoutConfirm() {
  logoutConfirmVisible.value = false
}

function confirmLogout() {
  logoutConfirmVisible.value = false
  localStorage.removeItem('user')
  localStorage.removeItem('satoken')
  router.push('/login')
}

onMounted(() => {
  fetchUnreadCount()
  window.addEventListener('notification-update', fetchUnreadCount)
})

onBeforeUnmount(() => {
  window.removeEventListener('notification-update', fetchUnreadCount)
})
</script>
<style scoped>
.workspace-app-root {
  min-height: 100vh;
}

.workspace-app-frame {
  position: relative;
  min-height: 100vh;
  padding: 6px;
}

.workspace-app-glow {
  position: fixed;
  inset: auto;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  filter: blur(44px);
  opacity: 0.34;
  pointer-events: none;
}

.workspace-app-glow--left {
  top: 46px;
  left: 4px;
  background: rgba(207, 230, 234, 0.7);
}

.workspace-app-glow--right {
  right: 4px;
  bottom: 4px;
  background: rgba(226, 231, 244, 0.64);
}

.logout-modal {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 17, 20, 0.36);
  backdrop-filter: blur(6px);
}

.logout-card {
  overflow: hidden;
  position: relative;
  width: 100%;
  max-width: 290px;
  border-radius: 12px;
  background-color: #ffffff;
  text-align: left;
  box-shadow:
    0 20px 25px -5px rgba(0, 0, 0, 0.1),
    0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.logout-card__header {
  padding: 20px 16px 16px;
  background-color: #ffffff;
}

.logout-card__image {
  display: flex;
  width: 48px;
  height: 48px;
  margin-left: auto;
  margin-right: auto;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background-color: #fee2e2;
  color: #dc2626;
}

.logout-card__image svg {
  width: 24px;
  height: 24px;
}

.logout-card__content {
  margin-top: 12px;
  text-align: center;
}

.logout-card__title {
  margin: 0;
  color: #111827;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.logout-card__message {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 14px;
  line-height: 20px;
}

.logout-card__actions {
  margin: 12px 16px 16px;
  background-color: #f9fafb;
}

.logout-card__action {
  display: inline-flex;
  width: 100%;
  justify-content: center;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 16px;
  line-height: 24px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.logout-card__action--danger {
  border: 1px solid transparent;
  background-color: #dc2626;
  color: #ffffff;
}

.logout-card__action--cancel {
  margin-top: 12px;
  border: 1px solid #d1d5db;
  background-color: #ffffff;
  color: #374151;
}

.workspace-app-shell {
  position: relative;
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr);
  gap: 10px;
  width: 100%;
  min-height: calc(100vh - 12px);
  margin: 0;
  padding: 8px;
  border-radius: 28px;
  background: var(--shell-surface);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--shadow-float);
  backdrop-filter: blur(14px);
}

.workspace-sidebar {
  display: flex;
  flex-direction: column;
  padding: 14px;
  border-radius: 24px;
  background: var(--sidebar-tint);
  border: 1px solid rgba(255, 255, 255, 0.45);
}

.workspace-sidebar__brand,
.workspace-sidebar__account {
  display: flex;
  align-items: center;
  gap: 12px;
}

.workspace-sidebar__brand {
  padding: 2px 2px 10px;
}

.workspace-sidebar__logo {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 15px;
  background: #13161a;
  color: #fff;
}

.workspace-sidebar__name {
  font-size: 18px;
  font-weight: 700;
}

.workspace-sidebar__tag,
.workspace-sidebar__caption,
.workspace-sidebar__role {
  color: var(--text-soft);
  font-size: 12px;
}

.workspace-sidebar__group {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.workspace-sidebar__group--secondary {
  margin-top: auto;
}

.workspace-sidebar__item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 42px;
  padding: 0 14px;
  border: none;
  border-radius: 18px;
  background: transparent;
  color: var(--text-body);
  text-align: left;
  cursor: pointer;
  transition: background var(--transition-soft), transform var(--transition-soft), box-shadow var(--transition-soft);
}

.workspace-sidebar__item:hover {
  background: rgba(255, 255, 255, 0.62);
}

.workspace-sidebar__item.active {
  background: rgba(255, 255, 255, 0.95);
  color: var(--text-strong);
  box-shadow: 0 10px 26px rgba(130, 143, 154, 0.1);
}

.workspace-sidebar__item small {
  margin-left: auto;
  color: var(--text-soft);
  font-size: 12px;
}

.workspace-sidebar__icon {
  width: 22px;
  display: inline-flex;
  justify-content: center;
}

.workspace-sidebar__account {
  margin-top: 14px;
  padding: 10px 6px 2px;
}

.workspace-sidebar__user {
  font-weight: 600;
}

.workspace-main-shell {
  display: flex;
  flex-direction: column;
  min-width: 0;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.62);
}

.workspace-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px 6px;
}

.workspace-topbar__left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.workspace-topbar__breadcrumb {
  color: var(--text-soft);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.workspace-topbar__status-pill {
  min-height: 32px;
  padding: 0 12px;
}

.workspace-topbar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.workspace-icon-button,
.workspace-user-button,
.teacher-stage-topbar__user,
.teacher-hub-panel__ghost,
.teacher-rail__brand,
.teacher-rail__item,
.teacher-hub-panel__link,
.teacher-hub-panel__nav-item {
  border: none;
  cursor: pointer;
}

.workspace-icon-button,
.workspace-user-button {
  background: rgba(255, 255, 255, 0.84);
  color: var(--text-strong);
}

.workspace-icon-button {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  display: grid;
  place-items: center;
}

.workspace-user-button,
.teacher-stage-topbar__user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 12px 7px 7px;
  border-radius: 18px;
}

.workspace-user-button strong,
.workspace-user-button span,
.teacher-stage-topbar__user strong,
.teacher-stage-topbar__user span {
  display: block;
  text-align: left;
}

.workspace-user-button span,
.teacher-stage-topbar__user span {
  color: var(--text-soft);
  font-size: 12px;
}

.workspace-stage-shell {
  min-height: 0;
  flex: 1;
  padding: 6px 14px 14px;
}

.teacher-shell-frame {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 14px;
  min-height: 100vh;
  padding: 14px;
  background: linear-gradient(180deg, #dce2e6 0%, #edf1f3 100%);

}

.teacher-shell-frame--with-panel {
  grid-template-columns: 92px 320px minmax(0, 1fr);
}

.teacher-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 18px 0;
  border-radius: 30px;
  background: linear-gradient(180deg, #17191d 0%, #0f1114 100%);
  box-shadow: 0 24px 50px rgba(16, 18, 22, 0.24);
}

.teacher-rail__brand,
.teacher-rail__item {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: 20px;
  background: transparent;
  color: rgba(255, 255, 255, 0.78);
  transition: transform var(--transition-soft), background var(--transition-soft), color var(--transition-soft), box-shadow var(--transition-soft);
}

.teacher-rail__item .el-icon {
  font-size: 18px;
}

.teacher-rail__brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.teacher-rail__brand-mark {
  display: block;
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0.08em;
  line-height: 1;
}

.teacher-rail__brand-mark--sub {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  opacity: 0.78;
}

.teacher-rail__group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.teacher-rail__group--primary {
  position: relative;
}

.teacher-rail__active-indicator {
  position: absolute;
  left: 0;
  top: 0;
  width: 56px;
  height: 56px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.05), 0 10px 24px rgba(255, 255, 255, 0.05);
  pointer-events: none;
  transition: transform 220ms ease, opacity 220ms ease;
}

.teacher-rail__group--bottom {
  margin-top: auto;
}

.teacher-rail__item {
  position: relative;
  z-index: 1;
}

.teacher-rail__item:hover,
.teacher-rail__item.active {
  color: #fff;
}

.teacher-rail__item.active {
  transform: none;
}

.teacher-rail__item:hover {
  transform: translateY(-1px);
}

.teacher-hub-panel {
  display: flex;
  flex-direction: column;
  gap: 22px;
  padding: 28px 26px;
  border-radius: 30px;
  background: linear-gradient(180deg, rgba(245, 249, 250, 0.98), rgba(251, 253, 253, 0.92));
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow: 0 18px 36px rgba(140, 150, 158, 0.12);
  transform-origin: left center;
  opacity: 0;
  transform: translateX(-12px) scale(0.985);
  transition: opacity 180ms ease, transform 180ms ease;
}

.teacher-hub-panel.is-visible {
  opacity: 1;
  transform: translateX(0) scale(1);
}


.teacher-hub-panel__header h2 {
  margin: 0;
  font-size: 36px;
  line-height: 1.05;
}
.teacher-hub-panel__header p:last-child,
.teacher-hub-panel__section-title,
.teacher-hub-panel__account span,
.teacher-stage-topbar__breadcrumb {
  color: var(--text-soft);
}

.teacher-hub-panel__nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border-radius: 20px;
  background: rgba(240, 244, 245, 0.9);
}

.teacher-hub-panel__nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
  padding: 0 14px;
  border-radius: 14px;
  background: transparent;
  color: var(--text-body);
  text-align: left;
}

.teacher-hub-panel__nav-item.active {
  background: #ffffff;
  color: var(--text-strong);
  box-shadow: 0 8px 20px rgba(121, 137, 147, 0.08);
}

.teacher-hub-panel__nav-item small {
  color: var(--text-faint);
}

.teacher-hub-panel__section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: auto;
}

.teacher-hub-panel__stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.teacher-hub-panel__section-title {
  font-size: 12px;
  font-weight: 600;
}

.teacher-hub-panel__link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  min-height: 58px;
  padding: 14px 16px;
  border-radius: 18px;
  background: transparent;
  color: var(--text-body);
  text-align: left;
  transition: background var(--transition-soft), box-shadow var(--transition-soft), transform var(--transition-soft);
}

.teacher-hub-panel__link-main {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.teacher-hub-panel__link-main strong {
  display: block;
  font-size: 14px;
}

.teacher-hub-panel__link-main em {
  display: block;
  margin-top: 4px;
  color: var(--text-soft);
  font-size: 12px;
  font-style: normal;
}

.teacher-hub-panel__link:hover,
.teacher-hub-panel__link.active {
  background: rgba(238, 242, 244, 0.96);
  color: var(--text-strong);
  box-shadow: inset 0 0 0 1px rgba(29, 35, 43, 0.04);
}

.teacher-hub-panel__link small {
  color: var(--text-faint);
}

.teacher-stage-shell {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 12px 12px 12px 0;
  overflow: hidden;
}

.teacher-stage-main {
  flex: 1;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

@media (max-width: 1380px) {
  .teacher-shell-frame--with-panel {
    grid-template-columns: 92px minmax(260px, 300px) minmax(0, 1fr);
  }
}

@media (max-width: 1180px) {
  .workspace-app-shell,
  .teacher-shell-frame {
    grid-template-columns: 1fr;
  }

  .workspace-sidebar {
    order: 2;
  }

  .teacher-rail {
    flex-direction: row;
    justify-content: center;
    padding: 12px;
  }

  .teacher-rail__group,
  .teacher-rail__group--bottom {
    flex-direction: row;
    margin-top: 0;
  }

  .teacher-hub-panel {
    order: 2;
  }

  .teacher-stage-shell {
    padding: 0;
  }
}

@media (max-width: 768px) {
  .workspace-app-frame,
  .teacher-shell-frame {
    padding: 4px;
  }

  .workspace-app-shell {
    min-height: calc(100vh - 8px);
    padding: 6px;
    border-radius: 22px;
  }

  .workspace-topbar,
  .workspace-stage-shell {
    padding-left: 10px;
    padding-right: 10px;
  }

  .workspace-topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .workspace-topbar__actions {
    justify-content: space-between;
  }

  .workspace-topbar__status-pill {
    display: none;
  }

  .workspace-user-button {
    flex: 1;
    justify-content: space-between;
  }

  .teacher-hub-panel {
    padding: 20px;
  }

  .teacher-shell-frame--with-panel {
    grid-template-columns: 1fr;
  }
}
</style>































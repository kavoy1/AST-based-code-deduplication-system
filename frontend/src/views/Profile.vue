<template>
  <div class="workspace-page profile-page" :class="{ 'profile-page--teacher': isTeacher }">
    <template v-if="isTeacher">
      <div class="teacher-settings-single" v-loading="loading">
        <div class="teacher-settings-single__nav">
          <button
            v-for="item in teacherSections"
            :key="item.key"
            type="button"
            class="teacher-settings-single__tab"
            :class="{ active: activeSection === item.key }"
            @click="activeSection = item.key"
          >
            {{ item.label }}
          </button>
        </div>

        <WorkspacePanel v-if="activeSection === 'profile'" title="个人资料" compact>
          <div class="teacher-profile-panel">
            <div class="teacher-profile-panel__identity">
              <el-avatar :size="78" :src="userInfo.avatar">{{ avatarText }}</el-avatar>
              <div>
                <h3>{{ userInfo.nickname || userInfo.username || '未命名教师' }}</h3>
                <p>{{ formatRole(userInfo.role) }}</p>
              </div>
            </div>

            <div class="teacher-profile-panel__fields">
              <div class="teacher-profile-panel__field"><span>用户名</span><strong>{{ userInfo.username || '-' }}</strong></div>
              <div class="teacher-profile-panel__field"><span>工号 / 学号</span><strong>{{ userInfo.uid || userInfo.studentNumber || '-' }}</strong></div>
              <div class="teacher-profile-panel__field"><span>姓名</span><strong>{{ userInfo.nickname || '-' }}</strong></div>
              <div class="teacher-profile-panel__field"><span>邮箱</span><strong>{{ userInfo.email || '-' }}</strong></div>
              <div class="teacher-profile-panel__field"><span>角色</span><strong>{{ formatRole(userInfo.role) }}</strong></div>
            </div>
          </div>
        </WorkspacePanel>

        <WorkspacePanel v-else-if="activeSection === 'security'" title="密码安全" compact>
          <div class="teacher-security-panel">
            <div class="teacher-setting-row">
              <div>
                <strong>修改密码</strong>
                <p>修改成功后需要重新登录。</p>
              </div>
              <el-button type="primary" round @click="dialogVisible = true">修改密码</el-button>
            </div>
            <div class="teacher-setting-row">
              <div>
                <strong>登录状态</strong>
                <p>当前账号状态。</p>
              </div>
              <span class="workspace-pill">{{ hasToken ? '有效' : '失效' }}</span>
            </div>
          </div>
        </WorkspacePanel>

        <WorkspacePanel v-else title="偏好设置" compact>
          <div class="teacher-preference-panel">
            <div class="teacher-setting-row">
              <div>
                <strong>默认进入班级工作区</strong>
              </div>
              <el-switch v-model="preferences.defaultClassEntry" />
            </div>
            <div class="teacher-setting-row">
              <div>
                <strong>优先显示未解决申请</strong>
              </div>
              <el-switch v-model="preferences.focusPendingApplications" />
            </div>
            <div class="teacher-setting-row">
              <div>
                <strong>通知提醒</strong>
              </div>
              <el-switch v-model="preferences.enableNoticeHint" />
            </div>
          </div>
        </WorkspacePanel>
      </div>
    </template>

    <template v-else>
      <div class="workspace-grid workspace-grid--two">
        <WorkspacePanel title="账号信息" subtitle="当前登录账号的基础资料">
          <div class="profile-grid" v-loading="loading">
            <div class="profile-avatar-card">
              <el-avatar :size="72" :src="userInfo.avatar">{{ avatarText }}</el-avatar>
              <h3>{{ userInfo.nickname || userInfo.username || '未命名用户' }}</h3>
              <p>{{ formatRole(userInfo.role) }}</p>
            </div>
            <div class="profile-info-list">
              <div class="profile-info-row"><span>用户名</span><strong>{{ userInfo.username || '-' }}</strong></div>
              <div class="profile-info-row"><span>UID</span><strong>{{ userInfo.uid || '-' }}</strong></div>
              <div class="profile-info-row"><span>姓名</span><strong>{{ userInfo.nickname || '-' }}</strong></div>
              <div class="profile-info-row"><span>邮箱</span><strong>{{ userInfo.email || '-' }}</strong></div>
              <div class="profile-info-row"><span>角色</span><strong>{{ formatRole(userInfo.role) }}</strong></div>
            </div>
          </div>
        </WorkspacePanel>

        <WorkspacePanel title="安全设置" subtitle="密码修改与登录安全" soft>
          <div class="workspace-list">
            <div class="workspace-list-item">
              <div>
                <p class="workspace-list-item__title">密码</p>
                <p class="workspace-list-item__meta">建议定期更换，修改后需重新登录。</p>
              </div>
              <el-button type="primary" @click="dialogVisible = true">修改密码</el-button>
            </div>
            <div class="workspace-list-item">
              <div>
                <p class="workspace-list-item__title">当前令牌</p>
                <p class="workspace-list-item__meta">使用本地存储维护登录状态。</p>
              </div>
              <span class="workspace-pill">{{ hasToken ? '已登录' : '未登录' }}</span>
            </div>
          </div>
        </WorkspacePanel>
      </div>
    </template>

    <el-dialog v-model="dialogVisible" title="修改密码" width="520px" @close="resetForm">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="rules" label-position="top">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUpdatePassword">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import WorkspacePanel from '../components/workspace/WorkspacePanel.vue'

const userInfo = ref({})
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const pwdFormRef = ref(null)
const activeSection = ref('profile')
const hasToken = computed(() => Boolean(localStorage.getItem('satoken')))
const avatarText = computed(() => (userInfo.value.nickname || userInfo.value.username || 'U').slice(0, 1).toUpperCase())
const isTeacher = computed(() => userInfo.value.role === 'TEACHER')
const preferenceStorageKey = computed(() => `teacherProfilePrefs_${userInfo.value.id || userInfo.value.username || 'default'}`)

const teacherSections = [
  { key: 'profile', label: '个人资料' },
  { key: 'security', label: '密码安全' },
  { key: 'preferences', label: '偏好设置' }
]

const preferences = reactive({
  defaultClassEntry: false,
  focusPendingApplications: true,
  enableNoticeHint: true
})

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirmPwd = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) callback(new Error('两次输入的新密码不一致'))
  else callback()
}

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

watch(preferenceStorageKey, (key) => {
  if (!key) return
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return
    Object.assign(preferences, JSON.parse(raw))
  } catch (error) {
    localStorage.removeItem(key)
  }
}, { immediate: true })

watch(preferences, (value) => {
  if (!isTeacher.value || !preferenceStorageKey.value) return
  localStorage.setItem(preferenceStorageKey.value, JSON.stringify(value))
}, { deep: true })

const fetchUserInfo = async () => {
  loading.value = true
  try {
    userInfo.value = await request.get('/user/info')
  } finally {
    loading.value = false
  }
}

const handleUpdatePassword = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await request.post('/user/password', {
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword
      })
      ElMessage.success('密码修改成功，请重新登录')
      localStorage.removeItem('user')
      localStorage.removeItem('satoken')
      window.location.href = '/login'
    } finally {
      submitting.value = false
    }
  })
}

const resetForm = () => {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdFormRef.value?.clearValidate()
}

const formatRole = (role) => ({ ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[role] || '-')

onMounted(fetchUserInfo)
</script>

<style scoped>
.profile-page--teacher {
  height: 100%;
}

.teacher-settings-single {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 100%;
}

.teacher-settings-single__nav {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(29, 35, 43, 0.05);
  width: fit-content;
}

.teacher-settings-single__tab {
  border: 0;
  background: transparent;
  color: var(--text-soft);
  padding: 10px 18px;
  border-radius: 999px;
  cursor: pointer;
  transition: all var(--transition-soft);
}

.teacher-settings-single__tab.active {
  background: #121926;
  color: #fff;
  box-shadow: 0 12px 24px rgba(18, 25, 38, 0.12);
}

.teacher-profile-panel {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
}

.teacher-profile-panel__identity {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px;
  border-radius: 22px;
  background: rgba(247, 249, 252, 0.88);
}

.teacher-profile-panel__identity h3,
.teacher-profile-panel__identity p {
  margin: 0;
}

.teacher-profile-panel__identity p,
.teacher-profile-panel__field span,
.teacher-setting-row p,
.profile-avatar-card p,
.profile-info-row span {
  color: var(--text-soft);
}

.teacher-profile-panel__fields,
.teacher-security-panel,
.teacher-preference-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.teacher-profile-panel__field,
.teacher-setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(247, 249, 252, 0.88);
  border: 1px solid rgba(29, 35, 43, 0.05);
}

.teacher-setting-row {
  align-items: flex-start;
}

.teacher-setting-row p {
  margin: 6px 0 0;
}

.profile-grid {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 20px;
}

.profile-avatar-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 240px;
  border-radius: 24px;
  background: rgba(248, 250, 251, 0.84);
}

.profile-avatar-card h3,
.profile-avatar-card p {
  margin: 0;
}

.profile-info-list {
  display: flex;
  flex-direction: column;
}

.profile-info-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 0;
  border-bottom: 1px solid rgba(29, 35, 43, 0.06);
}

@media (max-width: 980px) {
  .teacher-profile-panel,
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .teacher-settings-single__nav {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>

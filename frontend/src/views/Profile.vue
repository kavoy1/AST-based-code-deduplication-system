<template>
  <div class="profile-container">
    <div class="profile-sidebar">
      <div class="back-home" @click="$router.push('/home')">
        <el-icon><ArrowLeft /></el-icon> 首页
      </div>
      <div class="menu-item active">
        <el-icon><User /></el-icon> 账号
      </div>
    </div>
    
    <div class="profile-content">
      <h2 class="page-title">账号信息</h2>
      
      <div class="info-card">
        <div class="info-row">
            <div class="info-label">用户名</div>
            <div class="info-value">{{ userInfo.username }}</div>
        </div>
        <div class="info-row">
            <div class="info-label">UID</div>
            <div class="info-value">
                <el-tag size="small" effect="plain">{{ userInfo.uid || '未设置' }}</el-tag>
            </div>
        </div>
        <div class="info-row">
            <div class="info-label">姓名</div>
            <div class="info-value">{{ userInfo.nickname }}</div>
        </div>
        <div class="info-row">
            <div class="info-label">邮箱</div>
            <div class="info-value">{{ userInfo.email }}</div>
        </div>
        <div class="info-row">
            <div class="info-label">角色</div>
            <div class="info-value">{{ formatRole(userInfo.role) }}</div>
        </div>
        <div class="info-row">
            <div class="info-label">密码</div>
            <div class="info-value password-value">
                <span>********</span>
                <span class="modify-pwd-btn" @click="dialogVisible = true">修改密码</span>
            </div>
        </div>
      </div>

      <el-dialog v-model="dialogVisible" title="修改密码" width="500px" @close="resetForm">
        <el-form :model="pwdForm" :rules="rules" ref="pwdFormRef" label-width="100px">
            <el-form-item label="旧密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
        </el-form>
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleUpdatePassword" :loading="loading">确定</el-button>
            </span>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../api/request'
import { ElMessage } from 'element-plus'

const userInfo = ref({})
const loading = ref(false)
const dialogVisible = ref(false)
const pwdFormRef = ref(null)

const pwdForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

const validateConfirmPwd = (rule, value, callback) => {
    if (value !== pwdForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
    } else {
        callback()
    }
}

const rules = {
    oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
    newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请确认新密码', trigger: 'blur' },
        { validator: validateConfirmPwd, trigger: 'blur' }
    ]
}

const fetchUserInfo = async () => {
    try {
        const res = await request.get('/user/info')
        userInfo.value = res
    } catch (e) {
        console.error(e)
    }
}

const handleUpdatePassword = async () => {
    if (!pwdFormRef.value) return
    await pwdFormRef.value.validate(async (valid) => {
        if (valid) {
            loading.value = true
            try {
                await request.post('/user/password', {
                    oldPassword: pwdForm.oldPassword,
                    newPassword: pwdForm.newPassword
                })
                ElMessage.success('密码修改成功，请重新登录')
                // 登出逻辑
                localStorage.removeItem('user')
                localStorage.removeItem('satoken')
                setTimeout(() => {
                    window.location.href = '/login'
                }, 1000)
            } catch (e) {
                console.error(e)
            } finally {
                loading.value = false
            }
        }
    })
}

const formatRole = (role) => {
    const map = {
        'ADMIN': '管理员',
        'TEACHER': '教师',
        'STUDENT': '学生'
    }
    return map[role] || role
}

const resetForm = () => {
    if (pwdFormRef.value) {
        pwdFormRef.value.resetFields()
    }
}

onMounted(() => {
    fetchUserInfo()
})
</script>

<style scoped>
.profile-container {
    display: flex;
    min-height: 100vh;
    background-color: var(--bg-color);
}

.profile-sidebar {
    width: 240px;
    background-color: var(--card-bg);
    border-right: 1px solid var(--border-color);
    padding: 20px 0;
    display: flex;
    flex-direction: column;
}

.back-home {
    padding: 12px 24px;
    cursor: pointer;
    color: var(--text-muted);
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
    margin-bottom: 20px;
}
.back-home:hover {
    color: var(--primary-color);
}

.menu-item {
    padding: 12px 24px;
    display: flex;
    align-items: center;
    gap: 12px;
    color: var(--text-main);
    cursor: pointer;
}

.menu-item.active {
    background-color: rgba(19, 91, 236, 0.1);
    color: var(--primary-color);
    border-right: 3px solid var(--primary-color);
}

.profile-content {
    flex: 1;
    padding: 40px 60px;
    background-color: var(--card-bg);
    margin: 20px;
    border-radius: var(--card-radius);
    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.page-title {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-main);
    margin-bottom: 24px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--border-color);
}

.info-card {
    border: 1px solid var(--border-color);
    border-radius: var(--card-radius);
    padding: 0;
    margin-bottom: 40px;
}

.info-row {
    display: flex;
    align-items: center;
    padding: 20px 24px;
    border-bottom: 1px solid var(--border-color);
}

.info-row:last-child {
    border-bottom: none;
}

.info-label {
    width: 120px;
    font-size: 14px;
    color: var(--text-muted);
    font-weight: 500;
}

.info-value {
    flex: 1;
    font-size: 14px;
    color: var(--text-main);
}

.password-value {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.modify-pwd-btn {
    font-size: 12px;
    color: #f56c6c;
    background-color: #fef0f0;
    border: 1px solid #fde2e2;
    padding: 4px 12px;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s;
}

.modify-pwd-btn:hover {
    background-color: #f56c6c;
    color: white;
    border-color: #f56c6c;
}
</style>

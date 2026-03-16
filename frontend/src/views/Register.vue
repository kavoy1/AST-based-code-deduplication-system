<template>
  <div class="register-container">
    <!-- 动态星空背景 -->
    <div class="stars-container">
      <div id="stars"></div>
      <div id="stars2"></div>
      <div id="stars3"></div>
    </div>

    <div class="form-wrapper">
      <el-form 
        :model="registerForm" 
        :rules="rules" 
        ref="registerRef" 
        class="form"
        label-width="0"
      >
        <div class="logo-area">
          <div class="preloader"> 
            <div class="crack crack1"></div> 
            <div class="crack crack2"></div> 
            <div class="crack crack3"></div> 
            <div class="crack crack4"></div> 
            <div class="crack crack5"></div> 
          </div> 
        </div>
        
        <p id="heading">加入 AST</p>

        <!-- 第一步：基本信息 -->
        <div v-show="step === 1">
          <!-- 用户名 -->
          <div class="field">
            <el-icon class="input-icon"><User /></el-icon>
            <el-form-item prop="user.username" class="inner-item">
              <input 
                v-model="registerForm.user.username" 
                placeholder="用户名" 
                class="input-field" 
                type="text" 
              />
            </el-form-item>
          </div>

          <!-- 密码 -->
          <div class="field">
            <el-icon class="input-icon"><Lock /></el-icon>
            <el-form-item prop="user.password" class="inner-item">
              <input 
                v-model="registerForm.user.password" 
                placeholder="密码 (至少6位)" 
                class="input-field" 
                type="password" 
              />
            </el-form-item>
          </div>

          <!-- 真实姓名 -->
          <div class="field">
            <el-icon class="input-icon"><Postcard /></el-icon>
            <el-form-item prop="user.nickname" class="inner-item">
              <input 
                v-model="registerForm.user.nickname" 
                placeholder="真实姓名" 
                class="input-field" 
                type="text" 
              />
            </el-form-item>
          </div>

          <!-- 邮箱 -->
          <div class="field">
            <el-icon class="input-icon"><Message /></el-icon>
            <el-form-item prop="user.email" class="inner-item">
              <input 
                v-model="registerForm.user.email" 
                placeholder="电子邮箱" 
                class="input-field" 
                type="text" 
              />
            </el-form-item>
          </div>

          <!-- 验证码 -->
          <div class="field code-field">
            <el-icon class="input-icon"><Key /></el-icon>
            <el-form-item prop="emailCode" class="inner-item">
              <input 
                v-model="registerForm.emailCode" 
                placeholder="邮箱验证码" 
                class="input-field" 
                type="text" 
              />
            </el-form-item>
            <button 
                type="button" 
                class="send-code-btn" 
                :disabled="codeLoading || countdown > 0"
                @click="handleSendCode"
            >
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </button>
          </div>

          <!-- 角色选择 -->
          <div class="role-selection">
              <div 
                  class="role-option" 
                  :class="{ active: registerForm.user.role === 'STUDENT' }"
                  @click="registerForm.user.role = 'STUDENT'"
              >
                  <el-icon><User /></el-icon> 学生
              </div>
              <div 
                  class="role-option" 
                  :class="{ active: registerForm.user.role === 'TEACHER' }"
                  @click="registerForm.user.role = 'TEACHER'"
              >
                  <el-icon><User /></el-icon> 教师
              </div>
          </div>

          <!-- 邀请码 (仅教师) -->
          <transition name="el-zoom-in-top">
            <div class="field" v-if="registerForm.user.role === 'TEACHER'">
              <el-icon class="input-icon"><Key /></el-icon>
              <el-form-item prop="inviteCode" class="inner-item">
                <input 
                  v-model="registerForm.inviteCode" 
                  placeholder="教师邀请码" 
                  class="input-field" 
                  type="text" 
                />
              </el-form-item>
            </div>
          </transition>
        </div>

        <!-- 第二步：详细信息 -->
        <div v-show="step === 2">
            <p class="sub-heading">完善{{ registerForm.user.role === 'STUDENT' ? '学生' : '教师' }}信息</p>
            
            <!-- 学生信息字段 -->
            <template v-if="registerForm.user.role === 'STUDENT'">
                <div class="field">
                    <el-icon class="input-icon"><User /></el-icon>
                    <el-form-item prop="studentInfo.school" class="inner-item">
                        <input v-model="registerForm.studentInfo.school" placeholder="学校名称" class="input-field" type="text" />
                    </el-form-item>
                </div>

                <div class="field">
                    <el-icon class="input-icon"><User /></el-icon>
                    <el-form-item prop="studentInfo.college" class="inner-item">
                        <input v-model="registerForm.studentInfo.college" placeholder="所属学院" class="input-field" type="text" />
                    </el-form-item>
                </div>

                <div class="field">
                    <el-icon class="input-icon"><User /></el-icon>
                    <el-form-item prop="studentInfo.major" class="inner-item">
                        <input v-model="registerForm.studentInfo.major" placeholder="专业名称" class="input-field" type="text" />
                    </el-form-item>
                </div>

                <div class="field">
                    <el-icon class="input-icon"><User /></el-icon>
                    <el-form-item prop="studentInfo.className" class="inner-item">
                        <input v-model="registerForm.studentInfo.className" placeholder="行政班级" class="input-field" type="text" />
                    </el-form-item>
                </div>

                <div class="field">
                    <el-icon class="input-icon"><User /></el-icon>
                    <el-form-item prop="studentInfo.studentNumber" class="inner-item">
                        <input v-model="registerForm.studentInfo.studentNumber" placeholder="学号" class="input-field" type="text" />
                    </el-form-item>
                </div>
            </template>

            <!-- 教师信息字段 -->
            <template v-if="registerForm.user.role === 'TEACHER'">
                <div class="field">
                    <el-icon class="input-icon"><User /></el-icon>
                    <el-form-item prop="teacherInfo.school" class="inner-item">
                        <input v-model="registerForm.teacherInfo.school" placeholder="学校名称" class="input-field" type="text" />
                    </el-form-item>
                </div>

                <div class="field">
                    <el-icon class="input-icon"><User /></el-icon>
                    <el-form-item prop="teacherInfo.college" class="inner-item">
                        <input v-model="registerForm.teacherInfo.college" placeholder="所属学院" class="input-field" type="text" />
                    </el-form-item>
                </div>

                <div class="field">
                    <el-icon class="input-icon"><User /></el-icon>
                    <el-form-item prop="teacherInfo.teacherNumber" class="inner-item">
                        <input v-model="registerForm.teacherInfo.teacherNumber" placeholder="教师编号" class="input-field" type="text" />
                    </el-form-item>
                </div>
            </template>
        </div>

        <div class="btn">
          <!-- 按钮逻辑 -->
          <template v-if="step === 1">
              <button 
                class="button1" 
                type="button" 
                @click="handleNext" 
                :disabled="loading"
              >
                下一步
              </button>
          </template>
          <template v-else>
              <button class="button2" type="button" @click="step = 1" :disabled="loading">上一步</button>
              <button class="button1" type="button" @click="handleRegister" :disabled="loading">
                {{ loading ? '注册中...' : '完成注册' }}
              </button>
          </template>
          
          <button v-if="step === 1" class="button2" type="button" @click="router.push('/login')">返回登录</button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import { User, Lock, Postcard, Message, Key } from '@element-plus/icons-vue'

const router = useRouter()
const registerRef = ref(null)
const loading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)
const step = ref(1)

// 数据结构完全匹配后端 RegisterDTO
const registerForm = reactive({
  user: {
    username: '',
    password: '',
    nickname: '',
    email: '',
    role: 'STUDENT'
  },
  emailCode: '',
  inviteCode: '',
  studentInfo: {
      school: '',
      college: '',
      major: '',
      className: '',
      studentNumber: ''
  },
  teacherInfo: {
      school: '',
      college: '',
      teacherNumber: ''
  }
})

// 校验规则
const rules = {
  'user.username': [
    { required: true, message: '用户名不能为空', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  'user.password': [
    { required: true, message: '密码不能为空', trigger: 'blur' },
    { min: 6, message: '密码长度至少为 6 位', trigger: 'blur' }
  ],
  'user.nickname': [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  'user.email': [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  inviteCode: [
    { required: true, message: '教师注册必须填写邀请码', trigger: 'blur' }
  ],
  // 学生信息校验
  'studentInfo.school': [{ required: true, message: '请输入学校', trigger: 'blur' }],
  'studentInfo.college': [{ required: true, message: '请输入学院', trigger: 'blur' }],
  'studentInfo.major': [{ required: true, message: '请输入专业', trigger: 'blur' }],
  'studentInfo.className': [{ required: true, message: '请输入班级', trigger: 'blur' }],
  'studentInfo.studentNumber': [{ required: true, message: '请输入学号', trigger: 'blur' }],
  // 教师信息校验
  'teacherInfo.school': [{ required: true, message: '请输入学校', trigger: 'blur' }],
  'teacherInfo.college': [{ required: true, message: '请输入学院', trigger: 'blur' }],
  'teacherInfo.teacherNumber': [{ required: true, message: '请输入教师编号', trigger: 'blur' }]
}

const handleSendCode = async () => {
    // 校验邮箱格式
    try {
        await registerRef.value.validateField('user.email')
    } catch (e) {
        return
    }

    codeLoading.value = true
    try {
        await request.post('/code', null, {
            params: { email: registerForm.user.email }
        })
        ElMessage.success('验证码已发送，请查收邮箱')
        
        // 开始倒计时
        countdown.value = 60
        const timer = setInterval(() => {
            countdown.value--
            if (countdown.value <= 0) {
                clearInterval(timer)
            }
        }, 1000)
        
    } catch (e) {
        console.error(e)
    } finally {
        codeLoading.value = false
    }
}

const handleNext = async () => {
    // 无论是学生还是教师，都先校验第一步的字段
    if (!registerRef.value) return
    
    // 手动校验第一步的字段
    const fieldsToValidate = ['user.username', 'user.password', 'user.nickname', 'user.email', 'emailCode']
    
    // 如果是教师，还需要校验邀请码
    if (registerForm.user.role === 'TEACHER') {
        fieldsToValidate.push('inviteCode')
    }

    let valid = true
    for (const field of fieldsToValidate) {
        try {
            await registerRef.value.validateField(field)
        } catch (e) {
            valid = false
        }
    }

    if (!valid) {
        ElMessage.warning('请先完善基本信息')
        return
    }

    // 调用后端接口校验验证码
    try {
        loading.value = true
        await request.post('/verify-code', null, {
            params: { 
                email: registerForm.user.email,
                code: registerForm.emailCode
            }
        })
        step.value = 2
    } catch (e) {
        console.error(e)
        // 错误信息已由拦截器处理，或在此处显示
        // ElMessage.error(e.message || '验证码错误')
    } finally {
        loading.value = false
    }
}

const handleRegister = () => {
  registerRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await request.post('/register', registerForm)
        
        ElMessage.success({
          message: '注册成功！正在跳转登录页...',
          type: 'success',
          duration: 2000
        })
        
        setTimeout(() => {
          router.push('/login')
        }, 1500)
        
      } catch (error) {
        console.error('注册失败:', error)
      } finally {
        loading.value = false
      }
    } else {
      ElMessage.warning('请检查表单填写是否正确')
      return false
    }
  })
}
</script>

<style scoped lang="scss">
@use "sass:math";
@use "sass:string";

.register-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  background: radial-gradient(ellipse at bottom, #1B2735 0%, #090A0F 100%);
  position: relative;
  overflow: hidden;
}

/* 星空背景 SCSS 逻辑 */
@function multiple-box-shadow ($n) {
  $value: '#{math.random(2000)}px #{math.random(2000)}px #FFF';
  @for $i from 2 through $n {
    $value: '#{$value} , #{math.random(2000)}px #{math.random(2000)}px #FFF';
  }
  @return string.unquote($value);
}

$shadows-small:  multiple-box-shadow(700);
$shadows-medium: multiple-box-shadow(200);
$shadows-big:    multiple-box-shadow(100);

.stars-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

#stars {
  width: 1px;
  height: 1px;
  background: transparent;
  box-shadow: $shadows-small;
  animation: animStar 50s linear infinite;

  &:after {
    content: " ";
    position: absolute;
    top: 2000px;
    width: 1px;
    height: 1px;
    background: transparent;
    box-shadow: $shadows-small;
  }
}

#stars2 {
  width: 2px;
  height: 2px;
  background: transparent;
  box-shadow: $shadows-medium;
  animation: animStar 100s linear infinite;

  &:after {
    content: " ";
    position: absolute;
    top: 2000px;
    width: 2px;
    height: 2px;
    background: transparent;
    box-shadow: $shadows-medium;
  }
}

#stars3 {
  width: 3px;
  height: 3px;
  background: transparent;
  box-shadow: $shadows-big;
  animation: animStar 150s linear infinite;

  &:after {
    content: " ";
    position: absolute;
    top: 2000px;
    width: 3px;
    height: 3px;
    background: transparent;
    box-shadow: $shadows-big;
  }
}

@keyframes animStar {
  from {
    transform: translateY(0px);
  }
  to {
    transform: translateY(-2000px);
  }
}

/* 表单容器 */
.form-wrapper {
  position: relative;
  z-index: 10;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 15px; /* 增加输入栏间距 */
  padding: 3em 2.5em; /* 增加内边距 */
  background-color: #171717;
  border-radius: 25px;
  transition: .4s ease-in-out;
  width: 450px; /* 增加宽度 */
  box-shadow: 0 20px 50px rgba(0,0,0,0.5);
}

.form:hover {
  transform: scale(1.02);
  border: 1px solid #222;
}

.logo-area {
  width: 100%;
  height: 60px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 1em;
}

/* Logo 动画 */
.preloader { 
  position: relative; 
  width: 100%; 
  height: 100%; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  filter: drop-shadow(0 0 5px rgba(255, 255, 255, 0.4)); 
} 
 
.crack { 
  position: absolute; 
  width: 25px; 
  aspect-ratio: 1; 
  background-color: #fef3fc; 
  clip-path: polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%); 
  animation: star-rotate 6s infinite linear; 
} 
 
.crack2 { width: 35px; animation-delay: 1s; opacity: 0.8; } 
.crack3 { width: 45px; animation-delay: 1.5s; opacity: 0.6; } 
.crack4 { width: 55px; animation-delay: 2s; opacity: 0.4; } 
.crack5 { width: 65px; animation-delay: 2.5s; opacity: 0.2; } 
 
@keyframes star-rotate { 
  to { transform: rotate(360deg); } 
} 

#heading {
  text-align: center;
  margin: 0 0 1.5em 0;
  color: rgb(255, 255, 255);
  font-size: 1.5em;
  font-weight: 700;
  letter-spacing: 1px;
}

.sub-heading {
    text-align: center;
    color: #ccc;
    margin-bottom: 1em;
    font-size: 1.1em;
}

.field {
  display: flex;
  align-items: center;
  gap: 0.5em;
  border-radius: 25px;
  padding: 0.8em; /* 增加输入框高度 */
  border: none;
  outline: none;
  color: white;
  background-color: #171717;
  box-shadow: inset 2px 5px 10px rgb(5, 5, 5);
  margin-bottom: 8px; /* 增加底部间距 */
}

.inner-item {
  margin-bottom: 0 !important;
  width: 100%;
}

:deep(.el-form-item__content) {
  line-height: normal;
}

.input-icon {
  height: 1.3em;
  width: 1.3em;
  color: white;
  flex-shrink: 0;
  margin-left: 5px;
}

.input-field {
  background: none;
  border: none;
  outline: none;
  width: 100%;
  color: #d3d3d3;
  font-size: 14px;
}

/* 解决自动填充背景 */
.input-field:-webkit-autofill {
  -webkit-box-shadow: 0 0 0px 1000px #171717 inset !important;
  -webkit-text-fill-color: #d3d3d3 !important;
  transition: background-color 5000s ease-in-out 0s;
}

/* 角色选择样式 */
.role-selection {
    display: flex;
    gap: 15px;
    margin: 10px 0;
}

.role-option {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px 10px; /* 增加角色选择按钮高度 */
    border-radius: 15px;
    background-color: #252525;
    color: #888;
    cursor: pointer;
    transition: all 0.3s ease;
    border: 1px solid transparent;
}

.role-option:hover {
    background-color: #333;
    color: white;
}

.role-option.active {
    background-color: #171717;
    color: white;
    border: 1px solid #555;
    box-shadow: inset 2px 5px 10px rgb(5, 5, 5);
}

.code-field {
    display: flex;
    gap: 10px;
}

.send-code-btn {
    padding: 0 15px;
    height: 100%;
    border-radius: 20px;
    border: none;
    background-color: #252525;
    color: white;
    cursor: pointer;
    font-size: 13px;
    white-space: nowrap;
    transition: all 0.3s;
}

.send-code-btn:hover:not(:disabled) {
    background-color: #333;
}

.send-code-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.btn {
  display: flex;
  justify-content: center;
  flex-direction: row;
  margin-top: 1.5em;
  gap: 10px;
}

.button1, .button2 {
  flex: 1;
  padding: 0.8em;
  border-radius: 8px;
  border: none;
  outline: none;
  transition: .4s ease-in-out;
  background-color: #252525;
  color: white;
  cursor: pointer;
  font-weight: 600;
}

.button1:hover, .button2:hover {
  background-color: black;
  box-shadow: 0 0 15px rgba(255,255,255,0.1);
}

.button1:disabled {
    opacity: 0.7;
    cursor: not-allowed;
}
</style>

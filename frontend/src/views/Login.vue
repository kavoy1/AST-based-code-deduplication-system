<template>
  <div class="login-container">
    <!-- 动态星空背景 -->
    <div class="stars-container">
      <div id="stars"></div>
      <div id="stars2"></div>
      <div id="stars3"></div>
    </div>

    <div class="form-wrapper">
      <transition name="fade-scale" mode="out-in">
        <!-- 登录表单 -->
        <el-form v-if="!isResetting" :model="loginForm" :rules="loginRules" ref="loginRef" class="form" key="login">
          <div class="logo-area">
            <div class="preloader"> 
              <div class="crack crack1"></div> 
              <div class="crack crack2"></div> 
              <div class="crack crack3"></div> 
              <div class="crack crack4"></div> 
              <div class="crack crack5"></div> 
            </div> 
          </div>
          <p id="heading">AST 智能查重</p>
          <div class="field">
            <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
              <path d="M13.106 7.222c0-2.967-2.249-5.032-5.482-5.032-3.35 0-5.646 2.318-5.646 5.702 0 3.493 2.235 5.708 5.762 5.708.862 0 1.689-.123 2.304-.335v-.862c-.43.199-1.354.328-2.29.328-2.926 0-4.813-1.88-4.813-4.798 0-2.844 1.921-4.881 4.594-4.881 2.735 0 4.608 1.688 4.608 4.156 0 1.682-.554 2.769-1.416 2.769-.492 0-.772-.28-.772-.76V5.206H8.923v.834h-.11c-.266-.595-.881-.964-1.6-.964-1.4 0-2.378 1.162-2.378 2.823 0 1.737.957 2.906 2.379 2.906.8 0 1.415-.39 1.709-1.087h.11c.081.67.703 1.148 1.503 1.148 1.572 0 2.57-1.415 2.57-3.643zm-7.177.704c0-1.197.54-1.907 1.456-1.907.93 0 1.524.738 1.524 1.907S8.308 9.84 7.371 9.84c-.895 0-1.442-.725-1.442-1.914z"></path>
            </svg>
            <el-form-item prop="username" class="inner-item">
              <input autocomplete="off" v-model="loginForm.username" placeholder="用户名" class="input-field" type="text" @keyup.enter="handleLogin">
            </el-form-item>
          </div>
          <div class="field">
            <svg class="input-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
              <path d="M8 1a2 2 0 0 1 2 2v4H6V3a2 2 0 0 1 2-2zm3 6V3a3 3 0 0 0-6 0v4a2 2 0 0 0-2 2v5a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2z"></path>
            </svg>
            <el-form-item prop="password" class="inner-item">
              <input v-model="loginForm.password" placeholder="密码" class="input-field" type="password" @keyup.enter="handleLogin">
            </el-form-item>
          </div>
          
          <div class="remember-me">
            <CustomCheckbox v-model="loginForm.rememberMe" size="12px" />
            <span class="remember-text">记住我</span>
          </div>

          <div class="btn">
            <button class="button1" type="button" @click="handleLogin" :disabled="loading">{{ loading ? '...' : '立即登录' }}</button>
            <button class="button2" type="button" @click="router.push('/register')">申请加入</button>
          </div>
          <button class="button3" type="button" @click="isResetting = true">忘记密码？</button>
        </el-form>

        <!-- 找回密码表单 -->
        <el-form v-else :model="resetForm" :rules="resetRules" ref="resetRef" class="form" key="reset">
          <p id="heading">找回密码</p>
          <div class="field">
            <el-form-item prop="email" class="inner-item">
              <div class="input-with-action">
                <input v-model="resetForm.email" placeholder="注册邮箱" class="input-field" type="text">
                <button type="button" class="action-link" @click="sendCode" :disabled="codeDisabled">{{ codeBtnText }}</button>
              </div>
            </el-form-item>
          </div>
          <div class="field">
            <el-form-item prop="newPassword" class="inner-item">
              <input v-model="resetForm.newPassword" placeholder="设置新密码" class="input-field" type="password">
            </el-form-item>
          </div>
          <div class="field">
            <el-form-item prop="code" class="inner-item">
              <input v-model="resetForm.code" placeholder="6位验证码" class="input-field" type="text">
            </el-form-item>
          </div>
          <div class="btn">
            <button class="button1" type="button" @click="handleReset" :disabled="loading">重置并登录</button>
          </div>
          <button class="button3" type="button" @click="isResetting = false">返回登录</button>
        </el-form>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const router = useRouter()
const isResetting = ref(false)
const loading = ref(false)

// Login Logic
const loginRef = ref(null)
const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = () => {
  loginRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await request.post('/login', loginForm)
        localStorage.setItem('user', JSON.stringify(res.user))
        localStorage.setItem('latestNotice', JSON.stringify(res.notice))
        if (res?.token) {
          localStorage.setItem('satoken', res.token)
        }
        sessionStorage.setItem('showNotice', 'true')
        ElMessage.success('欢迎回来！')
        router.push('/home')
      } catch (e) {
        console.error(e)
      } finally {
        loading.value = false
      }
    }
  })
}

// Reset Logic
const resetRef = ref(null)
const resetForm = reactive({
  email: '',
  newPassword: '',
  code: ''
})
const resetRules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur', type: 'email' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const codeBtnText = ref('发送验证码')
const codeDisabled = ref(false)
let timer = null

const sendCode = async () => {
  if (!resetForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  try {
    await request.post('/code', null, { params: { email: resetForm.email } })
    ElMessage.success('验证码已发送！')
    codeDisabled.value = true
    let count = 60
    codeBtnText.value = `${count}s`
    timer = setInterval(() => {
      count--
      codeBtnText.value = `${count}s`
      if (count <= 0) {
        clearInterval(timer)
        codeBtnText.value = '发送验证码'
        codeDisabled.value = false
      }
    }, 1000)
  } catch (e) {
    console.error(e)
  }
}

const handleReset = () => {
  resetRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await request.post('/reset-password', resetForm)
        ElMessage.success('密码重置成功！')
        isResetting.value = false
      } catch (e) {
        console.error(e)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
@use "sass:math";
@use "sass:string";
.login-container {
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

/* 用户提供的模板样式 */
.form-wrapper {
  position: relative;
  z-index: 10;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-left: 2em;
  padding-right: 2em;
  padding-bottom: 0.4em;
  background-color: #171717;
  border-radius: 25px;
  transition: .4s ease-in-out;
  width: 360px;
  box-shadow: 0 20px 50px rgba(0,0,0,0.5);
}

.form:hover {
  transform: scale(1.05);
  border: 1px solid #222;
}

.logo-area {
  width: 100%;
  height: 80px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 2em;
}

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
  clip-path: polygon( 
    50% 0%, 
    61% 35%, 
    98% 35%, 
    68% 57%, 
    79% 91%, 
    50% 70%, 
    21% 91%, 
    32% 57%, 
    2% 35%, 
    39% 35% 
  ); 
  animation: star-rotate 6s infinite linear; 
} 
 
.crack2 { width: 35px; animation-delay: 1s; opacity: 0.8; } 
.crack3 { width: 45px; animation-delay: 1.5s; opacity: 0.6; } 
.crack4 { width: 55px; animation-delay: 2s; opacity: 0.4; } 
.crack5 { width: 65px; animation-delay: 2.5s; opacity: 0.2; } 
 
@keyframes star-rotate { 
  to { 
    transform: rotate(360deg); 
  } 
} 

#heading {
  text-align: center;
  margin: 1.5em 2em;
  color: rgb(255, 255, 255);
  font-size: 1.5em;
  font-weight: 700;
  letter-spacing: 1px;
}

.field {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5em;
  border-radius: 25px;
  padding: 0.6em;
  border: none;
  outline: none;
  color: white;
  background-color: #171717;
  box-shadow: inset 2px 5px 10px rgb(5, 5, 5);
  margin-bottom: 10px;
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
  fill: white;
  flex-shrink: 0;
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

.form .btn {
  display: flex;
  justify-content: center;
  flex-direction: row;
  margin-top: 2.5em;
  gap: 10px;
}

.button1, .button2, .button3 {
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

.button1 { flex: 1; }
.button2 { flex: 1; }

.button1:hover, .button2:hover {
  background-color: black;
  box-shadow: 0 0 15px rgba(255,255,255,0.1);
}

.button3 {
  margin-top: 1em;
  margin-bottom: 3em;
  color: #888;
  font-size: 13px;
}

.button3:hover {
  background-color: #dc2626;
  color: white;
}

.input-with-action {
  display: flex;
  align-items: center;
  width: 100%;
}

.action-link {
  background: transparent;
  border: none;
  color: #6366f1;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
  padding-left: 10px;
}

.action-link:hover:not(:disabled) {
  text-decoration: underline;
}

.action-link:disabled {
  color: #444;
}

.fade-scale-enter-active, .fade-scale-leave-active {
  transition: all 0.4s ease;
}
.fade-scale-enter-from { opacity: 0; transform: scale(0.9); }
.fade-scale-leave-to { opacity: 0; transform: scale(1.1); }

.remember-me {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 5px;
  padding-left: 5px;
}

.remember-text {
  color: #d3d3d3;
  font-size: 14px;
  cursor: pointer;
}
</style>

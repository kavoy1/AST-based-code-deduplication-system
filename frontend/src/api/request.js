import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { clearAuthSession, getAccessToken, setAccessToken } from './authStorage'

const request = axios.create({
  baseURL: '/api',
  timeout: 5000,
  withCredentials: true
})

const refreshClient = axios.create({
  baseURL: '/api',
  timeout: 5000,
  withCredentials: true
})

let refreshPromise = null

function redirectToLogin() {
  if (router.currentRoute.value.path !== '/login') {
    router.push('/login')
  }
}

function shouldSkipRefresh(config = {}) {
  const url = String(config.url || '')
  return config._retry || url.includes('/login') || url.includes('/auth/refresh') || url.includes('/logout')
}

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = refreshClient.post('/auth/refresh')
      .then((response) => {
        const res = response.data
        if (!(res.code === 200 || res.success)) {
          throw new Error(res.msg || '刷新登录状态失败')
        }
        const payload = res.data !== undefined ? res.data : res
        if (!payload?.accessToken) {
          throw new Error('刷新接口未返回访问令牌')
        }
        setAccessToken(payload.accessToken)
        return payload.accessToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

async function retryWithRefresh(config) {
  if (shouldSkipRefresh(config)) {
    throw new Error('refresh skipped')
  }
  config._retry = true
  try {
    const nextAccessToken = await refreshAccessToken()
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${nextAccessToken}`
    return request(config)
  } catch (error) {
    clearAuthSession()
    ElMessage.error('登录已过期，请重新登录')
    redirectToLogin()
    throw error
  }
}

request.interceptors.request.use((config) => {
  const isFormData = typeof FormData !== 'undefined' && config.data instanceof FormData
  if (!isFormData) {
    config.headers['Content-Type'] = 'application/json;charset=utf-8'
  }
  const accessToken = getAccessToken()
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
}, (error) => Promise.reject(error))

request.interceptors.response.use(async (response) => {
  if (response.config?.responseType === 'blob') {
    return response.data
  }

  const res = response.data
  if (res.code === 200 || res.success) {
    return res.data !== undefined ? res.data : res
  }

  if (res.code === 401) {
    return retryWithRefresh(response.config)
  }

  ElMessage.error(res.msg || '系统异常')
  return Promise.reject(new Error(res.msg || '系统异常'))
}, async (error) => {
  const status = error.response?.status
  const config = error.config || {}

  if (status === 401) {
    return retryWithRefresh(config)
  }

  let message = error.response?.data?.msg || error.response?.data?.message || error.message || '系统异常'
  if (status === 403) {
    message = error.response?.data?.msg || '无权限访问'
  }
  ElMessage.error(message)
  return Promise.reject(error)
})

export default request

import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
    baseURL: '/api',
    timeout: 5000
})

request.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';
    // 尝试从 localStorage 获取 token 并添加到 Header
    const token = localStorage.getItem('satoken');
    if (token) {
        config.headers['satoken'] = token;
    }
    return config
}, error => {
    return Promise.reject(error)
})

request.interceptors.response.use(response => {
    let res = response.data;
    // 假设后端成功返回 code 200 或 success 字段
    if (res.code === 200 || res.success) {
        return res.data || res; // 兼容返回结构
    } else {
        if (res.code === 401) {
            localStorage.removeItem('satoken')
            localStorage.removeItem('user')
            localStorage.removeItem('latestNotice')
            ElMessage.error(res.msg || '登录已过期，请重新登录')
            router.push('/login')
            return Promise.reject(res.msg || '未登录')
        }
        // 如果业务状态码表示未登录（例如 401 或 5001，具体取决于后端）
        // 这里暂时假设后端如果 Token 无效可能也会通过 HTTP 200 返回特定 code
        // 但通常 Sa-Token 全局异常处理会抛出异常，Spring Boot 可能会返回 HTTP 500
        // 如果后端配置了 GlobalExceptionHandler 并设置了 HTTP 状态码，则会在 error 回调中处理
        
        ElMessage.error(res.msg || '系统异常');
        return Promise.reject(res.msg);
    }
}, error => {
    // 处理 HTTP 状态码错误
    let message = '系统异常'
    
    // 处理 Token 失效或未登录的情况
    if (error.response && error.response.status === 500) {
        // Sa-Token 默认抛出异常时，如果未特殊处理，可能是 500，且 message 包含 "NotLoginException"
        if (error.response.data && error.response.data.message && error.response.data.message.includes('NotLoginException')) {
             localStorage.removeItem('satoken')
             localStorage.removeItem('user')
             localStorage.removeItem('latestNotice')
             ElMessage.error('登录已过期，请重新登录')
             router.push('/login')
             return Promise.reject(error)
        }
    }
    
    if (error.response && error.response.data && error.response.data.msg) {
        message = error.response.data.msg
    } else {
        message = error.message
    }
    ElMessage.error(message);
    return Promise.reject(error)
})

export default request

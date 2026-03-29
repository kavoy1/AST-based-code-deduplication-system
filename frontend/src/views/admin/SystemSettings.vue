<template>
  <div class="workspace-page">
    <WorkspaceShellSection eyebrow="admin config" title="系统设置" description="配置保存后立即影响新请求和新 Job，运行中的 Job 不追溯，敏感密钥仅支持替换与清空。">
      <template #tools>
        <el-button @click="loadConfig">刷新配置</el-button>
        <el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button>
      </template>
    </WorkspaceShellSection>

    <div class="workspace-grid workspace-grid--two">
      <WorkspacePanel title="运行参数" subtitle="查重阈值、上传限制与策略设置">
        <el-form label-position="top" class="settings-grid" v-loading="loading">
          <div class="settings-group">
            <h3>查重参数</h3>
            <el-form-item label="相似度阈值"><el-input-number v-model="form.threshold" :min="0" :max="1" :step="0.01" :precision="2" /></el-form-item>
            <el-form-item label="TopK"><el-input-number v-model="form.topK" :min="1" :max="200" /></el-form-item>
          </div>
          <div class="settings-group">
            <h3>上传限制</h3>
            <el-form-item label="最大文件数"><el-input-number v-model="form.maxFiles" :min="1" :max="200" /></el-form-item>
            <el-form-item label="单文件大小（MB）"><el-input-number v-model="form.maxFileSizeMb" :min="1" :max="1024" /></el-form-item>
            <el-form-item label="允许扩展名（JSON 数组）"><el-input v-model="form.allowedExtsRaw" type="textarea" :rows="4" /></el-form-item>
          </div>
          <div class="settings-group">
            <h3>存储设置</h3>
            <div class="workspace-badge-soft workspace-badge-soft--amber">修改存储路径仅影响新上传，不自动迁移旧文件。</div>
            <el-form-item label="存储根路径"><el-input v-model="form.storageBasePath" placeholder="例如 D:/data/uploads" /></el-form-item>
          </div>
        </el-form>
      </WorkspacePanel>

      <WorkspacePanel title="AI 配置" subtitle="服务端主密钥保护下的密文配置" soft>
        <el-form label-position="top" v-loading="loading">
          <el-form-item label="启用 AI 解释"><el-switch v-model="form.aiEnabled" /></el-form-item>
          <el-form-item label="AI Base URL"><el-input v-model="form.aiBaseUrl" /></el-form-item>
          <el-form-item label="模型名称"><el-input v-model="form.aiModel" /></el-form-item>
          <el-form-item label="超时（ms）"><el-input-number v-model="form.aiTimeoutMs" :min="1000" :max="120000" :step="500" /></el-form-item>
          <div class="workspace-list">
            <div class="workspace-list-item"><div><p class="workspace-list-item__title">AI Key 状态</p><p class="workspace-list-item__meta">不回显明文，只显示配置状态和掩码。</p></div><span class="workspace-badge-soft" :class="secretConfigured ? 'workspace-badge-soft--green' : ''">{{ secretConfigured ? '已配置' : '未配置' }}</span></div>
          </div>
          <div v-if="secretMasked" class="settings-secret-mask">{{ secretMasked }}</div>
          <el-form-item label="替换 AI Key"><el-input v-model="secretValue" type="password" show-password placeholder="输入后会覆盖旧密钥" /></el-form-item>
          <div class="workspace-toolbar__group">
            <el-button :loading="secretSubmitting" type="primary" @click="saveSecret">更新密钥</el-button>
            <el-button :loading="secretClearing" @click="clearSecret">清空密钥</el-button>
          </div>
        </el-form>
      </WorkspacePanel>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import WorkspaceShellSection from '../../components/workspace/WorkspaceShellSection.vue'

const loading = ref(false)
const saving = ref(false)
const secretSubmitting = ref(false)
const secretClearing = ref(false)
const secretConfigured = ref(false)
const secretMasked = ref('')
const secretValue = ref('')

const form = reactive({
  threshold: 0.8,
  topK: 20,
  maxFiles: 20,
  maxFileSizeMb: 5,
  allowedExtsRaw: '[".java"]',
  storageBasePath: 'uploads',
  aiEnabled: false,
  aiBaseUrl: '',
  aiModel: 'qwen-plus',
  aiTimeoutMs: 8000
})

const extractValue = (configMap, key, fallback = null) => {
  const item = configMap?.[key]
  return item?.value ?? fallback
}

const loadConfig = async () => {
  loading.value = true
  try {
    const data = await request.get('/admin/config')
    form.threshold = Number(extractValue(data, 'plagiarism.threshold', 0.8))
    form.topK = Number(extractValue(data, 'plagiarism.topK', 20))
    form.maxFiles = Number(extractValue(data, 'upload.max_files', 20))
    form.maxFileSizeMb = Number(extractValue(data, 'upload.max_file_size_mb', 5))
    form.allowedExtsRaw = JSON.stringify(extractValue(data, 'upload.allowed_exts', ['.java']), null, 2)
    form.storageBasePath = String(extractValue(data, 'storage.base_path', 'uploads') || '')
    form.aiEnabled = Boolean(extractValue(data, 'ai.enabled', false))
    form.aiBaseUrl = String(extractValue(data, 'ai.base_url', '') || '')
    form.aiModel = String(extractValue(data, 'ai.model', 'qwen-plus') || '')
    form.aiTimeoutMs = Number(extractValue(data, 'ai.timeout_ms', 8000))
    const secretItem = data?.['ai.api_key'] || {}
    secretConfigured.value = Boolean(secretItem.configured)
    secretMasked.value = secretItem.masked || ''
  } finally {
    loading.value = false
  }
}

const saveConfig = async () => {
  let parsedExts
  try {
    parsedExts = JSON.parse(form.allowedExtsRaw)
    if (!Array.isArray(parsedExts) || parsedExts.some((ext) => typeof ext !== 'string')) throw new Error('bad')
  } catch {
    ElMessage.error('允许扩展名必须是 JSON 字符串数组')
    return
  }
  saving.value = true
  try {
    await request.put('/admin/config', {
      items: [
        { key: 'plagiarism.threshold', value: String(form.threshold) },
        { key: 'plagiarism.topK', value: String(form.topK) },
        { key: 'upload.max_files', value: String(form.maxFiles) },
        { key: 'upload.max_file_size_mb', value: String(form.maxFileSizeMb) },
        { key: 'upload.allowed_exts', value: JSON.stringify(parsedExts) },
        { key: 'storage.base_path', value: form.storageBasePath || '' },
        { key: 'ai.enabled', value: String(form.aiEnabled) },
        { key: 'ai.base_url', value: form.aiBaseUrl || '' },
        { key: 'ai.model', value: form.aiModel || '' },
        { key: 'ai.timeout_ms', value: String(form.aiTimeoutMs) }
      ]
    })
    ElMessage.success('系统配置已保存')
    await loadConfig()
  } finally {
    saving.value = false
  }
}

const saveSecret = async () => {
  if (!secretValue.value.trim()) {
    ElMessage.warning('请先输入新的密钥')
    return
  }
  secretSubmitting.value = true
  try {
    await request.put('/admin/config/secret', { key: 'ai.api_key', secretValue: secretValue.value.trim(), clear: false })
    secretValue.value = ''
    ElMessage.success('密钥已更新')
    await loadConfig()
  } finally {
    secretSubmitting.value = false
  }
}

const clearSecret = async () => {
  secretClearing.value = true
  try {
    await request.put('/admin/config/secret', { key: 'ai.api_key', clear: true })
    secretValue.value = ''
    ElMessage.success('密钥已清空')
    await loadConfig()
  } finally {
    secretClearing.value = false
  }
}

onMounted(loadConfig)
</script>

<style scoped>
.settings-grid {
  display: grid;
  gap: 18px;
}

.settings-group {
  padding: 18px;
  border-radius: 24px;
  background: rgba(248, 250, 251, 0.86);
}

.settings-group h3 {
  margin: 0 0 16px;
  font-size: 16px;
}

.settings-secret-mask {
  margin: 10px 0 18px;
  color: var(--text-soft);
  font-family: monospace;
}
</style>

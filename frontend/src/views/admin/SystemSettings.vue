<template>
  <div class="workspace-page system-settings-page" v-loading="loading">
    <section class="settings-hero">
      <div class="settings-hero__copy">
        <p class="settings-hero__eyebrow">configuration center</p>
        <h1>配置中心</h1>
        <p class="settings-hero__description">
          配置保存后会立即影响新请求。这里只保留查重参数、存储路径以及 AI 能力与密钥管理。
        </p>
      </div>

      <div class="settings-hero__actions">
        <button type="button" class="settings-pill settings-pill--soft">即时生效</button>
        <button type="button" class="settings-pill" @click="fetchConfig">重新载入</button>
        <button type="button" class="settings-primary" :disabled="savingConfig" @click="saveConfig">
          {{ savingConfig ? '保存中...' : '保存基础配置' }}
        </button>
      </div>
    </section>

    <div class="settings-shell">
      <div class="settings-main">
        <section class="settings-card">
          <div class="settings-card__header">
            <div>
              <p class="settings-card__eyebrow">查重参数</p>
              <h2>用更直观的方式控制检测力度</h2>
            </div>
            <span class="settings-badge settings-badge--blue">{{ thresholdText }}</span>
          </div>

          <div class="settings-grid settings-grid--two">
            <div class="settings-field settings-field--full">
              <label>相似度阈值</label>
              <el-slider
                v-model="form.plagiarismThreshold"
                :min="0"
                :max="1"
                :step="0.01"
                :show-tooltip="false"
              />
              <div class="settings-field__hint">
                <span>触发老师优先关注的最低百分比</span>
                <strong>{{ thresholdText }}</strong>
              </div>
            </div>

            <div class="settings-field">
              <label>TOPK 结果数</label>
              <el-input-number v-model="form.plagiarismTopK" :min="1" :max="100" controls-position="right" />
              <p class="settings-field__sub">返回每名学生最值得复核的匹配结果数。</p>
            </div>
          </div>
        </section>

        <section class="settings-card settings-card--storage">
          <div class="settings-card__header">
            <div>
              <p class="settings-card__eyebrow">存储引擎</p>
              <h2>保留一个明确且稳定的代码文件落盘位置</h2>
            </div>
          </div>

          <div class="settings-grid">
            <div class="settings-field settings-field--full">
              <label>存储根路径</label>
              <el-input v-model="form.storageBasePath" placeholder="例如 uploads 或 D:/storage/documents" />
            </div>
          </div>

          <div class="settings-callout settings-callout--warning">
            <strong>风险提示：路径变更会影响已有任务读取</strong>
            <span>系统运行时改动存储路径，可能导致历史文件引用失效。建议在完成备份后再切换。</span>
          </div>
        </section>
      </div>

      <aside class="settings-side">
        <section class="settings-side-card settings-side-card--dark">
          <div class="settings-side-card__header">
            <div>
              <p class="settings-side-card__eyebrow">AI 配置</p>
              <h2>让解释与增强能力随时可用</h2>
            </div>
            <el-switch v-model="form.aiEnabled" inline-prompt active-text="开" inactive-text="关" />
          </div>

          <div class="settings-side-card__grid">
            <div class="settings-field settings-field--dark settings-field--full">
              <label>API 基础地址</label>
              <el-input v-model="form.aiBaseUrl" placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1" />
            </div>

            <div class="settings-field settings-field--dark">
              <label>模型引擎</label>
              <el-select v-model="form.aiModel">
                <el-option
                  v-for="model in aiModelOptions"
                  :key="model.value"
                  :label="model.label"
                  :value="model.value"
                />
              </el-select>
            </div>

            <div class="settings-field settings-field--dark">
              <label>超时时间（毫秒）</label>
              <el-input-number v-model="form.aiTimeoutMs" :min="1000" :max="120000" controls-position="right" />
            </div>
          </div>

          <div class="settings-divider" />

          <div class="settings-secret">
            <div class="settings-side-card__header settings-side-card__header--compact">
              <div>
                <p class="settings-side-card__eyebrow settings-side-card__eyebrow--danger">密钥管理</p>
                <h3>单独更新，避免误覆盖</h3>
              </div>
            </div>

            <div class="settings-secret__mask">
              <span>当前密钥</span>
              <strong>{{ secretMasked || '未配置' }}</strong>
            </div>

            <div class="settings-field settings-field--dark settings-field--full">
              <label>API 密钥</label>
              <el-input
                v-model="secretValue"
                type="password"
                show-password
                clearable
                placeholder="输入新密钥后点击更新"
              />
            </div>

            <div class="settings-secret__actions">
              <button type="button" class="settings-primary settings-primary--light" :disabled="savingSecret" @click="updateSecret">
                {{ savingSecret ? '更新中...' : '更新密钥' }}
              </button>
              <button type="button" class="settings-ghost" :disabled="savingSecret || !secretConfigured" @click="clearSecret">
                清空密钥
              </button>
            </div>
          </div>
        </section>

        <section class="settings-status" :class="`settings-status--${settingsStatus.tone}`">
          <div class="settings-status__signal">
            <span />
            <span />
            <span />
          </div>
          <div>
            <p class="settings-status__label">{{ settingsStatus.label }}</p>
            <p class="settings-status__description">{{ settingsStatus.description }}</p>
          </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'
import {
  buildAiGenerationModelOptions,
  buildSettingsStatus,
  formatThresholdPercent
} from './systemSettingsHelpers'

const loading = ref(false)
const savingConfig = ref(false)
const savingSecret = ref(false)
const secretConfigured = ref(false)
const secretMasked = ref('')
const secretValue = ref('')

const form = reactive({
  plagiarismThreshold: 0.8,
  plagiarismTopK: 20,
  storageBasePath: 'uploads',
  aiEnabled: false,
  aiBaseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  aiModel: 'qwen-plus',
  aiTimeoutMs: 8000
})

const thresholdText = computed(() => formatThresholdPercent(form.plagiarismThreshold))
const settingsStatus = computed(() => buildSettingsStatus(form, secretConfigured.value))
const aiModelOptions = computed(() => buildAiGenerationModelOptions(form.aiModel))

function readItemValue(data, key, fallback) {
  const item = data?.[key]
  if (!item || item.value === undefined || item.value === null) {
    return fallback
  }
  return item.value
}

function applyConfig(data) {
  form.plagiarismThreshold = Number(readItemValue(data, 'plagiarism.threshold', 0.8))
  form.plagiarismTopK = Number(readItemValue(data, 'plagiarism.topK', 20))
  form.storageBasePath = String(readItemValue(data, 'storage.base_path', 'uploads') || 'uploads')
  form.aiEnabled = Boolean(readItemValue(data, 'ai.enabled', false))
  form.aiBaseUrl = String(readItemValue(data, 'ai.base_url', 'https://dashscope.aliyuncs.com/compatible-mode/v1') || '')
  form.aiModel = String(readItemValue(data, 'ai.model', 'qwen-plus') || 'qwen-plus')
  form.aiTimeoutMs = Number(readItemValue(data, 'ai.timeout_ms', 8000))
  secretConfigured.value = Boolean(data?.['ai.api_key']?.configured)
  secretMasked.value = data?.['ai.api_key']?.masked || ''
}

async function fetchConfig() {
  loading.value = true
  try {
    const data = await request.get('/admin/config')
    applyConfig(data)
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  savingConfig.value = true
  try {
    await request.put('/admin/config', {
      items: [
        { key: 'plagiarism.threshold', value: String(form.plagiarismThreshold) },
        { key: 'plagiarism.topK', value: String(form.plagiarismTopK) },
        { key: 'storage.base_path', value: form.storageBasePath.trim() },
        { key: 'ai.enabled', value: String(form.aiEnabled) },
        { key: 'ai.base_url', value: form.aiBaseUrl.trim() },
        { key: 'ai.model', value: form.aiModel.trim() },
        { key: 'ai.timeout_ms', value: String(form.aiTimeoutMs) }
      ]
    })
    ElMessage.success('基础配置已保存')
    await fetchConfig()
  } finally {
    savingConfig.value = false
  }
}

async function updateSecret() {
  if (!secretValue.value.trim()) {
    ElMessage.warning('请先输入新的 API 密钥')
    return
  }

  savingSecret.value = true
  try {
    await request.put('/admin/config/secret', {
      key: 'ai.api_key',
      secretValue: secretValue.value.trim(),
      clear: false
    })
    secretValue.value = ''
    ElMessage.success('密钥已更新')
    await fetchConfig()
  } finally {
    savingSecret.value = false
  }
}

async function clearSecret() {
  await ElMessageBox.confirm('确定清空当前 AI 密钥吗？清空后解释能力将无法继续调用。', '清空确认', {
    type: 'warning',
    confirmButtonText: '清空',
    cancelButtonText: '取消'
  })

  savingSecret.value = true
  try {
    await request.put('/admin/config/secret', {
      key: 'ai.api_key',
      clear: true
    })
    secretValue.value = ''
    ElMessage.success('密钥已清空')
    await fetchConfig()
  } finally {
    savingSecret.value = false
  }
}

onMounted(fetchConfig)
</script>

<style scoped>
.system-settings-page {
  gap: 20px;
}

.settings-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 26px 28px;
  border-radius: 30px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow: var(--shadow-float);
  background:
    radial-gradient(circle at 12% 16%, rgba(167, 203, 255, 0.34), transparent 26%),
    linear-gradient(135deg, rgba(248, 251, 255, 0.98), rgba(255, 255, 255, 0.94));
}

.settings-hero__copy {
  max-width: 760px;
}

.settings-hero__eyebrow {
  margin: 0 0 10px;
  color: #7e8a98;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.24em;
  text-transform: uppercase;
}

.settings-hero h1 {
  margin: 0;
  color: var(--text-strong);
  font-size: clamp(34px, 3.1vw, 52px);
  line-height: 1.04;
}

.settings-hero__description {
  margin: 14px 0 0;
  color: var(--text-body);
  font-size: 15px;
  line-height: 1.8;
}

.settings-hero__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.settings-pill,
.settings-primary,
.settings-ghost {
  border: none;
  cursor: pointer;
  transition: transform var(--transition-soft), box-shadow var(--transition-soft), background var(--transition-soft);
}

.settings-pill:hover,
.settings-primary:hover,
.settings-ghost:hover {
  transform: translateY(-1px);
}

.settings-pill {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--text-body);
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(29, 35, 43, 0.08);
}

.settings-pill--soft {
  background: rgba(208, 239, 221, 0.7);
  color: #2d5c47;
}

.settings-primary {
  min-height: 44px;
  padding: 0 18px;
  border-radius: 16px;
  background: #182234;
  color: #fff;
  font-weight: 700;
  box-shadow: 0 14px 26px rgba(24, 34, 52, 0.18);
}

.settings-primary:disabled,
.settings-ghost:disabled {
  cursor: not-allowed;
  opacity: 0.62;
  transform: none;
}

.settings-primary--light {
  width: 100%;
  background: rgba(255, 255, 255, 0.96);
  color: #1a2436;
  box-shadow: none;
}

.settings-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 420px);
  gap: 20px;
  min-height: 0;
}

.settings-main,
.settings-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 0;
}

.settings-card,
.settings-status {
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.74);
  box-shadow: var(--shadow-panel);
}

.settings-card {
  padding: 24px 24px 22px;
  background: rgba(255, 255, 255, 0.95);
}

.settings-card--storage {
  background: linear-gradient(180deg, rgba(255, 252, 252, 0.97), rgba(255, 255, 255, 0.94));
}

.settings-card__header,
.settings-side-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.settings-card__eyebrow,
.settings-side-card__eyebrow {
  margin: 0;
  color: #758293;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.settings-side-card__eyebrow--danger {
  color: #f0a19c;
}

.settings-card h2,
.settings-side-card h2,
.settings-side-card h3 {
  margin: 8px 0 0;
  color: var(--text-strong);
  font-size: 28px;
  line-height: 1.2;
}

.settings-side-card h2,
.settings-side-card h3 {
  color: #f7fbff;
}

.settings-side-card h3 {
  font-size: 20px;
}

.settings-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  height: 40px;
  padding: 0 14px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 800;
}

.settings-badge--blue {
  background: rgba(167, 203, 255, 0.26);
  color: #35538c;
}

.settings-grid {
  display: grid;
  gap: 16px;
  margin-top: 20px;
}

.settings-grid--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.settings-field {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.settings-field--full {
  grid-column: 1 / -1;
}

.settings-field label {
  color: var(--text-body);
  font-size: 13px;
  font-weight: 700;
}

.settings-field__hint,
.settings-field__sub {
  color: var(--text-soft);
  font-size: 12px;
  line-height: 1.7;
}

.settings-field__hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.settings-field__hint strong {
  color: var(--text-strong);
}

.settings-callout {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 20px;
  padding: 18px 20px;
  border-radius: 22px;
}

.settings-callout--warning {
  background: linear-gradient(135deg, rgba(254, 235, 232, 0.78), rgba(255, 247, 245, 0.92));
  color: #7a4e4a;
}

.settings-side-card {
  padding: 24px;
  border-radius: 28px;
}

.settings-side-card--dark {
  background: linear-gradient(180deg, rgba(24, 34, 52, 0.98), rgba(17, 25, 39, 0.98));
  color: #f6fbff;
  box-shadow: 0 24px 56px rgba(17, 25, 39, 0.22);
}

.settings-side-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.settings-field--dark label,
.settings-secret__mask span {
  color: rgba(234, 241, 250, 0.72);
}

.settings-side-card--dark :deep(.el-input__wrapper),
.settings-side-card--dark :deep(.el-select__wrapper),
.settings-side-card--dark :deep(.el-input-number) {
  background: rgba(17, 25, 39, 0.55) !important;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12) !important;
}

.settings-side-card--dark :deep(.el-input__inner),
.settings-side-card--dark :deep(.el-select__selected-item),
.settings-side-card--dark :deep(.el-input-number__decrease),
.settings-side-card--dark :deep(.el-input-number__increase) {
  color: #f7fbff !important;
}

.settings-divider {
  height: 1px;
  margin: 22px 0;
  background: rgba(255, 255, 255, 0.1);
}

.settings-secret {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.settings-side-card__header--compact {
  align-items: center;
}

.settings-secret__mask {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
}

.settings-secret__mask strong {
  color: #f7fbff;
  font-size: 16px;
}

.settings-secret__actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}

.settings-ghost {
  min-height: 44px;
  padding: 0 18px;
  border-radius: 16px;
  background: transparent;
  color: rgba(245, 249, 255, 0.86);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.16);
}

.settings-status {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 18px 20px;
  background: rgba(255, 255, 255, 0.94);
}

.settings-status--ready {
  background: linear-gradient(135deg, rgba(228, 247, 236, 0.94), rgba(249, 255, 251, 0.96));
}

.settings-status--warning {
  background: linear-gradient(135deg, rgba(255, 243, 213, 0.96), rgba(255, 250, 239, 0.96));
}

.settings-status__signal {
  display: grid;
  grid-template-columns: repeat(3, 10px);
  align-items: end;
  gap: 5px;
}

.settings-status__signal span {
  display: block;
  width: 10px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(147, 214, 181, 1), rgba(102, 189, 147, 0.68));
}

.settings-status--warning .settings-status__signal span {
  background: linear-gradient(180deg, rgba(255, 214, 133, 1), rgba(233, 166, 41, 0.7));
}

.settings-status__signal span:nth-child(1) { height: 20px; }
.settings-status__signal span:nth-child(2) { height: 28px; }
.settings-status__signal span:nth-child(3) { height: 36px; }

.settings-status__label {
  margin: 0;
  color: var(--text-strong);
  font-size: 18px;
  font-weight: 800;
}

.settings-status__description {
  margin: 6px 0 0;
  color: var(--text-body);
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 1180px) {
  .settings-shell {
    grid-template-columns: 1fr;
  }

  .settings-side-card__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .settings-hero,
  .settings-card,
  .settings-side-card,
  .settings-status {
    border-radius: 24px;
  }

  .settings-hero {
    flex-direction: column;
    padding: 22px;
  }

  .settings-hero__actions {
    justify-content: flex-start;
  }

  .settings-grid--two,
  .settings-secret__actions {
    grid-template-columns: 1fr;
  }
}
</style>

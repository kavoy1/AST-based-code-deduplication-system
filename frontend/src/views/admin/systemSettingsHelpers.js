export function sanitizeAllowedExtension(value) {
  const normalized = String(value || '').trim().toLowerCase()
  if (!normalized) return ''
  return normalized.startsWith('.') ? normalized : `.${normalized}`
}

export function normalizeAllowedExtensions(values = []) {
  const list = Array.isArray(values) ? values : []
  const seen = new Set()
  return list.reduce((result, item) => {
    const normalized = sanitizeAllowedExtension(item)
    if (!normalized || seen.has(normalized)) return result
    seen.add(normalized)
    result.push(normalized)
    return result
  }, [])
}

export function serializeAllowedExtensions(values = []) {
  return JSON.stringify(normalizeAllowedExtensions(values))
}

export function formatThresholdPercent(value) {
  return `${Math.round(Number(value || 0) * 100)}%`
}

export function buildSettingsStatus(form = {}, secretConfigured = false) {
  if (!form.aiEnabled) {
    return {
      tone: 'idle',
      label: '手动配置模式',
      description: '当前使用基础系统配置，AI 扩展能力处于关闭状态。'
    }
  }

  if (!secretConfigured) {
    return {
      tone: 'warning',
      label: '需要补充密钥',
      description: 'AI 已启用，但密钥未配置，当前解释与增强能力无法正常调用。'
    }
  }

  return {
    tone: 'ready',
    label: 'AI 配置可用',
    description: `当前已启用 ${form.aiModel || '指定模型'}，密钥与基础连接配置完整。`
  }
}

export function buildSupportedAiModels(currentModel = '') {
  const defaults = ['qwen-plus', 'qwen-max']
  const current = String(currentModel || '').trim().toLowerCase()
  if (current.startsWith('qwen') && !defaults.includes(current)) {
    return [current, ...defaults]
  }
  return defaults
}

export function buildAiGenerationModelOptions(currentModel = '') {
  const defaults = ['qwen3.6-plus', 'qwen3.5-flash', 'qwen-plus', 'qwen-max']
  const current = String(currentModel || '').trim()
  const options = defaults.map((value) => ({ value, label: value }))
  if (!current) return options
  return options.some((item) => item.value === current)
    ? options
    : [{ value: current, label: `${current}（当前值）` }, ...options]
}

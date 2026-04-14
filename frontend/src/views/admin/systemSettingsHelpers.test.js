import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildAiGenerationModelOptions,
  buildSupportedAiModels,
  buildSettingsStatus,
  formatThresholdPercent,
  normalizeAllowedExtensions,
  sanitizeAllowedExtension,
  serializeAllowedExtensions
} from './systemSettingsHelpers.js'

test('normalizeAllowedExtensions keeps unique lowercase dotted extensions', () => {
  assert.deepEqual(
    normalizeAllowedExtensions(['java', '.JAVA', ' txt ', '', null, '.java']),
    ['.java', '.txt']
  )
})

test('sanitizeAllowedExtension normalizes a single extension draft', () => {
  assert.equal(sanitizeAllowedExtension(' JAVA '), '.java')
  assert.equal(sanitizeAllowedExtension('.Pdf'), '.pdf')
  assert.equal(sanitizeAllowedExtension(''), '')
})

test('serializeAllowedExtensions returns a JSON array string', () => {
  assert.equal(
    serializeAllowedExtensions(['java', '.txt', '.JAVA']),
    '[".java",".txt"]'
  )
})

test('formatThresholdPercent maps decimal threshold to percentage text', () => {
  assert.equal(formatThresholdPercent(0.85), '85%')
  assert.equal(formatThresholdPercent(0.8), '80%')
})

test('buildSettingsStatus marks missing AI secret as warning when AI is enabled', () => {
  assert.deepEqual(
    buildSettingsStatus({ aiEnabled: true, aiModel: 'qwen-plus' }, false),
    {
      tone: 'warning',
      label: '需要补充密钥',
      description: 'AI 已启用，但密钥未配置，当前解释与增强能力无法正常调用。'
    }
  )
})

test('buildSettingsStatus marks active AI setup as ready', () => {
  assert.deepEqual(
    buildSettingsStatus({ aiEnabled: true, aiModel: 'qwen-plus' }, true),
    {
      tone: 'ready',
      label: 'AI 配置可用',
      description: '当前已启用 qwen-plus，密钥与基础连接配置完整。'
    }
  )
})

test('buildSettingsStatus marks disabled AI setup as manual mode', () => {
  assert.deepEqual(
    buildSettingsStatus({ aiEnabled: false, aiModel: 'qwen-plus' }, false),
    {
      tone: 'idle',
      label: '手动配置模式',
      description: '当前使用基础系统配置，AI 扩展能力处于关闭状态。'
    }
  )
})

test('buildSupportedAiModels only keeps supported qwen-family engines', () => {
  assert.deepEqual(
    buildSupportedAiModels('qwen3.5-flash'),
    ['qwen3.5-flash', 'qwen-plus', 'qwen-max']
  )
})

test('buildSupportedAiModels does not append unsupported engines', () => {
  assert.deepEqual(
    buildSupportedAiModels('gpt-4-turbo'),
    ['qwen-plus', 'qwen-max']
  )
})

test('buildAiGenerationModelOptions includes supported generation models and keeps current custom value', () => {
  assert.deepEqual(
    buildAiGenerationModelOptions('qwen3.5-flash').map((item) => item.value),
    ['qwen3.6-plus', 'qwen3.5-flash', 'qwen-plus', 'qwen-max']
  )
  assert.equal(
    buildAiGenerationModelOptions('qwen3.6-plus')[0].label,
    'qwen3.6-plus'
  )
})

<template>
  <section v-if="pairDetail" class="summary-page">
    <header class="summary-header">
      <div class="summary-header__main">
        <AppBackButton label="返回代码对比" @click="goCompare" />
        <div class="summary-header__title">
          <p class="summary-header__eyebrow">处理总结</p>
          <h1>{{ pairDetail.studentA }} / {{ pairDetail.studentB }}</h1>
          <div class="summary-header__facts">
            <span class="summary-fact-pill">系统相似度 {{ pairDetail.score }}%</span>
            <span class="summary-status-chip" :class="`summary-status-chip--${pairStatusTone}`">{{ pairStatusLabel }}</span>
          </div>
        </div>
      </div>

      <div class="summary-header__actions">
        <button type="button" class="summary-action summary-action--ghost" @click="goBack">返回结果列表</button>
        <button type="button" class="summary-action" @click="openDecisionDialog">处理本组结果</button>
      </div>
    </header>

    <section class="summary-workspace">
      <div class="summary-workspace__left">
        <article class="summary-card review-summary-card" :class="`review-summary-card--${teacherDecisionSummary.tone}`">
          <div class="summary-card__head">
            <div>
              <p class="summary-card__label">审阅摘要</p>
              <h2>老师当前重点</h2>
            </div>
            <span class="review-summary-card__score">系统相似度 {{ pairDetail.score }}%</span>
          </div>

          <div class="review-summary-card__facts">
            <div class="review-summary-card__fact">
              <span class="summary-card__label">当前状态</span>
              <strong>{{ pairStatusLabel }}</strong>
            </div>
            <div class="review-summary-card__fact">
              <span class="summary-card__label">系统建议</span>
              <strong>{{ teacherDecisionSummary.title }}</strong>
            </div>
            <div class="review-summary-card__fact review-summary-card__fact--note">
              <span class="summary-card__label">教师备注</span>
              <strong>{{ pairDetail.teacherNote || '暂无备注' }}</strong>
            </div>
          </div>

          <p class="review-summary-card__summary">{{ teacherDecisionSummary.summary }}</p>

          <div class="review-summary-card__reasons">
            <span
              v-for="reason in teacherDecisionSummary.reasons"
              :key="reason"
              class="review-summary-card__reason"
            >
              {{ reason }}
            </span>
          </div>
        </article>

        <article class="summary-card summary-card--evidence">
          <div class="summary-card__head">
            <div>
              <p class="summary-card__label">证据摘要</p>
              <h2>查重命中明细</h2>
            </div>
            <span>{{ evidenceViews.length }} 条</span>
          </div>

          <div v-if="evidenceViews.length" class="evidence-list">
            <section v-for="item in evidenceViews" :key="item.id" class="evidence-item">
              <div class="evidence-item__top">
                <strong>{{ item.title }}</strong>
                <span>权重 {{ item.weight }}</span>
              </div>

              <div class="evidence-metrics">
                <div v-if="item.totals?.M" class="evidence-metric">
                  <span>命中特征数</span>
                  <strong>{{ item.totals.M }}</strong>
                </div>
                <div v-if="item.totals?.AC !== null && item.totals?.AC !== undefined" class="evidence-metric">
                  <span>AC 相似系数</span>
                  <strong>{{ formatAc(item.totals.AC) }}</strong>
                </div>
                <div v-if="item.topMatches.length" class="evidence-metric">
                  <span>命中特征类别</span>
                  <strong>{{ item.topMatches.length }}</strong>
                </div>
                <div class="evidence-metric">
                  <span>证据权重</span>
                  <strong>{{ item.weight }}</strong>
                </div>
              </div>

              <section v-if="item.topMatches.length" class="evidence-feature-box">
                <div class="evidence-feature-box__head">
                  <span>支撑这次建议的关键特征</span>
                  <strong>{{ item.topMatches.length }} 项</strong>
                </div>

                <div class="evidence-feature-box__list">
                  <span
                    v-for="match in visibleEvidenceFeatures(item)"
                    :key="`${item.id}-${match.label}`"
                    class="summary-tag summary-tag--soft"
                  >
                    <strong>{{ match.label }}</strong>
                    <small v-if="match.matchedCount">×{{ match.matchedCount }}</small>
                  </span>
                </div>
                <button
                  v-if="item.topMatches.length > DEFAULT_VISIBLE_FEATURES"
                  type="button"
                  class="evidence-feature-box__toggle"
                  @click="toggleEvidenceFeatures(item.id)"
                >
                  {{ isEvidenceExpanded(item.id) ? '收起其余特征' : `展开剩余 ${item.topMatches.length - DEFAULT_VISIBLE_FEATURES} 项` }}
                </button>
              </section>
            </section>
          </div>
          <el-empty v-else description="暂无证据数据" />
        </article>
      </div>

      <aside class="summary-workspace__right">
        <article class="summary-card summary-card--ai">
          <div class="ai-panel">
            <div class="ai-panel__topbar">
              <div>
                <p class="summary-card__label">AI 解释</p>
                <h2>AI 对话记录</h2>
                <p class="ai-panel__model" v-if="currentAiRuntimeText">当前模型：{{ currentAiRuntimeText }}</p>
              </div>

              <span v-if="aiLoading" class="ai-panel__status">生成中</span>
            </div>

            <div class="ai-panel__conversation">
              <template v-if="chatAiRecords.length">
                <div class="ai-chat-stream">
                  <article v-for="item in chatAiRecords" :key="item.id" class="ai-message ai-message--assistant">
                    <div class="ai-message__avatar">AI</div>
                    <div class="ai-message__body">
                      <div class="ai-message__meta">
                        <strong>{{ item.createdAt || '刚刚生成' }}</strong>
                        <span v-if="item.model">{{ item.model }}</span>
                      </div>
                      <div class="ai-message__bubble">
                        <p>{{ buildAiChatMessage(item) }}</p>
                      </div>
                    </div>
                  </article>
                </div>
              </template>

              <div v-else class="ai-chat-empty">
                <span>还没有 AI 解释</span>
                <small>
                  {{ currentAiRuntimeText ? `当前将使用 ${currentAiRuntimeText} 生成解释。` : '选择模式后生成解释。' }}
                  旧解释会像聊天记录一样保留在这里。
                </small>
              </div>
            </div>

            <div class="ai-composer">
              <div class="ai-composer__modes">
                <button
                  v-for="option in explanationOptionsView"
                  :key="option.value"
                  type="button"
                  class="ai-composer__mode"
                  :class="{ 'ai-composer__mode--active': selectedExplanationMode === option.value }"
                  @click="selectedExplanationMode = option.value"
                >
                  {{ option.label }}
                </button>
              </div>

              <button type="button" class="summary-action ai-composer__submit" :disabled="aiLoading" @click="confirmGenerateAiExplanation">
                {{ aiLoading ? '生成中...' : '生成解释' }}
              </button>
            </div>
          </div>
        </article>
      </aside>
    </section>

    <el-dialog
      v-model="decisionDialogVisible"
      title="处理本组结果"
      width="560px"
      destroy-on-close
      class="summary-dialog"
    >
      <div class="summary-dialog__body">
        <section class="summary-dialog__section">
          <p class="summary-card__label">处理状态</p>
          <div class="status-option-list">
            <button
              v-for="option in statusOptions"
              :key="option.value"
              type="button"
              class="status-option"
              :class="{ 'status-option--active': form.status === option.value }"
              @click="form.status = option.value"
            >
              <strong>{{ option.label }}</strong>
              <span>{{ option.hint }}</span>
            </button>
          </div>
        </section>

        <section class="summary-dialog__section">
          <p class="summary-card__label">教师备注</p>
          <el-input
            v-model="form.teacherNote"
            type="textarea"
            :rows="6"
            placeholder="输入你对这组相似结果的判断、处理依据或后续建议"
          />
        </section>
      </div>

      <template #footer>
        <div class="summary-dialog__footer">
          <button type="button" class="summary-action summary-action--ghost" @click="decisionDialogVisible = false">取消</button>
          <button type="button" class="summary-action" :disabled="saving" @click="saveStatus">
            {{ saving ? '保存中...' : '保存处理结果' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </section>

  <section v-else class="summary-empty-shell">
    <el-empty description="未找到相似对详情">
      <AppBackButton label="返回代码对比" @click="goCompare" />
    </el-empty>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createTeacherAiExplanation,
  fetchTeacherAiExplanationHistory,
  fetchLatestTeacherAiExplanation,
  fetchTeacherPairDetail,
  updateTeacherPairStatus
} from '../../api/teacherAssignments'
import { buildTeacherDecisionSummary, formatDateTime, summarizeEvidenceList } from './assignmentMappers'
import AppBackButton from '../../components/AppBackButton.vue'

const route = useRoute()
const router = useRouter()

const pairDetail = ref(null)
const aiLoading = ref(false)
const saving = ref(false)
const decisionDialogVisible = ref(false)
const selectedExplanationMode = ref('CODE_ONLY')
const aiHistory = ref([])
let aiPollingTimer = null
const expandedEvidenceIds = ref([])

const DIRECT_CONFIRM_THRESHOLD = 85
const DEFAULT_VISIBLE_FEATURES = 6

const form = reactive({
  status: 'PENDING',
  teacherNote: ''
})

const statusOptions = [
  { value: 'PENDING', label: '待确认', hint: '仍需老师继续复核' },
  { value: 'CONFIRMED', label: '已确认', hint: '确认这组结果需要进一步处理' },
  { value: 'FALSE_POSITIVE', label: '误报', hint: '判定为正常相似或系统误报' }
]

const evidenceViews = computed(() => summarizeEvidenceList(pairDetail.value?.evidences || []))
const teacherDecisionSummary = computed(() =>
  buildTeacherDecisionSummary({
    score: pairDetail.value?.score || 0,
    evidences: evidenceViews.value,
    threshold: DIRECT_CONFIRM_THRESHOLD
  })
)

const pairStatusLabel = computed(() => {
  const map = {
    PENDING: '待确认',
    CONFIRMED: '已确认',
    FALSE_POSITIVE: '误报'
  }
  return map[pairDetail.value?.status] || pairDetail.value?.status || '待确认'
})

const pairStatusTone = computed(() => {
  const map = {
    PENDING: 'pending',
    CONFIRMED: 'confirmed',
    FALSE_POSITIVE: 'muted'
  }
  return map[pairDetail.value?.status] || 'pending'
})

const explanationOptionsView = [
  {
    value: 'CODE_ONLY',
    label: '仅代码'
  },
  {
    value: 'CODE_WITH_SYSTEM_EVIDENCE',
    label: '代码 + 系统证据'
  }
]

const chatAiRecords = computed(() => [...aiHistory.value].reverse())
const currentAiRuntimeText = computed(() => {
  const latestRecord = chatAiRecords.value[0] || pairDetail.value?.latestAiExplanation || null
  const provider = String(latestRecord?.provider || pairDetail.value?.currentAiProvider || '').trim()
  const model = String(latestRecord?.model || pairDetail.value?.currentAiModel || '').trim()
  if (provider && model) return `${provider} / ${model}`
  return model || provider || ''
})

onMounted(loadPage)
onBeforeUnmount(stopAiPolling)

async function loadPage() {
  const detail = await fetchTeacherPairDetail(route.params.pairId)
  pairDetail.value = detail
  form.status = detail.status || 'PENDING'
  form.teacherNote = detail.teacherNote || ''
  await refreshAiState(detail)
}

async function refreshAiState(detail = pairDetail.value) {
  if (!detail?.pairId) {
    aiHistory.value = []
    return
  }
  const latest = detail.latestAiExplanation || (await fetchLatestTeacherAiExplanation(detail.pairId).catch(() => null))
  pairDetail.value.latestAiExplanation = latest
  aiHistory.value = normalizeAiHistory(await fetchTeacherAiExplanationHistory(detail.pairId))
}

function stopAiPolling() {
  if (aiPollingTimer) {
    clearTimeout(aiPollingTimer)
    aiPollingTimer = null
  }
}

async function pollAiExplanation(pairId, targetId, attempt = 0) {
  if (!pairId) {
    aiLoading.value = false
    return
  }
  try {
    const latest = await fetchLatestTeacherAiExplanation(pairId).catch(() => null)
    const history = normalizeAiHistory(await fetchTeacherAiExplanationHistory(pairId))
    aiHistory.value = history
    pairDetail.value.latestAiExplanation = latest
    const target = history.find((item) => item.id === targetId) || (latest?.id === targetId ? normalizeAiHistory([latest])[0] : null)
    if (!target || target.status === 'GENERATING') {
      if (attempt >= 89) {
        aiLoading.value = false
        ElMessage.warning('AI 仍在后台生成，稍后会自动出现在聊天记录里。')
        return
      }
      aiPollingTimer = setTimeout(() => {
        pollAiExplanation(pairId, targetId, attempt + 1)
      }, 1000)
      return
    }
    aiLoading.value = false
    if (target.status === 'FAILED') {
      ElMessage.error(target.errorMsg || 'AI 解释生成失败')
      return
    }
    ElMessage.success('AI 解释已生成')
  } catch (error) {
    aiLoading.value = false
    ElMessage.error(error?.message || '获取 AI 解释状态失败')
  }
}

function openDecisionDialog() {
  if (!pairDetail.value) return
  form.status = pairDetail.value.status || 'PENDING'
  form.teacherNote = pairDetail.value.teacherNote || ''
  decisionDialogVisible.value = true
}

async function saveStatus() {
  await persistPairDecision(form.status, form.teacherNote, '处理结果已保存')
}

async function persistPairDecision(status, teacherNote, successMessage) {
  if (!pairDetail.value) return
  saving.value = true
  try {
    const updated = await updateTeacherPairStatus(pairDetail.value.pairId, {
      status,
      teacherNote
    })
    pairDetail.value.status = updated.status || status
    pairDetail.value.teacherNote = updated.teacherNote ?? teacherNote
    form.status = pairDetail.value.status
    form.teacherNote = pairDetail.value.teacherNote || ''
    decisionDialogVisible.value = false
    ElMessage.success(successMessage)
  } finally {
    saving.value = false
  }
}

function isEvidenceExpanded(id) {
  return expandedEvidenceIds.value.includes(id)
}

function toggleEvidenceFeatures(id) {
  expandedEvidenceIds.value = isEvidenceExpanded(id)
    ? expandedEvidenceIds.value.filter((item) => item !== id)
    : [...expandedEvidenceIds.value, id]
}

function visibleEvidenceFeatures(item) {
  if (!Array.isArray(item?.topMatches)) return []
  return isEvidenceExpanded(item.id) ? item.topMatches : item.topMatches.slice(0, DEFAULT_VISIBLE_FEATURES)
}

async function confirmGenerateAiExplanation() {
  if (!pairDetail.value) return
  stopAiPolling()
  aiLoading.value = true
  try {
    const created = await createTeacherAiExplanation(pairDetail.value.pairId, {
      mode: selectedExplanationMode.value,
      includeTeacherNote: false
    })
    await refreshAiState()
    await pollAiExplanation(pairDetail.value.pairId, created?.id)
  } catch (error) {
    aiLoading.value = false
  }
}

function normalizeAiHistory(records = []) {
  return records.map((item) => {
    const requestPayload = safeParseJsonText(item.requestPayload) || {}
    const responsePayload = safeParseJsonText(item.responsePayload) || {}
    const structured = responsePayload.structured || {}
    const systemScore = normalizePercent(structured.systemScore ?? requestPayload.systemScore ?? pairDetail.value?.score ?? 0)
    const aiScore = normalizePercent(structured.aiScore ?? systemScore)
    const scoreDiff = normalizePercent(structured.scoreDiff ?? Math.abs(aiScore - systemScore))
    return {
      id: item.id,
      status: item.status || 'SUCCESS',
      statusLabel: item.status === 'FAILED' ? '生成失败' : item.status === 'GENERATING' ? '生成中' : '已生成',
      createdAt: formatDateTime(item.createTime),
      mode: requestPayload.mode || 'CODE_ONLY',
      includeTeacherNote: Boolean(requestPayload.includeTeacherNote),
      systemScore,
      aiScore,
      scoreDiff,
      riskLevel: structured.riskLevel || deriveRiskLevel(aiScore),
      model: item.model || '',
      conclusion: structured.conclusion || item.result || '',
      reasoning: structured.reasoning || '',
      evidenceSummary: structured.evidenceSummary || '',
      diffDirection: structured.diffDirection || deriveDiffDirection(aiScore, systemScore),
      errorMsg: item.errorMsg || ''
    }
  })
}

function safeParseJsonText(value) {
  if (!value) return null
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

function normalizePercent(value) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return 0
  return Math.max(0, Math.min(100, Math.round(numeric)))
}

function deriveRiskLevel(score) {
  if (score >= 85) return 'HIGH'
  if (score >= 60) return 'MEDIUM'
  return 'LOW'
}

function deriveDiffDirection(aiScore, systemScore) {
  if (aiScore > systemScore) return 'AI_HIGHER'
  if (aiScore < systemScore) return 'AI_LOWER'
  return 'MATCHED'
}

function formatAiModeLabel(mode) {
  return mode === 'CODE_WITH_SYSTEM_EVIDENCE' ? '代码 + 系统证据' : '仅代码'
}

function buildAiChatMessage(record) {
  if (!record) return ''
  if (record.status === 'GENERATING') {
    return `${formatAiModeLabel(record.mode)}：正在生成解释，请稍候...`
  }
  if (record.status === 'FAILED') {
    return `${formatAiModeLabel(record.mode)}：生成失败${record.errorMsg ? `，${record.errorMsg}` : ''}`
  }
  const parts = [record.conclusion, record.reasoning, record.evidenceSummary].filter(Boolean)
  return `${formatAiModeLabel(record.mode)}：${parts.join('\n\n') || '暂无解释内容'}`
}

function formatAc(value) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return '--'
  return numeric.toFixed(4)
}

function goCompare() {
  const assignmentId = route.query.assignmentId
  router.push({
    path: `/teacher/similarity-pairs/${route.params.pairId}`,
    query: assignmentId ? { assignmentId } : {}
  })
}

function goBack() {
  const assignmentId = route.query.assignmentId
  router.push(assignmentId ? `/teacher/assignments/${assignmentId}/plagiarism/results` : '/teacher/assignments/plagiarism/results')
}
</script>
<style scoped>
.summary-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
  padding: 22px;
  box-sizing: border-box;
  border-radius: 32px;
  background:
    radial-gradient(circle at top right, rgba(255, 122, 161, 0.08), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 248, 255, 0.95));
  border: 1px solid rgba(207, 217, 235, 0.65);
  overflow: hidden;
}

.summary-card--evidence {
  height: 100%;
}

.summary-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
}

.summary-header__main {
  display: flex;
  align-items: flex-start;
  gap: 18px;
}

.summary-header__title {
  display: grid;
  gap: 6px;
}

.summary-header__facts,
.summary-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.summary-header__eyebrow,
.summary-card__label {
  margin: 0;
  color: #7f8da3;
  font-size: 13px;
  font-weight: 700;
}

.summary-header__title h1,
.summary-card__head h2 {
  margin: 0;
  color: #121826;
}

.summary-fact-pill,
.summary-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(18, 24, 38, 0.05);
  color: #50617a;
  font-size: 13px;
  font-weight: 700;
}

.summary-tag--soft {
  justify-content: flex-start;
  gap: 6px;
  max-width: 100%;
  background: rgba(83, 107, 255, 0.09);
  color: #4b63c9;
  border: 1px solid rgba(104, 124, 230, 0.12);
}

.summary-tag--soft strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 84px;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.summary-status-chip--pending {
  background: rgba(70, 120, 255, 0.12);
  color: #466ce8;
}

.summary-status-chip--confirmed {
  background: rgba(28, 180, 112, 0.14);
  color: #14804f;
}

.summary-status-chip--muted {
  background: rgba(142, 154, 175, 0.16);
  color: #57657a;
}

.summary-action {
  border: none;
  border-radius: 999px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #111723, #232e51);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 14px 26px rgba(24, 31, 52, 0.16);
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
}

.summary-action:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 18px 30px rgba(24, 31, 52, 0.2);
}

.summary-action:disabled {
  cursor: not-allowed;
  opacity: 0.66;
}

.summary-action--ghost {
  background: rgba(255, 255, 255, 0.88);
  color: #31425d;
  border: 1px solid rgba(206, 214, 231, 0.9);
  box-shadow: 0 12px 24px rgba(168, 179, 201, 0.14);
}

.summary-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(460px, 640px);
  gap: 16px;
  align-items: stretch;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.summary-workspace__left {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 16px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.summary-workspace__right {
  min-height: 0;
}

.summary-card,
.summary-footer-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  border-radius: 26px;
  border: 1px solid rgba(209, 218, 236, 0.78);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 36px rgba(181, 193, 215, 0.12);
  min-height: 0;
}

.summary-card__head,
.evidence-item__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.summary-card__head span,
.evidence-item__top span {
  color: #73839b;
}

.review-summary-card {
  gap: 16px;
  background:
    radial-gradient(circle at top right, rgba(121, 145, 225, 0.08), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 255, 0.94));
}

.review-summary-card--confirm {
  border-color: rgba(93, 193, 136, 0.26);
}

.review-summary-card--review {
  border-color: rgba(102, 126, 235, 0.22);
}

.review-summary-card--caution {
  border-color: rgba(224, 170, 93, 0.24);
}

.review-summary-card__score {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(18, 24, 38, 0.05);
  color: #50617a;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.review-summary-card__facts {
  display: grid;
  grid-template-columns: 200px 240px minmax(0, 1fr);
  gap: 12px;
}

.review-summary-card__fact {
  display: grid;
  gap: 10px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(217, 224, 239, 0.72);
}

.review-summary-card__fact strong {
  color: #121826;
  font-size: 18px;
  line-height: 1.45;
}

.review-summary-card__fact--note strong {
  color: #50617a;
  font-size: 16px;
  font-weight: 600;
}

.review-summary-card__summary {
  margin: 0;
  color: #4f5f77;
  font-size: 15px;
  line-height: 1.8;
}

.review-summary-card__reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.review-summary-card__reason {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(214, 222, 237, 0.82);
  color: #4f6077;
  font-size: 13px;
  font-weight: 700;
}

.summary-record {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 14px;
}

.summary-record__status,
.summary-record__note {
  display: grid;
  gap: 10px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(18, 24, 38, 0.04);
}

.summary-record__note p,
.summary-dialog__section p {
  margin: 0;
  color: #50617a;
  line-height: 1.7;
  white-space: pre-wrap;
}

.decision-strip {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(212, 221, 239, 0.82);
  background: linear-gradient(180deg, rgba(247, 249, 255, 0.96), rgba(255, 255, 255, 0.94));
}

.decision-strip--confirm {
  border-color: rgba(49, 173, 110, 0.28);
  background:
    radial-gradient(circle at top right, rgba(62, 193, 127, 0.12), transparent 28%),
    linear-gradient(180deg, rgba(243, 251, 247, 0.98), rgba(255, 255, 255, 0.96));
}

.decision-strip--review {
  border-color: rgba(83, 107, 255, 0.24);
  background:
    radial-gradient(circle at top right, rgba(83, 107, 255, 0.12), transparent 28%),
    linear-gradient(180deg, rgba(244, 247, 255, 0.98), rgba(255, 255, 255, 0.96));
}

.decision-strip--caution {
  border-color: rgba(216, 154, 72, 0.28);
  background:
    radial-gradient(circle at top right, rgba(255, 190, 92, 0.12), transparent 28%),
    linear-gradient(180deg, rgba(255, 250, 243, 0.98), rgba(255, 255, 255, 0.96));
}

.decision-strip__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.decision-strip__head h3 {
  margin: 6px 0 0;
  color: #111827;
  font-size: 30px;
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.decision-strip__threshold {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(18, 24, 38, 0.05);
  color: #5c6a80;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.decision-strip__summary {
  margin: 0;
  color: #4f5f77;
  line-height: 1.8;
}

.decision-strip__reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.decision-strip__reason {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(214, 222, 237, 0.82);
  color: #4f6077;
  font-size: 13px;
  font-weight: 700;
}

.decision-strip__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.decision-strip__action {
  border: none;
  border-radius: 999px;
  min-height: 42px;
  padding: 0 18px;
  background: rgba(236, 241, 255, 0.92);
  color: #3f5488;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
}

.decision-strip__action:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 22px rgba(173, 185, 213, 0.16);
}

.decision-strip__action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.decision-strip__action--primary {
  background: linear-gradient(135deg, #111723, #232e51);
  color: #fff;
  box-shadow: 0 14px 24px rgba(28, 37, 63, 0.16);
}

.decision-strip__action--ghost {
  background: rgba(255, 255, 255, 0.94);
  color: #31425d;
  border: 1px solid rgba(206, 214, 231, 0.9);
}

.evidence-list {
  display: grid;
  gap: 14px;
  align-content: start;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.evidence-item {
  display: grid;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(18, 24, 38, 0.04);
}

.evidence-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
}

.evidence-metric {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(215, 222, 239, 0.72);
}

.evidence-metric span {
  color: #7988a0;
  font-size: 12px;
  font-weight: 600;
}

.evidence-metric strong {
  color: #1a2234;
  font-size: 22px;
  line-height: 1.1;
}

.evidence-feature-box {
  display: grid;
  gap: 10px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(215, 222, 239, 0.72);
}

.evidence-feature-box__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #7a88a0;
  font-size: 13px;
  font-weight: 600;
}

.evidence-feature-box__list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 10px;
}

.evidence-feature-box__toggle {
  width: fit-content;
  border: none;
  padding: 0;
  background: transparent;
  color: #4d64c7;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.summary-card--ai {
  padding: 0;
  overflow: hidden;
  height: 100%;
  background:
    radial-gradient(circle at top left, rgba(138, 158, 214, 0.08), transparent 22%),
    radial-gradient(circle at bottom right, rgba(223, 210, 193, 0.12), transparent 30%),
    rgba(255, 255, 255, 0.96);
}

.ai-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  background:
    radial-gradient(circle at top left, rgba(130, 149, 204, 0.08), transparent 24%),
    linear-gradient(180deg, rgba(252, 251, 249, 0.99), rgba(245, 242, 237, 0.97));
  height: 100%;
  min-height: 0;
  font-family: "SF Pro Display", "PingFang SC", "Microsoft YaHei UI", sans-serif;
}

.ai-panel__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 22px 14px;
  border-bottom: 1px solid rgba(226, 217, 205, 0.82);
}

.ai-panel__topbar .summary-card__label {
  color: #75829a;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.ai-panel__model {
  margin: 10px 0 0;
  color: #6f7d93;
  font-size: 13px;
  font-weight: 600;
}

.ai-panel__topbar h2 {
  margin: 10px 0 0;
  color: #111827;
  font-size: 40px;
  font-weight: 800;
  letter-spacing: -0.04em;
  line-height: 1.05;
}

.ai-panel__status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(18, 24, 38, 0.05);
  color: #5b6880;
  font-size: 12px;
  font-weight: 700;
}

.ai-panel__conversation {
  min-height: 0;
  overflow-y: auto;
  padding: 20px 22px 16px;
  background:
    radial-gradient(circle at top left, rgba(121, 139, 209, 0.07), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(247, 244, 240, 0.82));
}

.ai-chat-stream {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-message {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.ai-message__avatar {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(31, 39, 65, 0.94), rgba(55, 72, 118, 0.92));
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  flex-shrink: 0;
}

.ai-message__body {
  display: grid;
  gap: 8px;
  max-width: min(100%, 560px);
}

.ai-message__bubble {
  padding: 15px 18px;
  border-radius: 22px 22px 22px 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(250, 248, 244, 0.95));
  border: 1px solid rgba(218, 224, 236, 0.92);
  box-shadow: 0 18px 34px rgba(171, 180, 195, 0.12);
}

.ai-message__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: #7a869a;
  font-size: 12px;
  font-weight: 700;
}

.ai-message__meta span {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(18, 24, 38, 0.05);
  color: #5c6880;
  font-size: 11px;
  font-weight: 700;
}

.ai-message__bubble p {
  margin: 0;
}

.ai-message__bubble p {
  color: #344256;
  font-size: 15px;
  line-height: 1.9;
  letter-spacing: 0.01em;
  white-space: pre-wrap;
}

.ai-chat-empty {
  display: grid;
  gap: 8px;
  place-items: center;
  min-height: 320px;
  padding: 36px 24px;
  border-radius: 26px;
  border: 1px dashed rgba(206, 214, 228, 0.92);
  background:
    radial-gradient(circle at top, rgba(150, 168, 222, 0.08), transparent 20%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(248, 245, 241, 0.86));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    0 12px 26px rgba(194, 201, 216, 0.08);
  color: #7a879a;
  text-align: center;
}

.ai-chat-empty span {
  color: #111827;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.ai-chat-empty small {
  max-width: 340px;
  color: #6d7a8f;
  font-size: 14px;
  line-height: 1.85;
}

.ai-composer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px 18px;
  border-top: 1px solid rgba(226, 217, 205, 0.82);
  background:
    linear-gradient(180deg, rgba(252, 250, 247, 0.88), rgba(250, 247, 243, 0.98));
}

.ai-composer__modes {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.ai-composer__mode {
  min-width: 112px;
  padding: 11px 18px;
  border-radius: 999px;
  border: 1px solid rgba(216, 223, 236, 0.95);
  background: rgba(255, 255, 255, 0.95);
  color: #546278;
  font-size: 14px;
  font-weight: 700;
  text-align: center;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, color 0.18s ease;
}

.ai-composer__mode:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 22px rgba(173, 185, 213, 0.16);
}

.ai-composer__mode--active {
  border-color: rgba(28, 35, 57, 0.94);
  background: linear-gradient(135deg, #121826, #25324f);
  color: #fff;
  box-shadow: 0 14px 24px rgba(32, 41, 70, 0.2);
}

.ai-composer__submit {
  min-width: 148px;
  letter-spacing: -0.01em;
}

:deep(.summary-dialog) {
  border-radius: 30px;
}

:deep(.summary-dialog .el-dialog__body) {
  padding-top: 8px;
}

.summary-dialog__body {
  display: grid;
  gap: 20px;
}

.summary-dialog__section {
  display: grid;
  gap: 12px;
}

.status-option-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.status-option-list--dual {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.status-option {
  display: grid;
  gap: 6px;
  min-height: 108px;
  padding: 16px;
  border: 1px solid rgba(207, 217, 235, 0.9);
  border-radius: 20px;
  background: rgba(247, 249, 255, 0.96);
  color: #4d5d75;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.status-option:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 22px rgba(169, 181, 203, 0.18);
}

.status-option strong {
  color: #121826;
  font-size: 16px;
}

.status-option span {
  line-height: 1.6;
}

.status-option--active {
  border-color: rgba(80, 107, 255, 0.88);
  background: linear-gradient(180deg, rgba(236, 240, 255, 0.98), rgba(225, 232, 255, 0.98));
  box-shadow: 0 16px 26px rgba(97, 116, 208, 0.18);
}

.summary-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.summary-empty-shell {
  display: grid;
  place-items: center;
  min-height: 60vh;
}

@media (max-width: 1260px) {
  .summary-workspace {
    grid-template-columns: 1fr;
    overflow: auto;
  }

  .summary-workspace__right {
    position: static;
  }
}

@media (max-width: 960px) {
  .summary-page {
    padding: 18px;
    overflow: auto;
  }

  .summary-header,
  .summary-header__main,
  .summary-header__actions,
  .summary-card__head,
  .summary-record,
  .evidence-item__top,
  .summary-dialog__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .summary-record,
  .review-summary-card__facts,
  .status-option-list,
  .status-option-list--dual {
    grid-template-columns: 1fr;
  }

  .summary-workspace__left {
    grid-template-rows: auto;
  }

  .ai-message__avatar {
    width: 40px;
    height: 40px;
    border-radius: 16px;
  }

  .ai-panel__topbar h2 {
    font-size: 32px;
  }

  .ai-composer {
    flex-direction: column;
    align-items: stretch;
  }

  .ai-composer__modes {
    width: 100%;
  }

  .ai-composer__mode {
    flex: 1 1 180px;
  }

  .ai-composer__submit {
    width: 100%;
  }
}
</style>


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
        <article class="summary-card">
          <div class="summary-card__head">
            <div>
              <p class="summary-card__label">处理记录</p>
              <h2>教师确认信息</h2>
            </div>
            <button type="button" class="summary-action summary-action--ghost" @click="openDecisionDialog">再次处理</button>
          </div>

          <div class="summary-record">
            <div class="summary-record__status">
              <span class="summary-card__label">当前状态</span>
              <span class="summary-status-chip" :class="`summary-status-chip--${pairStatusTone}`">{{ pairStatusLabel }}</span>
            </div>

            <div class="summary-record__note">
              <span class="summary-card__label">教师备注</span>
              <p>{{ pairDetail.teacherNote || '暂无备注' }}</p>
            </div>
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
                <strong>{{ item.summary || item.type }}</strong>
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
                  <span>后端返回命中特征</span>
                  <strong>{{ item.topMatches.length }} 项</strong>
                </div>

                <div class="evidence-feature-box__list">
                  <span
                    v-for="match in item.topMatches"
                    :key="`${item.id}-${match.label}`"
                    class="summary-tag summary-tag--soft"
                  >
                    <strong>{{ match.label }}</strong>
                    <small v-if="match.matchedCount">×{{ match.matchedCount }}</small>
                  </span>
                </div>
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
                <h2>双评分解释</h2>
              </div>

              <div class="ai-panel__top-actions">
                <span class="ai-panel__status">{{ aiResultStatus }}</span>
                <button type="button" class="summary-action" :disabled="aiLoading" @click="openGenerationDialog">
                  {{ aiLoading ? '生成中…' : '生成 AI 解释' }}
                </button>
              </div>
            </div>

            <div class="ai-panel__scroll">
              <div class="ai-panel__body">
                <template v-if="activeAiRecord">
                  <section class="ai-overview-card">
                    <div class="ai-overview-card__header">
                      <div>
                        <p class="summary-card__label">AI 总结</p>
                        <h3>{{ activeAiRecord.conclusion || '暂无结论' }}</h3>
                      </div>
                      <div class="ai-overview-card__chips">
                        <span class="summary-tag">{{ formatAiModeLabel(activeAiRecord.mode) }}</span>
                        <span class="summary-tag summary-tag--soft">误差 {{ activeAiRecord.scoreDiff }}%</span>
                        <span v-if="activeAiRecord.includeTeacherNote" class="summary-tag summary-tag--soft">附带备注</span>
                      </div>
                    </div>

                    <div class="ai-score-grid">
                      <article class="ai-score-card">
                        <span>AI 相似度</span>
                        <strong>{{ activeAiRecord.aiScore }}%</strong>
                      </article>
                      <article class="ai-score-card">
                        <span>系统相似度</span>
                        <strong>{{ activeAiRecord.systemScore }}%</strong>
                      </article>
                      <article class="ai-score-card">
                        <span>误差值</span>
                        <strong>{{ activeAiRecord.scoreDiff }}%</strong>
                      </article>
                      <article class="ai-score-card">
                        <span>风险等级</span>
                        <strong>{{ riskLabelMap[activeAiRecord.riskLevel] || activeAiRecord.riskLevel }}</strong>
                      </article>
                    </div>
                  </section>

                  <article class="ai-section-card">
                    <div class="ai-section-card__head">
                      <strong>误差判断</strong>
                      <span>{{ activeAiRecord.scoreDiff }}% 差值</span>
                    </div>
                    <p>{{ buildDiffText(activeAiRecord) }}</p>
                  </article>

                  <article class="ai-section-card">
                    <div class="ai-section-card__head">
                      <strong>关键依据</strong>
                      <span v-if="activeAiRecord.includeTeacherNote">附带教师备注</span>
                    </div>
                    <p>{{ activeAiRecord.reasoning || '暂无解释依据' }}</p>
                  </article>

                  <article v-if="activeAiRecord.evidenceSummary" class="ai-section-card ai-section-card--soft">
                    <div class="ai-section-card__head">
                      <strong>系统证据摘要</strong>
                      <span>结合系统命中片段</span>
                    </div>
                    <p class="ai-section-card__extra">
                      {{ activeAiRecord.evidenceSummary }}
                    </p>
                  </article>
                </template>

                <div v-else class="ai-chat-empty">
                  <span>暂无 AI 解释</span>
                  <small>可选择“仅代码”或“代码 + 系统证据”生成，并保留历史记录。</small>
                </div>
              </div>

              <div class="ai-history">
                <div class="ai-history__title-row">
                  <div>
                    <p class="summary-card__label">历史记录</p>
                    <h3>可回看每次解释</h3>
                  </div>
                  <span>{{ aiHistory.length }} 条</span>
                </div>

                <div v-if="aiHistory.length" class="ai-history__list">
                  <button
                    v-for="item in aiHistory"
                    :key="item.id"
                    type="button"
                    class="ai-history__item"
                    :class="{ 'ai-history__item--active': item.id === activeAiRecord?.id }"
                    @click="activeAiRecordId = item.id"
                  >
                    <div class="ai-history__head">
                      <strong>{{ formatAiModeLabel(item.mode) }}</strong>
                      <span>{{ item.createdAt || '--' }}</span>
                    </div>
                    <div class="ai-history__meta">
                      <span>AI {{ item.aiScore }}%</span>
                      <span>系统 {{ item.systemScore }}%</span>
                      <span>误差 {{ item.scoreDiff }}%</span>
                    </div>
                    <p class="ai-history__text">{{ item.conclusion || '暂无结论' }}</p>
                  </button>
                </div>
                <p v-else class="ai-history__empty">还没有生成过 AI 解释。</p>
              </div>
            </div>
          </div>
        </article>
      </aside>
    </section>

    <el-dialog
      v-model="generationDialogVisible"
      title="生成 AI 解释"
      width="560px"
      destroy-on-close
      class="summary-dialog"
    >
      <div class="summary-dialog__body">
        <section class="summary-dialog__section">
          <p class="summary-card__label">解释模式</p>
          <div class="status-option-list status-option-list--dual">
            <button
              v-for="option in explanationOptions"
              :key="option.value"
              type="button"
              class="status-option"
              :class="{ 'status-option--active': selectedExplanationMode === option.value }"
              @click="selectedExplanationMode = option.value"
            >
              <strong>{{ option.label }}</strong>
              <span>{{ option.hint }}</span>
            </button>
          </div>
        </section>

        <section class="summary-dialog__section">
          <label class="ai-note-toggle">
            <input v-model="includeTeacherNoteInAi" type="checkbox">
            <span>附带当前教师备注</span>
          </label>
        </section>
      </div>

      <template #footer>
        <div class="summary-dialog__footer">
          <button type="button" class="summary-action summary-action--ghost" @click="generationDialogVisible = false">取消</button>
          <button type="button" class="summary-action" :disabled="aiLoading" @click="confirmGenerateAiExplanation">
            {{ aiLoading ? '生成中…' : '开始生成' }}
          </button>
        </div>
      </template>
    </el-dialog>

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
            {{ saving ? '保存中…' : '保存处理结果' }}
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createTeacherAiExplanation,
  fetchTeacherAiExplanationHistory,
  fetchLatestTeacherAiExplanation,
  fetchTeacherPairDetail,
  updateTeacherPairStatus
} from '../../api/teacherAssignments'
import { formatDateTime, summarizeEvidenceList } from './assignmentMappers'
import AppBackButton from '../../components/AppBackButton.vue'

const route = useRoute()
const router = useRouter()

const pairDetail = ref(null)
const aiLoading = ref(false)
const saving = ref(false)
const decisionDialogVisible = ref(false)
const generationDialogVisible = ref(false)
const selectedExplanationMode = ref('CODE_ONLY')
const includeTeacherNoteInAi = ref(false)
const aiHistory = ref([])
const activeAiRecordId = ref(null)

const form = reactive({
  status: 'PENDING',
  teacherNote: ''
})

const statusOptions = [
  { value: 'PENDING', label: '待确认', hint: '仍需老师继续复核' },
  { value: 'CONFIRMED', label: '已确认', hint: '确认这组结果需要进一步处理' },
  { value: 'FALSE_POSITIVE', label: '误报', hint: '判定为正常相似或系统误报' }
]

const explanationOptions = [
  {
    value: 'CODE_ONLY',
    label: '仅代码',
    hint: '只把两段代码交给 AI，独立给出相似度判断。'
  },
  {
    value: 'CODE_WITH_SYSTEM_EVIDENCE',
    label: '代码 + 系统证据',
    hint: '同时把系统查重证据交给 AI，生成联合解释与误差分析。'
  }
]

const riskLabelMap = {
  HIGH: '高风险',
  MEDIUM: '中风险',
  LOW: '低风险'
}

const evidenceViews = computed(() => summarizeEvidenceList(pairDetail.value?.evidences || []))

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

const activeAiRecord = computed(() => {
  if (!aiHistory.value.length) return null
  return aiHistory.value.find(item => item.id === activeAiRecordId.value) || aiHistory.value[0]
})

const aiResultStatus = computed(() => {
  if (aiLoading.value) return '生成中'
  return activeAiRecord.value?.statusLabel || '未生成'
})

onMounted(loadPage)

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
    activeAiRecordId.value = null
    return
  }
  const latest = detail.latestAiExplanation || await fetchLatestTeacherAiExplanation(detail.pairId).catch(() => null)
  pairDetail.value.latestAiExplanation = latest
  aiHistory.value = normalizeAiHistory(await fetchTeacherAiExplanationHistory(detail.pairId))
  activeAiRecordId.value = aiHistory.value[0]?.id || null
}

function openDecisionDialog() {
  if (!pairDetail.value) return
  form.status = pairDetail.value.status || 'PENDING'
  form.teacherNote = pairDetail.value.teacherNote || ''
  decisionDialogVisible.value = true
}

async function saveStatus() {
  if (!pairDetail.value) return
  saving.value = true
  try {
    const updated = await updateTeacherPairStatus(pairDetail.value.pairId, {
      status: form.status,
      teacherNote: form.teacherNote
    })
    pairDetail.value.status = updated.status || form.status
    pairDetail.value.teacherNote = updated.teacherNote ?? form.teacherNote
    decisionDialogVisible.value = false
    ElMessage.success('处理结果已保存')
  } finally {
    saving.value = false
  }
}

function openGenerationDialog() {
  generationDialogVisible.value = true
}

async function confirmGenerateAiExplanation() {
  if (!pairDetail.value) return
  aiLoading.value = true
  try {
    await createTeacherAiExplanation(pairDetail.value.pairId, {
      mode: selectedExplanationMode.value,
      includeTeacherNote: includeTeacherNoteInAi.value
    })
    pairDetail.value.latestAiExplanation = await fetchLatestTeacherAiExplanation(pairDetail.value.pairId)
    aiHistory.value = normalizeAiHistory(await fetchTeacherAiExplanationHistory(pairDetail.value.pairId))
    activeAiRecordId.value = aiHistory.value[0]?.id || null
    generationDialogVisible.value = false
    ElMessage.success('AI 解释已生成')
  } finally {
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
      statusLabel: item.status === 'FAILED' ? '生成失败' : '已生成',
      createdAt: formatDateTime(item.createTime),
      mode: requestPayload.mode || 'CODE_ONLY',
      includeTeacherNote: Boolean(requestPayload.includeTeacherNote),
      systemScore,
      aiScore,
      scoreDiff,
      riskLevel: structured.riskLevel || deriveRiskLevel(aiScore),
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

function buildDiffText(record) {
  if (!record) return '暂无误差分析'
  if (record.diffDirection === 'MATCHED') {
    return 'AI 分数与系统分数基本一致，两者对这组代码的判断口径接近。'
  }
  if (record.diffDirection === 'AI_HIGHER') {
    return `AI 判断比系统高 ${record.scoreDiff}% ，说明 AI 认为结构或实现层面的相似性更明显。`
  }
  return `AI 判断比系统低 ${record.scoreDiff}% ，说明 AI 认为这组代码仍存在一定差异。`
}

function formatAiModeLabel(mode) {
  return mode === 'CODE_WITH_SYSTEM_EVIDENCE' ? '代码 + 系统证据' : '仅代码'
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
.summary-card__head h2,
.ai-history__title-row h3 {
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
  grid-template-columns: minmax(0, 1.2fr) minmax(380px, 460px);
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
.evidence-item__top,
.ai-section-card__head,
.ai-history__head,
.ai-history__title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.summary-card__head span,
.evidence-item__top span,
.ai-history__head span,
.ai-history__title-row span,
.ai-section-card__head span {
  color: #73839b;
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
.ai-section-card p,
.ai-history__text {
  margin: 0;
  color: #50617a;
  line-height: 1.7;
  white-space: pre-wrap;
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
  max-height: 240px;
  padding-right: 4px;
  overflow-y: auto;
}

.summary-card--ai {
  padding: 0;
  overflow: hidden;
  height: 100%;
}

.ai-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  background: linear-gradient(180deg, rgba(255, 252, 248, 0.98), rgba(247, 241, 236, 0.94));
  height: 100%;
  min-height: 0;
}

.ai-panel__topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px 14px;
  border-bottom: 1px solid rgba(226, 217, 205, 0.82);
}

.ai-panel__top-actions {
  display: grid;
  justify-items: end;
  gap: 10px;
}

.ai-panel__status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(31, 38, 66, 0.08);
  color: #59657a;
  font-size: 12px;
  font-weight: 700;
}

.ai-panel__scroll {
  display: grid;
  grid-template-rows: auto auto;
  min-height: 0;
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.ai-panel__body {
  display: grid;
  gap: 14px;
  padding: 18px 20px 16px;
  min-height: 0;
}

.ai-overview-card {
  display: grid;
  gap: 16px;
  padding: 18px;
  border-radius: 22px;
  background:
    radial-gradient(circle at top right, rgba(114, 132, 255, 0.12), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 248, 255, 0.96));
  border: 1px solid rgba(214, 223, 240, 0.9);
}

.ai-overview-card__header {
  display: grid;
  gap: 12px;
}

.ai-overview-card__header h3 {
  margin: 4px 0 0;
  color: #121826;
  font-size: 28px;
  line-height: 1.3;
}

.ai-overview-card__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ai-score-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.ai-score-card,
.ai-section-card {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(212, 221, 239, 0.8);
}

.ai-score-card span,
.ai-section-card__extra {
  color: #77869e;
}

.ai-score-card span {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.ai-score-card strong {
  color: #101826;
  font-size: 30px;
  line-height: 1;
}

.ai-section-card--soft {
  background: rgba(247, 249, 255, 0.94);
}

.ai-chat-empty {
  display: grid;
  gap: 10px;
  place-items: center;
  min-height: 260px;
  padding: 30px 24px;
  border-radius: 24px;
  border: 1px dashed rgba(205, 196, 182, 0.9);
  background: rgba(255, 255, 255, 0.66);
  color: #7d8aa0;
  text-align: center;
}

.ai-chat-empty small {
  max-width: 320px;
  line-height: 1.7;
}

.ai-history {
  display: grid;
  gap: 12px;
  padding: 0 20px 18px;
  border-top: 1px solid rgba(226, 217, 205, 0.82);
  background: rgba(255, 248, 242, 0.96);
  flex-shrink: 0;
}

.ai-history__title-row {
  padding-top: 16px;
}

.ai-history__list {
  display: grid;
  gap: 10px;
}

.ai-history__item {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(248, 250, 255, 0.88);
  border: 1px solid rgba(217, 224, 240, 0.9);
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.ai-history__item:hover {
  transform: translateY(-1px);
  border-color: rgba(138, 154, 233, 0.88);
  box-shadow: 0 12px 22px rgba(169, 181, 203, 0.16);
}

.ai-history__item--active {
  border-color: rgba(80, 107, 255, 0.88);
  background: linear-gradient(180deg, rgba(236, 240, 255, 0.98), rgba(225, 232, 255, 0.98));
  box-shadow: 0 16px 26px rgba(97, 116, 208, 0.18);
}

.ai-history__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  color: #61728d;
  font-size: 13px;
}

.ai-history__empty {
  margin: 0;
  color: #7f8da3;
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

.ai-note-toggle {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #50617a;
  font-weight: 600;
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
  .status-option-list,
  .status-option-list--dual,
  .ai-score-grid {
    grid-template-columns: 1fr;
  }

  .summary-workspace__left {
    grid-template-rows: auto;
  }
}
</style>

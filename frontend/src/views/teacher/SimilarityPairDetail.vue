<template>
  <div v-if="pairDetail" class="pair-detail-page">
    <section class="pair-detail-shell">
      <div class="pair-detail-shell__header">
        <div>
          <el-button text @click="goBack">返回查重与结果</el-button>
          <h1>相似对详情</h1>
          <p>Job #{{ pairDetail.jobId }} · {{ pairDetail.studentA }} / {{ pairDetail.studentB }}</p>
        </div>
        <div class="pair-detail-shell__actions">
          <span class="pair-detail-status">{{ pairStatusLabel }}</span>
          <el-button type="primary" round :loading="aiLoading" @click="generateAiExplanation">生成 AI 解释</el-button>
        </div>
      </div>

      <div class="pair-detail-summary-grid">
        <div class="pair-detail-card"><span>学生 A</span><strong>{{ pairDetail.studentA }}</strong></div>
        <div class="pair-detail-card"><span>学生 B</span><strong>{{ pairDetail.studentB }}</strong></div>
        <div class="pair-detail-card"><span>相似度</span><strong>{{ pairDetail.score }}</strong></div>
        <div class="pair-detail-card"><span>当前状态</span><strong>{{ pairStatusLabel }}</strong></div>
      </div>

      <div class="pair-detail-two-col">
        <article class="pair-detail-card pair-detail-card--flex">
          <div class="pair-detail-card__head"><h3>证据详情</h3><span>{{ pairDetail.evidences.length }} 条</span></div>
          <el-collapse>
            <el-collapse-item
              v-for="item in pairDetail.evidences"
              :key="item.id"
              :title="`${item.type} · 权重 ${item.weight}`"
              :name="String(item.id)"
            >
              <p class="pair-detail-text">{{ item.summary }}</p>
              <pre class="pair-detail-pre">{{ prettyJson(item.payloadJson) }}</pre>
            </el-collapse-item>
          </el-collapse>
        </article>

        <article class="pair-detail-card pair-detail-card--flex">
          <div class="pair-detail-card__head"><h3>教师处理</h3></div>
          <el-form label-position="top" :model="form">
            <el-form-item label="处理状态">
              <el-select v-model="form.status">
                <el-option label="待确认" value="PENDING" />
                <el-option label="已确认" value="CONFIRMED" />
                <el-option label="误报" value="FALSE_POSITIVE" />
              </el-select>
            </el-form-item>
            <el-form-item label="教师备注">
              <el-input v-model="form.teacherNote" type="textarea" :rows="5" placeholder="请输入备注" />
            </el-form-item>
            <el-button type="primary" :loading="saving" @click="saveStatus">保存处理结果</el-button>
          </el-form>

          <div class="pair-detail-ai">
            <div class="pair-detail-card__head"><h3>AI 解释</h3></div>
            <p class="pair-detail-text">{{ pairDetail.latestAiExplanation?.result || '暂无 AI 解释' }}</p>
            <p v-if="pairDetail.latestAiExplanation" class="pair-detail-meta">
              {{ pairDetail.latestAiExplanation.provider }} / {{ pairDetail.latestAiExplanation.model }} / {{ pairDetail.latestAiExplanation.status }}
            </p>
          </div>
        </article>
      </div>
    </section>
  </div>

  <div v-else class="pair-detail-empty">
    <el-empty description="未找到相似对详情">
      <el-button type="primary" round @click="goBack">返回查重与结果</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createTeacherAiExplanation,
  fetchLatestTeacherAiExplanation,
  fetchTeacherPairDetail,
  updateTeacherPairStatus
} from '../../api/teacherAssignments'

const route = useRoute()
const router = useRouter()

const pairDetail = ref(null)
const aiLoading = ref(false)
const saving = ref(false)
const form = reactive({ status: 'PENDING', teacherNote: '' })

const pairStatusLabel = computed(() => {
  const map = {
    PENDING: '待确认',
    CONFIRMED: '已确认',
    FALSE_POSITIVE: '误报'
  }
  return map[pairDetail.value?.status] || pairDetail.value?.status || '-'
})

onMounted(loadPage)

async function loadPage() {
  const detail = await fetchTeacherPairDetail(route.params.pairId)
  pairDetail.value = detail
  form.status = detail.status
  form.teacherNote = detail.teacherNote || ''
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
    ElMessage.success('处理结果已保存')
  } finally {
    saving.value = false
  }
}

async function generateAiExplanation() {
  if (!pairDetail.value) return
  aiLoading.value = true
  try {
    await createTeacherAiExplanation(pairDetail.value.pairId)
    pairDetail.value.latestAiExplanation = await fetchLatestTeacherAiExplanation(pairDetail.value.pairId)
    ElMessage.success('AI 解释已生成')
  } finally {
    aiLoading.value = false
  }
}

function prettyJson(value) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value || ''
  }
}

function goBack() {
  router.push({
    path: '/teacher/assignments',
    query: {
      tab: 'plagiarism',
      assignmentId: route.query.assignmentId || undefined
    }
  })
}
</script>

<style scoped>
.pair-detail-page {
  height: 100%;
}

.pair-detail-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 24px 26px;
  border-radius: 32px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 255, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
  overflow: hidden;
}

.pair-detail-shell__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.pair-detail-shell__header h1 {
  margin: 8px 0 6px;
  font-size: 30px;
}

.pair-detail-shell__header p,
.pair-detail-text,
.pair-detail-meta {
  margin: 0;
  color: #6f7f96;
}

.pair-detail-shell__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pair-detail-status {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(18, 25, 38, 0.08);
  color: #53627a;
}

.pair-detail-summary-grid,
.pair-detail-two-col {
  display: grid;
  gap: 14px;
}

.pair-detail-summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.pair-detail-two-col {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-height: 0;
  flex: 1;
}

.pair-detail-card {
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(127, 142, 163, 0.14);
  box-shadow: 0 16px 38px rgba(160, 176, 199, 0.08);
}

.pair-detail-card--flex {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.pair-detail-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pair-detail-card__head h3 {
  margin: 0;
}

.pair-detail-card span {
  color: #7f8ea3;
}

.pair-detail-card strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
}

.pair-detail-pre {
  margin: 0;
  padding: 12px;
  border-radius: 16px;
  background: #121926;
  color: #eaf1ff;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
}

.pair-detail-ai {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(18, 25, 38, 0.04);
}

.pair-detail-empty {
  display: grid;
  place-items: center;
  height: 100%;
}

@media (max-width: 1280px) {
  .pair-detail-shell__header,
  .pair-detail-shell__actions,
  .pair-detail-summary-grid,
  .pair-detail-two-col {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

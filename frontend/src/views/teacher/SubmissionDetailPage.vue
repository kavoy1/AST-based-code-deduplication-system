<template>
  <div v-if="submission" class="submission-detail-page">
    <section class="submission-detail-shell">
      <div class="submission-detail-shell__header">
        <div>
          <AppBackButton label="返回提交列表" @click="goBack" />
          <h1>鎻愪氦璇︽儏</h1>
          <p>瀛︾敓ID {{ submission.studentNumber }} 路 鎻愪氦ID {{ submission.submissionId }}</p>
        </div>
      </div>

      <div class="submission-detail-tabs">
        <button
          v-for="item in tabs"
          :key="item.value"
          type="button"
          class="submission-detail-tabs__item"
          :class="{ active: currentTab === item.value }"
          @click="currentTab = item.value"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="submission-detail-summary-grid">
        <div class="submission-detail-card"><span>鐗堟湰鍙?/span><strong>{{ submission.version }}</strong></div>
        <div class="submission-detail-card"><span>鏂囦欢鏁?/span><strong>{{ submission.fileCount }}</strong></div>
        <div class="submission-detail-card"><span>瑙ｆ瀽鎴愬姛鏁?/span><strong>{{ submission.parseOkFiles }}</strong></div>
        <div class="submission-detail-card"><span>鎻愪氦鐘舵€?/span><strong>{{ submission.isLate ? '杩熶氦' : '姝ｅ父' }}</strong></div>
      </div>

      <div v-if="currentTab === 'history'" class="submission-detail-panel">
        <article class="submission-detail-card submission-detail-card--flex">
          <div class="submission-detail-card__head"><h3>鐗堟湰鍘嗗彶</h3><span>{{ historyRows.length }} 涓増鏈?/span></div>
          <el-table :data="historyRows" height="100%" empty-text="鏆傛棤鐗堟湰鍘嗗彶">
            <el-table-column prop="version" label="鐗堟湰" width="90" />
            <el-table-column label="鏂囦欢鏁? width="100"><template #default="scope">{{ scope.row.fileCount }}</template></el-table-column>
            <el-table-column label="瑙ｆ瀽鎴愬姛鏁? width="120"><template #default="scope">{{ scope.row.parseOkFiles }}</template></el-table-column>
            <el-table-column label="鎻愪氦鏃堕棿" min-width="170"><template #default="scope">{{ scope.row.lastSubmittedAt || '鍚庣鏆傛湭杩斿洖' }}</template></el-table-column>
            <el-table-column label="杩熶氦" width="90" align="center"><template #default="scope">{{ scope.row.isLate ? '鏄? : '鍚? }}</template></el-table-column>
            <el-table-column label="鏈€鏂扮増鏈? width="100" align="center"><template #default="scope">{{ scope.row.submissionId === submission.submissionId ? '鏄? : '鍚? }}</template></el-table-column>
          </el-table>
        </article>
      </div>

      <div v-else class="submission-detail-panel">
        <article class="submission-detail-card submission-detail-card--flex">
          <div class="submission-detail-card__head"><h3>鏂囦欢鏄庣粏</h3></div>
          <div class="submission-detail-placeholder">
            <strong>鍓嶇楠ㄦ灦宸查鐣?/strong>
            <p>鍚庣褰撳墠鏈彁渚涙彁浜ゆ枃浠舵槑缁嗘帴鍙ｃ€傛湰椤靛厛淇濈暀鏂囦欢鏄庣粏鍖哄煙锛屽緟鎺ュ彛琛ラ綈鍚庣洿鎺ユ帴鍏ユ枃浠跺悕銆佸ぇ灏忋€丼HA256銆佽В鏋愮姸鎬佸拰閿欒鍘熷洜銆?/p>
            <ul>
              <li>褰撳墠鐗堟湰鏂囦欢鏁帮細{{ submission.fileCount }}</li>
              <li>瑙ｆ瀽鎴愬姛鏂囦欢鏁帮細{{ submission.parseOkFiles }}</li>
              <li>鍙瘮瀵圭姸鎬侊細{{ submission.parseOkFiles > 0 ? '鍙瘮瀵? : '涓嶅彲姣斿' }}</li>
            </ul>
          </div>
        </article>
      </div>
    </section>
  </div>

  <div v-else class="submission-detail-empty">
    <el-empty description="鏈壘鍒拌鎻愪氦">
      <AppBackButton label="返回提交列表" @click="goBack" />
    </el-empty>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchTeacherAssignmentSubmissions } from '../../api/teacherAssignments'
import AppBackButton from '../../components/AppBackButton.vue'

const route = useRoute()
const router = useRouter()

const tabs = [
  { value: 'history', label: '鐗堟湰鍘嗗彶' },
  { value: 'files', label: '鏂囦欢鏄庣粏' }
]

const currentTab = ref('history')
const submission = ref(null)
const historyRows = ref([])

const assignmentId = computed(() => String(route.query.assignmentId || ''))

onMounted(loadPage)

async function loadPage() {
  if (!assignmentId.value) return
  const rows = await fetchTeacherAssignmentSubmissions(assignmentId.value)
  const currentSubmissionId = Number(route.params.submissionId)
  const current = rows.find((item) => Number(item.submissionId) === currentSubmissionId) || null
  submission.value = current
  if (!current) return
  historyRows.value = rows
    .filter((item) => Number(item.studentId) === Number(current.studentId))
    .sort((a, b) => Number(b.version || 0) - Number(a.version || 0))
}

function goBack() {
  router.push(assignmentId.value ? `/teacher/assignments/${assignmentId.value}/submissions` : '/teacher/assignments')
}
</script>

<style scoped>
.submission-detail-page {
  height: 100%;
}

.submission-detail-shell {
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

.submission-detail-shell__header h1 {
  margin: 8px 0 6px;
  font-size: 30px;
}

.submission-detail-shell__header p,
.submission-detail-placeholder p,
.submission-detail-placeholder li {
  margin: 0;
  color: #6f7f96;
}

.submission-detail-tabs {
  display: inline-flex;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(18, 25, 38, 0.06);
}

.submission-detail-tabs__item {
  border: 0;
  padding: 10px 18px;
  border-radius: 999px;
  background: transparent;
  color: #68798f;
  cursor: pointer;
}

.submission-detail-tabs__item.active {
  background: #121926;
  color: #fff;
}

.submission-detail-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.submission-detail-panel {
  min-height: 0;
  flex: 1;
  display: flex;
}

.submission-detail-card {
  width: 100%;
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(127, 142, 163, 0.14);
  box-shadow: 0 16px 38px rgba(160, 176, 199, 0.08);
}

.submission-detail-card--flex {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.submission-detail-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.submission-detail-card__head h3 {
  margin: 0;
}

.submission-detail-card span {
  color: #7f8ea3;
}

.submission-detail-card strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
}

.submission-detail-placeholder {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(18, 25, 38, 0.04);
}

.submission-detail-placeholder strong {
  font-size: 16px;
}

.submission-detail-placeholder ul {
  margin: 0;
  padding-left: 18px;
}

.submission-detail-empty {
  display: grid;
  place-items: center;
  height: 100%;
}

@media (max-width: 1280px) {
  .submission-detail-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>




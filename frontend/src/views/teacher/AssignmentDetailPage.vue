<template>
  <div v-if="assignment" class="assignment-detail-page">
    <section class="assignment-detail-shell">
      <div class="assignment-detail-shell__header">
        <div>
          <AppBackButton label="返回作业总览" @click="goBack" />
          <h1>{{ assignment.title }}</h1>
          <p>{{ assignment.classNamesText }} 璺?{{ assignment.startTime }} - {{ assignment.endTime }}</p>
        </div>
        <div class="assignment-detail-shell__actions">
          <span class="assignment-detail-shell__status">{{ assignment.statusLabel }}</span>
          <el-button
            v-if="assignment.status === 'active'"
            round
            :loading="endAssignmentLoading"
            @click="handleEndAssignment"
          >
            閹绘劕澧犵紒鎾存将娴ｆ粈绗?          </el-button>
          <el-button
            v-else-if="assignment.status === 'ended'"
            round
            @click="openReopenDialog"
          >
            闁插秵鏌婂鈧崥?          </el-button>
          <el-button type="primary" round :loading="jobLoading" @click="handleCreatePlagiarismJob">閸欐垼鎹ｉ弻銉╁櫢</el-button>
        </div>
      </div>

      <div class="assignment-detail-tabs">
        <button
          v-for="item in detailTabs"
          :key="item.value"
          type="button"
          class="assignment-detail-tabs__item"
          :class="{ active: currentTab === item.value }"
          @click="currentTab = item.value"
        >
          {{ item.label }}
        </button>
      </div>

      <div v-if="currentTab === 'basic'" class="assignment-detail-panel">
        <div class="assignment-detail-grid">
          <div class="assignment-detail-info-card"><span>缂傛牜鈻肩拠顓♀枅</span><strong>{{ assignment.language }}</strong></div>
          <div class="assignment-detail-info-card"><span>閻濐厾楠囬弫浼村櫤</span><strong>{{ assignment.classes.length }}</strong></div>
          <div class="assignment-detail-info-card"><span>閺堚偓婢堆勬瀮娴犺埖鏆?/span><strong>{{ assignment.maxFiles }}</strong></div>
          <div class="assignment-detail-info-card"><span>瀵偓婵妞傞梻?/span><strong>{{ assignment.startTime }}</strong></div>
          <div class="assignment-detail-info-card"><span>閹搭亝顒涢弮鍫曟？</span><strong>{{ assignment.endTime }}</strong></div>
          <div class="assignment-detail-info-card"><span>娴ｆ粈绗熼悩鑸碘偓?/span><strong>{{ assignment.statusLabel }}</strong></div>
          <div class="assignment-detail-info-card"><span>閸忎浇顔忛柌宥咁槻閹绘劒姘?/span><strong>{{ assignment.allowResubmit ? '閺? : '閸? }}</strong></div>
          <div class="assignment-detail-info-card"><span>閸忎浇顔忔潻鐔舵唉</span><strong>{{ assignment.allowLateSubmit ? '閺? : '閸? }}</strong></div>
          <div class="assignment-detail-info-card"><span>鐠у嫭鏋￠梽鍕</span><strong>{{ assignment.materials.length }}</strong></div>
        </div>

        <div class="assignment-detail-two-col">
          <article class="assignment-detail-card">
            <div class="assignment-detail-card__head"><h3>閻濐厾楠囬懠鍐ㄦ纯</h3></div>
            <el-table :data="assignment.classes" height="100%" empty-text="閺嗗倹妫ら悵顓犻獓">
              <el-table-column prop="className" label="閻濐厾楠? min-width="150" />
              <el-table-column prop="studentCount" label="鎼存柧姘︽禍鐑樻殶" width="100" />
              <el-table-column prop="submittedStudentCount" label="瀹歌弓姘︽禍鐑樻殶" width="100" />
            </el-table>
          </article>

          <article class="assignment-detail-card">
            <div class="assignment-detail-card__head"><h3>娴ｆ粈绗熺拠瀛樻</h3></div>
            <p class="assignment-detail-card__text">{{ assignment.description || '閺嗗倹妫ゆ担婊€绗熺拠瀛樻閵? }}</p>
          </article>
        </div>
      </div>

      <div v-else-if="currentTab === 'submissions'" class="assignment-detail-panel">
        <div class="assignment-detail-summary-row">
          <div class="assignment-detail-summary-card"><span>鎼存柧姘︽禍鐑樻殶</span><strong>{{ stats?.studentCount || 0 }}</strong></div>
          <div class="assignment-detail-summary-card"><span>瀹歌弓姘︽禍鐑樻殶</span><strong>{{ stats?.submittedStudentCount || 0 }}</strong></div>
          <div class="assignment-detail-summary-card"><span>閺堫亙姘︽禍鐑樻殶</span><strong>{{ stats?.unsubmittedStudentCount || 0 }}</strong></div>
          <div class="assignment-detail-summary-card"><span>鏉╃喍姘︽禍鐑樻殶</span><strong>{{ stats?.lateSubmissionCount || 0 }}</strong></div>
        </div>

        <article class="assignment-detail-card assignment-detail-card--flex">
          <div class="assignment-detail-card__head"><h3>閹绘劒姘﹂崚妤勩€?/h3><span>閻愮懓鍤潻娑樺弳閹绘劒姘︾拠锔藉剰</span></div>
          <el-table :data="submissions" height="100%" empty-text="閺嗗倹妫ら幓鎰唉鐠佹澘缍?>
            <el-table-column prop="studentNumber" label="鐎涳妇鏁揑D" min-width="140" />
            <el-table-column prop="version" label="閻楀牊婀伴弫? width="100" />
            <el-table-column prop="fileCount" label="閺傚洣娆㈤弫? width="90" />
            <el-table-column prop="parseOkFiles" label="鐟欙絾鐎介幋鎰閺? width="120" />
            <el-table-column label="鏉╃喍姘? width="90" align="center"><template #default="scope">{{ scope.row.isLate ? '閺? : '閸? }}</template></el-table-column>
            <el-table-column label="閺堚偓閺傜増褰佹禍銈嗘闂? min-width="160"><template #default="scope">{{ scope.row.lastSubmittedAt || '閸氬海顏弳鍌涙弓鏉╂柨娲? }}</template></el-table-column>
            <el-table-column label="閹垮秳缍? width="120" align="center">
              <template #default="scope">
                <el-button link type="primary" @click="goToSubmissionDetail(scope.row)">閺屻儳婀呯拠锔藉剰</el-button>
              </template>
            </el-table-column>
          </el-table>
        </article>
      </div>

      <div v-else-if="currentTab === 'files'" class="assignment-detail-panel">
        <div class="assignment-detail-two-col">
          <article class="assignment-detail-card assignment-detail-card--flex">
            <div class="assignment-detail-card__head">
              <h3>娴ｆ粈绗熺挧鍕灐</h3>
              <el-upload :auto-upload="false" :show-file-list="false" multiple accept="*" @change="handleMaterialSelect">
                <el-button type="primary">娑撳﹣绱剁挧鍕灐</el-button>
              </el-upload>
            </div>
            <el-table :data="materials" height="100%" empty-text="閺嗗倹妫ょ挧鍕灐闂勫嫪娆?>
              <el-table-column prop="originalName" label="閺傚洣娆㈤崥? min-width="220" show-overflow-tooltip />
              <el-table-column label="婢堆冪毈" width="110"><template #default="scope">{{ formatBytes(scope.row.sizeBytes) }}</template></el-table-column>
              <el-table-column prop="contentType" label="缁鐎? min-width="150" show-overflow-tooltip />
              <el-table-column label="閹垮秳缍? width="150" align="center">
                <template #default="scope">
                  <el-button link type="primary" @click="downloadMaterial(scope.row)">娑撳娴?/el-button>
                  <el-button link type="danger" @click="removeMaterial(scope.row)">閸掔娀娅?/el-button>
                </template>
              </el-table-column>
            </el-table>
          </article>

          <article class="assignment-detail-card">
            <div class="assignment-detail-card__head"><h3>閹绘劒姘﹂弬鍥︽閹懎鍠?/h3></div>
            <div class="assignment-placeholder-list">
              <div>
                <strong>閺傚洣娆㈤弰搴ｇ矎閹恒儱褰?/strong>
                <p>閸氬海顏ぐ鎾冲閺堫亝褰佹笟娑欏瘻閹绘劒姘﹂悧鍫熸拱鏉╂柨娲栭弬鍥︽閺勫海绮忛惃鍕复閸欙綇绱濇潻娆撳櫡閸忓牅绻氶悾娆忓缁旑垶顎囬弸璁圭礉鐎圭偤妾弬鍥︽閸掓銆冮崷銊﹀絹娴溿倛顕涢幆鍛淬€夐幍鎸庡复閵?/p>
              </div>
              <div>
                <strong>瑜版挸澧犻崣顖濐潌閺佺増宓?/strong>
                <p>瀹歌弓绮犻幓鎰唉濮掑倽顫嶆稉顓犵埠鐠佲剝鈧粯褰佹禍銈嗘殶 {{ submissions.length }}閿涘苯鍙炬稉顓⌒掗弸鎰灇閸旂喐鏋冩禒鑸碘偓缁樻殶 {{ totalParseOkFiles }}閵?/p>
              </div>
            </div>
          </article>
        </div>
      </div>

      <div v-else class="assignment-detail-panel">
        <div class="assignment-detail-two-col">
          <article class="assignment-detail-card assignment-detail-card--flex">
            <div class="assignment-detail-card__head"><h3>Job 閸樺棗褰?/h3><span>{{ jobs.length }} 娑?/span></div>
            <el-table :data="jobs" height="100%" empty-text="閺嗗倹妫ら弻銉╁櫢娴犺濮?>
              <el-table-column prop="id" label="Job ID" min-width="150" />
              <el-table-column prop="status" label="閻樿埖鈧? width="110" />
              <el-table-column label="鏉╂稑瀹? width="120"><template #default="scope">{{ scope.row.progressDone }}/{{ scope.row.progressTotal }}</template></el-table-column>
              <el-table-column label="閹笛嗩攽濡€崇础" width="110"><template #default="scope">{{ scope.row.executionMode }}</template></el-table-column>
              <el-table-column label="闂冨牆鈧? width="90"><template #default="scope">{{ scope.row.threshold }}</template></el-table-column>
              <el-table-column label="TopK" width="90"><template #default="scope">{{ scope.row.topK }}</template></el-table-column>
              <el-table-column label="閸涙垝鑵戠紒鎾寸亯" width="100"><template #default="scope">{{ scope.row.thresholdMatchedPairs }}</template></el-table-column>
              <el-table-column label="閹垮秳缍? width="110" align="center">
                <template #default="scope"><el-button link type="primary" @click="selectJob(scope.row.id)">閺屻儳婀呴幎銉ユ啞</el-button></template>
              </el-table-column>
            </el-table>
          </article>

        <article class="assignment-detail-card">
            <div class="assignment-detail-card__head"><h3>閹躲儱鎲￠幗妯款洣</h3><span>{{ pairs.length }} 鐎?/span></div>
            <p class="assignment-meta-text">{{ reportMessage || '閺嗗倹妫ら崣顖滄暏閹躲儱鎲￠妴? }}</p>
            <div class="assignment-report-summary">
              <div><span>閺堚偓娴ｅ骸鍨庨弫?/span><strong>{{ reportFilters.minScore }}</strong></div>
              <div><span>TopK</span><strong>{{ reportFilters.perStudentTopK }}</strong></div>
              <div><span>婢跺嫮鎮婇悩鑸碘偓?/span><strong>{{ selectedPairStatusLabel }}</strong></div>
              <div><span>閹烘帒绨弬鐟扮础</span><strong>{{ selectedSortLabel }}</strong></div>
              <div><span>娑撳秴褰插В鏂款嚠</span><strong>{{ incomparableSubmissions.length }}</strong></div>
            </div>
            <div class="assignment-detail-toolbar">
              <el-select v-model="selectedPairStatus" class="assignment-detail-toolbar__select" placeholder="婢跺嫮鎮婇悩鑸碘偓?>
                <el-option v-for="item in pairStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-select v-model="selectedSortOption" class="assignment-detail-toolbar__select" placeholder="閹烘帒绨弬鐟扮础">
                <el-option v-for="item in sortOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-button :disabled="!activeJobId" @click="selectJob(activeJobId)">閸掗攱鏌婇幎銉ユ啞</el-button>
              <el-dropdown :disabled="!activeJobId" @command="handleExportReport">
                <el-button :disabled="!activeJobId">鐎电厧鍤紒鎾寸亯</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="csv">鐎电厧鍤?CSV</el-dropdown-item>
                    <el-dropdown-item command="zip">鐎电厧鍤?ZIP 閺佸鎮婇崠?/el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <div class="assignment-status-pills">
              <button
                v-for="item in pairStatusChips"
                :key="item.value"
                type="button"
                class="assignment-status-pill"
                :class="{ active: selectedPairStatus === item.value }"
                @click="selectedPairStatus = item.value"
              >
                <span>{{ item.label }}</span>
                <strong>{{ item.count }}</strong>
              </button>
            </div>
            <div v-if="reportStats" class="assignment-report-summary assignment-report-summary--metrics">
              <div><span>閸欘垱鐦€佃褰佹禍?/span><strong>{{ reportStats.comparableSubmissionCount }}</strong></div>
              <div><span>閻炲棜顔?Pair</span><strong>{{ reportStats.comparablePairCount }}</strong></div>
              <div><span>婢堆冪毈鐠哄疇绻?/span><strong>{{ reportStats.sizeSkippedPairs }}</strong></div>
              <div><span>缁銆婄捄瀹犵箖</span><strong>{{ reportStats.bucketSkippedPairs }}</strong></div>
              <div><span>鐎瑰本鏆ｇ拋锛勭暬</span><strong>{{ reportStats.fullCalculatedPairs }}</strong></div>
              <div><span>閸涙垝鑵戦梼鍫濃偓?/span><strong>{{ reportStats.thresholdMatchedPairs }}</strong></div>
              <div><span>閹笛嗩攽濡€崇础</span><strong>{{ reportStats.executionMode }}</strong></div>
              <div><span>婢跺秶鏁ら弶銉︾爱</span><strong>{{ reportStats.reusedFromJobId || '-' }}</strong></div>
            </div>
          </article>
        </div>

        <article class="assignment-detail-card assignment-detail-card--flex">
          <div class="assignment-detail-card__head"><h3>閻╅晲鎶€鐎?/h3><span>閻愮懓鍤潻娑樺弳閻╅晲鎶€鐎电顕涢幆?/span></div>
          <el-table :data="pairs" height="100%" empty-text="閺嗗倹妫ら惄闀愭妧鐎?>
            <el-table-column prop="studentA" label="鐎涳妇鏁?A" min-width="120" />
            <el-table-column prop="studentB" label="鐎涳妇鏁?B" min-width="120" />
            <el-table-column prop="score" label="閻╅晲鎶€鎼? width="100" />
            <el-table-column prop="status" label="婢跺嫮鎮婇悩鑸碘偓? width="140" />
            <el-table-column prop="teacherNote" label="閺佹瑥绗€婢跺洦鏁? min-width="160" show-overflow-tooltip />
            <el-table-column label="閹垮秳缍? width="120" align="center">
              <template #default="scope">
                <el-button link type="primary" @click="goToPairDetail(scope.row)">閺屻儳婀呯拠锔藉剰</el-button>
              </template>
            </el-table-column>
          </el-table>
        </article>
      </div>
    </section>

    <el-dialog v-model="reopenDialogVisible" title="闁插秵鏌婂鈧崥顖欑稊娑? width="420px">
      <el-form label-position="top">
        <el-form-item label="閺傛壆娈戦幋顏咁剾閺冨爼妫?>
          <el-date-picker
            v-model="reopenEndAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="闁瀚ㄩ弬鎵畱閹搭亝顒涢弮鍫曟？"
            class="assignment-detail-dialog__picker"
          />
        </el-form-item>
        <el-form-item label="鐠囧瓨妲戦敍鍫モ偓澶婏綖閿?>
          <el-input
            v-model="reopenReason"
            type="textarea"
            :rows="4"
            maxlength="120"
            show-word-limit
            placeholder="娓氬顩ч敍姘乘夋禍銈囩崶閸?/ 缂冩垹绮堕弫鍛存鐞涖儲褰佹禍銈夆偓姘朵壕"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reopenDialogVisible = false">閸欐牗绉?/el-button>
        <el-button type="primary" :loading="reopenLoading" @click="submitReopen">绾喛顓婚柌宥呯磻</el-button>
      </template>
    </el-dialog>
  </div>

  <div v-else class="assignment-detail-empty">
    <el-empty description="閺堫亝澹橀崚鎷岊嚉娴ｆ粈绗?>
      <AppBackButton label="返回作业总览" @click="goBack" />
    </el-empty>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createTeacherPlagiarismJob,
  deleteTeacherAssignmentMaterial,
  exportTeacherPlagiarismReport,
  fetchTeacherAssignmentDetail,
  fetchTeacherAssignmentPlagiarism,
  fetchTeacherAssignmentStats,
  fetchTeacherAssignmentSubmissions,
  fetchTeacherPlagiarismReport,
  reopenTeacherAssignment,
  getTeacherAssignmentMaterialDownloadUrl,
  updateTeacherAssignment,
  uploadTeacherAssignmentMaterials
} from '../../api/teacherAssignments'
import { formatBytes } from './assignmentMappers'
import AppBackButton from '../../components/AppBackButton.vue'
const route = useRoute()
const router = useRouter()

const detailTabs = [
  { value: 'basic', label: '閸╃儤婀版穱鈩冧紖' },
  { value: 'submissions', label: '閹绘劒姘﹂幆鍛枌' },
  { value: 'files', label: '閺傚洣娆㈤幆鍛枌' },
  { value: 'plagiarism', label: '閺屻儵鍣哥拋鏉跨秿' }
]
const pairStatusOptions = [
  { value: '', label: '閸忋劑鍎存径鍕倞閻樿埖鈧? },
  { value: 'PENDING', label: '瀵板懎顦查弽? },
  { value: 'CONFIRMED', label: '瀹歌尙鈥樼拋? },
  { value: 'FALSE_POSITIVE', label: '鐠囶垱濮? }
]
const sortOptions = [
  { value: 'score_desc', label: '閹稿鍨庨弫棰佺矤妤傛ê鍩屾担? },
  { value: 'score_asc', label: '閹稿鍨庨弫棰佺矤娴ｅ骸鍩屾? },
  { value: 'status_asc', label: '閹稿顦╅悶鍡欏Ц閹? },
  { value: 'studentA_asc', label: '閹稿顒熼悽鐑? },
  { value: 'pairId_desc', label: '閹稿绮ㄩ弸娣欴閺堚偓閺? }
]

const currentTab = ref('basic')
const assignment = ref(null)
const stats = ref(null)
const submissions = ref([])
const materials = ref([])
const jobs = ref([])
const pairs = ref([])
const incomparableSubmissions = ref([])
const reportMessage = ref('')
const reportStats = ref(null)
const jobLoading = ref(false)
const endAssignmentLoading = ref(false)
const reopenLoading = ref(false)
const activeJobId = ref('')
const reopenDialogVisible = ref(false)
const reopenEndAt = ref('')
const reopenReason = ref('')
const reportFilters = ref({ minScore: 80, perStudentTopK: 10, statuses: [], sortBy: 'score', sortDirection: 'desc' })
const selectedPairStatus = ref('')
const selectedSortOption = ref('score_desc')

const totalParseOkFiles = computed(() => submissions.value.reduce((sum, item) => sum + Number(item.parseOkFiles || 0), 0))
const pairStatusChips = computed(() => {
  const counts = pairs.value.reduce((acc, item) => {
    const key = item.status || 'PENDING'
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, { PENDING: 0, CONFIRMED: 0, FALSE_POSITIVE: 0 })
  return [
    { value: '', label: '閸忋劑鍎?, count: pairs.value.length },
    { value: 'PENDING', label: '瀵板懎顦查弽?, count: counts.PENDING || 0 },
    { value: 'CONFIRMED', label: '瀹歌尙鈥樼拋?, count: counts.CONFIRMED || 0 },
    { value: 'FALSE_POSITIVE', label: '鐠囶垱濮?, count: counts.FALSE_POSITIVE || 0 }
  ]
})
const selectedPairStatusLabel = computed(() => pairStatusOptions.find((item) => item.value === selectedPairStatus.value)?.label || '閸忋劑鍎存径鍕倞閻樿埖鈧?)
const selectedSortLabel = computed(() => sortOptions.find((item) => item.value === selectedSortOption.value)?.label || '閹稿鍨庨弫棰佺矤妤傛ê鍩屾担?)

onMounted(() => {
  loadPage()
})

watch(selectedPairStatus, (value) => {
  reportFilters.value = {
    ...reportFilters.value,
    statuses: value ? [value] : []
  }
  if (activeJobId.value) {
    selectJob(activeJobId.value)
  }
})

watch(selectedSortOption, (value) => {
  const [sortBy, sortDirection] = String(value || 'score_desc').split('_')
  reportFilters.value = {
    ...reportFilters.value,
    sortBy: sortBy || 'score',
    sortDirection: sortDirection || 'desc'
  }
  if (activeJobId.value) {
    selectJob(activeJobId.value)
  }
})

async function loadPage() {
  const assignmentId = route.params.assignmentId
  const [detail, statsResult, submissionResult, plagiarismResult] = await Promise.all([
    fetchTeacherAssignmentDetail(assignmentId),
    fetchTeacherAssignmentStats(assignmentId),
    fetchTeacherAssignmentSubmissions(assignmentId),
    fetchTeacherAssignmentPlagiarism(assignmentId, reportFilters.value)
  ])

  assignment.value = detail
  stats.value = statsResult
  submissions.value = submissionResult || []
  materials.value = detail?.materials || []
  jobs.value = plagiarismResult?.jobs || []
  activeJobId.value = plagiarismResult?.activeJobId || ''
  pairs.value = plagiarismResult?.report?.pairs || []
  incomparableSubmissions.value = plagiarismResult?.report?.incomparableSubmissions || []
  reportMessage.value = plagiarismResult?.report?.message || ''
  reportStats.value = plagiarismResult?.report?.jobStats || null
}

async function handleMaterialSelect(uploadFile) {
  const raw = uploadFile?.raw
  if (!raw || !assignment.value?.id) return
  await uploadTeacherAssignmentMaterials(assignment.value.id, [raw])
  ElMessage.success('鐠у嫭鏋″韫瑐娴?)
  await loadPage()
}

function downloadMaterial(material) {
  window.open(getTeacherAssignmentMaterialDownloadUrl(assignment.value.id, material.id), '_blank')
}

async function removeMaterial(material) {
  await ElMessageBox.confirm(`绾喛顓婚崚鐘绘珟鐠у嫭鏋?${material.originalName} 閸氭绱礰, '閸掔娀娅庣涵顔款吇', { type: 'warning' })
  await deleteTeacherAssignmentMaterial(assignment.value.id, material.id)
  ElMessage.success('鐠у嫭鏋″鎻掑灩闂?)
  await loadPage()
}

async function handleCreatePlagiarismJob() {
  if (!assignment.value?.id) return
  jobLoading.value = true
  try {
    const job = await createTeacherPlagiarismJob(assignment.value.id, reportFilters.value)
    activeJobId.value = job.id
    ElMessage.success(`瀹告彃鍨卞鐑樼叀闁插秳鎹㈤崝?#${job.id}`)
    currentTab.value = 'plagiarism'
    await loadPage()
  } finally {
    jobLoading.value = false
  }
}

async function handleEndAssignment() {
  if (!assignment.value?.id) return
  await ElMessageBox.confirm('绾喛顓婚幓鎰缂佹挻娼潻娆庨嚋娴ｆ粈绗熼崥妤嬬吹缂佹挻娼崥搴☆劅閻㈢喎鐨㈤弮鐘崇《缂佈呯敾閹绘劒姘﹂妴?, '閹绘劕澧犵紒鎾存将娴ｆ粈绗?, {
    type: 'warning',
    confirmButtonText: '绾喛顓荤紒鎾存将',
    cancelButtonText: '閸欐牗绉?
  })

  endAssignmentLoading.value = true
  try {
    await updateTeacherAssignment(assignment.value.id, {
      title: assignment.value.title,
      language: assignment.value.language,
      classIds: assignment.value.classIds,
      startAt: assignment.value.startAt,
      endAt: new Date(),
      description: assignment.value.description,
      allowResubmit: assignment.value.allowResubmit,
      allowLateSubmit: assignment.value.allowLateSubmit,
      maxFiles: assignment.value.maxFiles
    })
    ElMessage.success('娴ｆ粈绗熷鍙夊絹閸撳秶绮ㄩ弶?)
    await loadPage()
  } finally {
    endAssignmentLoading.value = false
  }
}

function openReopenDialog() {
  const now = new Date()
  now.setHours(now.getHours() + 24)
  reopenEndAt.value = `${now.getFullYear()}-${`${now.getMonth() + 1}`.padStart(2, '0')}-${`${now.getDate()}`.padStart(2, '0')}T${`${now.getHours()}`.padStart(2, '0')}:${`${now.getMinutes()}`.padStart(2, '0')}:${`${now.getSeconds()}`.padStart(2, '0')}`
  reopenReason.value = ''
  reopenDialogVisible.value = true
}

async function submitReopen() {
  if (!assignment.value?.id) return
  if (!reopenEndAt.value) {
    ElMessage.warning('鐠囩兘鈧瀚ㄩ弬鎵畱閹搭亝顒涢弮鍫曟？')
    return
  }

  reopenLoading.value = true
  try {
    await reopenTeacherAssignment(assignment.value.id, {
      startAt: new Date(),
      endAt: reopenEndAt.value,
      reason: reopenReason.value
    })
    reopenDialogVisible.value = false
    ElMessage.success('娴ｆ粈绗熷鏌ュ櫢閺傛澘绱戦崥?)
    await loadPage()
  } finally {
    reopenLoading.value = false
  }
}

async function selectJob(jobId) {
  activeJobId.value = jobId
  const report = await fetchTeacherPlagiarismReport(jobId, reportFilters.value)
  pairs.value = report.pairs || []
  incomparableSubmissions.value = report.incomparableSubmissions || []
  reportMessage.value = report.message || ''
  reportStats.value = report.jobStats || null
}

async function handleExportReport(format = 'csv') {
  if (!activeJobId.value) return
  const blob = await exportTeacherPlagiarismReport(activeJobId.value, {
    ...reportFilters.value,
    format
  })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `plagiarism-report-${activeJobId.value}.${format === 'zip' ? 'zip' : 'csv'}`
  link.click()
  window.URL.revokeObjectURL(url)
}

function goToSubmissionDetail(row) {
  router.push({ path: `/teacher/submissions/${row.submissionId}`, query: { assignmentId: assignment.value.id } })
}

function goToPairDetail(row) {
  router.push({ path: `/teacher/similarity-pairs/${row.pairId}`, query: { assignmentId: assignment.value.id, jobId: activeJobId.value || '' } })
}

function goBack() {
  router.push('/teacher/assignments')
}
</script>

<style scoped>
.assignment-detail-page {
  height: 100%;
}

.assignment-detail-shell {
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

.assignment-detail-shell__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.assignment-detail-shell__header h1 {
  margin: 8px 0 6px;
  font-size: 32px;
  line-height: 1.06;
}

.assignment-detail-shell__header p,
.assignment-meta-text,
.assignment-detail-card__text {
  margin: 0;
  color: #6f7f96;
}

.assignment-detail-shell__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.assignment-detail-shell__status {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(18, 25, 38, 0.08);
  color: #53627a;
}

.assignment-detail-tabs {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(18, 25, 38, 0.06);
}

.assignment-detail-tabs__item {
  border: 0;
  padding: 10px 18px;
  border-radius: 999px;
  background: transparent;
  color: #68798f;
  cursor: pointer;
}

.assignment-detail-tabs__item.active {
  background: #121926;
  color: #fff;
}

.assignment-detail-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 0;
  flex: 1;
}

.assignment-detail-grid,
.assignment-detail-summary-row,
.assignment-detail-two-col,
.assignment-report-summary {
  display: grid;
  gap: 14px;
}

.assignment-detail-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.assignment-detail-summary-row {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.assignment-detail-two-col {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  min-height: 0;
  flex: 1;
}

.assignment-report-summary {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.assignment-report-summary--metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.assignment-detail-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
}

.assignment-detail-toolbar__select {
  width: 180px;
}

.assignment-status-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.assignment-status-pill {
  border: 1px solid rgba(127, 142, 163, 0.16);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  padding: 10px 14px;
  min-width: 104px;
  color: #5f7188;
  cursor: pointer;
  transition: all 0.2s ease;
}

.assignment-status-pill span,
.assignment-status-pill strong {
  display: block;
}

.assignment-status-pill strong {
  margin-top: 6px;
  font-size: 18px;
  color: #121926;
}

.assignment-status-pill.active {
  border-color: rgba(18, 25, 38, 0.22);
  background: #121926;
  color: rgba(255, 255, 255, 0.78);
}

.assignment-status-pill.active strong {
  color: #fff;
}

.assignment-detail-info-card,
.assignment-detail-summary-card,
.assignment-detail-card {
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(127, 142, 163, 0.14);
  box-shadow: 0 16px 38px rgba(160, 176, 199, 0.08);
}

.assignment-detail-info-card span,
.assignment-detail-summary-card span,
.assignment-report-summary span {
  color: #7f8ea3;
}

.assignment-detail-info-card strong,
.assignment-detail-summary-card strong,
.assignment-report-summary strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
}

.assignment-detail-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.assignment-detail-card--flex {
  min-height: 0;
  flex: 1;
}

.assignment-detail-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.assignment-detail-card__head h3 {
  margin: 0;
}

.assignment-placeholder-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.assignment-placeholder-list div {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(18, 25, 38, 0.04);
}

.assignment-placeholder-list strong {
  display: block;
  margin-bottom: 8px;
}

.assignment-placeholder-list p {
  margin: 0;
  color: #6f7f96;
  line-height: 1.7;
}

.assignment-detail-empty {
  display: grid;
  place-items: center;
  height: 100%;
}

.assignment-detail-dialog__picker {
  width: 100%;
}

@media (max-width: 1280px) {
  .assignment-detail-shell__header,
  .assignment-detail-shell__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .assignment-detail-grid,
  .assignment-detail-summary-row,
  .assignment-detail-two-col,
  .assignment-report-summary {
    grid-template-columns: 1fr;
  }
}
</style>



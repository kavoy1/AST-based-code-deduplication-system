<template>
  <div v-if="pairDetail" class="pair-detail-page">
    <section class="pair-detail-shell">
      <header class="pair-detail-shell__hero">
        <div>
          <AppBackButton label="返回结果列表" @click="goBack" />
          <p class="pair-detail-shell__eyebrow">Review Detail</p>
          <h1>{{ pairDetail.studentA }} 閳?{{ pairDetail.studentB }}</h1>
          <p class="pair-detail-shell__subtitle">
            閸忓牏婀呯紒鎾诡啈閿涘苯鍟€閻鐦夐幑顔衡偓鍌氭倵缂侇叀绻栭柌灞肩窗缂佈呯敾閹恒儱鍙嗘禒锝囩垳閻楀洦顔屽ǎ杈鐎佃鐦妴?          </p>
        </div>

        <div class="pair-detail-shell__actions">
          <span class="pair-detail-shell__pill" :class="`is-${risk.tone}`">{{ risk.label }}</span>
          <span class="pair-detail-shell__pill is-status">{{ pairStatusLabel }}</span>
          <span class="pair-detail-shell__score">{{ pairDetail.score }} 閸?/span>
        </div>
      </header>

      <section class="pair-detail-hero-cards">
        <article class="pair-detail-card pair-detail-card--hero">
          <span>閼颁礁绗€閸忓牏婀呮潻娆庨嚋閸掋倖鏌?/span>
          <strong>{{ risk.label }}</strong>
          <p>{{ conclusionText }}</p>
        </article>
        <article class="pair-detail-card">
          <span>瑜版挸澧犳径鍕倞閻樿埖鈧?/span>
          <strong>{{ pairStatusLabel }}</strong>
          <p>{{ statusHint }}</p>
        </article>
        <article class="pair-detail-card">
          <span>閺屻儵鍣告禒璇插</span>
          <strong>#{{ pairDetail.jobId }}</strong>
          <p>鏉╂瑧绮嶇紒鎾寸亯閺夈儴鍤滆ぐ鎾冲娴ｆ粈绗熼惃鍕濞嗏剝鐓￠柌宥勬崲閸斅扳偓?/p>
        </article>
      </section>

      <section class="pair-detail-layout">
        <article class="pair-detail-card pair-detail-card--evidence">
          <div class="pair-detail-card__head">
            <div>
              <h2>缁崵绮烘稉杞扮矆娑斿牊褰佺粈楦跨箹缂佸嫮绮ㄩ弸?/h2>
              <p>鏉╂瑩鍣锋稉宥呯潔缁€鍝勫斧婵?JSON閿涘矁鈧本妲搁幎濠佸瘜鐟曚礁鎳℃稉顓犵波閺嬪嫭鏆ｉ悶鍡樺灇閼颁礁绗€閺囨潙顔愰弰鎾舵倞鐟欙絿娈戠拠顓♀枅閵?/p>
            </div>
            <span>{{ evidenceViews.length }} 閺壜ょ槈閹?/span>
          </div>

          <div v-if="evidenceViews.length" class="pair-detail-evidence-list">
            <section v-for="item in evidenceViews" :key="item.id" class="pair-detail-evidence-item">
              <div class="pair-detail-evidence-item__top">
                <div>
                  <h3>{{ evidenceTitle(item) }}</h3>
                  <p>{{ item.summary || '缁崵绮哄Λ鈧ù瀣煂娑撱倓鍞ゆ禒锝囩垳閸︺劌顦跨粔宥囩波閺嬪嫪绗傜€涙ê婀弰搴㈡▔闁插秴鎮庨妴? }}</p>
                </div>
                <span class="pair-detail-evidence-item__weight">閺夊啴鍣?{{ item.weight }}</span>
              </div>

              <div v-if="item.topMatches.length" class="pair-detail-highlight-list">
                <div v-for="match in item.topMatches" :key="`${item.id}-${match.label}`" class="pair-detail-highlight">
                  <strong>{{ match.label }}</strong>
                  <span>閸忓崬鎮撻崨鎴掕厬 {{ match.matchedCount }} 濞?/span>
                  <small>鐠愶紕灏為崡鐘崇槷 {{ formatRatio(match.contributionRatio) }}</small>
                </div>
              </div>

              <div v-if="item.totals" class="pair-detail-totals">
                <div><span>鐎涳妇鏁?A 閼哄倻鍋ｉ弫?/span><strong>{{ item.totals.N1 }}</strong></div>
                <div><span>鐎涳妇鏁?B 閼哄倻鍋ｉ弫?/span><strong>{{ item.totals.N2 }}</strong></div>
                <div><span>閸栧綊鍘ら懞鍌滃仯閺?/span><strong>{{ item.totals.M }}</strong></div>
                <div><span>AC 閸?/span><strong>{{ formatAc(item.totals.AC) }}</strong></div>
              </div>

              <div v-if="hasParseFailures(item.parseFailures)" class="pair-detail-warning">
                鐎涙ê婀憴锝嗙€芥径杈Е閺傚洣娆㈤敍姘崇箹娴兼艾濂栭崫宥嗘拱濞喡ょ槈閹诡喚娈戠€瑰本鏆ｉ幀褝绱濇担鍡曠瑝閺€鐟板綁缁崵绮哄鑼病鐠囧棗鍩嗛崚鎵畱閸涙垝鑵戠紒鎾寸€妴?              </div>
            </section>
          </div>

          <el-empty v-else description="瑜版挸澧犲▽鈩冩箒閺囨潙顦跨紒鎾寸€拠浣瑰祦閸欘垰鐫嶇粈? />

          <section class="pair-detail-code-placeholder">
            <div class="pair-detail-card__head">
              <div>
                <h2>娴狅絿鐖滈悧鍥唽鐎佃鐦?/h2>
                <p>娑撳绔村銉ょ窗閸︺劏绻栭柌灞界潔缁€鍝勫讲閼充粙鐝惔锕傚櫢婢跺秶娈戞禒锝囩垳閻楀洦顔岄敍灞借嫙娴ｈ法鏁ゅǎ杈妤傛ü瀵掗獮鑸靛笓鐎佃鐦妴?/p>
              </div>
              <span>閸氬海鐢绘晶鐐插繁</span>
            </div>
            <div class="pair-detail-code-placeholder__panel">
              <div>
                <span>鐎涳妇鏁?A 閻楀洦顔?/span>
                <strong>缁涘绶熼崥搴ｎ伂鏉╂柨娲栨禒锝囩垳閻楀洦顔岀痪褑鐦夐幑?/strong>
              </div>
              <div>
                <span>鐎涳妇鏁?B 閻楀洦顔?/span>
                <strong>閸氬海鐢绘导姘暜閹镐焦绻侀懝鎻掕嫙閹烘帒顕В?/strong>
              </div>
            </div>
          </section>
        </article>

        <aside class="pair-detail-side">
          <article class="pair-detail-card">
            <div class="pair-detail-card__head">
              <div>
                <h2>閼颁礁绗€婢跺嫮鎮?/h2>
                <p>绾喛顓婚妴浣圭垼鐠佹媽顕ら幎銉﹀灗閻ｆ瑤绗呮径鍥ㄦ暈閿涘矂鍏樻导姘辨纯閹恒儱濂栭崫宥堢箹缂佸嫮绮ㄩ弸婊呮畱閸氬海鐢荤仦鏇犮仛閵?/p>
              </div>
            </div>

            <el-form label-position="top" :model="form">
              <el-form-item label="婢跺嫮鎮婇悩鑸碘偓?>
                <el-select v-model="form.status">
                  <el-option label="瀵板懓鈧礁绗€婢跺秵鐗? value="PENDING" />
                  <el-option label="閼颁礁绗€瀹歌尙鈥樼拋? value="CONFIRMED" />
                  <el-option label="閸掋倕鐣炬稉楦款嚖閹? value="FALSE_POSITIVE" />
                </el-select>
              </el-form-item>
              <el-form-item label="閺佹瑥绗€婢跺洦鏁?>
                <el-input
                  v-model="form.teacherNote"
                  type="textarea"
                  :rows="6"
                  placeholder="娓氬顩ч敍姘辩波閺嬪嫰鐝惔锔藉复鏉╂埊绱濆楦款唴鏉╂稐绔村銉ゆ眽瀹搞儲鐦€?
                />
              </el-form-item>
              <el-button type="primary" round :loading="saving" @click="saveStatus">娣囨繂鐡ㄦ径鍕倞缂佹挻鐏?/el-button>
            </el-form>
          </article>

          <article class="pair-detail-card">
            <div class="pair-detail-card__head">
              <div>
                <h2>AI 鐟欙綁鍣?/h2>
                <p>AI 閸欘亣绀嬬拹锝堢窡閸斺晞顕╅弰搴礉娑撳秴寮稉搴㈡付缂佸牆鍨界€规哎鈧?/p>
              </div>
              <el-button type="primary" plain round :loading="aiLoading" @click="generateAiExplanation">閻㈢喐鍨?AI 鐟欙綁鍣?/el-button>
            </div>

            <div v-if="pairDetail.latestAiExplanation?.result" class="pair-detail-ai-result">
              <p>{{ pairDetail.latestAiExplanation.result }}</p>
              <small>
                {{ pairDetail.latestAiExplanation.provider }} / {{ pairDetail.latestAiExplanation.model }} /
                {{ pairDetail.latestAiExplanation.status }}
              </small>
            </div>
            <el-empty v-else description="閺嗗倹妞傛潻妯荤梾閺?AI 鐟欙綁鍣? />
          </article>
        </aside>
      </section>
    </section>
  </div>

  <div v-else class="pair-detail-empty">
    <el-empty description="閺堫亝澹橀崚鎵祲娴艰偐绮ㄩ弸婊嗩嚊閹?>">
      <AppBackButton label="返回结果列表" @click="goBack" />
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
import { buildPairRisk, summarizeEvidenceList } from './assignmentMappers'
import AppBackButton from '../../components/AppBackButton.vue'

const route = useRoute()
const router = useRouter()

const pairDetail = ref(null)
const aiLoading = ref(false)
const saving = ref(false)
const form = reactive({ status: 'PENDING', teacherNote: '' })

const evidenceViews = computed(() => summarizeEvidenceList(pairDetail.value?.evidences || []))
const risk = computed(() => buildPairRisk(pairDetail.value?.score || 0))

const pairStatusLabel = computed(() => {
  const map = {
    PENDING: '瀵板懓鈧礁绗€婢跺秵鐗?,
    CONFIRMED: '閼颁礁绗€瀹歌尙鈥樼拋?,
    FALSE_POSITIVE: '閸掋倕鐣炬稉楦款嚖閹?
  }
  return map[pairDetail.value?.status] || pairDetail.value?.status || '-'
})

const conclusionText = computed(() => {
  if (!pairDetail.value) return ''
  if (pairDetail.value.teacherNote) return pairDetail.value.teacherNote
  if (pairDetail.value.status === 'CONFIRMED') return '閼颁礁绗€瀹歌尙绮＄涵顔款吇鏉╂瑧绮嶇紒鎾寸亯閸婄厧绶遍崗铏暈閿涘苯鎮楃紒顓炲讲缂佹挸鎮庨弫娆忣劅鐟曚焦鐪扮紒褏鐢绘径鍕倞閵?
  if (pairDetail.value.status === 'FALSE_POSITIVE') return '閼颁礁绗€瀹歌尙绮￠幎濠呯箹缂佸嫮绮ㄩ弸婊冨灲鐎规矮璐熺拠顖涘Г閿涘瞼閮寸紒鐔锋倵缂侇厺绱板鍗炲鐎瑰啰娈戠仦鏇犮仛娴兼ê鍘涚痪褋鈧?
  return `${risk.value.hint}閿涘苯缂撶拋顔煎帥閻绗呴弬纭呯槈閹诡喗鎲崇憰渚婄礉閸愬秴鍠呯€规碍妲搁崥锔锯€樼拋銈冣偓淇?})

const statusHint = computed(() => {
  if (pairDetail.value?.status === 'CONFIRMED') return '鏉╂瑧绮嶇紒鎾寸亯瀹歌尙绮＄€瑰本鍨氭径宥嗙壋閵?
  if (pairDetail.value?.status === 'FALSE_POSITIVE') return '鏉╂瑧绮嶇紒鎾寸亯瀹歌尙绮＄悮顐ｅ笓闂勩們鈧?
  return '鏉╂瑧绮嶇紒鎾寸亯鏉╂ê婀粵澶婄窡閼颁礁绗€閸嬫碍娓剁紒鍫濆灲閺傤厹鈧?
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
    ElMessage.success('婢跺嫮鎮婄紒鎾寸亯瀹歌弓绻氱€?)
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
    ElMessage.success('AI 鐟欙綁鍣村鑼晸閹?)
  } finally {
    aiLoading.value = false
  }
}

function evidenceTitle(item) {
  if (item.type === 'SIGNATURE_OVERLAP_TOP') return '缂佹挻鐎崨鎴掕厬閹芥顩?
  return item.type || '鐠囦焦宓侀幗妯款洣'
}

function formatRatio(value) {
  const number = Number(value || 0)
  return `${Math.round(number * 100)}%`
}

function formatAc(value) {
  const number = Number(value || 0)
  return number.toFixed(3)
}

function hasParseFailures(parseFailures) {
  return Boolean(parseFailures?.sideA?.length || parseFailures?.sideB?.length)
}

function goBack() {
  const assignmentId = route.query.assignmentId
  router.push(assignmentId ? `/teacher/assignments/${assignmentId}/plagiarism/results` : '/teacher/assignments')
}
</script>

<style scoped>
.pair-detail-page {
  height: 100%;
}

.pair-detail-shell {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 18px;
  padding: 24px 26px;
  border-radius: 32px;
  background:
    radial-gradient(circle at top right, rgba(255, 122, 161, 0.08), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(247, 250, 255, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
  overflow: hidden;
}

.pair-detail-shell__hero,
.pair-detail-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.pair-detail-shell__eyebrow {
  margin: 10px 0 8px;
  color: #ff5a86;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.pair-detail-shell__hero h1,
.pair-detail-card__head h2,
.pair-detail-evidence-item h3 {
  margin: 0;
  color: #121826;
}

.pair-detail-shell__hero h1 {
  font-size: 34px;
}

.pair-detail-shell__subtitle,
.pair-detail-card__head p,
.pair-detail-card p,
.pair-detail-ai-result p,
.pair-detail-warning,
.pair-detail-code-placeholder__panel span {
  margin: 0;
  color: #6f7f96;
  line-height: 1.6;
}

.pair-detail-shell__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pair-detail-shell__pill,
.pair-detail-shell__score {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 14px;
  border-radius: 999px;
  font-weight: 700;
}

.pair-detail-shell__pill {
  background: rgba(18, 24, 38, 0.06);
  color: #57677f;
}

.pair-detail-shell__pill.is-danger {
  background: rgba(255, 90, 134, 0.14);
  color: #ce2f68;
}

.pair-detail-shell__pill.is-warning {
  background: rgba(255, 167, 38, 0.14);
  color: #b56c00;
}

.pair-detail-shell__pill.is-neutral,
.pair-detail-shell__pill.is-status {
  background: rgba(18, 24, 38, 0.06);
  color: #55667f;
}

.pair-detail-shell__score {
  background: #121826;
  color: #fff;
}

.pair-detail-hero-cards {
  display: grid;
  grid-template-columns: 1.2fr repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.pair-detail-layout {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(340px, 0.78fr);
  gap: 16px;
}

.pair-detail-side,
.pair-detail-card--evidence {
  min-height: 0;
  display: grid;
  gap: 16px;
}

.pair-detail-card {
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(127, 142, 163, 0.14);
  box-shadow: 0 16px 38px rgba(160, 176, 199, 0.08);
}

.pair-detail-card--hero {
  background: linear-gradient(180deg, rgba(18, 24, 38, 0.96), rgba(33, 43, 70, 0.96));
}

.pair-detail-card--hero span,
.pair-detail-card--hero p {
  color: rgba(232, 238, 255, 0.78);
}

.pair-detail-card--hero strong {
  display: block;
  margin: 12px 0 10px;
  font-size: 34px;
  color: #fff;
}

.pair-detail-card span,
.pair-detail-totals span,
.pair-detail-highlight span,
.pair-detail-highlight small,
.pair-detail-ai-result small {
  color: #7b8aa2;
}

.pair-detail-card > strong {
  display: block;
  margin: 10px 0 8px;
  font-size: 28px;
  color: #121826;
}

.pair-detail-evidence-list {
  min-height: 0;
  display: grid;
  gap: 14px;
  overflow: auto;
  padding-right: 4px;
}

.pair-detail-evidence-item {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 20px;
  background: rgba(248, 250, 255, 0.92);
}

.pair-detail-evidence-item__top,
.pair-detail-totals {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.pair-detail-evidence-item__weight {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(18, 24, 38, 0.06);
  color: #55667f;
  font-size: 13px;
  font-weight: 700;
}

.pair-detail-highlight-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.pair-detail-highlight {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 18px;
  background: #121826;
}

.pair-detail-highlight strong {
  color: #fff;
}

.pair-detail-highlight span,
.pair-detail-highlight small {
  color: rgba(221, 231, 255, 0.8);
}

.pair-detail-totals {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.pair-detail-totals div {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.84);
}

.pair-detail-totals strong {
  color: #121826;
  font-size: 22px;
}

.pair-detail-warning {
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 167, 38, 0.12);
  color: #916000;
}

.pair-detail-code-placeholder {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(15, 22, 38, 0.98), rgba(30, 41, 66, 0.96));
}

.pair-detail-code-placeholder .pair-detail-card__head h2,
.pair-detail-code-placeholder .pair-detail-card__head p,
.pair-detail-code-placeholder .pair-detail-card__head span {
  color: #eef3ff;
}

.pair-detail-code-placeholder__panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.pair-detail-code-placeholder__panel > div {
  display: grid;
  gap: 10px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.06);
}

.pair-detail-code-placeholder__panel strong {
  color: #fff;
}

.pair-detail-ai-result {
  display: grid;
  gap: 10px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(18, 24, 38, 0.04);
}

.pair-detail-empty {
  display: grid;
  place-items: center;
  height: 100%;
}

@media (max-width: 1440px) {
  .pair-detail-hero-cards,
  .pair-detail-layout,
  .pair-detail-highlight-list,
  .pair-detail-totals,
  .pair-detail-code-placeholder__panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .pair-detail-shell {
    overflow: auto;
  }

  .pair-detail-shell__hero,
  .pair-detail-card__head,
  .pair-detail-evidence-item__top {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>




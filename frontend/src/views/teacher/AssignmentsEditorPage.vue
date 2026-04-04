<template>
  <section class="assignment-editor-page">
    <header class="assignment-editor-page__hero">
      <div>
        <p class="assignment-editor-page__eyebrow">Compose Assignment</p>
        <h1>{{ pageTitle }}</h1>
        <p>{{ pageDescription }}</p>
      </div>
      <AppBackButton label="返回总览" @click="goBack" />
    </header>

    <nav class="assignment-stepper" aria-label="閸欐垵绔峰銉╊€?>
      <button
        v-for="(step, index) in steps"
        :key="step.value"
        type="button"
        class="assignment-stepper__item"
        :class="{
          active: currentStep === step.value,
          done: index < currentStepIndex,
          locked: index > furthestUnlockedStep
        }"
        :disabled="index > furthestUnlockedStep"
        @click="jumpToStep(index)"
      >
        <span class="assignment-stepper__index">{{ index + 1 }}</span>
        <span class="assignment-stepper__text">
          <strong>{{ step.label }}</strong>
          <small>{{ step.hint }}</small>
        </span>
      </button>
    </nav>

    <section class="assignment-editor-page__workspace">
      <article class="assignment-editor-panel">
        <el-form label-position="top" :model="form" class="assignment-editor-form">
          <template v-if="currentStep === 'basic'">
            <div class="assignment-editor-panel__head">
              <div>
                <h2>閸╄櫣顢呮穱鈩冧紖</h2>
                <p>閸欘亙绻氶悾娆掆偓浣哥瑎閸欐垵绔锋担婊€绗熼張鈧敮鍝ユ暏閻ㄥ嫬鍤戞い鐟板敶鐎瑰箍鈧?/p>
              </div>
            </div>

            <div class="assignment-editor-grid assignment-editor-grid--two">
              <el-form-item label="娴ｆ粈绗熼弽鍥暯" required>
                <el-input v-model="form.title" maxlength="80" show-word-limit placeholder="娓氬顩ч敍娆絘va 闂嗗棗鎮庢稉搴＄磽鐢摜鎮ｉ崥鍫㈢矊娑? />
              </el-form-item>
              <el-form-item label="缂傛牜鈻肩拠顓♀枅" required>
                <el-select v-model="form.language" placeholder="闁瀚ㄧ拠顓♀枅">
                  <el-option v-for="item in languageOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="娴ｆ粈绗熺拠瀛樻">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="8"
                maxlength="800"
                show-word-limit
                placeholder="鏉堟挸鍙嗘０妯兼窗鐟曚焦鐪伴妴浣瑰絹娴溿倛顕╅弰搴℃嫲鐠囧嫬鍨庨柌宥囧仯"
              />
            </el-form-item>
          </template>

          <template v-else-if="currentStep === 'schedule'">
            <div class="assignment-editor-panel__head">
              <div>
                <h2>閻濐厾楠囨稉搴㈡闂?/h2>
                <p>绾喖鐣炬潻娆庡敜娴ｆ粈绗熼崣鎴濈缂佹瑨鐨濋敍灞间簰閸欏﹤婀禒鈧稊鍫熸閸婃瑥绱戞慨瀣嫲缂佹挻娼妴?/p>
              </div>
            </div>

            <el-form-item label="閸欐垵绔烽悵顓犻獓" required>
              <el-select v-model="form.classIds" multiple collapse-tags collapse-tags-tooltip placeholder="闁瀚ㄦ稉鈧稉顏呭灗婢舵矮閲滈悵顓犻獓">
                <el-option
                  v-for="item in classList"
                  :key="item.id"
                  :label="`${item.name} 璺?${item.studentCount} 娴滅"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>

            <div class="assignment-editor-grid assignment-editor-grid--two">
              <el-form-item label="瀵偓婵妞傞梻? required>
                <el-date-picker
                  v-model="form.startAt"
                  type="datetime"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  placeholder="闁瀚ㄥ鈧慨瀣闂?
                />
              </el-form-item>
              <el-form-item label="閹搭亝顒涢弮鍫曟？" required>
                <el-date-picker
                  v-model="form.endAt"
                  type="datetime"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  placeholder="闁瀚ㄩ幋顏咁剾閺冨爼妫?
                />
              </el-form-item>
            </div>

            <div class="assignment-inline-note">
              <span>瀹告煡鈧?{{ selectedClasses.length }} 娑擃亞褰痪?/span>
              <strong>妫板嫯顓告惔鏂炬唉 {{ selectedStudentCount }} 娴?/strong>
            </div>
          </template>

          <template v-else-if="currentStep === 'rules'">
            <div class="assignment-editor-panel__head">
              <div>
                <h2>閹绘劒姘︾憴鍕灟</h2>
                <p>閹跺﹤顒熼悽鐔告付鐎硅妲楅柆鍥у煂閻ㄥ嫭褰佹禍銈堢珶閻ｅ本褰侀崜宥堫嚛濞撳懏顨熼妴?/p>
              </div>
            </div>

            <div class="assignment-editor-rule-grid">
              <button type="button" class="assignment-editor-rule" :class="{ active: form.allowResubmit }" @click="form.allowResubmit = !form.allowResubmit">
                <span>閸忎浇顔忛柌宥咁槻閹绘劒姘?/span>
                <strong>{{ form.allowResubmit ? '瀵偓閸? : '閸忔娊妫? }}</strong>
                <small>瀵偓閸氼垰鎮楃€涳妇鏁撻崣顖炲櫢閺傜増褰佹禍銈嗘煀閻楀牊婀?/small>
              </button>

              <button type="button" class="assignment-editor-rule" :class="{ active: form.allowLateSubmit }" @click="form.allowLateSubmit = !form.allowLateSubmit">
                <span>閸忎浇顔忔潻鐔舵唉</span>
                <strong>{{ form.allowLateSubmit ? '瀵偓閸? : '閸忔娊妫? }}</strong>
                <small>閹搭亝顒涢崥搴㈡Ц閸氾箑鍘戠拋姝屗夋禍?/small>
              </button>

              <div class="assignment-editor-rule assignment-editor-rule--number">
                <span>閸楁洘顐奸張鈧径褎鏋冩禒鑸垫殶</span>
                <strong>{{ form.maxFiles }}</strong>
                <el-input-number v-model="form.maxFiles" :min="1" :max="50" />
              </div>
            </div>
          </template>

          <template v-else>
            <div class="assignment-editor-panel__head">
              <div>
                <h2>绾喛顓婚崣鎴濈</h2>
                <p>閺堚偓閸氬骸褰ч惇瀣彠闁款喕淇婇幁顖ょ礉绾喛顓婚弮鐘侯嚖閸氬骸鍟€閸欐垵绔烽妴?/p>
              </div>
            </div>

            <div class="assignment-confirm-compact">
              <article class="assignment-confirm-card">
                <span>娴ｆ粈绗熼弽鍥暯</span>
                <strong>{{ form.title || '閺堫亜锝為崘? }}</strong>
              </article>
              <article class="assignment-confirm-card">
                <span>缂傛牜鈻肩拠顓♀枅</span>
                <strong>{{ form.language }}</strong>
              </article>
              <article class="assignment-confirm-card">
                <span>閸欐垵绔烽悵顓犻獓</span>
                <strong>{{ selectedClasses.length ? `${selectedClasses.length} 娑擃亞褰痪顪?: '閺堫亪鈧瀚? }}</strong>
              </article>
              <article class="assignment-confirm-card">
                <span>妫板嫯顓告惔鏂炬唉</span>
                <strong>{{ selectedStudentCount }} 娴?/strong>
              </article>
              <article class="assignment-confirm-card">
                <span>瀵偓婵妞傞梻?/span>
                <strong>{{ displayDate(form.startAt) }}</strong>
              </article>
              <article class="assignment-confirm-card">
                <span>閹搭亝顒涢弮鍫曟？</span>
                <strong>{{ displayDate(form.endAt) }}</strong>
              </article>
              <article class="assignment-confirm-card">
                <span>闁插秴顦查幓鎰唉</span>
                <strong>{{ form.allowResubmit ? '閸忎浇顔? : '閸忔娊妫? }}</strong>
              </article>
              <article class="assignment-confirm-card">
                <span>鏉╃喍姘︾粵鏍殣</span>
                <strong>{{ form.allowLateSubmit ? '閸忎浇顔? : '閸忔娊妫? }}</strong>
              </article>
            </div>

            <div class="assignment-confirm-description">
              <strong>娴ｆ粈绗熺拠瀛樻</strong>
              <p>{{ form.description || '閺堫亜锝為崘娆庣稊娑撴俺顕╅弰搴涒偓? }}</p>
            </div>
          </template>
        </el-form>
      </article>

      <aside class="assignment-editor-aside">
        <article class="assignment-editor-aside__card">
          <span>瑜版挸澧犲銉╊€?/span>
          <strong>{{ currentStepMeta.label }}</strong>
          <small>{{ currentStepMeta.hint }}</small>
        </article>

        <article class="assignment-editor-aside__card">
          <span>閸欐垵绔烽幗妯款洣</span>
          <strong>{{ form.title || '閺堫亜鎳￠崥宥勭稊娑? }}</strong>
          <small>{{ selectedClasses.length }} 娑擃亞褰痪?璺?{{ selectedStudentCount }} 娴?璺?{{ form.language }}</small>
        </article>

        <article class="assignment-editor-aside__card">
          <span>閹绘劒姘︾憴鍕灟</span>
          <strong>{{ form.allowResubmit ? '閸欘垶鍣告径宥嗗絹娴? : '娴犲懍绔村▎鈩冨絹娴? }}</strong>
          <small>{{ form.allowLateSubmit ? '閸忎浇顔忔潻鐔舵唉' : '娑撳秴鍘戠拋姝岀箿娴? }} 璺?閺堚偓婢?{{ form.maxFiles }} 娑擃亝鏋冩禒?/small>
        </article>
      </aside>
    </section>

    <footer class="assignment-editor-page__footer">
      <div class="assignment-editor-page__footer-meta">
        <span>瑜版挸澧犲銉╊€?/span>
        <strong>{{ currentStepMeta.label }}</strong>
      </div>
      <div class="assignment-editor-page__footer-actions">
        <el-button v-if="currentStepIndex > 0" round @click="goPrevStep">娑撳﹣绔村?/el-button>
        <el-button v-if="currentStep !== 'confirm'" type="primary" round @click="goNextStep">娑撳绔村?/el-button>
        <el-button v-else type="primary" round :loading="submitLoading" @click="submitAssignment">
          {{ editingAssignmentId ? '娣囨繂鐡ㄦ穱顔芥暭' : '閸欐垵绔锋担婊€绗? }}
        </el-button>
      </div>
    </footer>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../api/request'
import AppBackButton from '../../components/AppBackButton.vue'
import {
  createTeacherAssignment,
  fetchTeacherAssignmentDetail,
  updateTeacherAssignment
} from '../../api/teacherAssignments'
import { formatDateTimeForInput, normalizeClasses } from './assignmentMappers'

const route = useRoute()
const router = useRouter()

const languageOptions = ['JAVA', 'PYTHON', 'C', 'C++', 'JAVASCRIPT', 'TYPESCRIPT', 'GO']
const steps = [
  { value: 'basic', label: '閸╄櫣顢呮穱鈩冧紖', hint: '閺嶅洭顣芥稉搴ゎ嚛閺? },
  { value: 'schedule', label: '閻濐厾楠囨稉搴㈡闂?, hint: '閸欐垵绔烽弮鍫曟？缁? },
  { value: 'rules', label: '閹绘劒姘︾憴鍕灟', hint: '鐎涳妇鏁撻幓鎰唉鏉堝湱鏅? },
  { value: 'confirm', label: '绾喛顓婚崣鎴濈', hint: '閺堚偓閸氬海鈥樼拋? }
]

const classList = ref([])
const submitLoading = ref(false)
const editingAssignmentId = ref('')
const currentStep = ref('basic')

const form = reactive({
  title: '',
  classIds: [],
  startAt: '',
  endAt: '',
  description: '',
  language: 'JAVA',
  allowResubmit: true,
  allowLateSubmit: false,
  maxFiles: 20
})

const selectedClasses = computed(() => classList.value.filter((item) => form.classIds.includes(item.id)))
const selectedStudentCount = computed(() => selectedClasses.value.reduce((sum, item) => sum + Number(item.studentCount || 0), 0))
const currentStepIndex = computed(() => steps.findIndex((item) => item.value === currentStep.value))
const currentStepMeta = computed(() => steps[currentStepIndex.value] || steps[0])
const pageTitle = computed(() => (editingAssignmentId.value ? '缂傛牞绶担婊€绗? : '閸欐垵绔锋担婊€绗?))
const pageDescription = computed(() => (
  editingAssignmentId.value
    ? '閹稿顒炴銈堢殶閺佺繝缍旀稉姘愁啎缂冾噯绱濇稉宥呭晙鐞氼偅鏆ｆい浣冦€冮崡鏇熷ⅵ閺傤厹鈧?
    : '娑撯偓濮濄儰绔村銉ョ暚閹存劕褰傜敮鍐跨礉鐠佲晞鈧礁绗€閸︺劋绔存稉顏勭潌楠炴洟鍣风€瑰本鍨氭稉鏄忣洣閹垮秳缍旈妴?
))

const isBasicStepComplete = computed(() => Boolean(form.title.trim() && form.language))
const isScheduleStepComplete = computed(() => {
  if (!form.classIds.length || !form.startAt || !form.endAt) return false
  return new Date(form.endAt).getTime() > new Date(form.startAt).getTime()
})
const isRulesStepComplete = computed(() => Number(form.maxFiles) >= 1)
const furthestUnlockedStep = computed(() => {
  if (!isBasicStepComplete.value) return 0
  if (!isScheduleStepComplete.value) return 1
  if (!isRulesStepComplete.value) return 2
  return 3
})

onMounted(async () => {
  await fetchClasses()
  if (route.params.assignmentId) {
    await fillEditForm(route.params.assignmentId)
  }
})

async function fetchClasses() {
  const result = await request.get('/teacher/classes', { params: { page: 1, limit: 100 } })
  classList.value = normalizeClasses(result)
}

async function fillEditForm(assignmentId) {
  const detail = await fetchTeacherAssignmentDetail(assignmentId)
  editingAssignmentId.value = String(assignmentId)
  Object.assign(form, {
    title: detail.title,
    classIds: [...detail.classIds],
    startAt: formatDateTimeForInput(detail.startAt),
    endAt: formatDateTimeForInput(detail.endAt),
    description: detail.description || '',
    language: detail.language || 'JAVA',
    allowResubmit: Boolean(detail.allowResubmit),
    allowLateSubmit: Boolean(detail.allowLateSubmit),
    maxFiles: Number(detail.maxFiles || 20)
  })
}

function validateStep(stepValue = currentStep.value) {
  if (stepValue === 'basic') {
    if (!form.title.trim()) {
      ElMessage.warning('鐠囧嘲鍘涙繅顐㈠晸娴ｆ粈绗熼弽鍥暯')
      return false
    }
    if (!form.language) {
      ElMessage.warning('鐠囩兘鈧瀚ㄧ紓鏍柤鐠囶叀鈻?)
      return false
    }
    return true
  }

  if (stepValue === 'schedule') {
    if (!form.classIds.length) {
      ElMessage.warning('鐠囩柉鍤︾亸鎴︹偓澶嬪娑撯偓娑擃亞褰痪?)
      return false
    }
    if (!form.startAt || !form.endAt) {
      ElMessage.warning('鐠囩兘鈧瀚ㄥ鈧慨瀣嫲閹搭亝顒涢弮鍫曟？')
      return false
    }
    if (new Date(form.endAt).getTime() <= new Date(form.startAt).getTime()) {
      ElMessage.warning('閹搭亝顒涢弮鍫曟？韫囧懘銆忛弲姘艾瀵偓婵妞傞梻?)
      return false
    }
    return true
  }

  if (stepValue === 'rules') {
    if (Number(form.maxFiles) < 1) {
      ElMessage.warning('閺堚偓婢堆勬瀮娴犺埖鏆熼懛鍐茬毌娑?1')
      return false
    }
    return true
  }

  return true
}

function jumpToStep(index) {
  if (index > furthestUnlockedStep.value) return
  currentStep.value = steps[index].value
}

function goNextStep() {
  if (!validateStep()) return
  const nextIndex = Math.min(currentStepIndex.value + 1, steps.length - 1)
  currentStep.value = steps[nextIndex].value
}

function goPrevStep() {
  const prevIndex = Math.max(currentStepIndex.value - 1, 0)
  currentStep.value = steps[prevIndex].value
}

async function submitAssignment() {
  if (!validateStep('basic') || !validateStep('schedule') || !validateStep('rules')) return

  submitLoading.value = true
  try {
    if (editingAssignmentId.value) {
      await updateTeacherAssignment(editingAssignmentId.value, form)
      ElMessage.success('娴ｆ粈绗熷鍙夋纯閺?)
      router.push(`/teacher/assignments/${editingAssignmentId.value}/settings`)
      return
    }

    await createTeacherAssignment(form)
    ElMessage.success('娴ｆ粈绗熷鎻掑灡瀵?)
    router.push('/teacher/assignments')
  } finally {
    submitLoading.value = false
  }
}

function displayDate(value) {
  return value ? formatDateTimeForInput(value).replace('T', ' ') : '閺堫亣顔曠純?
}

function goBack() {
  router.push('/teacher/assignments')
}
</script>

<style scoped>
.assignment-editor-page {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  gap: 12px;
  padding: 18px 20px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top right, rgba(111, 91, 255, 0.08), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(246, 249, 255, 0.93));
  border: 1px solid rgba(207, 217, 235, 0.6);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
  overflow: hidden;
}

.assignment-editor-page__eyebrow {
  margin: 0 0 8px;
  color: #7f66ff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.assignment-editor-page__hero,
.assignment-editor-page__footer,
.assignment-editor-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.assignment-editor-page__hero h1,
.assignment-editor-panel__head h2 {
  margin: 0;
  color: #101626;
}

.assignment-editor-page__hero h1 {
  font-size: 32px;
  line-height: 1.05;
}

.assignment-editor-page__hero p,
.assignment-editor-panel__head p,
.assignment-editor-page__footer-meta span,
.assignment-confirm-card span,
.assignment-editor-aside__card span {
  margin: 8px 0 0;
  color: #71829b;
}

.assignment-stepper {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.assignment-stepper__item,
.assignment-editor-rule {
  border: 1px solid rgba(207, 217, 235, 0.72);
  background: rgba(255, 255, 255, 0.84);
  cursor: pointer;
  transition: all 180ms ease;
}

.assignment-stepper__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 18px;
  text-align: left;
}

.assignment-stepper__item:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(179, 191, 216, 0.14);
}

.assignment-stepper__item:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.assignment-stepper__item.active {
  border-color: rgba(18, 25, 38, 0.16);
  background: #121926;
  color: #fff;
}

.assignment-stepper__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: rgba(18, 25, 38, 0.08);
  color: #101626;
  font-weight: 700;
  flex-shrink: 0;
}

.assignment-stepper__item.active .assignment-stepper__index {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

.assignment-stepper__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.assignment-stepper__text strong {
  font-size: 14px;
}

.assignment-stepper__text small {
  color: inherit;
  opacity: 0.72;
}

.assignment-editor-page__workspace {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) 280px;
  gap: 12px;
}

.assignment-editor-panel,
.assignment-editor-aside__card {
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(207, 217, 235, 0.72);
  box-shadow: 0 16px 38px rgba(179, 191, 216, 0.12);
}

.assignment-editor-panel {
  min-height: 0;
  overflow: auto;
}

.assignment-editor-form {
  display: grid;
  gap: 12px;
}

.assignment-editor-grid,
.assignment-editor-rule-grid,
.assignment-confirm-compact {
  display: grid;
  gap: 12px;
}

.assignment-editor-grid--two,
.assignment-editor-rule-grid,
.assignment-confirm-compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.assignment-inline-note {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(16, 22, 38, 0.04);
  color: #71829b;
}

.assignment-inline-note strong {
  color: #101626;
}

.assignment-editor-rule {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border-radius: 18px;
  text-align: left;
}

.assignment-editor-rule.active {
  border-color: rgba(80, 104, 241, 0.22);
  background: rgba(111, 91, 255, 0.08);
}

.assignment-editor-rule span,
.assignment-editor-rule small {
  color: #7788a1;
}

.assignment-editor-rule strong,
.assignment-confirm-card strong,
.assignment-editor-page__footer-meta strong,
.assignment-editor-aside__card strong {
  display: block;
  color: #101626;
}

.assignment-editor-rule--number {
  cursor: default;
}

.assignment-confirm-card {
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(248, 250, 251, 0.88);
  border: 1px solid rgba(207, 217, 235, 0.56);
}

.assignment-confirm-description {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(16, 22, 38, 0.04);
  color: #5c6e86;
  line-height: 1.6;
}

.assignment-confirm-description strong {
  color: #101626;
}

.assignment-confirm-description p {
  margin: 0;
}

.assignment-editor-aside {
  display: grid;
  gap: 12px;
  align-content: start;
}

.assignment-editor-aside__card {
  display: grid;
  gap: 6px;
}

.assignment-editor-page__footer {
  align-items: center;
  padding-top: 2px;
}

.assignment-editor-page__footer-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 1280px) {
  .assignment-stepper,
  .assignment-editor-page__workspace,
  .assignment-editor-grid--two,
  .assignment-editor-rule-grid,
  .assignment-confirm-compact {
    grid-template-columns: 1fr;
  }
}
</style>



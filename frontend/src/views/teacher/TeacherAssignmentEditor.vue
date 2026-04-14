<template>
  <section class="assignment-editor-page">
    <div class="assignment-editor-page__topbar">
      <AppBackButton label="返回总览" @click="goBack" />
    </div>

    <nav class="assignment-stepper" aria-label="发布步骤">
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
        <span class="assignment-stepper__text">{{ step.label }}</span>
      </button>
    </nav>

    <section class="assignment-editor-page__workspace">
      <article class="assignment-editor-panel">
        <el-form label-position="top" :model="form" class="assignment-editor-form">
          <template v-if="currentStep === 'basic'">
            <div class="assignment-editor-panel__head">
              <h2>基础信息</h2>
              <p>先填写作业标题、语言和题目说明。</p>
            </div>

            <div class="assignment-editor-grid assignment-editor-grid--two">
              <el-form-item label="作业标题" required>
                <el-input v-model="form.title" maxlength="80" show-word-limit placeholder="例如：Java 集合与异常综合练习" />
              </el-form-item>
              <el-form-item label="编程语言" required>
                <el-select v-model="form.language" placeholder="选择语言">
                  <el-option v-for="item in languageOptions" :key="item" :label="item" :value="item" />
                </el-select>
                <p class="assignment-editor-section-tip assignment-editor-section-tip--inline">
                  当前系统仅支持 Java 解析与查重，发布作业时先固定为 JAVA。
                </p>
              </el-form-item>
            </div>

            <el-form-item label="作业说明">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="8"
                maxlength="800"
                show-word-limit
                placeholder="输入题目要求、提交说明和评分重点"
              />
            </el-form-item>
          </template>

          <template v-else-if="currentStep === 'schedule'">
            <div class="assignment-editor-panel__head">
              <h2>班级与时间</h2>
              <p>确定发布范围以及开始、截止时间。</p>
            </div>

            <el-form-item label="发布班级" required>
              <el-select v-model="form.classIds" multiple collapse-tags collapse-tags-tooltip placeholder="选择一个或多个班级">
                <el-option
                  v-for="item in classList"
                  :key="item.id"
                  :label="`${item.name} · ${item.studentCount} 人`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>

            <p class="assignment-editor-section-tip">
              建议先选班级，再一次性设置统一的开始与截止时间，老师后续管理会更省心。
            </p>

            <div class="assignment-editor-grid assignment-editor-grid--schedule">
              <div class="assignment-editor-field-card">
                <div class="assignment-editor-field-card__title">
                  <span class="assignment-editor-field-card__required">*</span>
                  <span>开始时间</span>
                </div>
                <el-date-picker
                  v-model="form.startAt"
                  class="assignment-editor-field-card__picker"
                  type="datetime"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  placeholder="选择开始时间"
                />
              </div>
              <div class="assignment-editor-field-card">
                <div class="assignment-editor-field-card__title">
                  <span class="assignment-editor-field-card__required">*</span>
                  <span>截止时间</span>
                </div>
                <el-date-picker
                  v-model="form.endAt"
                  class="assignment-editor-field-card__picker"
                  type="datetime"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  placeholder="选择截止时间"
                />
              </div>
            </div>

            <div class="assignment-inline-note assignment-inline-note--summary">
              <div class="assignment-inline-note__group">
                <span>已选班级</span>
                <strong>{{ selectedClasses.length }} 个</strong>
              </div>
              <div class="assignment-inline-note__group">
                <span>预计应交</span>
                <strong>{{ selectedStudentCount }} 人</strong>
              </div>
            </div>
          </template>

          <template v-else-if="currentStep === 'rules'">
            <div class="assignment-editor-panel__head">
              <h2>提交规则</h2>
              <p>把学生提交时最常见的边界条件提前说明清楚。</p>
            </div>

            <div class="assignment-editor-rule-grid">
              <button type="button" class="assignment-editor-rule" :class="{ active: form.allowResubmit }" @click="form.allowResubmit = !form.allowResubmit">
                <span>允许重复提交</span>
                <strong>{{ form.allowResubmit ? '开启' : '关闭' }}</strong>
                <small>开启后学生可以重新提交新版本</small>
              </button>

              <button type="button" class="assignment-editor-rule" :class="{ active: form.allowLateSubmit }" @click="form.allowLateSubmit = !form.allowLateSubmit">
                <span>允许迟交</span>
                <strong>{{ form.allowLateSubmit ? '开启' : '关闭' }}</strong>
                <small>截止后是否允许补交</small>
              </button>

              <div class="assignment-editor-rule assignment-editor-rule--number">
                <span>单次最大文件数</span>
                <strong>{{ form.maxFiles }}</strong>
                <el-input-number v-model="form.maxFiles" :min="1" :max="50" />
              </div>
            </div>
          </template>

          <template v-else>
            <div class="assignment-editor-panel__head">
              <h2>确认发布</h2>
              <p>最后只看关键信息，确认无误后再发布。</p>
            </div>

            <div class="assignment-confirm-compact">
              <article class="assignment-confirm-card">
                <span>作业标题</span>
                <strong>{{ form.title || '未填写' }}</strong>
              </article>
              <article class="assignment-confirm-card">
                <span>编程语言</span>
                <strong>{{ form.language }}</strong>
              </article>
              <article class="assignment-confirm-card">
                <span>发布班级</span>
                <strong>{{ selectedClasses.length ? `${selectedClasses.length} 个班级` : '未选择' }}</strong>
              </article>
              <article class="assignment-confirm-card">
                <span>预计应交</span>
                <strong>{{ selectedStudentCount }} 人</strong>
              </article>
              <article class="assignment-confirm-card">
                <span>开始时间</span>
                <strong>{{ displayDate(form.startAt) }}</strong>
              </article>
              <article class="assignment-confirm-card">
                <span>截止时间</span>
                <strong>{{ displayDate(form.endAt) }}</strong>
              </article>
              <article class="assignment-confirm-card">
                <span>重复提交</span>
                <strong>{{ form.allowResubmit ? '允许' : '关闭' }}</strong>
              </article>
              <article class="assignment-confirm-card">
                <span>迟交策略</span>
                <strong>{{ form.allowLateSubmit ? '允许' : '关闭' }}</strong>
              </article>
            </div>

            <div class="assignment-confirm-description">
              <strong>作业说明</strong>
              <p>{{ form.description || '未填写作业说明。' }}</p>
            </div>
          </template>
        </el-form>

        <footer class="assignment-editor-panel__footer">
          <div class="assignment-editor-page__footer-actions">
            <button v-if="currentStepIndex > 0" type="button" class="assignment-editor-page__plain-action" @click="goPrevStep">
              上一步
            </button>
            <button v-if="currentStep !== 'confirm'" type="button" class="learn-more" @click="goNextStep">
              <span class="circle" aria-hidden="true">
                <span class="icon arrow"></span>
              </span>
              <span class="button-text">下一步</span>
            </button>
            <el-button v-else type="primary" round :loading="submitLoading" @click="submitAssignment">
              {{ editingAssignmentId ? '保存修改' : '发布作业' }}
            </el-button>
          </div>
        </footer>
      </article>
    </section>
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

const languageOptions = ['JAVA']
const steps = [
  { value: 'basic', label: '基础信息' },
  { value: 'schedule', label: '班级与时间' },
  { value: 'rules', label: '提交规则' },
  { value: 'confirm', label: '确认发布' }
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
    language: languageOptions.includes(detail.language) ? detail.language : 'JAVA',
    allowResubmit: Boolean(detail.allowResubmit),
    allowLateSubmit: Boolean(detail.allowLateSubmit),
    maxFiles: Number(detail.maxFiles || 20)
  })
}

function validateStep(stepValue = currentStep.value) {
  if (stepValue === 'basic') {
    if (!form.title.trim()) {
      ElMessage.warning('请先填写作业标题')
      return false
    }
    if (!form.language) {
      ElMessage.warning('请选择编程语言')
      return false
    }
    return true
  }

  if (stepValue === 'schedule') {
    if (!form.classIds.length) {
      ElMessage.warning('请至少选择一个班级')
      return false
    }
    if (!form.startAt || !form.endAt) {
      ElMessage.warning('请选择开始和截止时间')
      return false
    }
    if (new Date(form.endAt).getTime() <= new Date(form.startAt).getTime()) {
      ElMessage.warning('截止时间必须晚于开始时间')
      return false
    }
    return true
  }

  if (stepValue === 'rules') {
    if (Number(form.maxFiles) < 1) {
      ElMessage.warning('最大文件数至少为 1')
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
      ElMessage.success('作业已更新')
      router.push(`/teacher/assignments/${editingAssignmentId.value}/settings`)
      return
    }

    await createTeacherAssignment(form)
    ElMessage.success('作业已创建')
    router.push('/teacher/assignments')
  } finally {
    submitLoading.value = false
  }
}

function displayDate(value) {
  return value ? formatDateTimeForInput(value).replace('T', ' ') : '未设置'
}

function goBack() {
  router.push('/teacher/assignments')
}
</script>

<style scoped>
.assignment-editor-page {
  min-height: calc(100vh - 132px);
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 16px;
  padding: 20px 22px 22px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top right, rgba(111, 91, 255, 0.08), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(246, 249, 255, 0.93));
  border: 1px solid rgba(207, 217, 235, 0.6);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.assignment-editor-page__topbar {
  display: flex;
  justify-content: flex-end;
}

.assignment-stepper {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.assignment-stepper__item,
.assignment-editor-rule,
.assignment-editor-page__plain-action,
button.learn-more {
  position: relative;
  display: inline-block;
  cursor: pointer;
  outline: none;
  border: 0;
  vertical-align: middle;
  text-decoration: none;
  background: transparent;
  padding: 0;
  font-size: inherit;
  font-family: inherit;
}

.assignment-stepper__item,
.assignment-editor-rule {
  border: 1px solid rgba(207, 217, 235, 0.72);
  background: rgba(255, 255, 255, 0.84);
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
  font-size: 14px;
  font-weight: 700;
}

.assignment-editor-page__workspace {
  min-height: 0;
  display: grid;
  align-items: stretch;
}

.assignment-editor-panel {
  height: 100%;
  min-height: calc(100vh - 258px);
  display: flex;
  flex-direction: column;
  padding: 28px 30px 30px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(207, 217, 235, 0.72);
  box-shadow: 0 16px 38px rgba(179, 191, 216, 0.12);
}

.assignment-editor-form {
  display: grid;
  flex: 1;
  align-content: start;
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
  gap: 18px;
}

.assignment-editor-panel__head {
  display: grid;
  gap: 10px;
  max-width: 620px;
}

.assignment-editor-panel__head h2 {
  margin: 0;
  color: #101626;
  font-size: 28px;
}

.assignment-editor-panel__head p,
.assignment-confirm-card span {
  margin: 0;
  color: #71829b;
  line-height: 1.7;
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

.assignment-editor-grid--two {
  grid-template-columns: minmax(0, 1.12fr) minmax(320px, 1fr);
  align-items: start;
}

.assignment-editor-grid--schedule {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  align-items: start;
}

.assignment-editor-section-tip {
  margin: -2px 0 2px;
  color: #7a8ba3;
  line-height: 1.7;
}

.assignment-editor-section-tip--inline {
  margin: 10px 0 0;
}

.assignment-editor-field-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 18px 22px;
  border-radius: 22px;
  background: rgba(248, 250, 252, 0.72);
  border: 1px solid rgba(207, 217, 235, 0.56);
  align-self: start;
}

.assignment-editor-field-card__title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #344257;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.2;
}

.assignment-editor-field-card__required {
  color: #ff6a5c;
  font-size: 18px;
  line-height: 1;
}

.assignment-editor-field-card small {
  display: block;
  width: 100%;
  color: #7a8ba3;
  line-height: 1.6;
}

.assignment-editor-field-card__picker,
.assignment-editor-field-card :deep(.el-date-editor),
.assignment-editor-field-card :deep(.el-date-editor.el-input) {
  width: 100%;
  max-width: none;
}

.assignment-inline-note {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  border-radius: 16px;
  background: rgba(16, 22, 38, 0.04);
  color: #71829b;
}

.assignment-inline-note strong {
  color: #101626;
}

.assignment-inline-note--summary {
  gap: 18px;
}

.assignment-inline-note__group {
  display: grid;
  gap: 4px;
}

.assignment-inline-note__group span {
  color: #7a8ba3;
}

.assignment-inline-note__group strong {
  font-size: 26px;
  line-height: 1.1;
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
.assignment-confirm-card strong {
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

.assignment-editor-panel__footer {
  display: flex;
  justify-content: center;
  margin-top: auto;
  padding-top: 28px;
  border-top: 1px solid rgba(207, 217, 235, 0.56);
}

.assignment-editor-page__footer-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  width: 100%;
  padding: 2px 0 0;
}

.assignment-editor-page__plain-action {
  color: #7d889b;
  font-size: 15px;
  font-weight: 600;
}

button.learn-more {
  width: 12rem;
  height: auto;
}

button.learn-more .circle {
  transition: all 0.45s cubic-bezier(0.65, 0, 0.076, 1);
  position: relative;
  display: block;
  margin: 0;
  width: 3rem;
  height: 3rem;
  background: #282936;
  border-radius: 1.625rem;
}

button.learn-more .circle .icon {
  transition: all 0.45s cubic-bezier(0.65, 0, 0.076, 1);
  position: absolute;
  top: 0;
  bottom: 0;
  margin: auto;
  background: #fff;
}

button.learn-more .circle .icon.arrow {
  transition: all 0.45s cubic-bezier(0.65, 0, 0.076, 1);
  left: 0.625rem;
  width: 1.125rem;
  height: 0.125rem;
  background: none;
}

button.learn-more .circle .icon.arrow::before {
  position: absolute;
  content: '';
  top: -0.29rem;
  right: 0.0625rem;
  width: 0.625rem;
  height: 0.625rem;
  border-top: 0.125rem solid #fff;
  border-right: 0.125rem solid #fff;
  transform: rotate(45deg);
}

button.learn-more .button-text {
  transition: all 0.45s cubic-bezier(0.65, 0, 0.076, 1);
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 0.75rem 0;
  margin: 0 0 0 1.85rem;
  color: #282936;
  font-weight: 700;
  line-height: 1.6;
  text-align: center;
  text-transform: uppercase;
}

button.learn-more:hover .circle {
  width: 100%;
}

button.learn-more:hover .circle .icon.arrow {
  background: #fff;
  transform: translate(1rem, 0);
}

button.learn-more:hover .button-text {
  color: #fff;
}

.assignment-editor-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.assignment-editor-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  color: #344257;
  font-size: 16px;
  font-weight: 600;
}


.assignment-editor-form :deep(.el-input__wrapper),
.assignment-editor-form :deep(.el-select__wrapper),
.assignment-editor-form :deep(.el-date-editor.el-input__wrapper) {
  min-height: 54px;
  padding-inline: 16px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.92);
  box-shadow: inset 0 0 0 1px rgba(207, 217, 235, 0.72);
}

.assignment-editor-form :deep(.el-input__wrapper.is-focus),
.assignment-editor-form :deep(.el-select__wrapper.is-focused),
.assignment-editor-form :deep(.el-date-editor.el-input__wrapper.is-focus) {
  box-shadow: inset 0 0 0 1px rgba(94, 124, 255, 0.85);
}

.assignment-editor-form :deep(.el-textarea__inner) {
  min-height: 260px !important;
  padding: 18px 20px;
  border-radius: 22px;
  line-height: 1.75;
  background: rgba(248, 250, 252, 0.92);
  box-shadow: inset 0 0 0 1px rgba(207, 217, 235, 0.72);
  resize: vertical;
}

.assignment-editor-form :deep(.el-textarea .el-input__count) {
  right: 14px;
  bottom: 12px;
}

@media (max-width: 1280px) {
  .assignment-stepper,
  .assignment-editor-grid--two,
  .assignment-editor-grid--schedule,
  .assignment-editor-rule-grid,
  .assignment-confirm-compact {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .assignment-editor-page {
    padding: 18px 14px 18px;
  }

  .assignment-stepper {
    gap: 8px;
  }

  .assignment-stepper__item {
    padding: 10px 12px;
  }

  .assignment-editor-page__footer-actions {
    flex-direction: column;
  }
}
</style>



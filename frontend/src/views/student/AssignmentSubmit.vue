<template>
  <div class="workspace-page student-submit-page">
    <WorkspacePanel soft>
      <template #header>
        <div class="student-submit-toolbar">
          <div class="student-submit-toolbar__lead">
            <AppBackButton label="返回详情" @click="goBackDetail" />
            <div class="student-submit-toolbar__meta">
              <p class="student-submit-toolbar__eyebrow">提交作业</p>
              <h1>{{ assignment.title || '当前作业提交' }}</h1>
            </div>
          </div>
          <div class="student-submit-toolbar__actions">
            <span v-if="assignment.id && !loading" class="workspace-pill">截止 {{ assignment.endAtLabel }}</span>
            <el-button
              plain
              class="student-submit-toolbar__button"
              :loading="loading || currentLoading"
              @click="reloadAll"
            >
              <el-icon v-if="!(loading || currentLoading)"><RefreshRight /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <div class="student-submit-shell">
        <div v-if="loading" class="workspace-empty">
          <LoadingSpinner label="正在加载提交页面…" />
        </div>

        <WorkspaceEmpty
          v-else-if="!assignment.id"
          title="未找到作业"
          description="当前作业不存在，或你暂时没有访问权限。"
        />

        <template v-else>
          <div class="student-submit-topline">
            <div class="student-submit-mode" :class="{ 'student-submit-mode--single': !submissionPolicy.supportsTextMode }">
              <button :class="{ active: submissionMode === 'files' }" @click="submissionMode = 'files'">文件上传</button>
              <button :class="{ active: submissionMode === 'text' }" @click="submissionMode = 'text'">文本编辑</button>
            </div>

            <div class="student-submit-topline__meta">
              <span class="workspace-pill">{{ submitRuleLabel }}</span>
              <span class="workspace-pill workspace-pill--dynamic">{{ submissionPolicy.maxFilesSummary }}</span>
              <span class="workspace-pill">最多 {{ assignment.maxFiles || 1 }} 个文件</span>
            </div>
          </div>

          <div v-if="currentSubmission" class="student-submit-warning">
            <strong>当前已保留一份提交</strong>
            <span>再次提交会直接覆盖 {{ currentSubmission.submitTimeLabel }} 的内容。</span>
          </div>

          <div v-if="submissionPolicy.language === 'C'" class="student-submit-warning student-submit-warning--info">
            <strong>C 作业只支持文件上传</strong>
            <span>{{ submissionPolicy.detailGuidance }}</span>
          </div>

          <section v-if="submissionMode === 'files'" class="student-submit-section">
            <div class="student-submit-section__header student-submit-section__header--files">
              <p class="student-submit-section__summary">{{ submissionPolicy.uploadTitle }}</p>
              <h3>上传 Java 文件</h3>
            </div>

            <el-upload
              ref="uploadRef"
              class="student-upload"
              drag
              :multiple="submissionPolicy.effectiveFileLimit > 1"
              :auto-upload="false"
              :show-file-list="true"
              :limit="submissionPolicy.effectiveFileLimit"
              :accept="submissionPolicy.fileAccept"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
            >
              <el-icon class="student-upload__icon"><UploadFilled /></el-icon>
              <div class="student-upload__copy">
                <div class="student-upload__copy-title">{{ submissionPolicy.uploadTriggerText }}</div>
                <div class="student-upload__copy-tip">{{ submissionPolicy.uploadTip }}</div>
              </div>
              <div class="el-upload__text">拖拽到这里，或点击选择 `.java` 文件</div>
              <template #tip>
                <div class="el-upload__tip">只接受 `.java` 文件，新的文件集合会覆盖当前提交。</div>
              </template>
            </el-upload>

            <div class="student-submit-actions">
              <span class="workspace-pill">{{ fileSelectionLabel }}</span>
              <el-button type="primary" :loading="submittingFiles" :disabled="!canSubmitFiles" @click="submitFiles">
                提交并覆盖当前内容
              </el-button>
            </div>
          </section>

          <section v-else-if="submissionPolicy.supportsTextMode" class="student-submit-section">
            <div class="student-submit-section__header">
              <h3>文本编辑</h3>
              <el-button plain :disabled="!canReplaceSubmission" @click="addCodeEntry">新增文件</el-button>
            </div>

            <div class="student-editor">
              <aside class="student-editor__sidebar">
                <button
                  v-for="item in codeEntries"
                  :key="item.id"
                  type="button"
                  class="student-editor__file"
                  :class="{ active: item.id === activeEntryId }"
                  @click="activeEntryId = item.id"
                >
                  <span>{{ item.filename || '未命名文件' }}</span>
                  <small>{{ previewContent(item.content) }}</small>
                </button>
              </aside>

              <div class="student-editor__main">
                <template v-if="activeEntry">
                  <div class="student-editor__field">
                    <label>文件名</label>
                    <div class="student-editor__field-row">
                      <el-input v-model="activeEntry.filename" placeholder="例如 Main.java" clearable />
                      <el-button
                        text
                        type="danger"
                        :disabled="codeEntries.length === 1 || !canReplaceSubmission"
                        @click="removeActiveEntry"
                      >
                        删除文件
                      </el-button>
                    </div>
                  </div>

                  <div class="student-editor__field student-editor__field--grow">
                    <label>代码内容</label>
                    <el-input
                      v-model="activeEntry.content"
                      type="textarea"
                      :rows="16"
                      resize="none"
                      placeholder="请输入 Java 源代码"
                    />
                  </div>
                </template>
              </div>
            </div>

            <div class="student-submit-actions">
              <span class="workspace-pill">本次将提交 {{ normalizedDraftEntries.length }} 个文本文件</span>
              <el-button type="primary" :loading="submittingText" :disabled="!canSubmitText" @click="submitText">
                提交并覆盖当前内容
              </el-button>
            </div>
          </section>
        </template>
      </div>
    </WorkspacePanel>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RefreshRight, UploadFilled } from '@element-plus/icons-vue'
import {
  fetchStudentAssignmentDetail,
  fetchStudentCurrentSubmission,
  submitStudentAssignmentFiles,
  submitStudentAssignmentText
} from '../../api/student'
import AppBackButton from '../../components/AppBackButton.vue'
import WorkspaceEmpty from '../../components/workspace/WorkspaceEmpty.vue'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import LoadingSpinner from '../../components/LoadingSpinner.vue'
import {
  buildCodeEntriesFromCurrentSubmission,
  createEmptyCodeEntry,
  normalizeDraftEntries
} from './studentSubmissionHelpers'
import { buildStudentSubmissionPolicy } from './studentSubmissionRules'

const route = useRoute()
const router = useRouter()

const uploadRef = ref(null)
const assignment = ref({})
const currentSubmission = ref(null)
const loading = ref(false)
const currentLoading = ref(false)
const submittingFiles = ref(false)
const submittingText = ref(false)
const submissionMode = ref('files')
const selectedFiles = ref([])
const codeEntries = ref([createEmptyCodeEntry(0)])
const activeEntryId = ref(codeEntries.value[0].id)

const assignmentId = computed(() => route.params.assignmentId)
const classId = computed(() => route.query.classId || '')
const className = computed(() => route.query.className || '')
const activeEntry = computed(() => codeEntries.value.find((entry) => entry.id === activeEntryId.value) || codeEntries.value[0] || null)
const submissionPolicy = computed(() => buildStudentSubmissionPolicy(assignment.value))
const canReplaceSubmission = computed(() => {
  if (!assignment.value.canSubmit) return false
  if (!currentSubmission.value) return true
  return assignment.value.allowResubmit
})
const normalizedDraftEntries = computed(() => normalizeDraftEntries(codeEntries.value))
const canSubmitFiles = computed(() => canReplaceSubmission.value && selectedFiles.value.length > 0)
const canSubmitText = computed(
  () => submissionPolicy.value.supportsTextMode && canReplaceSubmission.value && normalizedDraftEntries.value.length > 0
)
const fileSelectionLabel = computed(() => {
  if (!selectedFiles.value.length) return '尚未选择文件'
  return `已选择 ${selectedFiles.value.length} 个文件`
})
const submitRuleLabel = computed(() => {
  if (!assignment.value?.id) return '当前提交会直接覆盖旧内容'
  if (!assignment.value.canSubmit) return '当前作业暂不可提交'
  return assignment.value.allowResubmit ? '当前提交会直接覆盖旧内容' : '本作业仅允许提交一次'
})

async function loadAssignment() {
  loading.value = true
  try {
    const detail = await fetchStudentAssignmentDetail(assignmentId.value, {
      classId: classId.value,
      className: className.value
    })
    assignment.value = {
      ...detail,
      maxFiles: buildStudentSubmissionPolicy(detail).effectiveFileLimit
    }
  } finally {
    loading.value = false
  }
}

watch(
  [submissionPolicy, submissionMode],
  ([policy, mode]) => {
    if (!policy.supportsTextMode && mode !== 'files') {
      submissionMode.value = 'files'
    }
  },
  { immediate: true }
)

async function loadCurrentSubmission() {
  currentLoading.value = true
  try {
    currentSubmission.value = await fetchStudentCurrentSubmission(assignmentId.value)
    syncDraftEntriesFromCurrentSubmission()
  } finally {
    currentLoading.value = false
  }
}

function syncDraftEntriesFromCurrentSubmission() {
  const drafts = currentSubmission.value
    ? buildCodeEntriesFromCurrentSubmission(currentSubmission.value)
    : [createEmptyCodeEntry(0)]
  codeEntries.value = drafts
  activeEntryId.value = drafts[0]?.id || ''
}

async function reloadAll() {
  await Promise.all([loadAssignment(), loadCurrentSubmission()])
}

function handleFileChange(file, fileList) {
  selectedFiles.value = fileList.map((item) => item.raw).filter(Boolean)
}

function handleFileRemove(file, fileList) {
  selectedFiles.value = fileList.map((item) => item.raw).filter(Boolean)
}

function addCodeEntry() {
  if (!canReplaceSubmission.value) return
  const entry = createEmptyCodeEntry(codeEntries.value.length)
  codeEntries.value.push(entry)
  activeEntryId.value = entry.id
}

function removeActiveEntry() {
  if (!activeEntry.value || codeEntries.value.length === 1) return
  const currentIndex = codeEntries.value.findIndex((entry) => entry.id === activeEntry.value.id)
  codeEntries.value = codeEntries.value.filter((entry) => entry.id !== activeEntry.value.id)
  const fallback = codeEntries.value[Math.max(0, currentIndex - 1)] || codeEntries.value[0]
  activeEntryId.value = fallback?.id || ''
}

function previewContent(content) {
  const cleaned = String(content || '').replace(/\s+/g, ' ').trim()
  return cleaned || '等待输入代码'
}

async function submitFiles() {
  if (!canSubmitFiles.value) return
  submittingFiles.value = true
  try {
    await submitStudentAssignmentFiles(assignmentId.value, selectedFiles.value)
    ElMessage.success('文件已提交，并覆盖当前内容')
    selectedFiles.value = []
    uploadRef.value?.clearFiles?.()
    await reloadAll()
  } finally {
    submittingFiles.value = false
  }
}

async function submitText() {
  if (!canSubmitText.value) return
  submittingText.value = true
  try {
    await submitStudentAssignmentText(assignmentId.value, normalizedDraftEntries.value)
    ElMessage.success(`已提交 ${normalizedDraftEntries.value.length} 个文本文件，并覆盖当前内容`)
    await reloadAll()
  } finally {
    submittingText.value = false
  }
}

function goBackDetail() {
  router.push({
    path: `/student/assignments/${assignmentId.value}`,
    query: {
      classId: classId.value,
      className: className.value
    }
  })
}

onMounted(reloadAll)
</script>

<style scoped>
.student-submit-page {
  gap: 16px;
}

.student-submit-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.student-submit-toolbar__lead {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.student-submit-toolbar__meta {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.student-submit-toolbar__eyebrow {
  margin: 0;
  color: #456da8;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.student-submit-toolbar__meta h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.06;
}

.student-submit-toolbar__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.student-submit-toolbar__button {
  min-width: 112px;
}

.student-submit-shell {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.student-submit-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.student-submit-topline__meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.student-submit-topline__meta .workspace-pill:last-child {
  display: none;
}

.student-submit-mode {
  display: inline-flex;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(72, 102, 142, 0.08);
}

.student-submit-mode button {
  border: 0;
  padding: 12px 18px;
  border-radius: 999px;
  background: transparent;
  color: var(--text-muted);
  font-weight: 700;
  cursor: pointer;
}

.student-submit-mode button.active {
  background: #fff;
  color: var(--text-title);
  box-shadow: 0 10px 20px rgba(31, 41, 55, 0.08);
}

.student-submit-mode--single button:nth-child(2) {
  display: none;
}

.student-submit-warning {
  display: grid;
  gap: 4px;
  padding: 12px 16px;
  border-radius: 18px;
  background: rgba(71, 115, 173, 0.08);
  color: #36527a;
}

.student-submit-warning strong,
.student-submit-warning span {
  display: block;
}

.student-submit-warning--info {
  background: rgba(76, 145, 120, 0.1);
  color: #2d5f4d;
}

.student-submit-section {
  display: grid;
  gap: 14px;
}

.student-submit-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.student-submit-section__header h3 {
  margin: 0;
  font-size: 20px;
}

.student-submit-section__header--files h3 {
  display: none;
}

.student-submit-section__summary {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-title);
}

.student-upload :deep(.el-upload-dragger) {
  border-radius: 24px;
  padding: 28px 24px;
  border-color: rgba(109, 128, 166, 0.18);
  background: rgba(255, 255, 255, 0.68);
}

.student-upload__icon {
  font-size: 30px;
  color: #4f7ac4;
}

.student-upload__copy {
  display: grid;
  gap: 6px;
  justify-items: center;
  margin-top: 8px;
}

.student-upload__copy-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-title);
}

.student-upload__copy-tip {
  max-width: 540px;
  color: var(--text-muted);
  line-height: 1.65;
}

.student-upload :deep(.el-upload__text),
.student-upload :deep(.el-upload__tip) {
  display: none;
}

.student-editor {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 16px;
  min-height: 420px;
  height: clamp(460px, 62vh, 720px);
  align-items: stretch;
}

.student-editor__sidebar {
  display: grid;
  gap: 10px;
  align-content: start;
  min-height: 0;
  max-height: 100%;
  overflow-y: auto;
  padding-right: 6px;
  scrollbar-width: thin;
  scrollbar-color: rgba(109, 128, 166, 0.28) transparent;
}

.student-editor__sidebar::-webkit-scrollbar {
  width: 8px;
}

.student-editor__sidebar::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(109, 128, 166, 0.24);
}

.student-editor__sidebar::-webkit-scrollbar-track {
  background: transparent;
}

.student-editor__file {
  width: 100%;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(109, 128, 166, 0.14);
  background: rgba(255, 255, 255, 0.74);
  text-align: left;
  cursor: pointer;
  display: grid;
  gap: 6px;
}

.student-editor__file.active {
  border-color: rgba(64, 108, 172, 0.28);
  background: rgba(236, 244, 255, 0.9);
  box-shadow: 0 14px 24px rgba(87, 119, 168, 0.1);
}

.student-editor__file span {
  font-weight: 700;
  color: var(--text-title);
}

.student-editor__file small {
  color: var(--text-muted);
  line-height: 1.5;
}

.student-editor__main {
  min-width: 0;
  min-height: 0;
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(109, 128, 166, 0.14);
  background: rgba(255, 255, 255, 0.74);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 16px;
}

.student-editor__field {
  display: grid;
  gap: 8px;
}

.student-editor__field--grow {
  min-height: 0;
  grid-template-rows: auto minmax(0, 1fr);
}

.student-editor__field--grow :deep(.el-textarea) {
  height: 100%;
}

.student-editor__field--grow :deep(.el-textarea__inner) {
  height: 100%;
  min-height: 280px !important;
}

.student-editor__field label {
  color: var(--text-muted);
  font-size: 13px;
}

.student-editor__field-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.student-submit-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

@media (max-width: 980px) {
  .student-submit-toolbar,
  .student-submit-topline,
  .student-submit-section__header,
  .student-submit-warning,
  .student-editor {
    grid-template-columns: 1fr;
    flex-direction: column;
    height: auto;
  }

  .student-editor {
    display: grid;
    grid-template-columns: 1fr;
  }

  .student-editor__sidebar {
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }

  .student-submit-toolbar__actions,
  .student-submit-topline__meta {
    justify-content: flex-start;
  }

  .student-submit-toolbar__lead {
    align-items: flex-start;
  }
}
</style>

<template>
  <div class="workspace-page student-submit-page">
    <WorkspaceShellSection>
      <template #tools>
        <AppBackButton label="返回作业详情" @click="goBackDetail" />
        <el-button @click="reloadAll">刷新</el-button>
      </template>
    </WorkspaceShellSection>

    <div v-if="assignment.id && !loading" class="student-submit-page__hero">
      <div>
        <div class="student-submit-page__eyebrow">{{ className || '当前班级' }} · 提交作业</div>
        <h1>{{ assignment.title }}</h1>
        <p>{{ assignment.description || '请在截止时间前提交代码，支持上传多个 .java 文件，也支持一次填写多份文本文件后统一提交。' }}</p>
      </div>
      <div class="student-submit-page__hero-side">
        <el-tag :type="assignment.statusTagType" round>{{ assignment.statusText }}</el-tag>
        <div class="workspace-pill">截止 {{ assignment.endAtLabel }}</div>
      </div>
    </div>

    <div class="student-submit-layout">
      <WorkspacePanel
        title="提交内容"
        :subtitle="assignment.canSubmit ? '先选择提交方式，再一次性提交当前版本。' : '当前作业已关闭提交，请返回详情页查看规则。'"
      >
        <div v-if="loading" class="workspace-empty">
          <el-skeleton :rows="6" animated />
        </div>

        <WorkspaceEmpty
          v-else-if="!assignment.id"
          title="未找到作业"
          description="当前作业不存在，或者你暂时没有访问权限。"
        />

        <template v-else>
          <div class="student-submit-page__segment">
            <button :class="{ active: submissionMode === 'files' }" @click="submissionMode = 'files'">文件上传</button>
            <button :class="{ active: submissionMode === 'text' }" @click="submissionMode = 'text'">文本提交</button>
          </div>

          <section v-if="submissionMode === 'files'" class="student-submit-section">
            <div class="student-submit-section__header">
              <div>
                <h3>上传 Java 文件</h3>
                <p>支持一次选择多个 `.java` 文件，系统会生成新的提交版本。</p>
              </div>
              <div class="workspace-pill">最多 {{ assignment.maxFiles || 1 }} 个文件</div>
            </div>

            <el-upload
              class="student-upload"
              drag
              multiple
              :auto-upload="false"
              :show-file-list="true"
              :limit="assignment.maxFiles || 1"
              accept=".java"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
            >
              <el-icon class="student-upload__icon"><UploadFilled /></el-icon>
              <div class="el-upload__text">拖拽到这里，或点击选择 `.java` 文件</div>
              <template #tip>
                <div class="el-upload__tip">只接受 `.java` 文件，超过数量限制会被拦截。</div>
              </template>
            </el-upload>

            <div class="student-submit-actions">
              <span class="workspace-pill">{{ fileNames.length ? `已选择 ${fileNames.length} 个文件` : '尚未选择文件' }}</span>
              <el-button type="primary" :loading="submittingFiles" :disabled="!canSubmitFiles" @click="submitFiles">提交文件</el-button>
            </div>
          </section>

          <section v-else class="student-submit-section">
            <div class="student-submit-section__header">
              <div>
                <h3>文本提交</h3>
                <p>每个输入块对应一个 Java 文件；你可以一次填写多份文本文件并统一提交。</p>
              </div>
              <el-button plain @click="addCodeEntry">新增文本文件</el-button>
            </div>

            <div class="student-text-entries">
              <article v-for="(entry, index) in codeEntries" :key="entry.id" class="student-text-entry">
                <div class="student-text-entry__top">
                  <div class="student-text-entry__index">文件 {{ index + 1 }}</div>
                  <el-button text type="danger" :disabled="codeEntries.length === 1" @click="removeCodeEntry(index)">删除</el-button>
                </div>

                <div class="student-text-entry__filename">
                  <label>文件名</label>
                  <el-input v-model="entry.filename" placeholder="例如 Main.java" clearable />
                </div>

                <div class="student-text-entry__content">
                  <label>代码内容</label>
                  <el-input
                    v-model="entry.content"
                    type="textarea"
                    :rows="10"
                    placeholder="请输入 Java 源代码"
                  />
                </div>
              </article>
            </div>

            <div class="student-submit-actions">
              <span class="workspace-pill">本次将提交 {{ validTextEntries.length }} 个文本文件</span>
              <el-button type="primary" :loading="submittingText" :disabled="!canSubmitText" @click="submitText">提交文本</el-button>
            </div>
          </section>
        </template>
      </WorkspacePanel>

      <div class="student-submit-sidebar">
        <WorkspacePanel title="提交状态" compact>
          <div v-if="loading || historyLoading" class="workspace-empty">
            <el-skeleton :rows="4" animated />
          </div>
          <template v-else>
            <div class="student-submit-status">
              <div class="student-submit-status__item">
                <span>当前状态</span>
                <strong>{{ assignment.canSubmit ? '可以提交' : '当前不可提交' }}</strong>
              </div>
              <div class="student-submit-status__item">
                <span>重复提交</span>
                <strong>{{ assignment.allowResubmit ? '允许' : '不允许' }}</strong>
              </div>
              <div class="student-submit-status__item">
                <span>最新版本</span>
                <strong>{{ latestSubmission ? `第 ${latestSubmission.version} 次提交` : '暂无提交' }}</strong>
              </div>
            </div>
          </template>
        </WorkspacePanel>

        <WorkspacePanel title="最近提交" compact>
          <div v-if="historyLoading" class="workspace-empty">
            <el-skeleton :rows="4" animated />
          </div>

          <WorkspaceEmpty
            v-else-if="submissionHistory.length === 0"
            title="暂无提交记录"
            description="提交成功后，最近的版本会优先显示在这里。"
          />

          <div v-else class="student-history-list">
            <article v-for="item in recentSubmissions" :key="item.id" class="student-history-card">
              <div class="student-history-card__header">
                <div>
                  <h4>第 {{ item.version }} 次提交</h4>
                  <p>{{ item.submitTimeLabel }}</p>
                </div>
                <span class="workspace-badge-soft" :class="item.isLatest ? 'workspace-badge-soft--green' : ''">
                  {{ item.isLatest ? '当前版本' : '历史版本' }}
                </span>
              </div>
              <div class="student-history-card__meta">
                <span>{{ item.isLate ? '迟交' : '按时提交' }}</span>
                <span>{{ item.parseOkFiles }}/{{ item.totalFiles }} 个文件解析成功</span>
              </div>
            </article>
          </div>
        </WorkspacePanel>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  fetchStudentAssignmentDetail,
  fetchStudentSubmissionHistory,
  submitStudentAssignmentFiles,
  submitStudentAssignmentText
} from '../../api/student'
import AppBackButton from '../../components/AppBackButton.vue'
import WorkspaceEmpty from '../../components/workspace/WorkspaceEmpty.vue'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import WorkspaceShellSection from '../../components/workspace/WorkspaceShellSection.vue'

const route = useRoute()
const router = useRouter()

let entrySeed = 0

const assignment = ref({})
const submissionHistory = ref([])
const loading = ref(false)
const historyLoading = ref(false)
const submittingFiles = ref(false)
const submittingText = ref(false)
const submissionMode = ref('files')
const selectedFiles = ref([])
const codeEntries = ref([createCodeEntry(0)])

const assignmentId = computed(() => route.params.assignmentId)
const classId = computed(() => route.query.classId || '')
const className = computed(() => route.query.className || '')
const latestSubmission = computed(() => submissionHistory.value[0] || null)
const recentSubmissions = computed(() => submissionHistory.value.slice(0, 3))
const fileNames = computed(() => selectedFiles.value.map((file) => file.name))
const canSubmitFiles = computed(() => assignment.value.canSubmit && selectedFiles.value.length > 0)
const validTextEntries = computed(() => normalizeTextEntries(codeEntries.value))
const canSubmitText = computed(() => assignment.value.canSubmit && validTextEntries.value.length > 0)

function createCodeEntry(index) {
  entrySeed += 1
  return {
    id: `${Date.now()}-${entrySeed}`,
    filename: buildDefaultFilename(index),
    content: ''
  }
}

function buildDefaultFilename(index) {
  return index === 0 ? 'Main.java' : `Main_${index + 1}.java`
}

function ensureJavaFilename(filename) {
  const trimmed = (filename || '').trim()
  if (!trimmed) return ''
  return trimmed.toLowerCase().endsWith('.java') ? trimmed : `${trimmed}.java`
}

function normalizeTextEntries(entries) {
  const used = new Set()
  return entries
    .filter((entry) => entry.content && entry.content.trim())
    .map((entry, index) => {
      let filename = ensureJavaFilename(entry.filename) || buildDefaultFilename(index)
      let candidate = filename
      let duplicateIndex = 2
      while (used.has(candidate.toLowerCase())) {
        const extensionIndex = filename.lastIndexOf('.')
        const baseName = extensionIndex >= 0 ? filename.slice(0, extensionIndex) : filename
        const extension = extensionIndex >= 0 ? filename.slice(extensionIndex) : '.java'
        candidate = `${baseName}_${duplicateIndex}${extension}`
        duplicateIndex += 1
      }
      used.add(candidate.toLowerCase())
      return {
        filename: candidate,
        content: entry.content
      }
    })
}

async function loadAssignment() {
  loading.value = true
  try {
    assignment.value = await fetchStudentAssignmentDetail(assignmentId.value, {
      classId: classId.value,
      className: className.value
    })
  } finally {
    loading.value = false
  }
}

async function loadSubmissionHistory() {
  historyLoading.value = true
  try {
    submissionHistory.value = await fetchStudentSubmissionHistory(assignmentId.value)
  } finally {
    historyLoading.value = false
  }
}

async function reloadAll() {
  await Promise.all([loadAssignment(), loadSubmissionHistory()])
}

function handleFileChange(file, fileList) {
  selectedFiles.value = fileList.map((item) => item.raw).filter(Boolean)
}

function handleFileRemove(file, fileList) {
  selectedFiles.value = fileList.map((item) => item.raw).filter(Boolean)
}

function addCodeEntry() {
  codeEntries.value.push(createCodeEntry(codeEntries.value.length))
}

function removeCodeEntry(index) {
  codeEntries.value.splice(index, 1)
  if (codeEntries.value.length === 0) {
    codeEntries.value = [createCodeEntry(0)]
  }
}

async function submitFiles() {
  if (!canSubmitFiles.value) return
  submittingFiles.value = true
  try {
    await submitStudentAssignmentFiles(assignmentId.value, selectedFiles.value)
    ElMessage.success('文件提交成功')
    selectedFiles.value = []
    await reloadAll()
  } finally {
    submittingFiles.value = false
  }
}

async function submitText() {
  if (!canSubmitText.value) return
  submittingText.value = true
  try {
    await submitStudentAssignmentText(assignmentId.value, validTextEntries.value)
    ElMessage.success(`已提交 ${validTextEntries.value.length} 个文本文件`)
    codeEntries.value = [createCodeEntry(0)]
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
  gap: 20px;
}

.student-submit-page__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 28px;
  border-radius: 30px;
  background:
    radial-gradient(circle at top right, rgba(135, 94, 255, 0.14), transparent 28%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 245, 255, 0.92));
  border: 1px solid rgba(91, 91, 214, 0.08);
  box-shadow: 0 20px 40px rgba(135, 94, 255, 0.08);
}

.student-submit-page__eyebrow {
  margin-bottom: 10px;
  color: #7b6fc8;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.student-submit-page__hero h1,
.student-submit-page__hero p {
  margin: 0;
}

.student-submit-page__hero h1 {
  font-size: 34px;
  line-height: 1.08;
}

.student-submit-page__hero p {
  margin-top: 14px;
  max-width: 760px;
  color: var(--text-body);
  line-height: 1.8;
}

.student-submit-page__hero-side {
  display: grid;
  justify-items: end;
  gap: 10px;
}

.student-submit-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.75fr);
  gap: 20px;
}

.student-submit-page__segment {
  display: inline-flex;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(109, 128, 166, 0.08);
  margin-bottom: 20px;
}

.student-submit-page__segment button {
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-weight: 700;
  padding: 12px 20px;
  border-radius: 999px;
  cursor: pointer;
}

.student-submit-page__segment button.active {
  background: #ffffff;
  color: var(--text-title);
  box-shadow: 0 10px 24px rgba(31, 41, 55, 0.08);
}

.student-submit-section {
  display: grid;
  gap: 18px;
}

.student-submit-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.student-submit-section__header h3,
.student-submit-section__header p {
  margin: 0;
}

.student-submit-section__header h3 {
  font-size: 22px;
  margin-bottom: 8px;
}

.student-submit-section__header p {
  color: var(--text-body);
  line-height: 1.7;
}

.student-upload :deep(.el-upload-dragger) {
  border-radius: 26px;
  padding: 36px 24px;
  border-color: rgba(109, 128, 166, 0.18);
  background: rgba(255, 255, 255, 0.66);
}

.student-upload__icon {
  font-size: 32px;
  color: #6f62d8;
}

.student-submit-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.student-text-entries {
  display: grid;
  gap: 16px;
}

.student-text-entry {
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(109, 128, 166, 0.14);
  background: rgba(255, 255, 255, 0.72);
  display: grid;
  gap: 14px;
}

.student-text-entry__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.student-text-entry__index {
  font-weight: 700;
  color: var(--text-title);
}

.student-text-entry__filename,
.student-text-entry__content {
  display: grid;
  gap: 8px;
}

.student-text-entry__filename label,
.student-text-entry__content label {
  color: var(--text-muted);
  font-size: 13px;
}

.student-submit-sidebar {
  display: grid;
  gap: 20px;
}

.student-submit-status {
  display: grid;
  gap: 14px;
}

.student-submit-status__item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(109, 128, 166, 0.1);
}

.student-submit-status__item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.student-submit-status__item span {
  color: var(--text-muted);
}

.student-history-list {
  display: grid;
  gap: 12px;
}

.student-history-card {
  padding: 16px 18px;
  border-radius: 22px;
  border: 1px solid rgba(109, 128, 166, 0.14);
  background: rgba(255, 255, 255, 0.78);
  display: grid;
  gap: 12px;
}

.student-history-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.student-history-card__header h4,
.student-history-card__header p {
  margin: 0;
}

.student-history-card__header h4 {
  font-size: 17px;
  margin-bottom: 6px;
}

.student-history-card__header p,
.student-history-card__meta {
  color: var(--text-muted);
}

.student-history-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
}

@media (max-width: 1360px) {
  .student-submit-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .student-submit-page__hero {
    flex-direction: column;
  }

  .student-submit-page__hero-side {
    justify-items: start;
  }

  .student-submit-section__header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

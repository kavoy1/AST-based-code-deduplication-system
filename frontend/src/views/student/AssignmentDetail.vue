<template>
  <div class="workspace-page">
    <WorkspaceShellSection>
      <template #tools>
        <el-button @click="goBack">返回作业列表</el-button>
        <el-button @click="reloadAll">刷新</el-button>
      </template>
    </WorkspaceShellSection>

    <div class="workspace-grid workspace-grid--two">
      <WorkspacePanel title="作业信息" subtitle="对接学生作业详情接口，展示基础信息与可提交规则。">
        <div v-if="loading" class="workspace-empty">
          <el-skeleton :rows="6" animated />
        </div>

        <template v-else-if="assignment.id">
          <div class="student-assignment-hero">
            <div>
              <h2>{{ assignment.title }}</h2>
              <p>{{ className || '当前班级' }} · {{ assignment.language }}</p>
            </div>
            <el-tag :type="assignment.statusTagType" round>{{ assignment.statusText }}</el-tag>
          </div>

          <div class="student-assignment-summary">
            <div class="student-assignment-summary__item">
              <span>开始时间</span>
              <strong>{{ assignment.startAtLabel }}</strong>
            </div>
            <div class="student-assignment-summary__item">
              <span>截止时间</span>
              <strong>{{ assignment.endAtLabel }}</strong>
            </div>
            <div class="student-assignment-summary__item">
              <span>文件限制</span>
              <strong>最多 {{ assignment.maxFiles }} 个</strong>
            </div>
            <div class="student-assignment-summary__item">
              <span>重交规则</span>
              <strong>{{ assignment.allowResubmit ? '允许重交' : '仅可提交一次' }}</strong>
            </div>
          </div>

          <div class="student-assignment-description">
            {{ assignment.description || '当前作业未填写详细说明。' }}
          </div>
        </template>

        <WorkspaceEmpty
          v-else
          title="未找到作业"
          description="当前作业不存在，或者你暂时没有访问权限。"
        />
      </WorkspacePanel>

      <WorkspacePanel title="个人查重摘要" subtitle="对接学生查重摘要接口，只有教师完成查重后才会显示。">
        <div v-if="summaryLoading" class="workspace-empty">
          <el-skeleton :rows="4" animated />
        </div>
        <div v-else class="student-summary-card">
          <template v-if="plagiarismSummary.generated">
            <div class="student-summary-card__score">{{ plagiarismSummary.highestScore }}%</div>
            <div class="student-summary-card__status">{{ plagiarismSummary.status || '已生成摘要' }}</div>
            <p>{{ plagiarismSummary.message || '教师已完成当前作业查重，你可以查看摘要结果。' }}</p>
            <div v-if="plagiarismSummary.teacherNote" class="student-summary-card__note">
              教师备注：{{ plagiarismSummary.teacherNote }}
            </div>
          </template>
          <WorkspaceEmpty
            v-else
            title="暂无查重摘要"
            :description="plagiarismSummary.message || '教师尚未完成该作业查重任务。'"
          />
        </div>
      </WorkspacePanel>
    </div>

    <div class="workspace-grid workspace-grid--two student-submission-grid">
      <WorkspacePanel title="提交作业" subtitle="支持文件上传与文本框代码提交，对接学生提交接口。">
        <el-tabs v-model="submissionMode" class="student-detail-tabs">
          <el-tab-pane label="文件上传" name="files">
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
              <div class="el-upload__text">拖拽或点击上传 `.java` 文件</div>
              <template #tip>
                <div class="el-upload__tip">
                  当前允许最多 {{ assignment.maxFiles || 1 }} 个文件，仅支持 `.java`
                </div>
              </template>
            </el-upload>

            <div class="student-submit-actions">
              <span class="workspace-pill">{{ fileNames.length ? `已选择 ${fileNames.length} 个文件` : '尚未选择文件' }}</span>
              <el-button type="primary" :loading="submittingFiles" :disabled="!canSubmitFiles" @click="submitFiles">提交文件</el-button>
            </div>
          </el-tab-pane>

          <el-tab-pane label="文本提交" name="text">
            <div class="student-text-entries">
              <div v-for="(entry, index) in codeEntries" :key="index" class="student-text-entry">
                <div class="student-text-entry__header">
                  <el-input v-model="entry.filename" placeholder="文件名，例如 Main.java" />
                  <el-button text type="danger" :disabled="codeEntries.length === 1" @click="removeCodeEntry(index)">删除</el-button>
                </div>
                <el-input v-model="entry.content" type="textarea" :rows="7" placeholder="请输入 Java 代码内容" />
              </div>
            </div>

            <div class="student-submit-actions">
              <el-button @click="addCodeEntry">新增文件</el-button>
              <el-button type="primary" :loading="submittingText" :disabled="!canSubmitText" @click="submitText">提交文本</el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </WorkspacePanel>

      <WorkspacePanel title="提交历史" subtitle="对接提交历史接口，展示每次提交的版本与有效性。">
        <div v-if="historyLoading" class="workspace-empty">
          <el-skeleton :rows="5" animated />
        </div>
        <WorkspaceEmpty
          v-else-if="submissionHistory.length === 0"
          title="暂无提交记录"
          description="你还没有提交过当前作业。"
        />
        <div v-else class="student-history-list">
          <article v-for="item in submissionHistory" :key="item.id" class="student-history-card">
            <div class="student-history-card__header">
              <div>
                <h4>第 {{ item.version }} 次提交</h4>
                <p>{{ item.submitTimeLabel }}</p>
              </div>
              <div class="student-history-card__badges">
                <span class="workspace-badge-soft" :class="item.isLatest ? 'workspace-badge-soft--green' : ''">{{ item.isLatest ? '当前版本' : '历史版本' }}</span>
                <span class="workspace-badge-soft" :class="item.isValid ? 'workspace-badge-soft--blue' : 'workspace-badge-soft--danger'">
                  {{ item.isValid ? '有效' : '无效' }}
                </span>
              </div>
            </div>

            <div class="student-history-card__meta">
              <span>文件解析 {{ item.parseOkFiles }}/{{ item.totalFiles }}</span>
              <span>{{ item.isLate ? '迟交' : '按时提交' }}</span>
              <span>截止：{{ item.deadlineAtLabel }}</span>
            </div>
          </article>
        </div>
      </WorkspacePanel>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  fetchStudentAssignmentDetail,
  fetchStudentPlagiarismSummary,
  fetchStudentSubmissionHistory,
  submitStudentAssignmentFiles,
  submitStudentAssignmentText
} from '../../api/student'
import WorkspaceEmpty from '../../components/workspace/WorkspaceEmpty.vue'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import WorkspaceShellSection from '../../components/workspace/WorkspaceShellSection.vue'

const route = useRoute()
const router = useRouter()

const assignment = ref({})
const plagiarismSummary = ref({
  generated: false,
  message: '',
  highestScore: 0,
  status: '',
  teacherNote: ''
})
const submissionHistory = ref([])
const loading = ref(false)
const summaryLoading = ref(false)
const historyLoading = ref(false)
const submittingFiles = ref(false)
const submittingText = ref(false)
const submissionMode = ref('files')
const selectedFiles = ref([])
const codeEntries = ref([{ filename: 'Main.java', content: '' }])

const assignmentId = computed(() => route.params.assignmentId)
const classId = computed(() => route.query.classId || '')
const className = computed(() => route.query.className || '')
const canSubmitFiles = computed(() => assignment.value.canSubmit && selectedFiles.value.length > 0)
const canSubmitText = computed(() => assignment.value.canSubmit && codeEntries.value.some((entry) => entry.filename.trim() && entry.content.trim()))
const fileNames = computed(() => selectedFiles.value.map((file) => file.name))

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

async function loadPlagiarismSummary() {
  summaryLoading.value = true
  try {
    plagiarismSummary.value = await fetchStudentPlagiarismSummary(assignmentId.value)
  } catch (error) {
    plagiarismSummary.value = {
      generated: false,
      message: typeof error === 'string' ? error : '教师尚未完成当前作业查重任务。',
      highestScore: 0,
      status: '',
      teacherNote: ''
    }
  } finally {
    summaryLoading.value = false
  }
}

async function reloadAll() {
  await Promise.all([
    loadAssignment(),
    loadSubmissionHistory(),
    loadPlagiarismSummary()
  ])
}

function handleFileChange(file, fileList) {
  selectedFiles.value = fileList.map((item) => item.raw).filter(Boolean)
}

function handleFileRemove(file, fileList) {
  selectedFiles.value = fileList.map((item) => item.raw).filter(Boolean)
}

function addCodeEntry() {
  codeEntries.value.push({ filename: '', content: '' })
}

function removeCodeEntry(index) {
  codeEntries.value.splice(index, 1)
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
    const entries = codeEntries.value.filter((entry) => entry.filename.trim() && entry.content.trim())
    await submitStudentAssignmentText(assignmentId.value, entries)
    ElMessage.success('文本提交成功')
    codeEntries.value = [{ filename: 'Main.java', content: '' }]
    await reloadAll()
  } finally {
    submittingText.value = false
  }
}

function goBack() {
  if (classId.value) {
    router.push({
      path: '/student/tasks',
      query: {
        classId: classId.value,
        className: className.value
      }
    })
    return
  }
  router.push('/student/tasks')
}

onMounted(reloadAll)
</script>

<style scoped>
.student-assignment-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 18px;
}

.student-assignment-hero h2,
.student-assignment-hero p {
  margin: 0;
}

.student-assignment-hero h2 {
  font-size: 28px;
  margin-bottom: 10px;
}

.student-assignment-hero p,
.student-assignment-description {
  color: var(--text-body);
}

.student-assignment-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.student-assignment-summary__item {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(248, 250, 251, 0.88);
  border: 1px solid rgba(29, 35, 43, 0.05);
}

.student-assignment-summary__item span {
  display: block;
  color: var(--text-soft);
  margin-bottom: 8px;
}

.student-assignment-summary__item strong {
  font-size: 16px;
}

.student-assignment-description {
  line-height: 1.8;
}

.student-summary-card {
  min-height: 100%;
}

.student-summary-card__score {
  font-size: 56px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 12px;
}

.student-summary-card__status {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.student-summary-card p {
  margin: 0;
  color: var(--text-body);
  line-height: 1.8;
}

.student-summary-card__note {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(248, 250, 251, 0.88);
  color: var(--text-body);
}

.student-submission-grid {
  margin-top: 20px;
}

.student-upload :deep(.el-upload-dragger) {
  border-radius: 24px;
}

.student-upload__icon {
  font-size: 28px;
  margin-bottom: 10px;
}

.student-submit-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.student-text-entries {
  display: grid;
  gap: 14px;
}

.student-text-entry {
  padding: 16px;
  border-radius: 22px;
  background: rgba(248, 250, 251, 0.88);
  border: 1px solid rgba(29, 35, 43, 0.05);
}

.student-text-entry__header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.student-history-list {
  display: grid;
  gap: 14px;
}

.student-history-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(248, 250, 251, 0.88);
  border: 1px solid rgba(29, 35, 43, 0.05);
}

.student-history-card__header,
.student-history-card__meta,
.student-history-card__badges {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.student-history-card__header h4,
.student-history-card__header p {
  margin: 0;
}

.student-history-card__header h4 {
  margin-bottom: 8px;
}

.student-history-card__header p,
.student-history-card__meta {
  color: var(--text-body);
}

.student-history-card__badges {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.student-history-card__meta {
  flex-wrap: wrap;
  margin-top: 14px;
  font-size: 13px;
}
</style>

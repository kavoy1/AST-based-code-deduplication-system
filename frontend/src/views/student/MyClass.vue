<template>
  <div class="workspace-page">
    <WorkspaceShellSection>
      <template #tools>
        <el-button @click="fetchClasses">刷新</el-button>
        <el-button type="primary" @click="dialogVisible = true">加入班级</el-button>
      </template>
    </WorkspaceShellSection>

    <WorkspacePanel title="我的班级" subtitle="真实对接学生班级接口，点击班级后进入该班级的作业列表。">
      <div v-if="loading" class="workspace-empty">
        <el-skeleton :rows="5" animated />
      </div>

      <WorkspaceEmpty
        v-else-if="classList.length === 0"
        title="暂未加入任何班级"
        description="输入教师提供的邀请码后即可提交加入申请。"
        action-text="输入邀请码"
        @action="dialogVisible = true"
      />

      <div v-else class="student-class-grid">
        <article
          v-for="item in classList"
          :key="item.id"
          class="student-class-card"
          :class="{ 'student-class-card--pending': item.status !== 1 }"
          @click="openClassAssignments(item)"
        >
          <div class="student-class-card__header">
            <div>
              <h3>{{ item.className }}</h3>
              <p>{{ item.courseName }}</p>
            </div>
            <span class="workspace-badge-soft" :class="item.status === 1 ? 'workspace-badge-soft--green' : 'workspace-badge-soft--amber'">
              {{ item.statusText }}
            </span>
          </div>

          <div class="student-class-card__meta">
            <span>教师：{{ item.teacherName }}</span>
            <span>加入时间：{{ item.joinTimeLabel }}</span>
          </div>

          <div class="student-class-card__footer">
            <span class="workspace-pill">{{ item.status === 1 ? '可查看作业' : '等待审核' }}</span>
            <el-button type="primary" link :disabled="item.status !== 1" @click.stop="openClassAssignments(item)">
              查看作业
            </el-button>
          </div>
        </article>
      </div>
    </WorkspacePanel>

    <el-dialog v-model="dialogVisible" title="加入班级" width="420px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item prop="inviteCode" label="邀请码">
          <el-input v-model="form.inviteCode" placeholder="请输入 6 位邀请码" maxlength="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="joinLoading" @click="handleJoin">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchStudentClasses, joinStudentClass } from '../../api/student'
import WorkspaceEmpty from '../../components/workspace/WorkspaceEmpty.vue'
import WorkspacePanel from '../../components/workspace/WorkspacePanel.vue'
import WorkspaceShellSection from '../../components/workspace/WorkspaceShellSection.vue'

const router = useRouter()

const classList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const joinLoading = ref(false)
const formRef = ref(null)
const form = reactive({ inviteCode: '' })
const rules = {
  inviteCode: [
    { required: true, message: '请输入邀请码', trigger: 'blur' },
    { len: 6, message: '邀请码长度必须为 6 位', trigger: 'blur' }
  ]
}

async function fetchClasses() {
  loading.value = true
  try {
    classList.value = await fetchStudentClasses()
  } finally {
    loading.value = false
  }
}

function openClassAssignments(item) {
  if (item.status !== 1) {
    ElMessage.warning('班级仍在审核中，暂时无法查看作业')
    return
  }
  router.push({
    path: '/student/tasks',
    query: {
      classId: item.id,
      className: item.className
    }
  })
}

async function handleJoin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    joinLoading.value = true
    try {
      await joinStudentClass(form.inviteCode)
      ElMessage.success('申请已提交，请等待教师审核')
      dialogVisible.value = false
      form.inviteCode = ''
      await fetchClasses()
    } finally {
      joinLoading.value = false
    }
  })
}

onMounted(fetchClasses)
</script>

<style scoped>
.student-class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
}

.student-class-card {
  padding: 20px;
  border-radius: 28px;
  background: rgba(248, 250, 251, 0.88);
  border: 1px solid rgba(29, 35, 43, 0.06);
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}

.student-class-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(27, 38, 59, 0.08);
  border-color: rgba(80, 104, 241, 0.12);
}

.student-class-card--pending {
  opacity: 0.88;
}

.student-class-card__header,
.student-class-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.student-class-card h3,
.student-class-card p {
  margin: 0;
}

.student-class-card h3 {
  font-size: 20px;
  margin-bottom: 6px;
}

.student-class-card p,
.student-class-card__meta {
  color: var(--text-body);
}

.student-class-card__meta {
  display: grid;
  gap: 8px;
  margin-top: 16px;
  margin-bottom: 18px;
  font-size: 13px;
}
</style>

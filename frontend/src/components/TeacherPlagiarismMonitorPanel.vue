<template>
  <div v-if="tasks.length" class="teacher-plagiarism-monitor" :class="{ 'is-collapsed': state.collapsed }">
    <div class="teacher-plagiarism-monitor__header">
      <div>
        <strong>查重任务进度</strong>
        <span>{{ activeTaskCount }} 个进行中</span>
      </div>
      <button type="button" class="teacher-plagiarism-monitor__toggle" @click="toggleTeacherPlagiarismMonitorCollapsed">
        {{ state.collapsed ? '展开' : '收起' }}
      </button>
    </div>

    <div v-if="!state.collapsed" class="teacher-plagiarism-monitor__body">
      <article
        v-for="task in tasks"
        :key="task.key"
        class="teacher-plagiarism-monitor__card"
        :class="`status-${String(task.status || '').toLowerCase()}`"
      >
        <div class="teacher-plagiarism-monitor__card-head">
          <div>
            <h4>{{ task.assignmentTitle }}</h4>
            <p>Job #{{ task.jobId }} · {{ task.executionMode }}</p>
          </div>
          <span class="teacher-plagiarism-monitor__status">{{ task.status }}</span>
        </div>

        <div class="teacher-plagiarism-monitor__progress">
          <div class="teacher-plagiarism-monitor__progress-meta">
            <span>进度</span>
            <strong>{{ task.progressDone }}/{{ task.progressTotal }}</strong>
          </div>
          <el-progress
            :percentage="task.progressTotal > 0 ? Math.min(100, Math.round((task.progressDone / task.progressTotal) * 100)) : 0"
            :stroke-width="8"
            :show-text="false"
          />
        </div>

        <div class="teacher-plagiarism-monitor__meta">
          <span>更新时间：{{ formatDateTime(task.lastUpdatedAt) || '刚刚' }}</span>
          <span v-if="task.reusedFromJobId">复用来源：#{{ task.reusedFromJobId }}</span>
          <span v-else>命中结果：{{ task.thresholdMatchedPairs }}</span>
        </div>

        <p v-if="task.status === 'FAILED' && task.errorMsg" class="teacher-plagiarism-monitor__error">{{ task.errorMsg }}</p>

        <div class="teacher-plagiarism-monitor__actions">
          <el-button link type="primary" @click="openTeacherPlagiarismJob(task)">查看</el-button>
          <el-button link @click="dismissTeacherPlagiarismJob(task.jobId)">关闭</el-button>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatDateTime } from '../views/teacher/assignmentMappers'
import { useTeacherPlagiarismMonitor } from '../composables/useTeacherPlagiarismMonitor'

const {
  state,
  dismissTeacherPlagiarismJob,
  toggleTeacherPlagiarismMonitorCollapsed,
  openTeacherPlagiarismJob
} = useTeacherPlagiarismMonitor()

const tasks = computed(() => state.tasks)
const activeTaskCount = computed(() => state.tasks.filter((task) => !['DONE', 'FAILED'].includes(task.status)).length)
</script>

<style scoped>
.teacher-plagiarism-monitor {
  position: fixed;
  top: 18px;
  right: 24px;
  width: 360px;
  max-height: calc(100vh - 36px);
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border-radius: 24px;
  background: rgba(18, 25, 38, 0.92);
  color: #f4f7fb;
  box-shadow: 0 24px 60px rgba(12, 18, 28, 0.28);
  backdrop-filter: blur(16px);
  z-index: 3200;
}

.teacher-plagiarism-monitor.is-collapsed {
  width: 240px;
}

.teacher-plagiarism-monitor__header,
.teacher-plagiarism-monitor__card-head,
.teacher-plagiarism-monitor__progress-meta,
.teacher-plagiarism-monitor__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.teacher-plagiarism-monitor__header strong,
.teacher-plagiarism-monitor__card-head h4 {
  margin: 0;
}

.teacher-plagiarism-monitor__header span,
.teacher-plagiarism-monitor__card-head p,
.teacher-plagiarism-monitor__meta {
  color: rgba(235, 241, 248, 0.72);
}

.teacher-plagiarism-monitor__header span,
.teacher-plagiarism-monitor__card-head p,
.teacher-plagiarism-monitor__meta,
.teacher-plagiarism-monitor__toggle {
  font-size: 12px;
}

.teacher-plagiarism-monitor__toggle {
  border: 0;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  cursor: pointer;
}

.teacher-plagiarism-monitor__body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
}

.teacher-plagiarism-monitor__card {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
}

.teacher-plagiarism-monitor__card.status-done {
  border: 1px solid rgba(72, 199, 116, 0.35);
}

.teacher-plagiarism-monitor__card.status-failed {
  border: 1px solid rgba(255, 107, 107, 0.35);
}

.teacher-plagiarism-monitor__status {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 12px;
}

.teacher-plagiarism-monitor__progress,
.teacher-plagiarism-monitor__meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}

.teacher-plagiarism-monitor__error {
  margin: 10px 0 0;
  color: #ffb4b4;
  font-size: 12px;
  line-height: 1.4;
}

.teacher-plagiarism-monitor__actions {
  margin-top: 10px;
}

@media (max-width: 900px) {
  .teacher-plagiarism-monitor {
    top: 12px;
    right: 12px;
    left: 12px;
    width: auto;
  }
}
</style>

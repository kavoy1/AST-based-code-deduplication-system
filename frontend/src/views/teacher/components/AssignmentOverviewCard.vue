<template>
  <article class="assignment-overview-card">
    <div class="assignment-overview-card__head">
      <div class="assignment-overview-card__title-wrap">
        <span class="assignment-overview-card__language">{{ assignment.language }}</span>
        <h3>{{ assignment.title }}</h3>
        <p>{{ assignment.classNamesText }}</p>
      </div>
      <span class="assignment-overview-card__status" :class="`is-${assignment.status}`">
        {{ assignment.statusLabel }}
      </span>
    </div>

    <div class="assignment-overview-card__stats">
      <div class="assignment-overview-card__stat">
        <span>截止时间</span>
        <strong>{{ assignment.endTime || '未设置' }}</strong>
      </div>
      <div class="assignment-overview-card__stat">
        <span>提交进度</span>
        <strong>{{ assignment.submittedCount }}/{{ assignment.studentCount || 0 }}</strong>
      </div>
    </div>

    <div class="assignment-overview-card__subline">
      <span>未交 {{ assignment.unsubmittedCount }}</span>
      <span>迟交 {{ assignment.lateSubmissionCount }}</span>
    </div>

    <div class="assignment-overview-card__footer">
      <small class="assignment-overview-card__hint">{{ footerMessage }}</small>

      <div class="assignment-overview-card__actions">
        <button
          type="button"
          class="assignment-overview-card__link"
          @click="$emit('settings', assignment)"
        >
          设置
        </button>
        <button
          type="button"
          class="assignment-overview-card__link"
          :disabled="launchDisabled"
          @click="$emit('launch', assignment)"
        >
          发起查重
        </button>
        <button
          v-if="closeNowVisible"
          type="button"
          class="assignment-overview-card__link assignment-overview-card__link--danger"
          @click="$emit('close-now', assignment)"
        >
          立即结束
        </button>
        <button
          type="button"
          class="assignment-overview-card__delete-button"
          aria-label="删除作业"
          @click="$emit('delete', assignment)"
        >
          <svg viewBox="0 0 448 512" class="assignment-overview-card__delete-icon assignment-overview-card__delete-icon--top" aria-hidden="true">
            <path d="M135.2 17.7L128 32H32C14.3 32 0 46.3 0 64S14.3 96 32 96H176V64c0-17.7 14.3-32 32-32s32 14.3 32 32V96H416c17.7 0 32-14.3 32-32S433.7 32 416 32H320l-7.2-14.3C307.4 6.8 296.2 0 284.2 0H163.8c-12.1 0-23.2 6.8-28.6 17.7z" />
          </svg>
          <svg viewBox="0 0 448 512" class="assignment-overview-card__delete-icon" aria-hidden="true">
            <path d="M53.2 467c1.7 25.8 23.1 45 49 45H345.8c25.9 0 47.3-19.2 49-45L416 128H32L53.2 467z" />
          </svg>
        </button>
        <el-button
          type="primary"
          round
          class="assignment-overview-card__primary"
          :disabled="primaryAction.disabled"
          @click="$emit(primaryAction.event, assignment)"
        >
          {{ primaryAction.label }}
        </el-button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import {
  canOverviewCloseNow,
  getEndedAssignmentOverviewPrimaryAction,
  isOverviewLaunchDisabled
} from '../assignmentOverviewCardHelpers.js'

const props = defineProps({
  assignment: {
    type: Object,
    required: true
  }
})

defineEmits(['settings', 'launch', 'results', 'delete', 'close-now'])

const launchDisabled = computed(() => isOverviewLaunchDisabled(props.assignment))
const closeNowVisible = computed(() => canOverviewCloseNow(props.assignment))
const endedPrimaryAction = computed(() => getEndedAssignmentOverviewPrimaryAction(props.assignment))

const primaryAction = computed(() => {
  if (props.assignment?.hasPlagiarismJob) {
    return { label: '查看结果', event: 'results', disabled: false }
  }

  if (launchDisabled.value) {
    return { label: '暂不可发起', event: 'launch', disabled: true }
  }

  return { label: '发起查重', event: 'launch', disabled: false }
})

const footerMessage = computed(() => {
  if (props.assignment?.status === 'ended') {
    return endedPrimaryAction.value.message
  }

  if (launchDisabled.value) {
    return '作业进行中，若要提前查重，可以先立即结束这份作业。'
  }

  if (props.assignment?.hasPlagiarismJob) {
    return '已经生成查重结果，可以直接进入查看。'
  }

  return '作业已满足查重条件，现在可以发起查重。'
})
</script>

<style scoped>
.assignment-overview-card {
  display: grid;
  gap: 18px;
  min-height: 232px;
  padding: 24px;
  border-radius: 28px;
  background: #ffffff;
  border: 1px solid rgba(223, 229, 239, 0.9);
  box-shadow: 0 12px 24px rgba(181, 192, 212, 0.08);
}

.assignment-overview-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.assignment-overview-card__title-wrap {
  min-width: 0;
}

.assignment-overview-card__language {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(242, 245, 250, 0.95);
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.assignment-overview-card__head h3 {
  margin: 14px 0 8px;
  color: #111827;
  font-size: 28px;
  line-height: 1.18;
  word-break: break-word;
}

.assignment-overview-card__head p {
  margin: 0;
  color: #7a89a1;
  font-size: 15px;
}

.assignment-overview-card__status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(244, 246, 250, 1);
  color: #55657d;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.assignment-overview-card__status.is-active {
  background: rgba(228, 243, 234, 0.95);
  color: #2f7a55;
}

.assignment-overview-card__status.is-ended {
  background: rgba(241, 244, 248, 1);
  color: #4f5f77;
}

.assignment-overview-card__status.is-draft {
  background: rgba(239, 236, 255, 0.95);
  color: #6953e8;
}

.assignment-overview-card__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.assignment-overview-card__stat {
  display: grid;
  gap: 6px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.96);
  border: 1px solid rgba(229, 234, 241, 0.92);
}

.assignment-overview-card__stat span {
  color: #7a89a1;
  font-size: 13px;
}

.assignment-overview-card__stat strong {
  color: #111827;
  font-size: 16px;
  line-height: 1.4;
  word-break: break-word;
}

.assignment-overview-card__subline {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 18px;
  color: #7a89a1;
  font-size: 14px;
}

.assignment-overview-card__footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid rgba(231, 236, 244, 0.95);
}

.assignment-overview-card__hint {
  color: #78869c;
  font-size: 13px;
  line-height: 1.5;
}

.assignment-overview-card__actions {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
}

.assignment-overview-card__link {
  border: none;
  background: transparent;
  padding: 0;
  color: #64748b;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.assignment-overview-card__link:disabled {
  color: #b5c0cf;
  cursor: not-allowed;
}

.assignment-overview-card__link:not(:disabled):hover {
  color: #1f2937;
}

.assignment-overview-card__link--danger {
  color: #c2410c;
}

.assignment-overview-card__link--danger:not(:disabled):hover {
  color: #9a3412;
}

.assignment-overview-card__delete-button {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background-color: rgb(20, 20, 20);
  border: none;
  font-weight: 600;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.164);
  cursor: pointer;
  transition-duration: 0.3s;
  overflow: hidden;
  position: relative;
  gap: 2px;
  flex-shrink: 0;
}

.assignment-overview-card__delete-button:hover {
  background-color: rgb(255, 69, 69);
  gap: 0;
}

.assignment-overview-card__delete-icon {
  width: 12px;
  transition-duration: 0.3s;
}

.assignment-overview-card__delete-icon path {
  fill: white;
}

.assignment-overview-card__delete-icon--top {
  transform-origin: bottom right;
}

.assignment-overview-card__delete-button:hover .assignment-overview-card__delete-icon--top {
  transition-duration: 0.5s;
  transform: rotate(160deg);
}

.assignment-overview-card__primary {
  min-width: 112px;
  margin-left: 0;
  --el-button-bg-color: #111827;
  --el-button-border-color: #111827;
  --el-button-text-color: #ffffff;
  --el-button-hover-bg-color: #1f2937;
  --el-button-hover-border-color: #1f2937;
}

@media (max-width: 960px) {
  .assignment-overview-card__stats,
  .assignment-overview-card__footer {
    grid-template-columns: 1fr;
  }

  .assignment-overview-card__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .assignment-overview-card {
    padding: 20px;
    border-radius: 24px;
  }

  .assignment-overview-card__head {
    flex-direction: column;
  }

  .assignment-overview-card__head h3 {
    font-size: 24px;
  }

  .assignment-overview-card__actions {
    gap: 12px;
  }

  .assignment-overview-card__primary {
    width: 100%;
  }
}
</style>

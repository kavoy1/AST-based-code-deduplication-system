<template>
  <span class="tooltip-container" tabindex="0" :aria-label="label">
    <button type="button" class="icon teacher-explain-tip" :aria-label="label">
      <span class="teacher-explain-tip__mark">!</span>
    </button>

    <span class="tooltip teacher-explain-tip__tooltip" role="tooltip">
      <strong v-if="title">{{ title }}</strong>
      <span>{{ content }}</span>
      <small v-if="recommendation">{{ recommendation }}</small>
    </span>
  </span>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  content: {
    type: String,
    required: true
  },
  recommendation: {
    type: String,
    default: ''
  },
  label: {
    type: String,
    default: '查看参数说明'
  }
})
</script>

<style scoped>
.tooltip-container {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.teacher-explain-tip {
  width: 30px;
  height: 30px;
  margin: 0;
  padding: 0;
  border: none;
  border-radius: 999px;
  background: linear-gradient(180deg, #eef7ff, #d8ecff);
  box-shadow:
    0 10px 18px rgba(108, 153, 201, 0.18),
    inset 0 0 0 1px rgba(123, 175, 227, 0.42);
  cursor: pointer;
  transition:
    transform 0.3s ease,
    filter 0.3s ease;
}

.teacher-explain-tip:hover,
.tooltip-container:focus-within .teacher-explain-tip {
  transform: translateY(-1px);
  filter: brightness(1.03);
}

.teacher-explain-tip__mark {
  color: #3e7eb3;
  font-size: 18px;
  font-weight: 800;
  line-height: 1;
}

.tooltip {
  visibility: hidden;
  width: 240px;
  position: absolute;
  left: 50%;
  bottom: 125%;
  z-index: 20;
  margin-left: -120px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #2f3440;
  color: #fff;
  text-align: left;
  opacity: 0;
  transform: translateY(10px);
  transition:
    opacity 0.5s,
    transform 0.5s;
  box-shadow: 0 18px 32px rgba(33, 39, 52, 0.28);
  line-height: 1.65;
}

.tooltip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  margin-left: -5px;
  border-width: 5px;
  border-style: solid;
  border-color: #2f3440 transparent transparent transparent;
}

.tooltip-container:hover .tooltip,
.tooltip-container:focus-within .tooltip {
  visibility: visible;
  opacity: 1;
  transform: translateY(0);
  animation: bounce 0.6s ease;
}

.teacher-explain-tip__tooltip {
  display: grid;
  gap: 6px;
}

.teacher-explain-tip__tooltip strong {
  font-size: 14px;
  font-weight: 700;
}

.teacher-explain-tip__tooltip span,
.teacher-explain-tip__tooltip small {
  margin: 0;
  font-size: 13px;
}

.teacher-explain-tip__tooltip small {
  color: rgba(255, 255, 255, 0.78);
}

@keyframes bounce {
  0%,
  20%,
  50%,
  80%,
  100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-12px);
  }
  60% {
    transform: translateY(-6px);
  }
}
</style>

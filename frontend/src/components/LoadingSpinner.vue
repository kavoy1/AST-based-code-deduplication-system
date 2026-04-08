<template>
  <div class="loading-spinner" :class="{ 'loading-spinner--inline': inline }" role="status" :aria-label="label || '正在加载'">
    <div class="loading-spinner__icon-wrap">
      <div class="loader" :style="loaderStyle" aria-hidden="true">
        <span
          v-for="(segment, index) in segments"
          :key="index"
          class="bar"
          :style="{
            '--bar-height': `${segment.height}px`,
            '--bar-delay': `${segment.delay}s`,
            '--bar-margin-inline': `${segment.marginInline}px`
          }"
        />
      </div>
    </div>
    <p v-if="label" class="loading-spinner__label">{{ label }}</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { buildSpinnerSegments } from './loadingSpinnerModel'

const props = defineProps({
  label: {
    type: String,
    default: ''
  },
  size: {
    type: Number,
    default: 18
  },
  inline: {
    type: Boolean,
    default: false
  }
})

const segments = buildSpinnerSegments()

const loaderStyle = computed(() => ({
  '--loader-scale': `${Math.max(props.size / 18, 0.75)}`
}))
</script>

<style scoped>
.loading-spinner {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  min-height: 120px;
  color: #6f7f95;
}

.loading-spinner--inline {
  min-height: auto;
  gap: 12px;
}

.loading-spinner__label {
  margin: 0;
  color: #74849b;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.loading-spinner__icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  min-height: 56px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(12, 18, 28, 0.82);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.18);
}

.loader {
  display: flex;
  align-items: center;
  justify-content: center;
  transform: scale(var(--loader-scale));
  transform-origin: center;
}

.bar {
  display: inline-block;
  width: 3px;
  height: var(--bar-height);
  margin: 0 var(--bar-margin-inline);
  background-color: rgba(255, 255, 255, 0.5);
  border-radius: 10px;
  animation: scale-up4 1s linear infinite;
  animation-delay: var(--bar-delay);
}

@keyframes scale-up4 {
  20% {
    background-color: #ffffff;
    transform: scaleY(1.5);
  }

  50% {
    transform: scaleY(1);
  }
}
</style>

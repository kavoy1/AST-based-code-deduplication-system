<template>
  <el-avatar v-if="normalizedSrc" :size="size" :src="normalizedSrc" />
  <div
    v-else
    class="app-avatar-fallback"
    :style="fallbackStyle"
  >
    {{ displayText }}
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { normalizeAvatarSrc } from '../utils/avatar'

const props = defineProps({
  size: {
    type: Number,
    default: 40
  },
  src: {
    type: [String, null],
    default: undefined
  },
  text: {
    type: String,
    default: 'U'
  }
})

const normalizedSrc = computed(() => normalizeAvatarSrc(props.src))
const displayText = computed(() => (props.text || 'U').slice(0, 1).toUpperCase())
const fallbackStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  fontSize: `${Math.max(14, Math.round(props.size * 0.42))}px`
}))
</script>

<style scoped>
.app-avatar-fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #d7dde5 0%, #c5ccd6 100%);
  color: #1f2937;
  font-weight: 700;
  line-height: 1;
  user-select: none;
  flex-shrink: 0;
}
</style>
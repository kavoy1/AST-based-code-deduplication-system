<template>
  <div :class="['workspace-stat-chip', toneClass]">
    <slot name="icon" />
    <div>
      <div class="workspace-muted">{{ label }}</div>
      <strong>{{ value }}</strong>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  tone: { type: String, default: 'default' }
})

const toneClass = computed(() => props.tone === 'default' ? '' : `workspace-stat-chip--${props.tone}`)
</script>

<style scoped>
.workspace-stat-chip--blue { background: rgba(167, 203, 255, 0.2); }
.workspace-stat-chip--green { background: rgba(205, 238, 219, 0.4); }
.workspace-stat-chip--amber { background: rgba(247, 231, 184, 0.4); }
.workspace-stat-chip--purple { background: rgba(217, 205, 252, 0.28); }
</style>

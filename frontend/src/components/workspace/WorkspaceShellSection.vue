<template>
  <section v-if="$slots.tools" class="workspace-shell-section workspace-shell-section--tools-only">
    <div class="workspace-shell-section__tools">
      <slot name="tools" />
    </div>
  </section>
</template>

<script setup>
defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, default: '' },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.workspace-shell-section {
  display: flex;
  justify-content: flex-end;
}

.workspace-shell-section--tools-only {
  min-height: 0;
}

.workspace-shell-section__tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
  width: 100%;
}
</style>

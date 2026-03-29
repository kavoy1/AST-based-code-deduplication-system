<template>
  <section :class="['workspace-panel', soft && 'workspace-panel--soft']">
    <header v-if="title || $slots.header" class="workspace-panel__header">
      <slot name="header">
        <div>
          <h3 v-if="title" class="workspace-panel__title">{{ title }}</h3>
          <p v-if="subtitle" class="workspace-panel__sub">{{ subtitle }}</p>
        </div>
      </slot>
      <slot name="extra" />
    </header>
    <div class="workspace-panel__body" :class="compact && 'workspace-panel__body--compact'">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  soft: { type: Boolean, default: false },
  compact: { type: Boolean, default: false }
})
</script>

<style scoped>
.workspace-panel__body--compact {
  padding-top: 14px;
}
</style>

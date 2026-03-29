<template>
  <div class="workspace-empty">
    <div>
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
      <div v-if="actionText" class="workspace-empty__action">
        <el-button type="primary" @click="$emit('action')">{{ actionText }}</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineEmits(['action'])
defineProps({
  title: { type: String, default: '暂无内容' },
  description: { type: String, default: '当前没有可展示的数据。' },
  actionText: { type: String, default: '' }
})
</script>

<style scoped>
h3 {
  margin: 0 0 10px;
  font-size: 18px;
}

p {
  margin: 0;
  color: var(--text-soft);
}

.workspace-empty__action {
  margin-top: 18px;
}
</style>

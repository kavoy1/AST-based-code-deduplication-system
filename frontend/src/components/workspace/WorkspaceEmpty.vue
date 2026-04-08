<template>
  <div class="workspace-empty">
    <slot>
      <div>
        <LoadingSpinner v-if="showLoadingSpinner" :label="title" inline />
        <template v-else>
          <h3>{{ title }}</h3>
          <p>{{ description }}</p>
          <div v-if="actionText" class="workspace-empty__action">
            <el-button type="primary" @click="$emit('action')">{{ actionText }}</el-button>
          </div>
        </template>
      </div>
    </slot>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import LoadingSpinner from '../LoadingSpinner.vue'

defineEmits(['action'])
const props = defineProps({
  title: { type: String, default: '暂无内容' },
  description: { type: String, default: '当前没有可展示的数据。' },
  actionText: { type: String, default: '' }
})

const showLoadingSpinner = computed(() => String(props.title || '').includes('正在加载'))
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

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import LoadingSpinner from './components/LoadingSpinner.vue'

const isLoading = ref(false)
const router = useRouter()

onMounted(() => {
  router.beforeEach((to, from, next) => {
    isLoading.value = true
    next()
  })

  router.afterEach(() => {
    // 立即关闭加载动画，避免与页面内部的 loading 重叠
    isLoading.value = false
  })
})
</script>

<template>
  <div v-if="isLoading" class="global-loading">
    <LoadingSpinner />
  </div>
  <router-view />
</template>

<style>
/* 全局样式覆盖 */
.global-loading {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.1); /* 改为深色透明，避免泛白 */
  backdrop-filter: blur(2px); /* 添加毛玻璃效果 */
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

/* 适配暗色模式 */
@media (prefers-color-scheme: dark) {
  .global-loading {
    background-color: rgba(0, 0, 0, 0.3);
  }
}
</style>

<template>
  <div class="teacher-redirect-placeholder"></div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  const tab = route.query.tab === 'stats' ? 'students' : 'students'
  router.replace({ path: '/teacher/classes', query: { tab } })
})
</script>

<style scoped>
.teacher-redirect-placeholder {
  display: none;
}
</style>

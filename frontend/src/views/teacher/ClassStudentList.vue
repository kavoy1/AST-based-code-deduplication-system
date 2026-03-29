<template>
  <div class="teacher-redirect-placeholder"></div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  router.replace({
    path: '/teacher/classes',
    query: {
      tab: 'students',
      classId: route.params.classId
    }
  })
})
</script>

<style scoped>
.teacher-redirect-placeholder {
  display: none;
}
</style>

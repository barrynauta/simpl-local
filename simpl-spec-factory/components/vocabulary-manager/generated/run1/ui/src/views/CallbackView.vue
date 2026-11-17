<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { completeLogin } from '../auth'

const router = useRouter()
const error = ref('')

onMounted(async () => {
  try {
    const target = await completeLogin()
    router.replace(target || '/')
  } catch (e) {
    error.value = (e as Error).message
  }
})
</script>

<template>
  <p v-if="!error" aria-busy="true">Signing in...</p>
  <div v-else class="error-box">Sign-in failed: {{ error }} <RouterLink to="/">Try again</RouterLink></div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { completeLogin } from '../auth'

const error = ref('')
onMounted(async () => {
  try {
    const target = await completeLogin()
    window.location.replace(target)
  } catch (e) {
    error.value = (e as Error).message
  }
})
</script>

<template>
  <p v-if="error" data-testid="error-message" class="error">{{ error }}</p>
  <p v-else>Signing in...</p>
</template>

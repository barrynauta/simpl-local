<script setup lang="ts">
/** An error message with the problem detail (D34, D35, D60, D72): role alert. */
defineProps<{ text: string; detail?: string | null }>();
</script>

<template>
  <div role="alert" class="message error">
    <p>{{ text }}</p>
    <p v-if="detail" class="detail">{{ detail }}</p>
  </div>
</template>

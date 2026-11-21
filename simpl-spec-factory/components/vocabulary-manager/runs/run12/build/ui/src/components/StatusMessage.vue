<script setup lang="ts">
defineProps<{ text: string }>();
</script>

<template>
  <p
    role="status"
    class="my-3 rounded border border-green-700 bg-green-50 px-3 py-2 text-green-900"
  >
    {{ text }}
  </p>
</template>

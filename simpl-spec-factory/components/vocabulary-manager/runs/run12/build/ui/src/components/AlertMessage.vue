<script setup lang="ts">
/** D114: one element with role alert holding the decided text followed by the problem detail. */
defineProps<{ text: string; detail?: string }>();
</script>

<template>
  <div role="alert" class="my-3 rounded border border-red-700 bg-red-50 px-3 py-2 text-red-900">
    <p>{{ text }}</p>
    <p v-if="detail">{{ detail }}</p>
    <slot />
  </div>
</template>

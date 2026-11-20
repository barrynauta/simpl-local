<script setup lang="ts">
/** Field error text with reserved space, so the layout does not shift (D101). */
defineProps<{ id: string; message: string }>();
</script>

<template>
  <p :id="id" class="field-error">{{ message }}</p>
</template>

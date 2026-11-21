<script setup lang="ts">
defineProps<{ id: string; message: string }>();
</script>

<template>
  <!-- Reserved space so an error that appears never moves the controls below (D101). -->
  <p :id="id" class="min-h-6 text-sm text-red-800">{{ message }}</p>
</template>

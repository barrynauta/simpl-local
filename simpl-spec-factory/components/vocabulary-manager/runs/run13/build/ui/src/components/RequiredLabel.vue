<script setup lang="ts">
defineProps<{ forId: string; text: string }>();
</script>

<template>
  <label :for="forId" class="block font-medium text-slate-900"
    >{{ text }} <span aria-hidden="true" class="text-red-800">*</span></label
  >
</template>

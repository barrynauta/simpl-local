<script setup lang="ts">
/** A label with the decided text and a visible '*' (D60, D141). */
defineProps<{ target: string; text: string }>();
</script>

<template>
  <label :for="target">{{ text }}<span class="required" aria-hidden="true"> *</span></label>
</template>

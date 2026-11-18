<script setup lang="ts">
import VocabularyForm from './VocabularyForm.vue';

defineProps<{ name: string }>();
</script>

<template>
  <VocabularyForm mode="version" :name="name" />
</template>

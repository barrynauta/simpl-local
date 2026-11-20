<script setup lang="ts">
import VocabularyForm from '../components/VocabularyForm.vue';

defineProps<{ name: string }>();
</script>

<template>
  <VocabularyForm :key="name" mode="version" :name="name" />
</template>

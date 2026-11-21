<script setup lang="ts">
import { computed } from 'vue';
import type { Problem } from '../api';
import { errorsFound } from '../format';

/** D14, D90, D109, D125: the validation report region of a 400 answer. */
const props = defineProps<{ problem: Problem }>();
const issues = computed(() => props.problem.issues ?? []);
</script>

<template>
  <section
    aria-label="Validation report"
    class="my-3 rounded border border-red-700 bg-red-50 px-3 py-2 text-red-900"
  >
    <h2 class="font-semibold">Validation report</h2>
    <p role="alert">{{ errorsFound(issues.length) }}</p>
    <ul class="mt-2 list-disc pl-5">
      <li v-for="(issue, index) in issues" :key="index">
        <span>{{ issue.detail ?? issue.title }}</span>
        <span v-if="issue.location || issue.name"> ({{ issue.location ?? issue.name }})</span>
      </li>
    </ul>
    <p v-if="problem.truncated" class="mt-2">
      Only the first {{ issues.length }} errors are shown.
    </p>
  </section>
</template>

<script setup lang="ts">
import type { ProblemIssue } from '../api';
import { countText } from '../format';

defineProps<{ issues: ProblemIssue[] }>();
</script>

<template>
  <section aria-label="Validation report" class="mb-4 rounded border border-red-700 bg-red-50 p-3">
    <p role="alert" class="font-semibold text-red-800">
      {{ countText(issues.length, 'error', 'errors') }}
    </p>
    <ul class="mt-2 list-disc pl-6 text-red-900">
      <li v-for="(issue, index) in issues" :key="index">
        {{ issue.detail }}<span v-if="issue.location"> ({{ issue.location }})</span>
      </li>
    </ul>
  </section>
</template>

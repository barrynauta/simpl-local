<script setup lang="ts">
import type { Issue } from '../lib/api';
import { countText } from '../lib/texts';

defineProps<{ issues: Issue[] }>();
</script>

<template>
  <section aria-label="Validation report" class="alert-error">
    <p role="alert" class="font-semibold">{{ countText(issues.length, 'error found', 'errors found') }}</p>
    <ul class="mt-2 list-disc pl-5">
      <li v-for="(issue, index) in issues" :key="index">
        {{ issue.detail }}<span v-if="issue.location" class="text-gray-800"> ({{ issue.location }})</span>
      </li>
    </ul>
  </section>
</template>

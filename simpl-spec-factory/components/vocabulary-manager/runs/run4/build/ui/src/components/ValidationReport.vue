<script setup lang="ts">
import type { ProblemIssue } from '../types';

defineProps<{ issues: ProblemIssue[] }>();
</script>

<template>
  <div class="validation-report" role="alert" aria-label="Validation report">
    <h3>Validation report</h3>
    <p>The vocabulary was not created. Correct these errors and submit again.</p>
    <ul>
      <li v-for="(issue, i) in issues" :key="i">
        <span class="issue-detail">{{ issue.detail }}</span>
        <span v-if="issue.location" class="issue-location">Location: {{ issue.location }}</span>
        <span v-else-if="issue.name" class="issue-location">Field: {{ issue.name }}</span>
      </li>
    </ul>
  </div>
</template>

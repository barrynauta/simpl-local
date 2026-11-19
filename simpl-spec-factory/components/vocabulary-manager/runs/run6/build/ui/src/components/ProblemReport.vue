<script setup lang="ts">
import type { ProblemIssue } from "../api";

// Validation report panel above the form (decisions D14, D37): every issue with its detail and location.
defineProps<{ message: string; detail?: string; issues: ProblemIssue[] }>();
</script>

<template>
  <div class="message error" role="alert">
    <p>{{ message }}</p>
    <p v-if="detail">{{ detail }}</p>
    <ul v-if="issues.length > 0" aria-label="Validation report">
      <li v-for="(issue, i) in issues" :key="i">
        <span class="issue-detail">{{ issue.detail }}</span>
        <span v-if="issue.location" class="issue-location"> ({{ issue.location }})</span>
        <span v-else-if="issue.name" class="issue-location"> ({{ issue.name }})</span>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import type { ProblemIssue } from "../api";
import { count } from "../texts";

/** The validation report (D14, D90, D109): a region with an alert line and the issues. */
defineProps<{ issues: ProblemIssue[] }>();
</script>

<template>
  <section class="report" aria-label="Validation report">
    <p role="alert">{{ count(issues.length, "error") }}</p>
    <ul>
      <li v-for="(issue, index) in issues" :key="index">
        <span class="issue-detail">{{ issue.detail }}</span>
        <span v-if="issue.location" class="issue-location"> ({{ issue.location }})</span>
      </li>
    </ul>
  </section>
</template>

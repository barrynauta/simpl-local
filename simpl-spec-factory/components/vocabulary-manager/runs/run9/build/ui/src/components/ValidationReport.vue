<script setup lang="ts">
/** The validation report of a 400 (D14, D90): a region with an alert line '<n> errors found'
 * (the decided text, also for one issue) and the list of issues. */
defineProps<{ issues: { detail?: string; location?: string; name?: string }[] }>();
</script>

<template>
  <section aria-label="Validation report" class="panel error">
    <p role="alert">{{ issues.length }} errors found</p>
    <ul>
      <li v-for="(issue, index) in issues" :key="index">
        {{ issue.detail }}
        <span v-if="issue.location" class="location">({{ issue.location }})</span>
        <span v-else-if="issue.name" class="location">({{ issue.name }})</span>
      </li>
    </ul>
  </section>
</template>

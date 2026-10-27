<script setup lang="ts">
import { computed } from 'vue'
import type { ApiError } from '../api'

/** Shows an API error, including the structured validation report (SIMPL-18193). */
const props = defineProps<{ error: ApiError; title: string }>()
const issues = computed(() => props.error.problem?.issues ?? [])
const warnings = computed(() => props.error.problem?.validationWarnings ?? [])
const isValidation = computed(() => props.error.status === 400 && issues.value.some((i) => i.name === 'vocabularyFile'))
</script>

<template>
  <div class="error-box" role="alert" data-testid="problem-report">
    <strong>{{ title }}</strong>
    <div>
      <span v-if="error.status">HTTP {{ error.status }}<span v-if="error.problem?.title"> {{ error.problem.title }}</span>: </span>
      {{ error.problem?.detail ?? error.message }}
    </div>
    <div v-if="error.status === 403">You are not authorised to perform this action.</div>
    <div v-if="error.status >= 500 || error.status === 0">Please retry. If the problem persists, contact support.</div>
    <template v-if="issues.length">
      <p style="margin:0.5rem 0 0.2rem 0">
        <strong>{{ isValidation ? 'Validation report' : 'Details' }}</strong>
        ({{ issues.length }} {{ issues.length === 1 ? 'issue' : 'issues' }})
      </p>
      <table data-testid="validation-report">
        <thead><tr><th>#</th><th>Problem</th><th>Location</th><th>Field</th></tr></thead>
        <tbody>
          <tr v-for="(issue, i) in issues" :key="i">
            <td>{{ i + 1 }}</td>
            <td>{{ issue.detail ?? issue.title }}</td>
            <td><code v-if="issue.location">{{ issue.location }}</code></td>
            <td>{{ issue.name }}<span v-if="issue.in"> ({{ issue.in }})</span></td>
          </tr>
        </tbody>
      </table>
    </template>
    <template v-if="warnings.length">
      <p style="margin:0.5rem 0 0.2rem 0"><strong>Warnings</strong></p>
      <ul><li v-for="(w, i) in warnings" :key="i">{{ w.description }} <small>({{ w.location }})</small></li></ul>
    </template>
  </div>
</template>

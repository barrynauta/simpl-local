<script setup lang="ts">
import type { ValidationWarning } from '../api';
import { warningsFound } from '../format';

/** D14, D90, D109: the warnings region shown after a creation with warnings. */
defineProps<{ warnings: ValidationWarning[] }>();
</script>

<template>
  <section
    aria-label="Validation warnings"
    class="my-3 rounded border border-amber-700 bg-amber-50 px-3 py-2 text-amber-950"
  >
    <p role="status" class="font-semibold">{{ warningsFound(warnings.length) }}</p>
    <ul class="mt-2 list-disc pl-5">
      <li v-for="(warning, index) in warnings" :key="index">
        <span>{{ warning.description }}</span>
        <span v-if="warning.location"> ({{ warning.location }})</span>
      </li>
    </ul>
  </section>
</template>

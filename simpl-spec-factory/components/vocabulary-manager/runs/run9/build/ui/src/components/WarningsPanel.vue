<script setup lang="ts">
import type { ValidationWarning } from '../api';

/** Validation warnings after a successful creation (D14, D90): a region with a status line. */
defineProps<{ warnings: ValidationWarning[] }>();
</script>

<template>
  <section v-if="warnings.length > 0" aria-label="Validation warnings" class="panel warning">
    <p role="status">{{ warnings.length }} {{ warnings.length === 1 ? 'warning' : 'warnings' }} found</p>
    <ul>
      <li v-for="(warning, index) in warnings" :key="index">
        {{ warning.description }} <span class="location">({{ warning.location }})</span>
      </li>
    </ul>
  </section>
</template>

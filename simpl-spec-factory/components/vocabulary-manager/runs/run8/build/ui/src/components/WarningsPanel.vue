<script setup lang="ts">
import type { ValidationWarning } from '../api';

/** Validation warnings of a created version (D14): description and location. */
defineProps<{ warnings: ValidationWarning[] }>();
</script>

<template>
  <section v-if="warnings.length" class="panel warnings" aria-label="Validation warnings">
    <h2>Validation warnings</h2>
    <ul>
      <li v-for="(warning, index) in warnings" :key="index">
        <span>{{ warning.description }}</span>
        <span class="location">{{ warning.location }}</span>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import type { ValidationWarning } from "../api";

// Success notification and warnings panel after a creation (decision D14).
defineProps<{ success: string; warnings: ValidationWarning[] }>();
</script>

<template>
  <div v-if="success" class="message success" role="status">{{ success }}</div>
  <div v-if="warnings.length > 0" class="message warning" role="region" aria-label="Validation warnings">
    <p><strong>Validation warnings</strong></p>
    <ul>
      <li v-for="(w, i) in warnings" :key="i">
        <span class="warning-description">{{ w.description }}</span>
        <span class="warning-location"> ({{ w.location }})</span>
      </li>
    </ul>
  </div>
</template>

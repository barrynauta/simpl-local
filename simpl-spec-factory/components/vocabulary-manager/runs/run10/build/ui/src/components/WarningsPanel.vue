<script setup lang="ts">
import type { ValidationWarning } from '../api';
import { countText } from '../texts';

/** The region 'Validation warnings' of D14, D90 and D109. */
defineProps<{ warnings: ValidationWarning[] }>();
</script>

<template>
  <section class="warnings" aria-label="Validation warnings">
    <p role="status">{{ countText(warnings.length, 'warning', 'warnings') }}</p>
    <ul>
      <li v-for="(warning, index) in warnings" :key="index">
        {{ warning.description }} <span class="location">({{ warning.location }})</span>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import type { ValidationWarning } from '../lib/api';
import { countText } from '../lib/texts';

defineProps<{ warnings: ValidationWarning[] }>();
</script>

<template>
  <section v-if="warnings.length > 0" aria-label="Validation warnings" class="alert-warning">
    <p role="status" class="font-semibold">
      {{ countText(warnings.length, 'warning found', 'warnings found') }}
    </p>
    <ul class="mt-2 list-disc pl-5">
      <li v-for="(warning, index) in warnings" :key="index">
        {{ warning.description }} <span class="text-gray-800">({{ warning.location }})</span>
      </li>
    </ul>
  </section>
</template>

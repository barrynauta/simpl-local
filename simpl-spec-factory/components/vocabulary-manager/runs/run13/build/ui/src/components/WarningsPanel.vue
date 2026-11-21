<script setup lang="ts">
import type { ValidationWarning } from '../api';
import { countText } from '../format';

defineProps<{ warnings: ValidationWarning[] }>();
</script>

<template>
  <section
    aria-label="Validation warnings"
    class="mb-4 rounded border border-amber-700 bg-amber-50 p-3"
  >
    <p role="status" class="font-semibold text-amber-900">
      {{ countText(warnings.length, 'warning', 'warnings') }}
    </p>
    <ul class="mt-2 list-disc pl-6 text-amber-950">
      <li v-for="(warning, index) in warnings" :key="index">
        {{ warning.description }} <span class="text-slate-800">({{ warning.location }})</span>
      </li>
    </ul>
  </section>
</template>

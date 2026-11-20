<script setup lang="ts">
import type { ValidationWarning } from "../api";
import { count } from "../texts";

/** Validation warnings after a successful creation (D14, D90, D109). */
defineProps<{ warnings: ValidationWarning[] }>();
</script>

<template>
  <section class="warnings" aria-label="Validation warnings">
    <p role="status">{{ count(warnings.length, "warning") }}</p>
    <ul>
      <li v-for="(warning, index) in warnings" :key="index">
        {{ warning.description }} <span class="issue-location">({{ warning.location }})</span>
      </li>
    </ul>
  </section>
</template>

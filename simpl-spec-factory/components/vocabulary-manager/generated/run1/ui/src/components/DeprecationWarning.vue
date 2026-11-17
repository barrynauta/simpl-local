<script setup lang="ts">
/** Deprecation warning with a link to the latest ACTIVE version when there is one (SIMPL-18178, 18559, D25). */
defineProps<{ name: string; version?: number; latestActive?: number | null; subject?: 'vocabulary' | 'version' }>()
</script>

<template>
  <div class="warning" role="alert" data-testid="deprecation-warning">
    <strong>Warning: deprecated.</strong>
    <template v-if="subject === 'version'"> Version {{ version }} of {{ name }} is deprecated.</template>
    <template v-else> Vocabulary {{ name }} is deprecated.</template>
    Do not use it in new resources.
    <template v-if="latestActive">
      Use the latest active version instead:
      <RouterLink :to="`/vocabularies/${name}/versions/${latestActive}`" data-testid="latest-active-link">
        {{ name }} version {{ latestActive }}</RouterLink>.
    </template>
    <template v-else> No active version of this vocabulary is available.</template>
  </div>
</template>

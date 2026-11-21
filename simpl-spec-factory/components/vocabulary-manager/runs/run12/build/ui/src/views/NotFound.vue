<script setup lang="ts">
import { TEXT } from '../texts';
import { linkClass } from '../ui';
</script>

<template>
  <div>
    <h1 class="mb-3 text-2xl font-semibold">Page not found</h1>
    <RouterLink to="/vocabularies" :class="linkClass">{{ TEXT.backToList }}</RouterLink>
  </div>
</template>

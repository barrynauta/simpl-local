<script setup lang="ts">
import { statusLabel } from '../format'
defineProps<{ status: string }>()
</script>

<template>
  <span :class="['badge', status === 'ACTIVE' ? 'active' : 'deprecated']" :data-status="status">
    {{ status === 'DEPRECATED' ? 'Warning: ' : '' }}{{ statusLabel(status) }}
  </span>
</template>

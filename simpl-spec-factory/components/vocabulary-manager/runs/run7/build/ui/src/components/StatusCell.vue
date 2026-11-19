<script setup lang="ts">
import { statusLabel } from '../lib/format'
import { TEXT } from '../lib/messages'

/** Vocabulary status with the deprecation icon of D36 and D72 (name and tooltip, not visible text). */
const props = defineProps<{ status: string }>()
</script>

<template>
  <span class="status" :class="props.status === 'DEPRECATED' ? 'status-deprecated' : 'status-active'">
    {{ statusLabel(props.status) }}
    <span
      v-if="props.status === 'DEPRECATED'"
      role="img"
      class="warning-icon"
      :aria-label="TEXT.vocabularyDeprecated"
      :title="TEXT.vocabularyDeprecated"
      >&#9888;</span
    >
  </span>
</template>

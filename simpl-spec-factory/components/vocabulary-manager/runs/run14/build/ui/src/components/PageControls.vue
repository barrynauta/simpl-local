<script setup lang="ts">
defineProps<{ page: number; totalPages: number }>();
const emit = defineEmits<{ go: [page: number] }>();
</script>

<template>
  <nav v-if="totalPages > 0" aria-label="Pagination" class="mt-3 flex items-center gap-3 text-sm">
    <button type="button" class="btn-secondary" :disabled="page <= 1" @click="emit('go', page - 1)">
      Previous page
    </button>
    <span>Page {{ page }} of {{ totalPages }}</span>
    <button type="button" class="btn-secondary" :disabled="page >= totalPages" @click="emit('go', page + 1)">
      Next page
    </button>
  </nav>
</template>

<script setup lang="ts">
import Dialog from 'primevue/dialog';
import { nextTick, ref, watch } from 'vue';

const props = defineProps<{ visible: boolean; title: string; text: string }>();
const emit = defineEmits<{ confirm: []; cancel: [] }>();
const cancelButton = ref<HTMLButtonElement | null>(null);

watch(
  () => props.visible,
  async (shown) => {
    if (shown) {
      await nextTick();
      cancelButton.value?.focus();
    }
  },
);

function onVisible(value: boolean): void {
  if (!value) {
    emit('cancel');
  }
}
</script>

<template>
  <Dialog
    :visible="visible"
    modal
    :header="title"
    :draggable="false"
    :pt="{
      mask: { class: 'fixed inset-0 z-40 flex items-center justify-center bg-black/40' },
      root: { class: 'w-full max-w-md rounded bg-white p-4 shadow-xl' },
      header: { class: 'mb-3 flex items-center justify-between' },
      title: { class: 'text-lg font-semibold' },
      pcCloseButton: { root: { class: 'btn-secondary', 'aria-label': 'Close' } },
      content: { class: 'mb-4 text-sm' },
      footer: { class: 'flex justify-end gap-2' },
    }"
    @update:visible="onVisible"
  >
    <p>{{ text }}</p>
    <template #footer>
      <button ref="cancelButton" type="button" class="btn-secondary" @click="emit('cancel')">Cancel</button>
      <button type="button" class="btn" @click="emit('confirm')">Confirm</button>
    </template>
  </Dialog>
</template>

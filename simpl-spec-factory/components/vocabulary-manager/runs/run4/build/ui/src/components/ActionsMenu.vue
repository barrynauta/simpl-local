<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import type { MenuAction } from '../types';

defineProps<{ actions: MenuAction[]; label?: string }>();
const open = ref(false);
const root = ref<HTMLElement | null>(null);

function choose(action: MenuAction): void {
  open.value = false;
  action.run();
}

function onDocumentClick(event: MouseEvent): void {
  if (root.value && !root.value.contains(event.target as Node)) open.value = false;
}

onMounted(() => document.addEventListener('click', onDocumentClick));
onBeforeUnmount(() => document.removeEventListener('click', onDocumentClick));
</script>

<template>
  <div ref="root" class="actions-menu">
    <button
      type="button"
      class="menu-button"
      :aria-label="label ?? 'Actions'"
      :aria-expanded="open"
      aria-haspopup="menu"
      @click="open = !open"
    >
      &#8942;
    </button>
    <ul v-if="open" class="menu" role="menu">
      <li v-for="action in actions" :key="action.label" role="none">
        <button type="button" role="menuitem" @click="choose(action)">{{ action.label }}</button>
      </li>
    </ul>
  </div>
</template>

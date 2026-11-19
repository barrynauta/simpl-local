<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref } from 'vue';

/** A row actions menu: one button named by `label` that opens a menu of actions (D75). */
export interface MenuItem {
  label: string;
  run: () => void;
}

const props = defineProps<{ label: string; items: MenuItem[] }>();
const open = ref(false);
const root = ref<HTMLElement | null>(null);
const toggleButton = ref<HTMLButtonElement | null>(null);

async function toggle() {
  open.value = !open.value;
  if (open.value) {
    await nextTick();
    root.value?.querySelector<HTMLButtonElement>('[role="menuitem"]')?.focus();
  }
}

function choose(item: MenuItem) {
  open.value = false;
  item.run();
}

function close(event: MouseEvent) {
  if (open.value && root.value && !root.value.contains(event.target as Node)) {
    open.value = false;
  }
}

function onKey(event: KeyboardEvent) {
  if (event.key === 'Escape' && open.value) {
    open.value = false;
    toggleButton.value?.focus();
  }
}

document.addEventListener('click', close);
onBeforeUnmount(() => document.removeEventListener('click', close));
</script>

<template>
  <div ref="root" class="menu" @keydown="onKey">
    <button
      ref="toggleButton"
      type="button"
      class="menu-toggle"
      :aria-label="props.label"
      aria-haspopup="menu"
      :aria-expanded="open ? 'true' : 'false'"
      @click.stop="toggle"
    >
      <span aria-hidden="true">&#8942;</span>
    </button>
    <ul v-if="open" role="menu" class="menu-list" :aria-label="props.label">
      <li v-for="item in props.items" :key="item.label" role="none">
        <button type="button" role="menuitem" @click="choose(item)">{{ item.label }}</button>
      </li>
    </ul>
  </div>
</template>

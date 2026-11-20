<script setup lang="ts">
import { nextTick, ref } from "vue";

/** Row actions menu (D75): one button per row named 'Actions for ...', keyboard reachable. */
export interface MenuItem {
  label: string;
  run: () => void;
}

defineProps<{ label: string; items: MenuItem[] }>();

const open = ref(false);
const menu = ref<HTMLElement | null>(null);

async function toggle(): Promise<void> {
  open.value = !open.value;
  if (open.value) {
    await nextTick();
    menu.value?.querySelector<HTMLButtonElement>("button")?.focus();
  }
}

function choose(item: MenuItem): void {
  open.value = false;
  item.run();
}
</script>

<template>
  <div class="actions" @keydown.escape="open = false">
    <button
      type="button"
      class="menu-button"
      :aria-label="label"
      aria-haspopup="menu"
      :aria-expanded="open"
      @click="toggle"
    >
      &#8942;
    </button>
    <ul v-if="open" ref="menu" role="menu" class="menu">
      <li v-for="item in items" :key="item.label" role="none">
        <button type="button" role="menuitem" @click="choose(item)">{{ item.label }}</button>
      </li>
    </ul>
  </div>
</template>

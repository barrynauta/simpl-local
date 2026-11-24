<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref } from 'vue';

export interface MenuItem {
  label: string;
  action: () => void;
}

const props = defineProps<{ label: string; items: MenuItem[] }>();

const open = ref(false);
const root = ref<HTMLElement | null>(null);
const trigger = ref<HTMLButtonElement | null>(null);
const itemRefs = ref<HTMLButtonElement[]>([]);

async function toggle(): Promise<void> {
  open.value = !open.value;
  if (open.value) {
    document.addEventListener('click', outside, true);
    await nextTick();
    itemRefs.value[0]?.focus();
  } else {
    document.removeEventListener('click', outside, true);
  }
}

function close(focusTrigger: boolean): void {
  open.value = false;
  document.removeEventListener('click', outside, true);
  if (focusTrigger) {
    trigger.value?.focus();
  }
}

function outside(event: Event): void {
  if (root.value && !root.value.contains(event.target as Node)) {
    close(false);
  }
}

function choose(item: MenuItem): void {
  close(false);
  item.action();
}

function onKey(event: KeyboardEvent, index: number): void {
  const count = props.items.length;
  if (event.key === 'ArrowDown') {
    event.preventDefault();
    itemRefs.value[(index + 1) % count]?.focus();
  } else if (event.key === 'ArrowUp') {
    event.preventDefault();
    itemRefs.value[(index - 1 + count) % count]?.focus();
  } else if (event.key === 'Escape') {
    event.preventDefault();
    close(true);
  } else if (event.key === 'Tab') {
    close(false);
  }
}

onBeforeUnmount(() => document.removeEventListener('click', outside, true));
</script>

<template>
  <div ref="root" class="relative inline-block">
    <button
      ref="trigger"
      type="button"
      class="btn-secondary"
      :aria-label="label"
      aria-haspopup="menu"
      :aria-expanded="open ? 'true' : 'false'"
      @click="toggle"
    >
      <span aria-hidden="true">⋯</span>
    </button>
    <ul
      v-if="open"
      role="menu"
      :aria-label="label"
      class="absolute right-0 z-10 mt-1 min-w-56 rounded border border-gray-600 bg-white py-1 shadow-lg"
    >
      <li v-for="(item, index) in items" :key="item.label" role="none">
        <button
          :ref="(el) => (itemRefs[index] = el as HTMLButtonElement)"
          type="button"
          role="menuitem"
          class="block w-full px-3 py-1.5 text-left text-sm text-gray-900 hover:bg-gray-100 focus:bg-gray-200 focus:outline-none"
          @click="choose(item)"
          @keydown="onKey($event, index)"
        >
          {{ item.label }}
        </button>
      </li>
    </ul>
  </div>
</template>

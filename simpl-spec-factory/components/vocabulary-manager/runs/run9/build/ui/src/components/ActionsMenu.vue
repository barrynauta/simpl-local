<script setup lang="ts">
import { nextTick, ref } from 'vue';

/** One actions menu button per row (D75), reachable and usable by keyboard (Q6). */
export interface MenuAction {
  label: string;
  run: () => void;
}

const props = defineProps<{ label: string; actions: MenuAction[] }>();
const open = ref(false);
const menu = ref<HTMLElement | null>(null);
const button = ref<HTMLButtonElement | null>(null);

async function toggle(): Promise<void> {
  open.value = !open.value;
  if (open.value) {
    await nextTick();
    menu.value?.querySelector<HTMLButtonElement>('[role="menuitem"]')?.focus();
  }
}

function close(): void {
  open.value = false;
  button.value?.focus();
}

function choose(action: MenuAction): void {
  open.value = false;
  action.run();
}

function move(event: KeyboardEvent): void {
  const items = Array.from(menu.value?.querySelectorAll<HTMLButtonElement>('[role="menuitem"]') ?? []);
  const index = items.indexOf(document.activeElement as HTMLButtonElement);
  if (event.key === 'ArrowDown') {
    items[(index + 1) % items.length]?.focus();
    event.preventDefault();
  } else if (event.key === 'ArrowUp') {
    items[(index - 1 + items.length) % items.length]?.focus();
    event.preventDefault();
  } else if (event.key === 'Escape') {
    close();
  }
}
</script>

<template>
  <div class="menu">
    <button
      ref="button"
      type="button"
      class="secondary"
      :aria-label="props.label"
      aria-haspopup="menu"
      :aria-expanded="open ? 'true' : 'false'"
      @click="toggle"
    >
      &#8942;
    </button>
    <div v-if="open" ref="menu" role="menu" :aria-label="props.label" class="menu-list" @keydown="move">
      <button
        v-for="action in props.actions"
        :key="action.label"
        type="button"
        role="menuitem"
        @click="choose(action)"
      >
        {{ action.label }}
      </button>
    </div>
  </div>
</template>

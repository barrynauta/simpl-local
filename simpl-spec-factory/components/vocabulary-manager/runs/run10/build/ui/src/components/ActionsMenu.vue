<script setup lang="ts">
import { nextTick, ref } from 'vue';
import type { MenuAction } from './menu';

/** A row actions menu (D75): one button named 'Actions for ...' that opens a menu; keyboard operable. */

const props = defineProps<{ label: string; actions: MenuAction[] }>();
const emit = defineEmits<{ select: [key: string] }>();

const open = ref(false);
const button = ref<HTMLButtonElement | null>(null);
const items = ref<HTMLButtonElement[]>([]);

async function toggle(): Promise<void> {
  open.value = !open.value;
  if (open.value) {
    await nextTick();
    items.value[0]?.focus();
  }
}

function close(): void {
  open.value = false;
  button.value?.focus();
}

function choose(key: string): void {
  open.value = false;
  emit('select', key);
}

function move(index: number, step: number): void {
  const count = props.actions.length;
  items.value[(index + step + count) % count]?.focus();
}
</script>

<template>
  <div class="menu" @keydown.esc.stop="close">
    <button
      ref="button"
      type="button"
      class="menu-button"
      :aria-label="label"
      aria-haspopup="menu"
      :aria-expanded="open ? 'true' : 'false'"
      @click="toggle"
    >
      <span aria-hidden="true">&#8942;</span>
    </button>
    <ul v-if="open" class="menu-list" role="menu" :aria-label="label">
      <li v-for="(action, index) in actions" :key="action.key" role="none">
        <button
          ref="items"
          type="button"
          role="menuitem"
          @click="choose(action.key)"
          @keydown.down.prevent="move(index, 1)"
          @keydown.up.prevent="move(index, -1)"
        >
          {{ action.label }}
        </button>
      </li>
    </ul>
  </div>
</template>

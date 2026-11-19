<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref } from 'vue'

/** Row actions menu: a menu button named by the caller (D75) with keyboard support (Q6). */
export interface MenuAction {
  label: string
  run: () => void
}

const props = defineProps<{ label: string; actions: MenuAction[] }>()
const open = ref(false)
const button = ref<HTMLButtonElement | null>(null)
const menu = ref<HTMLUListElement | null>(null)
const menuId = `menu-${Math.random().toString(36).slice(2, 10)}`

function items(): HTMLElement[] {
  return Array.from(menu.value?.querySelectorAll<HTMLElement>('[role="menuitem"]') ?? [])
}

async function toggle() {
  open.value = !open.value
  if (open.value) {
    document.addEventListener('click', onOutside)
    await nextTick()
    items()[0]?.focus()
  } else {
    close()
  }
}

function close(returnFocus = false) {
  open.value = false
  document.removeEventListener('click', onOutside)
  if (returnFocus) button.value?.focus()
}

function onOutside(event: MouseEvent) {
  const target = event.target as Node
  if (!menu.value?.contains(target) && !button.value?.contains(target)) close()
}

function choose(action: MenuAction) {
  close()
  action.run()
}

function onKey(event: KeyboardEvent) {
  const list = items()
  const index = list.indexOf(document.activeElement as HTMLElement)
  if (event.key === 'Escape') {
    event.preventDefault()
    close(true)
  } else if (event.key === 'ArrowDown') {
    event.preventDefault()
    list[(index + 1) % list.length]?.focus()
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    list[(index - 1 + list.length) % list.length]?.focus()
  } else if (event.key === 'Tab') {
    close()
  }
}

onBeforeUnmount(() => document.removeEventListener('click', onOutside))
</script>

<template>
  <div class="menu-wrapper">
    <button
      ref="button"
      type="button"
      class="menu-button"
      :aria-label="props.label"
      aria-haspopup="menu"
      :aria-expanded="open ? 'true' : 'false'"
      :aria-controls="menuId"
      @click.stop="toggle"
    >
      <span aria-hidden="true">&#8942;</span>
    </button>
    <ul
      v-show="open"
      :id="menuId"
      ref="menu"
      role="menu"
      :aria-label="props.label"
      class="menu"
      @keydown="onKey"
    >
      <li v-for="action in props.actions" :key="action.label" role="none">
        <button type="button" role="menuitem" tabindex="-1" @click="choose(action)">
          {{ action.label }}
        </button>
      </li>
    </ul>
  </div>
</template>

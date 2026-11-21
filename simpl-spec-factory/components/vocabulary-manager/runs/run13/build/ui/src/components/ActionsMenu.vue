<script setup lang="ts">
import { ref, useId } from 'vue';
import Menu from 'primevue/menu';
import type { MenuItem } from 'primevue/menuitem';

defineProps<{ label: string; items: MenuItem[] }>();
const menu = ref<InstanceType<typeof Menu>>();
const menuId = `actions-${useId()}`;
const open = ref(false);

function toggle(event: Event): void {
  menu.value?.toggle(event);
}
</script>

<template>
  <button
    type="button"
    class="rounded border border-slate-500 px-2 py-1 text-sm text-slate-900 hover:bg-slate-100 focus:outline-2 focus:outline-blue-700"
    :aria-label="label"
    aria-haspopup="menu"
    :aria-expanded="open ? 'true' : 'false'"
    :aria-controls="menuId"
    @click="toggle"
  >
    <span aria-hidden="true">&#8943;</span>
  </button>
  <Menu
    :id="menuId"
    ref="menu"
    :model="items"
    :popup="true"
    :aria-label="label"
    :pt="{
      root: { class: 'z-50 min-w-48 rounded border border-slate-400 bg-white py-1 shadow-lg' },
      list: { class: 'm-0 list-none p-0 outline-none' },
      item: { class: 'cursor-pointer' },
      itemContent: { class: 'hover:bg-slate-100' },
      itemLink: { class: 'block px-3 py-2 text-sm text-slate-900 no-underline' },
      itemLabel: { class: '' },
    }"
    @show="open = true"
    @hide="open = false"
  />
</template>

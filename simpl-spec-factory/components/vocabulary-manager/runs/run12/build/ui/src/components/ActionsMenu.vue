<script setup lang="ts">
import { ref, useId } from 'vue';
import Button from 'primevue/button';
import Menu from 'primevue/menu';

export interface ActionItem {
  label: string;
  run: () => void;
}

const props = defineProps<{ label: string; items: ActionItem[] }>();

const menu = ref<InstanceType<typeof Menu> | null>(null);
const menuId = `actions-${useId()}`;

const model = () => props.items.map((item) => ({ label: item.label, command: () => item.run() }));

function toggle(event: Event) {
  menu.value?.toggle(event);
}
</script>

<template>
  <Button
    type="button"
    :aria-label="label"
    aria-haspopup="menu"
    class="rounded border border-gray-600 bg-white px-2 py-0.5 text-gray-900 hover:bg-gray-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600"
    @click="toggle"
  >
    <span aria-hidden="true">&#8942;</span>
  </Button>
  <Menu
    :id="menuId"
    ref="menu"
    :model="model()"
    :popup="true"
    :pt="{
      root: { class: 'z-50 min-w-52 rounded border border-gray-400 bg-white py-1 shadow-lg' },
      list: { class: 'outline-none' },
      itemContent: {
        class: 'cursor-pointer px-3 py-1.5 text-sm text-gray-900 hover:bg-blue-50',
      },
      itemLink: { class: 'flex items-center no-underline text-gray-900' },
      item: { class: 'data-[p-focused=true]:bg-blue-100' },
    }"
  />
</template>

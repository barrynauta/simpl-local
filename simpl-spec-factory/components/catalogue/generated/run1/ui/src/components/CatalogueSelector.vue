<template>
  <fieldset class="checks">
    <legend>Catalogues</legend>
    <div class="row">
      <label>
        <input type="checkbox" data-testid="catalogue-option-local" value="local" v-model="selected" />
        Own catalogue
      </label>
      <label v-for="c in catalogues" :key="c.id">
        <input type="checkbox" :data-testid="`catalogue-option-${c.id}`" :value="c.id" :disabled="!c.active" v-model="selected" />
        {{ c.name }}<span v-if="!c.active"> (inactive)</span>
      </label>
    </div>
    <p v-if="error" class="error" data-testid="catalogue-error">The list of federated catalogues could not be loaded. {{ error }}</p>
  </fieldset>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { getJson, type Catalogue } from '../api';

const props = defineProps<{ modelValue: string[] }>();
const emit = defineEmits<{ (e: 'update:modelValue', v: string[]): void }>();
const catalogues = ref<Catalogue[]>([]);
const error = ref('');
const selected = ref<string[]>([...props.modelValue]);

watch(selected, (v) => emit('update:modelValue', [...v]));

// D71: the list is loaded when a search tab opens; on failure only the own option shows.
onMounted(async () => {
  try {
    // First page without a query string (defaults page 1, size 20), further pages only when needed.
    const first = await getJson<{ items: Catalogue[]; total: number; pageSize: number }>('/api/v1/catalogues');
    const all = [...(first.items ?? [])];
    for (let page = 2; all.length < (first.total ?? 0) && page <= 50; page++) {
      const next = await getJson<{ items: Catalogue[] }>(`/api/v1/catalogues?page=${page}&pageSize=${first.pageSize}`);
      if (!next.items?.length) break;
      all.push(...next.items);
    }
    catalogues.value = all;
  } catch (e: any) {
    catalogues.value = [];
    error.value = e?.message ?? '';
  }
});
</script>

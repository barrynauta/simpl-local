<template>
  <div>
    <div class="tabs">
      <button data-testid="quick-search-tab" :class="{ active: mode === 'quick' }" @click="setMode('quick')">Quick search</button>
      <button data-testid="advanced-search-tab" :class="{ active: mode === 'advanced' }" @click="setMode('advanced')">Advanced search</button>
    </div>
    <div class="panel" :key="mode">
      <CatalogueSelector v-model="selected" />
      <div class="row" v-if="mode === 'quick'">
        <label>
          Search terms
          <input data-testid="search-input" type="text" v-model="term" @keyup.enter="canSearch && search()" />
        </label>
      </div>
      <AdvancedForm v-else :federated="federated" @criteria="criteria = $event" />
      <QualityFilters @change="quality = $event" />
      <div class="row" style="margin-top: 0.6rem">
        <button class="primary" data-testid="search-button" :disabled="!canSearch" @click="search()">Search</button>
        <span v-if="loading">Searching...</span>
        <span v-if="error" class="error">{{ error }}</span>
      </div>
    </div>
    <ResultsTabs :groups="groups" @page="pageGroup" @open="open" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import CatalogueSelector from '../components/CatalogueSelector.vue';
import QualityFilters from '../components/QualityFilters.vue';
import ResultsTabs from '../components/ResultsTabs.vue';
import AdvancedForm from '../components/AdvancedForm.vue';
import { getJson, postJson, type Group, type SearchResponse } from '../api';

const router = useRouter();
const mode = ref<'quick' | 'advanced'>('quick');
const selected = ref<string[]>(['local']);
const term = ref('');
const criteria = ref<Record<string, unknown>>({});
const quality = ref<{ minimumScore?: number; minimumClassification?: string; valid: boolean }>({ valid: true });
const groups = ref<Group[]>([]);
const loading = ref(false);
const error = ref('');
let lastRequest: { mode: 'quick' | 'advanced'; term: string; criteria: Record<string, unknown>; quality: typeof quality.value } | null = null;

const federated = computed(() => selected.value.some((s) => s !== 'local'));
const canSearch = computed(() => quality.value.valid && selected.value.length > 0
  && (mode.value === 'advanced' || term.value.trim() !== ''));

function setMode(m: 'quick' | 'advanced') {
  if (mode.value === m) return;
  mode.value = m;
  selected.value = ['local'];
  groups.value = [];
  criteria.value = {};
  error.value = '';
}

async function request(ids: string[], page: number, req: NonNullable<typeof lastRequest>): Promise<SearchResponse> {
  if (req.mode === 'quick') {
    const p = new URLSearchParams();
    p.set('q', req.term);
    ids.forEach((id) => p.append('catalogueIds', id));
    if (req.quality.minimumScore !== undefined) p.set('minimumScore', String(req.quality.minimumScore));
    if (req.quality.minimumClassification) p.set('minimumClassification', req.quality.minimumClassification);
    p.set('page', String(page));
    p.set('pageSize', '20');
    return getJson<SearchResponse>(`/api/v1/selfDescriptions?${p}`);
  }
  const body: Record<string, unknown> = { ...req.criteria, catalogueIds: ids };
  if (req.quality.minimumScore !== undefined) body.minimumScore = req.quality.minimumScore;
  if (req.quality.minimumClassification) body.minimumClassification = req.quality.minimumClassification;
  return postJson<SearchResponse>(`/api/v1/selfDescriptions/advanced?page=${page}&pageSize=20`, body);
}

async function search() {
  error.value = '';
  loading.value = true;
  lastRequest = { mode: mode.value, term: term.value.trim(), criteria: { ...criteria.value }, quality: { ...quality.value } };
  try {
    const res = await request([...selected.value], 1, lastRequest);
    groups.value = res.groups;
  } catch (e: any) {
    groups.value = [];
    error.value = `Search failed: ${e?.message ?? e}`;
  } finally {
    loading.value = false;
  }
}

// D73: paging one tab re-requests only that catalogue and replaces only that tab.
async function pageGroup(catalogueId: string, page: number) {
  if (!lastRequest) return;
  try {
    const res = await request([catalogueId], page, lastRequest);
    const g = res.groups.find((x) => x.catalogueId === catalogueId) ?? res.groups[0];
    if (g) groups.value = groups.value.map((x) => (x.catalogueId === catalogueId ? g : x));
  } catch (e: any) {
    error.value = `Paging failed: ${e?.message ?? e}`;
  }
}

function open(id: string, catalogueId: string) {
  router.push({ path: '/details', query: { id, catalogueId } });
}
</script>

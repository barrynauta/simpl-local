<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import ActionsMenu, { type ActionItem } from '../components/ActionsMenu.vue';
import AlertMessage from '../components/AlertMessage.vue';
import StatusMessage from '../components/StatusMessage.vue';
import { api, saveBlob, type Vocabulary, type VocabularyStatus } from '../api';
import { isAdmin } from '../auth';
import { formatDate, isStatus, problemDetail, statusChangeError, statusLabel } from '../format';
import { TEXT } from '../texts';
import { btnPrimary, btnSecondary, inputClass, linkClass } from '../ui';

/** Vocabulary list (D23, D34, D36, D50, D60, D72, D100, D114, D115, D159, D163). */
type SortField = 'name' | 'status' | 'creationDate' | 'lastUpdated';

const PAGE_SIZE = 20;
const router = useRouter();

const items = ref<Vocabulary[]>([]);
const page = ref(1);
const totalPages = ref(0);
const loaded = ref(false);
const loadError = ref<{ text: string; detail: string } | null>(null);
const actionError = ref<{ text: string; detail: string } | null>(null);
const actionMessage = ref('');
const sort = reactive<{ field: SortField; descending: boolean }>({
  field: 'name',
  descending: false,
});
const filters = reactive({ name: '', status: '', created: '' });
const admin = computed(() => isAdmin());

let requestCounter = 0;
let nameTimer: ReturnType<typeof setTimeout> | undefined;

async function load() {
  const current = ++requestCounter;
  try {
    const result = await api.listVocabularies({
      page: page.value,
      pageSize: PAGE_SIZE,
      sort: (sort.descending ? '-' : '') + sort.field,
      name: filters.name || undefined,
      status: filters.status || undefined,
      creationDate: filters.created || undefined,
    });
    if (current !== requestCounter) return;
    items.value = result.items;
    totalPages.value = result.totalPages;
    loadError.value = null;
  } catch (error) {
    if (current !== requestCounter) return;
    items.value = [];
    totalPages.value = 0;
    loadError.value = isStatus(error, 403)
      ? { text: TEXT.notAuthorized, detail: '' }
      : { text: TEXT.listLoadFailed, detail: problemDetail(error) };
  } finally {
    if (current === requestCounter) loaded.value = true;
  }
}

function sortBy(field: SortField) {
  if (sort.field === field) {
    sort.descending = !sort.descending;
  } else {
    sort.field = field;
    sort.descending = false;
  }
  page.value = 1;
  void load();
}

function ariaSort(field: SortField): 'ascending' | 'descending' | 'none' {
  if (sort.field !== field) return 'none';
  return sort.descending ? 'descending' : 'ascending';
}

function arrow(field: SortField): string {
  if (sort.field !== field) return '';
  return sort.descending ? '▼' : '▲';
}

function applyFilters() {
  page.value = 1;
  void load();
}

watch(
  () => filters.name,
  () => {
    if (nameTimer) clearTimeout(nameTimer);
    nameTimer = setTimeout(applyFilters, 300);
  },
);
watch(() => [filters.status, filters.created], applyFilters);

function goTo(newPage: number) {
  page.value = newPage;
  void load();
}

const shownTotalPages = computed(() => Math.max(totalPages.value, 1));

async function changeStatus(vocabulary: Vocabulary) {
  const target: VocabularyStatus = vocabulary.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE';
  actionError.value = null;
  actionMessage.value = '';
  try {
    const updated = await api.changeStatus(vocabulary.name, target);
    actionMessage.value = `Status of ${updated.name} changed to ${statusLabel(updated.status)}.`;
    await load();
  } catch (error) {
    actionError.value = statusChangeError(error);
  }
}

async function download(vocabulary: Vocabulary) {
  actionError.value = null;
  actionMessage.value = '';
  try {
    const { blob, filename } = await api.downloadContent(vocabulary.name);
    saveBlob(blob, filename);
  } catch (error) {
    actionError.value = isStatus(error, 403)
      ? { text: TEXT.notAuthorized, detail: '' }
      : { text: TEXT.downloadFailed, detail: problemDetail(error) };
  }
}

function actionsFor(vocabulary: Vocabulary): ActionItem[] {
  const details = () => router.push(`/vocabularies/${vocabulary.name}`);
  const actions: ActionItem[] = [
    { label: 'View vocabulary details', run: details },
    { label: 'History', run: details },
    { label: 'Download vocabulary', run: () => void download(vocabulary) },
  ];
  if (admin.value) {
    actions.push({
      label: 'Update',
      run: () => router.push(`/vocabularies/${vocabulary.name}/versions/new`),
    });
    actions.push({
      label: vocabulary.status === 'ACTIVE' ? 'Deprecate' : 'Activate',
      run: () => void changeStatus(vocabulary),
    });
  }
  return actions;
}

const sortableColumns: { field: SortField; label: string }[] = [
  { field: 'name', label: 'Name' },
  { field: 'status', label: 'Status' },
];

onMounted(() => {
  document.title = 'Vocabularies - Vocabulary Manager';
  void load();
});
onBeforeUnmount(() => {
  if (nameTimer) clearTimeout(nameTimer);
});
</script>

<template>
  <div>
    <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
      <h1 class="text-2xl font-semibold">Vocabularies</h1>
      <Button
        v-if="admin"
        type="button"
        label="New Vocabulary +"
        :class="btnPrimary"
        @click="router.push('/vocabularies/new')"
      />
    </div>

    <form class="mb-4 flex flex-wrap items-end gap-4" role="search" @submit.prevent="applyFilters">
      <div>
        <label for="filter-name" class="block text-sm font-medium">Name contains</label>
        <InputText id="filter-name" v-model="filters.name" type="search" :class="inputClass" />
      </div>
      <div>
        <label for="filter-status" class="block text-sm font-medium">Vocabulary status</label>
        <select id="filter-status" v-model="filters.status" :class="inputClass">
          <option value="">All</option>
          <option value="ACTIVE">Active</option>
          <option value="DEPRECATED">Deprecated</option>
        </select>
      </div>
      <div>
        <label for="filter-created" class="block text-sm font-medium">Created on</label>
        <input id="filter-created" v-model="filters.created" type="date" :class="inputClass" />
      </div>
    </form>

    <StatusMessage v-if="actionMessage" :text="actionMessage" />
    <AlertMessage v-if="actionError" :text="actionError.text" :detail="actionError.detail" />
    <AlertMessage v-if="loadError" :text="loadError.text" :detail="loadError.detail" />

    <div class="overflow-x-auto">
      <table class="min-w-full border-collapse text-left text-sm">
        <caption class="sr-only">
          Vocabularies
        </caption>
        <thead class="border-b-2 border-gray-700">
          <tr>
            <th
              v-for="column in sortableColumns"
              :key="column.field"
              scope="col"
              :aria-sort="ariaSort(column.field)"
              class="px-2 py-2"
            >
              <button type="button" class="font-semibold underline" @click="sortBy(column.field)">
                {{ column.label }}<span aria-hidden="true">{{ arrow(column.field) }}</span>
              </button>
            </th>
            <th scope="col" class="px-2 py-2">Latest version</th>
            <th scope="col" class="px-2 py-2">Description</th>
            <th scope="col" class="px-2 py-2">Creator</th>
            <th scope="col" :aria-sort="ariaSort('creationDate')" class="px-2 py-2">
              <button type="button" class="font-semibold underline" @click="sortBy('creationDate')">
                Date created<span aria-hidden="true">{{ arrow('creationDate') }}</span>
              </button>
            </th>
            <th scope="col" :aria-sort="ariaSort('lastUpdated')" class="px-2 py-2">
              <button type="button" class="font-semibold underline" @click="sortBy('lastUpdated')">
                Last updated<span aria-hidden="true">{{ arrow('lastUpdated') }}</span>
              </button>
            </th>
            <th scope="col" class="px-2 py-2">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="vocabulary in items" :key="vocabulary.name" class="border-b border-gray-300">
            <td class="px-2 py-2">
              <RouterLink :to="`/vocabularies/${vocabulary.name}`" :class="linkClass">{{
                vocabulary.name
              }}</RouterLink>
            </td>
            <td class="px-2 py-2">
              {{ statusLabel(vocabulary.status) }}
              <span
                v-if="vocabulary.status === 'DEPRECATED'"
                role="img"
                :aria-label="TEXT.vocabularyDeprecated"
                :title="TEXT.vocabularyDeprecated"
                class="ml-1 text-amber-800"
                >&#9888;</span
              >
            </td>
            <td class="px-2 py-2">{{ vocabulary.version }}</td>
            <td class="px-2 py-2">{{ vocabulary.description }}</td>
            <td class="px-2 py-2">{{ vocabulary.createdBy }}</td>
            <td class="px-2 py-2 whitespace-nowrap">{{ formatDate(vocabulary.creationDate) }}</td>
            <td class="px-2 py-2 whitespace-nowrap">
              {{ formatDate(vocabulary.lastUpdateDate) }}
            </td>
            <td class="px-2 py-2">
              <ActionsMenu
                :label="`Actions for ${vocabulary.name}`"
                :items="actionsFor(vocabulary)"
              />
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <p v-if="loaded && !loadError && items.length === 0" class="my-4">{{ TEXT.noVocabularies }}</p>

    <nav aria-label="Pagination" class="mt-4 flex items-center gap-3">
      <button
        type="button"
        :class="btnSecondary"
        :disabled="page <= 1"
        @click="goTo(Math.min(page - 1, Math.max(totalPages, 1)))"
      >
        Previous page
      </button>
      <span>Page {{ page }} of {{ shownTotalPages }}</span>
      <button
        type="button"
        :class="btnSecondary"
        :disabled="page >= totalPages"
        @click="goTo(page + 1)"
      >
        Next page
      </button>
    </nav>
  </div>
</template>

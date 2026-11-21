<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import type { MenuItem } from 'primevue/menuitem';
import ActionsMenu from '../components/ActionsMenu.vue';
import {
  downloadContent,
  listVocabularies,
  patchVocabularyStatus,
  type Status,
  type Vocabulary,
} from '../api';
import { isAdmin } from '../auth';
import { loadErrorText, statusErrorText, submitErrorText } from '../errors';
import { formatDate, statusLabel } from '../format';
import { TEXT } from '../messages';

type SortField = 'name' | 'status' | 'creationDate' | 'lastUpdated';

const PAGE_SIZE = 20;
const router = useRouter();
const admin = isAdmin();

const items = ref<Vocabulary[]>([]);
const loading = ref(true);
const loadError = ref('');
const actionError = ref('');
const success = ref('');
const page = ref(1);
const totalPages = ref(0);
const nameFilter = ref('');
const statusFilter = ref<Status | ''>('');
const createdFilter = ref('');
const sortField = ref<SortField>('name');
const sortAscending = ref(true);

const columns: { field: SortField; label: string }[] = [
  { field: 'name', label: 'Name' },
  { field: 'status', label: 'Status' },
];
const dateColumns: { field: SortField; label: string }[] = [
  { field: 'creationDate', label: 'Date created' },
  { field: 'lastUpdated', label: 'Last updated' },
];

const sortParam = computed(() => (sortAscending.value ? '' : '-') + sortField.value);

async function load(): Promise<void> {
  loading.value = true;
  loadError.value = '';
  try {
    const data = await listVocabularies({
      name: nameFilter.value,
      status: statusFilter.value,
      creationDate: createdFilter.value,
      sort: sortParam.value,
      page: page.value,
      pageSize: PAGE_SIZE,
    });
    items.value = data.items;
    totalPages.value = data.totalPages;
  } catch (error) {
    items.value = [];
    totalPages.value = 0;
    loadError.value = loadErrorText(error, TEXT.listLoadError);
  } finally {
    loading.value = false;
  }
}

/** D50: a click on another column sorts it ascending; on the active column it reverses. */
function sortBy(field: SortField): void {
  if (sortField.value === field) {
    sortAscending.value = !sortAscending.value;
  } else {
    sortField.value = field;
    sortAscending.value = true;
  }
  page.value = 1;
  void load();
}

function ariaSort(field: SortField): 'ascending' | 'descending' | 'none' {
  if (sortField.value !== field) return 'none';
  return sortAscending.value ? 'ascending' : 'descending';
}

function arrow(field: SortField): string {
  if (sortField.value !== field) return '↕';
  return sortAscending.value ? '↑' : '↓';
}

let nameTimer: ReturnType<typeof setTimeout> | undefined;
watch(nameFilter, () => {
  clearTimeout(nameTimer);
  nameTimer = setTimeout(() => {
    page.value = 1;
    void load();
  }, 300);
});
watch([statusFilter, createdFilter], () => {
  page.value = 1;
  void load();
});

function goTo(newPage: number): void {
  page.value = newPage;
  void load();
}

async function changeStatus(vocabulary: Vocabulary): Promise<void> {
  const target: Status = vocabulary.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE';
  actionError.value = '';
  success.value = '';
  try {
    await patchVocabularyStatus(vocabulary.name, target);
    success.value = `Status of ${vocabulary.name} changed to ${statusLabel(target)}.`;
    await load();
  } catch (error) {
    actionError.value = statusErrorText(error);
  }
}

async function download(name: string): Promise<void> {
  actionError.value = '';
  try {
    await downloadContent(name);
  } catch (error) {
    actionError.value = submitErrorText(error);
  }
}

function actionsFor(vocabulary: Vocabulary): MenuItem[] {
  const details = `/vocabularies/${vocabulary.name}`;
  const menu: MenuItem[] = [
    { label: 'View vocabulary details', command: () => void router.push(details) },
    { label: 'History', command: () => void router.push(details) },
    { label: 'Download vocabulary', command: () => void download(vocabulary.name) },
  ];
  if (admin) {
    menu.push({
      label: 'Update',
      command: () => void router.push(`/vocabularies/${vocabulary.name}/versions/new`),
    });
    menu.push({
      label: vocabulary.status === 'ACTIVE' ? 'Deprecate' : 'Activate',
      command: () => void changeStatus(vocabulary),
    });
  }
  return menu;
}

onMounted(load);
defineExpose({ load, sortBy, changeStatus, actionsFor });
</script>

<template>
  <div>
    <div class="mb-4 flex items-center justify-between">
      <h1 class="text-2xl font-bold text-slate-900">Vocabularies</h1>
      <RouterLink
        v-if="admin"
        to="/vocabularies/new"
        class="rounded bg-blue-800 px-4 py-2 font-medium text-white hover:bg-blue-900 focus:outline-2 focus:outline-offset-2 focus:outline-blue-800"
        >New Vocabulary +</RouterLink
      >
    </div>

    <form class="mb-4 flex flex-wrap gap-4" role="search" aria-label="Filters" @submit.prevent>
      <div>
        <label for="filter-name" class="block text-sm font-medium text-slate-900"
          >Name contains</label
        >
        <input
          id="filter-name"
          v-model="nameFilter"
          type="text"
          class="rounded border border-slate-500 px-2 py-1"
        />
      </div>
      <div>
        <label for="filter-status" class="block text-sm font-medium text-slate-900"
          >Vocabulary status</label
        >
        <select
          id="filter-status"
          v-model="statusFilter"
          class="rounded border border-slate-500 px-2 py-1"
        >
          <option value="">All</option>
          <option value="ACTIVE">Active</option>
          <option value="DEPRECATED">Deprecated</option>
        </select>
      </div>
      <div>
        <label for="filter-created" class="block text-sm font-medium text-slate-900"
          >Created on</label
        >
        <input
          id="filter-created"
          v-model="createdFilter"
          type="date"
          class="rounded border border-slate-500 px-2 py-1"
        />
      </div>
    </form>

    <p v-if="success" role="status" class="mb-3 rounded bg-green-50 p-2 text-green-900">
      {{ success }}
    </p>
    <p v-if="actionError" role="alert" class="mb-3 rounded bg-red-50 p-2 text-red-900">
      {{ actionError }}
    </p>
    <p v-if="loadError" role="alert" class="mb-3 rounded bg-red-50 p-2 text-red-900">
      {{ loadError }}
    </p>

    <table v-if="!loadError" class="w-full border-collapse text-left text-sm">
      <caption class="sr-only">
        Vocabularies
      </caption>
      <thead>
        <tr class="border-b border-slate-400">
          <th
            v-for="column in columns"
            :key="column.field"
            scope="col"
            class="p-2"
            :aria-sort="ariaSort(column.field)"
          >
            <button type="button" class="font-semibold" @click="sortBy(column.field)">
              {{ column.label }} <span aria-hidden="true">{{ arrow(column.field) }}</span>
            </button>
          </th>
          <th scope="col" class="p-2">Latest version</th>
          <th scope="col" class="p-2">Description</th>
          <th scope="col" class="p-2">Creator</th>
          <th
            v-for="column in dateColumns"
            :key="column.field"
            scope="col"
            class="p-2"
            :aria-sort="ariaSort(column.field)"
          >
            <button type="button" class="font-semibold" @click="sortBy(column.field)">
              {{ column.label }} <span aria-hidden="true">{{ arrow(column.field) }}</span>
            </button>
          </th>
          <th scope="col" class="p-2">Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="vocabulary in items" :key="vocabulary.name" class="border-b border-slate-200">
          <td class="p-2">
            <RouterLink :to="`/vocabularies/${vocabulary.name}`" class="text-blue-800 underline">{{
              vocabulary.name
            }}</RouterLink>
          </td>
          <td class="p-2">
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
          <td class="p-2">{{ vocabulary.version }}</td>
          <td class="p-2">{{ vocabulary.description }}</td>
          <td class="p-2">{{ vocabulary.createdBy }}</td>
          <td class="p-2">{{ formatDate(vocabulary.creationDate) }}</td>
          <td class="p-2">{{ formatDate(vocabulary.lastUpdateDate) }}</td>
          <td class="p-2">
            <ActionsMenu
              :label="`Actions for ${vocabulary.name}`"
              :items="actionsFor(vocabulary)"
            />
          </td>
        </tr>
      </tbody>
    </table>
    <p v-if="!loading && !loadError && items.length === 0" class="mt-3 text-slate-800">
      {{ TEXT.noVocabularies }}
    </p>

    <nav v-if="!loadError" aria-label="Pagination" class="mt-4 flex items-center gap-4">
      <button
        type="button"
        class="rounded border border-slate-500 px-3 py-1 disabled:text-slate-500"
        :disabled="page <= 1"
        @click="goTo(page - 1)"
      >
        Previous page
      </button>
      <span>Page {{ page }} of {{ Math.max(totalPages, 1) }}</span>
      <button
        type="button"
        class="rounded border border-slate-500 px-3 py-1 disabled:text-slate-500"
        :disabled="page >= Math.max(totalPages, 1)"
        @click="goTo(page + 1)"
      >
        Next page
      </button>
    </nav>
  </div>
</template>

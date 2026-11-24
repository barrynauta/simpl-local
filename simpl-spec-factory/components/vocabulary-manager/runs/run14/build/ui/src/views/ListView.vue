<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import ActionsMenu, { type MenuItem } from '../components/ActionsMenu.vue';
import ConfirmDialog from '../components/ConfirmDialog.vue';
import PageControls from '../components/PageControls.vue';
import { ApiError, api, saveBlob, type Vocabulary } from '../lib/api';
import { isAdmin } from '../lib/auth';
import { TEXT, formatDate, problemDetail, statusLabel } from '../lib/texts';

type SortField = 'name' | 'status' | 'creationDate' | 'lastUpdated';

const router = useRouter();
const items = ref<Vocabulary[]>([]);
const page = ref(1);
const totalPages = ref(0);
const total = ref(0);
const loading = ref(true);
const loadError = ref('');
const actionError = ref('');
const success = ref('');
const nameFilter = ref('');
const statusFilter = ref('');
const createdFilter = ref('');
const sortField = ref<SortField>('name');
const sortAscending = ref(true);
const pending = ref<Vocabulary | null>(null);
let timer: ReturnType<typeof setTimeout> | undefined;

const admin = computed(() => isAdmin());
const columns: { key: SortField | null; label: string }[] = [
  { key: 'name', label: 'Name' },
  { key: 'status', label: 'Status' },
  { key: null, label: 'Latest version' },
  { key: null, label: 'Description' },
  { key: null, label: 'Creator' },
  { key: 'creationDate', label: 'Date created' },
  { key: 'lastUpdated', label: 'Last updated' },
  { key: null, label: 'Actions' },
];

async function load(): Promise<void> {
  loading.value = true;
  loadError.value = '';
  try {
    const result = await api.listVocabularies({
      name: nameFilter.value.trim() || undefined,
      status: statusFilter.value || undefined,
      creationDate: createdFilter.value || undefined,
      sort: (sortAscending.value ? '' : '-') + sortField.value,
      page: page.value,
      pageSize: 20,
    });
    items.value = result.items;
    totalPages.value = result.totalPages;
    total.value = result.total;
  } catch (error) {
    items.value = [];
    loadError.value = `${TEXT.listLoadError} ${problemDetail(error)}`.trim();
  } finally {
    loading.value = false;
  }
}

function filterChanged(): void {
  page.value = 1;
  void load();
}

function nameTyped(): void {
  clearTimeout(timer);
  timer = setTimeout(filterChanged, 300);
}

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

function ariaSort(field: SortField | null): 'ascending' | 'descending' | 'none' | undefined {
  if (field === null) {
    return undefined;
  }
  if (sortField.value !== field) {
    return 'none';
  }
  return sortAscending.value ? 'ascending' : 'descending';
}

function go(target: number): void {
  page.value = target;
  void load();
}

async function download(vocabulary: Vocabulary): Promise<void> {
  actionError.value = '';
  try {
    saveBlob(await api.content(vocabulary.name), `${vocabulary.name}.ttl`);
  } catch (error) {
    actionError.value = error instanceof ApiError && error.status === 403 ? TEXT.notAuthorized : TEXT.generic;
  }
}

function menu(vocabulary: Vocabulary): MenuItem[] {
  const entries: MenuItem[] = [
    { label: 'View vocabulary details', action: () => router.push(`/vocabularies/${vocabulary.name}`) },
    { label: 'History', action: () => router.push(`/vocabularies/${vocabulary.name}`) },
    { label: 'Download vocabulary', action: () => void download(vocabulary) },
  ];
  if (admin.value) {
    entries.push({
      label: 'Update',
      action: () => router.push(`/vocabularies/${vocabulary.name}/versions/new`),
    });
    entries.push({
      label: vocabulary.status === 'ACTIVE' ? 'Deprecate' : 'Activate',
      action: () => (pending.value = vocabulary),
    });
  }
  return entries;
}

const dialogText = computed(() => {
  const target = pending.value;
  if (!target) {
    return '';
  }
  return target.status === 'ACTIVE'
    ? `Do you want to deprecate '${target.name}'?`
    : `Do you want to activate '${target.name}'?`;
});

async function confirmStatus(): Promise<void> {
  const target = pending.value;
  pending.value = null;
  if (!target) {
    return;
  }
  success.value = '';
  actionError.value = '';
  const next = target.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE';
  try {
    await api.changeStatus(target.name, next);
    success.value = `Status of ${target.name} changed to ${statusLabel(next)}.`;
    await load();
  } catch (error) {
    actionError.value = statusErrorText(error);
  }
}

function statusErrorText(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return TEXT.statusNotFound;
    }
    if (error.status === 403) {
      return TEXT.notAuthorized;
    }
    if (error.status >= 500) {
      return `${TEXT.statusFailed} ${TEXT.generic} ${error.problem.detail ?? ''}`.trim();
    }
    return error.problem.detail ?? TEXT.statusFailed;
  }
  return `${TEXT.statusFailed} ${TEXT.generic}`;
}

onMounted(load);
</script>

<template>
  <div>
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-semibold">Vocabularies</h1>
      <RouterLink v-if="admin" to="/vocabularies/new" class="btn no-underline">New Vocabulary +</RouterLink>
    </div>

    <p v-if="success" role="status" class="alert-success">{{ success }}</p>
    <p v-if="actionError" role="alert" class="alert-error">{{ actionError }}</p>

    <form
      class="mt-4 flex flex-wrap items-end gap-4"
      role="search"
      aria-label="Filter vocabularies"
      @submit.prevent
    >
      <div>
        <label for="filter-name" class="block text-sm font-medium">Name contains</label>
        <input id="filter-name" v-model="nameFilter" type="text" class="field-input" @input="nameTyped" />
      </div>
      <div>
        <label for="filter-status" class="block text-sm font-medium">Vocabulary status</label>
        <select id="filter-status" v-model="statusFilter" class="field-input" @change="filterChanged">
          <option value="">All</option>
          <option value="ACTIVE">Active</option>
          <option value="DEPRECATED">Deprecated</option>
        </select>
      </div>
      <div>
        <label for="filter-created" class="block text-sm font-medium">Created on</label>
        <input
          id="filter-created"
          v-model="createdFilter"
          type="date"
          class="field-input"
          @change="filterChanged"
        />
      </div>
    </form>

    <p v-if="loadError" role="alert" class="alert-error">{{ loadError }}</p>
    <p v-else-if="!loading && total === 0" class="mt-4">{{ TEXT.noVocabularies }}</p>

    <table v-if="items.length > 0" class="data mt-4">
      <caption class="sr-only">
        Vocabularies
      </caption>
      <thead>
        <tr>
          <th v-for="column in columns" :key="column.label" scope="col" :aria-sort="ariaSort(column.key)">
            <button
              v-if="column.key"
              type="button"
              class="font-semibold text-gray-900 underline"
              @click="sortBy(column.key)"
            >
              {{ column.label }}
              <span aria-hidden="true">{{
                sortField === column.key ? (sortAscending ? '▲' : '▼') : '↕'
              }}</span>
            </button>
            <template v-else>{{ column.label }}</template>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="vocabulary in items" :key="vocabulary.name">
          <td>
            <RouterLink :to="`/vocabularies/${vocabulary.name}`">{{ vocabulary.name }}</RouterLink>
          </td>
          <td>
            {{ statusLabel(vocabulary.status) }}
            <span
              v-if="vocabulary.status === 'DEPRECATED'"
              role="img"
              :aria-label="TEXT.vocabularyDeprecated"
              :title="TEXT.vocabularyDeprecated"
              class="ml-1 text-amber-800"
              >⚠</span
            >
          </td>
          <td>{{ vocabulary.version }}</td>
          <td>{{ vocabulary.description }}</td>
          <td>{{ vocabulary.createdBy }}</td>
          <td>{{ formatDate(vocabulary.creationDate) }}</td>
          <td>{{ formatDate(vocabulary.lastUpdateDate) }}</td>
          <td>
            <ActionsMenu :label="`Actions for ${vocabulary.name}`" :items="menu(vocabulary)" />
          </td>
        </tr>
      </tbody>
    </table>

    <PageControls v-if="total > 0" :page="page" :total-pages="totalPages" @go="go" />

    <ConfirmDialog
      :visible="pending !== null"
      title="Update vocabulary status"
      :text="dialogText"
      @confirm="confirmStatus"
      @cancel="pending = null"
    />
  </div>
</template>

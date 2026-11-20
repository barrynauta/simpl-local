<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import ActionsMenu from '../components/ActionsMenu.vue';
import type { MenuAction } from '../components/menu';
import { ApiError, changeStatus, download, listVocabularies, type Status, type Vocabulary } from '../api';
import { isAdmin } from '../auth';
import { TEXT, formatDate, statusLabel } from '../texts';

/** Vocabulary list (D23, D34, D36, D50, D60, D72, D100, D114, D115). */
const PAGE_SIZE = 20;
const SORTABLE: Record<string, string> = {
  Name: 'name',
  Status: 'status',
  'Date created': 'creationDate',
  'Last updated': 'lastUpdated',
};
const COLUMNS = ['Name', 'Status', 'Description', 'Creator', 'Date created', 'Last updated', 'Actions'];

const router = useRouter();
const admin = isAdmin();
const filters = reactive({ name: '', status: '' as Status | '', creationDate: '' });
const sortField = ref('name');
const descending = ref(false);
const page = ref(1);
const totalPages = ref(0);
const items = ref<Vocabulary[]>([]);
const loading = ref(true);
const loadError = ref('');
const message = ref('');
const actionError = ref('');

async function load(): Promise<void> {
  loading.value = true;
  loadError.value = '';
  try {
    const result = await listVocabularies({
      name: filters.name.trim() || undefined,
      status: filters.status,
      creationDate: filters.creationDate || undefined,
      sort: (descending.value ? '-' : '') + sortField.value,
      page: page.value,
      pageSize: PAGE_SIZE,
    });
    items.value = result.items;
    totalPages.value = result.totalPages;
  } catch (error) {
    items.value = [];
    const detail = error instanceof ApiError ? (error.problem.detail ?? error.message) : String(error);
    loadError.value = `${TEXT.listFailed} ${detail}`;
  } finally {
    loading.value = false;
  }
}

function applyFilters(): void {
  page.value = 1;
  void load();
}

function sortBy(column: string): void {
  const field = SORTABLE[column];
  if (sortField.value === field) {
    descending.value = !descending.value;
  } else {
    sortField.value = field;
    descending.value = false;
  }
  page.value = 1;
  void load();
}

function ariaSort(column: string): 'ascending' | 'descending' | 'none' | undefined {
  const field = SORTABLE[column];
  if (!field) return undefined;
  if (field !== sortField.value) return 'none';
  return descending.value ? 'descending' : 'ascending';
}

function go(delta: number): void {
  page.value += delta;
  void load();
}

const pageText = computed(() => `Page ${totalPages.value === 0 ? 0 : page.value} of ${totalPages.value}`);

function actionsFor(vocabulary: Vocabulary): MenuAction[] {
  const actions: MenuAction[] = [
    { key: 'details', label: 'View vocabulary details' },
    { key: 'history', label: 'History' },
    { key: 'download', label: 'Download vocabulary' },
  ];
  if (admin) {
    actions.push({ key: 'update', label: 'Update' });
    actions.push(
      vocabulary.status === 'ACTIVE'
        ? { key: 'deprecate', label: 'Deprecate' }
        : { key: 'activate', label: 'Activate' },
    );
  }
  return actions;
}

async function onAction(vocabulary: Vocabulary, key: string): Promise<void> {
  message.value = '';
  actionError.value = '';
  if (key === 'details' || key === 'history') {
    await router.push(`/vocabularies/${vocabulary.name}`);
  } else if (key === 'update') {
    await router.push(`/vocabularies/${vocabulary.name}/versions/new`);
  } else if (key === 'download') {
    try {
      await download(vocabulary.name);
    } catch (error) {
      actionError.value = error instanceof ApiError ? describe(error) : String(error);
    }
  } else {
    await setStatus(vocabulary, key === 'deprecate' ? 'DEPRECATED' : 'ACTIVE');
  }
}

async function setStatus(vocabulary: Vocabulary, status: Status): Promise<void> {
  try {
    const updated = await changeStatus(vocabulary.name, status);
    const index = items.value.findIndex((item) => item.name === vocabulary.name);
    if (index >= 0) items.value[index] = updated;
    message.value = `Status of ${vocabulary.name} changed to ${statusLabel(updated.status)}.`;
  } catch (error) {
    actionError.value =
      error instanceof ApiError && error.status === 404
        ? TEXT.statusNotFound
        : error instanceof ApiError && error.status === 403
          ? TEXT.notAuthorized
          : `${TEXT.statusFailed} ${TEXT.generic} ${error instanceof ApiError ? (error.problem.detail ?? '') : ''}`.trim();
  }
}

function describe(error: ApiError): string {
  if (error.status === 403) return TEXT.notAuthorized;
  if (error.status === 404) return TEXT.vocabularyNotFound;
  return `${TEXT.generic} ${error.problem.detail ?? ''}`.trim();
}

onMounted(load);
</script>

<template>
  <section aria-labelledby="list-title">
    <div class="title-row">
      <h1 id="list-title">Vocabularies</h1>
      <RouterLink v-if="admin" class="button" to="/vocabularies/new">New Vocabulary +</RouterLink>
    </div>

    <form class="filters" @submit.prevent="applyFilters">
      <div class="field">
        <label for="filter-name">Name contains</label>
        <input id="filter-name" v-model="filters.name" type="search" maxlength="64" />
      </div>
      <div class="field">
        <label for="filter-status">Vocabulary status</label>
        <select id="filter-status" v-model="filters.status">
          <option value="">All</option>
          <option value="ACTIVE">Active</option>
          <option value="DEPRECATED">Deprecated</option>
        </select>
      </div>
      <div class="field">
        <label for="filter-created">Created on</label>
        <input id="filter-created" v-model="filters.creationDate" type="date" />
      </div>
      <button type="submit">Apply filters</button>
    </form>

    <p v-if="message" role="status" class="success">{{ message }}</p>
    <p v-if="actionError" role="alert" class="error">{{ actionError }}</p>
    <p v-if="loadError" role="alert" class="error">{{ loadError }}</p>

    <table v-if="!loadError">
      <thead>
        <tr>
          <th v-for="column in COLUMNS" :key="column" scope="col" :aria-sort="ariaSort(column)">
            <button v-if="SORTABLE[column]" type="button" class="sort" @click="sortBy(column)">
              {{ column }}
              <span aria-hidden="true">{{
                ariaSort(column) === 'ascending' ? '▲' : ariaSort(column) === 'descending' ? '▼' : ''
              }}</span>
            </button>
            <template v-else>{{ column }}</template>
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
              class="warning-icon"
              role="img"
              :aria-label="TEXT.deprecatedVocabulary"
              :title="TEXT.deprecatedVocabulary"
              >&#9888;</span
            >
          </td>
          <td>{{ vocabulary.description }}</td>
          <td>{{ vocabulary.createdBy }}</td>
          <td>{{ formatDate(vocabulary.creationDate) }}</td>
          <td>{{ formatDate(vocabulary.lastUpdateDate) }}</td>
          <td>
            <ActionsMenu
              :label="`Actions for ${vocabulary.name}`"
              :actions="actionsFor(vocabulary)"
              @select="(key) => onAction(vocabulary, key)"
            />
          </td>
        </tr>
      </tbody>
    </table>
    <p v-if="!loading && !loadError && items.length === 0">{{ TEXT.noVocabularies }}</p>

    <nav class="pager" aria-label="Pages">
      <button type="button" :disabled="page <= 1 || loading" @click="go(-1)">Previous page</button>
      <span>{{ pageText }}</span>
      <button type="button" :disabled="page >= totalPages || loading" @click="go(1)">Next page</button>
    </nav>
  </section>
</template>

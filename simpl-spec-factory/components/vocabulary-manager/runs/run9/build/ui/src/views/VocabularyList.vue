<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { api, ApiError, saveFile, type Vocabulary } from '../api';
import { session } from '../auth';
import ActionsMenu, { type MenuAction } from '../components/ActionsMenu.vue';
import { formatDate, statusLabel, TEXT } from '../texts';

/** Vocabulary list (D23, D34, D36, D50, D60, D72). */
type SortKey = 'name' | 'status' | 'creationDate' | 'lastUpdated';

const router = useRouter();
const admin = session().isAdmin();
const PAGE_SIZE = 20;

const filters = reactive({ name: '', status: '', creationDate: '' });
const sort = reactive<{ key: SortKey; descending: boolean }>({ key: 'name', descending: false });
const page = ref(1);
const items = ref<Vocabulary[]>([]);
const total = ref(0);
const totalPages = ref(0);
const loading = ref(false);
const loadError = ref('');
const success = ref('');
const actionError = ref<string[]>([]);

const columns: { key: SortKey; label: string }[] = [
  { key: 'name', label: 'Name' },
  { key: 'status', label: 'Status' },
  { key: 'creationDate', label: 'Date created' },
  { key: 'lastUpdated', label: 'Last updated' },
];

const sortParam = computed(() => (sort.descending ? '-' : '') + sort.key);

async function load(): Promise<void> {
  loading.value = true;
  loadError.value = '';
  try {
    const result = await api.listVocabularies({
      name: filters.name.trim(),
      status: filters.status,
      creationDate: filters.creationDate,
      sort: sortParam.value,
      page: page.value,
      pageSize: PAGE_SIZE,
    });
    items.value = result.items;
    total.value = result.total;
    totalPages.value = result.totalPages;
  } catch (error) {
    items.value = [];
    loadError.value = error instanceof ApiError ? (error.problem.detail ?? '') : String(error);
  } finally {
    loading.value = false;
  }
}

function applyFilters(): void {
  page.value = 1;
  void load();
}

/** D50: another column sorts ascending; the active column reverses. */
function sortBy(key: SortKey): void {
  if (sort.key === key) {
    sort.descending = !sort.descending;
  } else {
    sort.key = key;
    sort.descending = false;
  }
  page.value = 1;
  void load();
}

function ariaSort(key: SortKey): 'ascending' | 'descending' | 'none' {
  if (sort.key !== key) {
    return 'none';
  }
  return sort.descending ? 'descending' : 'ascending';
}

function goTo(target: number): void {
  page.value = target;
  void load();
}

async function download(vocabulary: Vocabulary): Promise<void> {
  try {
    saveFile(await api.content(vocabulary.name), `${vocabulary.name}.ttl`);
  } catch (error) {
    actionError.value = [TEXT.genericError, error instanceof ApiError ? (error.problem.detail ?? '') : ''];
  }
}

async function changeStatus(vocabulary: Vocabulary): Promise<void> {
  success.value = '';
  actionError.value = [];
  const next = vocabulary.status === 'ACTIVE' ? 'DEPRECATED' : 'ACTIVE';
  try {
    await api.changeStatus(vocabulary.name, next);
    success.value = `Status of ${vocabulary.name} changed to ${statusLabel(next)}.`;
    await load();
  } catch (error) {
    const problem = error instanceof ApiError ? error.problem : { status: 500, detail: String(error) };
    if (problem.status === 404) {
      actionError.value = [TEXT.statusNotFound];
    } else if (problem.status === 403) {
      actionError.value = [TEXT.notAuthorized];
    } else {
      actionError.value = [TEXT.statusError, TEXT.genericError, problem.detail ?? ''];
    }
  }
}

function actions(vocabulary: Vocabulary): MenuAction[] {
  const list: MenuAction[] = [
    { label: 'View vocabulary details', run: () => void router.push(`/vocabularies/${vocabulary.name}`) },
    { label: 'History', run: () => void router.push(`/vocabularies/${vocabulary.name}#history`) },
    { label: 'Download vocabulary', run: () => void download(vocabulary) },
  ];
  if (admin) {
    list.push({
      label: 'Update',
      run: () => void router.push(`/vocabularies/${vocabulary.name}/versions/new`),
    });
    list.push({
      label: vocabulary.status === 'ACTIVE' ? 'Deprecate' : 'Activate',
      run: () => void changeStatus(vocabulary),
    });
  }
  return list;
}

onMounted(load);
defineExpose({ load, sortBy, sort, page });
</script>

<template>
  <h1>Vocabularies</h1>
  <div class="actions">
    <button v-if="admin" type="button" @click="router.push('/vocabularies/new')">
      {{ TEXT.newVocabulary }}
    </button>
  </div>
  <form class="filters" role="search" aria-label="Filter vocabularies" @submit.prevent="applyFilters">
    <div class="field">
      <label for="filter-name">Name contains</label>
      <input id="filter-name" v-model="filters.name" type="text" maxlength="64" />
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
    <div class="field">
      <button type="submit">Apply filters</button>
    </div>
  </form>

  <p v-if="success" role="status" class="panel success">{{ success }}</p>
  <div v-if="actionError.length" role="alert" class="panel error">
    <p v-for="(line, index) in actionError.filter((l) => l)" :key="index">{{ line }}</p>
  </div>

  <div v-if="loadError" role="alert" class="panel error">
    <p>{{ TEXT.listError }}</p>
    <p>{{ loadError }}</p>
  </div>
  <p v-else-if="!loading && items.length === 0">{{ TEXT.noVocabularies }}</p>
  <table v-else aria-label="Vocabularies">
    <thead>
      <tr>
        <th
          v-for="column in columns.slice(0, 2)"
          :key="column.key"
          scope="col"
          :aria-sort="ariaSort(column.key)"
        >
          <button type="button" @click="sortBy(column.key)">{{ column.label }}</button>
        </th>
        <th scope="col">Latest version</th>
        <th scope="col">Description</th>
        <th
          v-for="column in columns.slice(2)"
          :key="column.key"
          scope="col"
          :aria-sort="ariaSort(column.key)"
        >
          <button type="button" @click="sortBy(column.key)">{{ column.label }}</button>
        </th>
        <th scope="col">Actions</th>
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
        <td>{{ vocabulary.version }}</td>
        <td>{{ vocabulary.description }}</td>
        <td>{{ formatDate(vocabulary.creationDate) }}</td>
        <td>{{ formatDate(vocabulary.lastUpdateDate) }}</td>
        <td><ActionsMenu :label="`Actions for ${vocabulary.name}`" :actions="actions(vocabulary)" /></td>
      </tr>
    </tbody>
  </table>
  <nav v-if="totalPages > 1" class="pager" aria-label="Pages">
    <button type="button" class="secondary" :disabled="page <= 1" @click="goTo(page - 1)">
      Previous page
    </button>
    <span>Page {{ page }} of {{ totalPages }} ({{ total }} vocabularies)</span>
    <button type="button" class="secondary" :disabled="page >= totalPages" @click="goTo(page + 1)">
      Next page
    </button>
  </nav>
</template>

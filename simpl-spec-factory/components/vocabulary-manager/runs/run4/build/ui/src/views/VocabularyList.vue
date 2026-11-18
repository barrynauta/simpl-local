<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import ActionsMenu from '../components/ActionsMenu.vue';
import { download, listVocabularies } from '../api';
import { isAdmin } from '../auth';
import { describeError } from '../errors';
import { notifyError } from '../notifications';
import { statusLabel, VOCABULARY_DEPRECATED } from '../labels';
import { changeStatus, statusActionLabel } from '../statusChange';
import type { MenuAction, Vocabulary, VocabularyStatus } from '../types';

type SortField = 'name' | 'status' | 'creationDate' | 'lastUpdated';
const PAGE_SIZE = 20;

const router = useRouter();
const admin = isAdmin();
const items = ref<Vocabulary[]>([]);
const total = ref(0);
const totalPages = ref(0);
const page = ref(1);
const loading = ref(false);
const loadError = ref<{ message: string; detail?: string } | undefined>();
const sort = reactive<{ field: SortField; descending: boolean }>({ field: 'name', descending: false });
const filters = reactive<{
  name: string;
  status: VocabularyStatus | '';
  creationDate: string;
  lastUpdated: string;
}>({
  name: '',
  status: '',
  creationDate: '',
  lastUpdated: '',
});
let nameTimer: ReturnType<typeof setTimeout> | undefined;

const sortParameter = computed(() => (sort.descending ? '-' : '') + sort.field);

async function load(): Promise<void> {
  loading.value = true;
  loadError.value = undefined;
  try {
    const result = await listVocabularies({
      name: filters.name,
      status: filters.status,
      creationDate: filters.creationDate,
      lastUpdated: filters.lastUpdated,
      sort: sortParameter.value,
      page: page.value,
      pageSize: PAGE_SIZE,
    });
    items.value = result.items;
    total.value = result.total;
    totalPages.value = result.totalPages;
  } catch (error) {
    items.value = [];
    const shown = describeError(error, 'The vocabulary list could not be loaded.');
    loadError.value =
      shown.message === 'The vocabulary list could not be loaded.'
        ? shown
        : { message: 'The vocabulary list could not be loaded.', detail: shown.detail ?? shown.message };
  } finally {
    loading.value = false;
  }
}

function applyFilters(): void {
  page.value = 1;
  void load();
}

function onNameInput(): void {
  if (nameTimer) clearTimeout(nameTimer);
  nameTimer = setTimeout(applyFilters, 300);
}

/** First click ascending, second click descending (decision D23). */
function sortBy(field: SortField): void {
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

function sortMark(field: SortField): string {
  if (sort.field !== field) return '';
  return sort.descending ? ' ▼' : ' ▲';
}

function goTo(target: number): void {
  page.value = target;
  void load();
}

async function downloadLatest(vocabulary: Vocabulary): Promise<void> {
  try {
    await download(vocabulary.name);
  } catch (error) {
    const shown = describeError(error, 'The download failed.');
    notifyError(shown.message, shown.detail);
  }
}

function actionsFor(vocabulary: Vocabulary): MenuAction[] {
  const details = () => router.push(`/vocabularies/${vocabulary.name}`);
  const actions: MenuAction[] = [
    { label: 'View vocabulary details', run: details },
    { label: 'History', run: details },
    { label: 'Download vocabulary', run: () => void downloadLatest(vocabulary) },
  ];
  if (admin) {
    actions.push({ label: 'Update', run: () => router.push(`/vocabularies/${vocabulary.name}/update`) });
    actions.push({
      label: statusActionLabel(vocabulary),
      run: async () => {
        if (await changeStatus(vocabulary)) await load();
      },
    });
  }
  return actions;
}

onMounted(load);
</script>

<template>
  <section class="vocabulary-list">
    <div class="title-row">
      <h1>Vocabularies</h1>
      <router-link v-if="admin" to="/vocabularies/new" class="button primary">New Vocabulary +</router-link>
    </div>

    <form class="filters" aria-label="Filters" @submit.prevent="applyFilters">
      <div class="field">
        <label for="filter-name">Name</label>
        <input
          id="filter-name"
          v-model="filters.name"
          type="search"
          placeholder="Name contains"
          @input="onNameInput"
        />
      </div>
      <div class="field">
        <label for="filter-status">Status</label>
        <select id="filter-status" v-model="filters.status" @change="applyFilters">
          <option value="">All</option>
          <option value="ACTIVE">Active</option>
          <option value="DEPRECATED">Deprecated</option>
        </select>
      </div>
      <div class="field">
        <label for="filter-created">Date created</label>
        <input id="filter-created" v-model="filters.creationDate" type="date" @change="applyFilters" />
      </div>
      <div class="field">
        <label for="filter-updated">Last updated</label>
        <input id="filter-updated" v-model="filters.lastUpdated" type="date" @change="applyFilters" />
      </div>
    </form>

    <div v-if="loadError" class="error-panel" role="alert">
      <p>{{ loadError.message }}</p>
      <p v-if="loadError.detail" class="detail">{{ loadError.detail }}</p>
    </div>

    <p v-else-if="!loading && items.length === 0" class="empty">No vocabularies found.</p>

    <table v-if="items.length > 0" class="data-table">
      <thead>
        <tr>
          <th scope="col">Actions</th>
          <th scope="col" :aria-sort="ariaSort('name')">
            <button type="button" class="sort" @click="sortBy('name')">Name{{ sortMark('name') }}</button>
          </th>
          <th scope="col">Description</th>
          <th scope="col">Latest version</th>
          <th scope="col" :aria-sort="ariaSort('status')">
            <button type="button" class="sort" @click="sortBy('status')">
              Status{{ sortMark('status') }}
            </button>
          </th>
          <th scope="col" :aria-sort="ariaSort('creationDate')">
            <button type="button" class="sort" @click="sortBy('creationDate')">
              Date created{{ sortMark('creationDate') }}
            </button>
          </th>
          <th scope="col" :aria-sort="ariaSort('lastUpdated')">
            <button type="button" class="sort" @click="sortBy('lastUpdated')">
              Last updated{{ sortMark('lastUpdated') }}
            </button>
          </th>
          <th scope="col">Creator</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="v in items" :key="v.name" :data-name="v.name">
          <td><ActionsMenu :actions="actionsFor(v)" /></td>
          <td>
            <router-link :to="`/vocabularies/${v.name}`">{{ v.name }}</router-link>
          </td>
          <td>{{ v.description }}</td>
          <td>{{ v.version }}</td>
          <td>
            <span class="status" :class="v.status.toLowerCase()">{{ statusLabel(v.status) }}</span>
            <span v-if="v.status === 'DEPRECATED'" class="deprecation-warning" :title="VOCABULARY_DEPRECATED">
              <span aria-hidden="true">&#9888;</span> {{ VOCABULARY_DEPRECATED }}
            </span>
          </td>
          <td>{{ v.creationDate }}</td>
          <td>{{ v.lastUpdateDate }}</td>
          <td>{{ v.createdBy }}</td>
        </tr>
      </tbody>
    </table>

    <nav v-if="totalPages > 1" class="pager" aria-label="Pages">
      <button type="button" :disabled="page <= 1" @click="goTo(page - 1)">Previous</button>
      <span>Page {{ page }} of {{ totalPages }} ({{ total }} vocabularies)</span>
      <button type="button" :disabled="page >= totalPages" @click="goTo(page + 1)">Next</button>
    </nav>
  </section>
</template>
